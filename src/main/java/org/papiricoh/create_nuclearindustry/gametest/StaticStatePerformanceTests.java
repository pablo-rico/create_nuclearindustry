package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicNetwork;
import org.papiricoh.create_nuclearindustry.explosive.BlastJobs;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.NuclearExplosionSoundManager;
import org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombMenu;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadMenu;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRegistry;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryProbe;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockChangeHandler;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelPoolSurvey;

/**
 * State kept outside the world that used to leak or be shared by every player: the bomb and launch
 * pad screens find their block from the position sent with them, the containment and seismic
 * registries and the crater and sound queues let go of their level, the repository probe cache is
 * emptied when the server stops, and a missile saves the chunk it forces so it can let it go later.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class StaticStatePerformanceTests {

    /** Two players opening two bombs (and two pads) each get the block they opened. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void eachPlayersMenuFindsItsOwnBlock(GameTestHelper helper) {
        BlockPos bombA = new BlockPos(3, 2, 3);
        BlockPos bombB = new BlockPos(9, 2, 9);
        BlockPos padA = new BlockPos(3, 2, 9);
        BlockPos padB = new BlockPos(9, 2, 3);
        helper.setBlock(bombA, AllNuclearBlocks.NUCLEAR_BOMB.get());
        helper.setBlock(bombB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        helper.setBlock(padA, MissileRegistry.LAUNCH_PAD.get());
        helper.setBlock(padB, MissileRegistry.LAUNCH_PAD.get());
        helper.runAfterDelay(1, () -> {
            Player first = helper.makeMockPlayer(GameType.SURVIVAL);
            Player second = helper.makeMockPlayer(GameType.SURVIVAL);
            // Opened one after the other, as the menus of two players would be; the first is checked last.
            NuclearBombMenu firstBomb = AllNuclearGUIs.NUCLEAR_BOMB_MENU.get().create(1, first.getInventory(), at(helper, bombA));
            NuclearBombMenu secondBomb = AllNuclearGUIs.NUCLEAR_BOMB_MENU.get().create(2, second.getInventory(), at(helper, bombB));
            LaunchPadMenu firstPad = MissileRegistry.LAUNCH_PAD_MENU.get().create(3, first.getInventory(), at(helper, padA));
            LaunchPadMenu secondPad = MissileRegistry.LAUNCH_PAD_MENU.get().create(4, second.getInventory(), at(helper, padB));
            helper.assertTrue(helper.absolutePos(bombA).equals(firstBomb.getBombPos()) && firstBomb.getBomb() != null
                            && firstBomb.getBomb().getBlockPos().equals(helper.absolutePos(bombA)),
                    "the first player's menu shows the first bomb, got " + firstBomb.getBombPos());
            helper.assertTrue(helper.absolutePos(bombB).equals(secondBomb.getBombPos()) && secondBomb.getBomb() != null,
                    "the second player's menu shows the second bomb, got " + secondBomb.getBombPos());
            helper.assertTrue(firstPad.slots.size() > 36 && secondPad.slots.size() > 36,
                    "both pad menus find their pad and show its slots");
            helper.succeed();
        });
    }

    private static RegistryFriendlyByteBuf at(GameTestHelper helper, BlockPos relative) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        buf.writeBlockPos(helper.absolutePos(relative));
        return buf;
    }

    /** Containment monitors and seismic detectors are forgotten with their level, so it can be collected. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_static_registries")
    public static void registriesLetGoOfAnUnloadedLevel(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), PwrRegistry.CONTAINMENT_MONITOR.get());
        helper.setBlock(new BlockPos(8, 2, 8), CivilDefenseRegistry.SEISMIC_DETECTOR.get());
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(ContainmentRegistry.tracks(level) && SeismicNetwork.tracks(level), "both registered in the level");
            LevelEvent.Unload unload = new LevelEvent.Unload(level);
            ContainmentRegistry.onLevelUnload(unload);
            SeismicNetwork.onLevelUnload(unload);
            helper.assertTrue(!ContainmentRegistry.tracks(level) && !SeismicNetwork.tracks(level),
                    "an unloaded level is no longer held by either registry");
            helper.setBlock(new BlockPos(4, 2, 4), Blocks.AIR);
            helper.setBlock(new BlockPos(8, 2, 8), Blocks.AIR);
            helper.succeed();
        });
    }

    /**
     * Sounds still on their way and probed repository sites go when the server stops. Craters still to
     * dig are saved with their level instead, to carry on after a restart: nothing static holds them.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_static_stop")
    public static void queuesAndCachesAreClearedWhenTheServerStops(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            int before = NuclearBlastManager.pending(level);
            // Queued and dropped in the same tick: the crater never digs a block.
            NuclearBlastManager.start(level, helper.absolutePos(new BlockPos(6, 2, 6)), DetonationProfile.craterOnly(2, 2, 1.0f));
            helper.assertTrue(NuclearBlastManager.pending(level) == before + 1, "the crater is queued");
            BlastJobs saved = BlastJobs.getIfPresent(level);
            helper.assertTrue(saved != null && saved.isDirty(), "in the level's own saved data, to be written with it");
            RepositoryProbe.site(level, helper.absolutePos(new BlockPos(6, 2, 6)));
            helper.assertTrue(RepositoryProbe.cachedSites() >= 1, "the probed site is cached");
            ServerStoppingEvent stopping = new ServerStoppingEvent(level.getServer());
            NuclearExplosionSoundManager.onServerStopping(stopping);
            RepositoryProbe.onServerStopping(stopping);
            helper.assertTrue(NuclearBlastManager.pending(level) == before + 1, "the crater stays with its level for the next start");
            NuclearBlastManager.cancelAll(level);
            helper.assertTrue(NuclearExplosionSoundManager.pending(level) == 0, "no sound outlives the server");
            helper.assertTrue(RepositoryProbe.cachedSites() == 0, "nor a cached site");
            helper.succeed();
        });
    }

    /**
     * The multiblock index, the radiation emitter index and caches and the spent fuel pool surveys
     * are forgotten with their level too: their entries hold block entities, which hold the level.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_static_indexes")
    public static void indexesLetGoOfAnUnloadedLevel(GameTestHelper helper) {
        BlockPos controller = new BlockPos(4, 2, 4);
        BlockPos water = new BlockPos(8, 2, 8);
        helper.setBlock(controller, AllNuclearBlocks.REACTOR_CONTROLLER.get());
        helper.setBlock(water, Blocks.WATER);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            RadiationEmitter emitter = PerformanceBenchmarks.emitter(level, helper.absoluteVec(new Vec3(2.5, 3.0, 6.5)));
            RadiationEmitters.register(emitter);
            RadiationHandler.cachedAmbientRate(level, helper.absoluteVec(new Vec3(6.5, 3.0, 6.5)));
            SpentFuelPoolSurvey.of(level, helper.absolutePos(water));
            helper.assertTrue(MultiblockRegistry.tracks(level) && RadiationEmitters.count(level) >= 1 && RadiationCaches.tracks(level)
                    && SpentFuelPoolSurvey.tracks(level), "all four hold something of the level");
            LevelEvent.Unload unload = new LevelEvent.Unload(level);
            MultiblockChangeHandler.onLevelUnload(unload);
            RadiationCaches.onLevelUnload(unload);
            SpentFuelPoolSurvey.onLevelUnload(unload);
            helper.assertTrue(!MultiblockRegistry.tracks(level), "the multiblock index let the level go");
            helper.assertTrue(RadiationEmitters.count(level) == 0 && !RadiationCaches.tracks(level), "so did the radiation index and caches");
            helper.assertTrue(!SpentFuelPoolSurvey.tracks(level), "and the pool surveys");
            helper.setBlock(controller, Blocks.AIR);
            helper.setBlock(water, Blocks.AIR);
            helper.succeed();
        });
    }

    /** Player ray caches, level caches, the emitter index and the pool surveys go when the server stops. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_static_stop_caches")
    public static void cachesAreClearedWhenTheServerStops(GameTestHelper helper) {
        BlockPos water = new BlockPos(8, 2, 8);
        helper.setBlock(water, Blocks.WATER);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            RadiationEmitter emitter = PerformanceBenchmarks.emitter(level, helper.absoluteVec(new Vec3(2.5, 3.0, 6.5)));
            RadiationEmitters.register(emitter);
            RadiationCaches.of(player);
            RadiationHandler.cachedAmbientRate(level, helper.absoluteVec(new Vec3(6.5, 3.0, 6.5)));
            SpentFuelPoolSurvey.of(level, helper.absolutePos(water));
            helper.assertTrue(RadiationCaches.tracks(player) && RadiationCaches.tracks(level) && RadiationEmitters.count(level) >= 1
                    && SpentFuelPoolSurvey.tracks(level), "the caches hold the player, the level and the pool");
            ServerStoppingEvent stopping = new ServerStoppingEvent(level.getServer());
            RadiationCaches.onServerStopping(stopping);
            SpentFuelPoolSurvey.onServerStopping(stopping);
            helper.assertTrue(!RadiationCaches.tracks(player) && !RadiationCaches.tracks(level) && RadiationEmitters.count(level) == 0,
                    "nothing of the radiation system outlives the server");
            helper.assertTrue(!SpentFuelPoolSurvey.tracks(level), "nor a pool survey");
            helper.setBlock(water, Blocks.AIR);
            helper.succeed();
        });
    }

    /**
     * A missile saves the chunk it forces, so after a restart it still lets it go. One saved by an
     * older version adopts a forced chunk only when its save proves the chunk is the one its flight
     * forced: one that never flew, or stands off its flight path, leaves a forced chunk (an admin's
     * {@code /forceload}, say) alone. Far from the test, so the test's own chunks are never touched.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_static_missile")
    public static void missileSavesTheChunkItForces(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            ChunkPos far = new ChunkPos((origin.getX() >> 4) + 40, (origin.getZ() >> 4) + 40);
            boolean wasForced = level.getForcedChunks().contains(far.toLong());
            helper.assertTrue(!wasForced, "the far chunk is free to use");
            level.setChunkForced(far.x, far.z, true);
            try {
                MissileEntity legacy = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                Vec3 inFar = new Vec3(far.getMiddleBlockX() + 0.5, 100, far.getMiddleBlockZ() + 0.5);
                legacy.configure(inFar, inFar.add(200, 0, 0), 1.0f, 4, 1.0f);

                // Saved by the older version before its first tick: it had forced nothing yet.
                MissileEntity unflown = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                unflown.load(legacySave(legacy, 0, inFar));
                helper.assertTrue(!unflown.adoptLegacyForcedChunk(level) && unflown.getForcedChunk() == null,
                        "a missile that never flew does not take a chunk forced by someone else");

                // Moved off its flight path (by a command, say): the chunk it stands in proves nothing.
                int flown = 3;
                MissileEntity displaced = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                displaced.load(legacySave(legacy, flown, legacy.trajectoryAt(flown).add(0, 5, 0)));
                helper.assertTrue(!displaced.adoptLegacyForcedChunk(level) && displaced.getForcedChunk() == null,
                        "a missile off its flight path does not take the chunk it stands in");

                // In flight where its own path put it, the chunk it forced still forced: that one is its own.
                Vec3 onPath = legacy.trajectoryAt(flown);
                helper.assertTrue(new ChunkPos(BlockPos.containing(onPath)).equals(far), "three ticks in, still in the far chunk");
                MissileEntity loaded = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                loaded.load(legacySave(legacy, flown, onPath));
                helper.assertTrue(loaded.getForcedChunk() == null, "an old save names no chunk");
                helper.assertTrue(loaded.adoptLegacyForcedChunk(level) && far.equals(loaded.getForcedChunk()),
                        "the old missile takes the chunk its flight forced as its own, got " + loaded.getForcedChunk());

                CompoundTag saved = loaded.saveWithoutId(new CompoundTag());
                helper.assertTrue(saved.contains("ForcedChunk"), "the forced chunk is saved with the missile");
                MissileEntity reloaded = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                reloaded.load(saved);
                helper.assertTrue(far.equals(reloaded.getForcedChunk()), "and comes back with it, got " + reloaded.getForcedChunk());

                MissileEntity none = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                none.configure(inFar, inFar.add(200, 0, 0), 1.0f, 4, 1.0f);
                MissileEntity noneLoaded = new MissileEntity(MissileRegistry.MISSILE.get(), level);
                noneLoaded.load(none.saveWithoutId(new CompoundTag()));
                helper.assertTrue(noneLoaded.getForcedChunk() == null, "a missile that forced nothing loads with nothing");
            } finally {
                level.setChunkForced(far.x, far.z, false);
            }
            helper.succeed();
        });
    }

    /** How an older version saved {@code missile} after {@code age} ticks at {@code pos}: no word of a forced chunk. */
    private static CompoundTag legacySave(MissileEntity missile, int age, Vec3 pos) {
        CompoundTag tag = missile.saveWithoutId(new CompoundTag());
        tag.remove("ForcedChunk");
        tag.remove("NoForcedChunk");
        tag.putInt("Age", age);
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(pos.x));
        position.add(DoubleTag.valueOf(pos.y));
        position.add(DoubleTag.valueOf(pos.z));
        tag.put("Pos", position);
        return tag;
    }
}
