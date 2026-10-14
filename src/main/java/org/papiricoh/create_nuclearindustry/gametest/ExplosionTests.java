package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.explosive.BlastJobs;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.NuclearFlash;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;

/**
 * A nuclear detonation's effects on the world: the crater with its raised rim and glassy floor, what the
 * blast and the flash leave around it, blast-proof blocks, chunks that are not loaded, saved jobs, the
 * shared budget, the flash and prompt radiation on living things, and fallout drifting downwind. The
 * detonations are scaled down to fit their arenas.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ExplosionTests {
    /** A detonation small enough for the 41 wide arena: a 6 block crater, wrecking out to 9.5, the flash 18 blocks out. */
    private static DetonationProfile small() {
        return new DetonationProfile(10, 2500, 12, 1, 0, false, 2, 6, 2, 8, 1, 10, 5, 7, 9.5, 18, 8, 0, 0);
    }

    private static final BlockPos CENTRE = new BlockPos(20, 4, 20);

    /** Stone with a skin of grass across the arena, the ground the crater is dug in. */
    private static void ground(GameTestHelper helper) {
        for (int x = 2; x <= 38; x++) {
            for (int z = 2; z <= 38; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "explosion_crater", timeoutTicks = 400)
    public static void aCraterHasARaisedRimAGlassFloorAndScorchedSurroundings(GameTestHelper helper) {
        ground(helper);
        // A blast door frame in the bowl, a plank shed in the moderate zone and a tree where the flash chars it.
        helper.setBlock(new BlockPos(20, 3, 22), CivilDefenseRegistry.BLAST_DOOR_FRAME.get());
        for (int x = 28; x <= 29; x++) {
            for (int y = 4; y <= 6; y++) {
                for (int z = 19; z <= 21; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.OAK_PLANKS);
                }
            }
        }
        // One tree in the wrecked zone, one just outside it where only the flash reaches.
        for (int z : new int[]{12, 30}) {
            for (int y = 4; y <= 6; y++) {
                helper.setBlock(new BlockPos(20, y, z), Blocks.OAK_LOG);
            }
            helper.setBlock(new BlockPos(20, 7, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
        NuclearBlastManager.BlastJob[] job = new NuclearBlastManager.BlastJob[1];
        helper.runAfterDelay(2, () -> job[0] = NuclearBlastManager.start(helper.getLevel(), helper.absolutePos(CENTRE), small()));
        helper.succeedWhen(() -> {
            helper.assertTrue(job[0] != null && job[0].finished(), "the crater is still being dug");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(20, 3, 20));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(20, 2, 20));
            helper.assertFalse(helper.getBlockState(new BlockPos(20, 1, 20)).is(Blocks.STONE), "the bowl's floor is glazed");
            helper.assertTrue(count(helper, ExplosiveRegistry.TRINITITE.get(), 5, 0, 2) >= 3, "the floor is mostly trinitite");
            helper.assertBlockPresent(CivilDefenseRegistry.BLAST_DOOR_FRAME.get(), new BlockPos(20, 3, 22));
            int lip = 0;
            for (int dx = -9; dx <= 9; dx++) {
                for (int dz = -9; dz <= 9; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r >= 6.4 && r < 7.2 && !helper.getBlockState(new BlockPos(20 + dx, 4, 20 + dz)).isAir()) {
                        lip++;
                    }
                }
            }
            helper.assertTrue(lip > 4, "the rim stands above the old ground round the bowl: " + lip);
            int planks = 0;
            for (int x = 28; x <= 29; x++) {
                for (int y = 4; y <= 6; y++) {
                    for (int z = 19; z <= 21; z++) {
                        planks += helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_PLANKS) ? 1 : 0;
                    }
                }
            }
            helper.assertTrue(planks < 18, "the blast wrecks part of the shed, " + planks + " of 18 planks left");
            BlockState wrecked = helper.getBlockState(new BlockPos(20, 6, 12));
            helper.assertTrue(helper.getBlockState(new BlockPos(20, 7, 12)).isAir(), "the wrecked tree's leaves are gone");
            helper.assertTrue(wrecked.isAir() || wrecked.is(ExplosiveRegistry.CHARRED_LOG.get()),
                    "and it is felled or charred: " + column(helper, 20, 12));
            helper.assertTrue(helper.getBlockState(new BlockPos(20, 7, 30)).isAir(), "the flash burns the far tree's leaves away");
            helper.assertTrue(helper.getBlockState(new BlockPos(20, 6, 30)).is(ExplosiveRegistry.CHARRED_LOG.get()),
                    "and chars its trunk: " + column(helper, 20, 30));
            helper.assertTrue(helper.getBlockState(new BlockPos(30, 3, 20)).is(Blocks.COARSE_DIRT),
                    "the flash burns the grass bare: " + column(helper, 30, 20));
        });
    }

    private static String column(GameTestHelper helper, int x, int z) {
        StringBuilder text = new StringBuilder();
        for (int y = 1; y <= 8; y++) {
            text.append(y).append('=').append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(helper.getBlockState(new BlockPos(x, y, z)).getBlock()).getPath()).append(' ');
        }
        return text.toString();
    }

    /** Blocks of {@code block} within {@code radius} of the centre column, in layers {@code y0} to {@code y1}. */
    private static int count(GameTestHelper helper, Block block, int radius, int y0, int y1) {
        int found = 0;
        int r = radius <= 0 ? 12 : radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = y0; y <= y1; y++) {
                    if (helper.getBlockState(new BlockPos(CENTRE.getX() + dx, y, CENTRE.getZ() + dz)).is(block)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    /** Left to wait (config off), a column in a chunk that is not loaded is never loaded for it: its chunk waits, and is dug when it loads. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_deferred", timeoutTicks = 300)
    public static void anUnloadedChunkWaitsForItsTurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(4096, 0, 4096);
        ChunkPos chunk = new ChunkPos(far);
        helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "the far chunk starts unloaded");
        NuclearBlastManager.BlastJob job = NuclearBlastManager.start(level, far.atY(level.getMinBuildHeight() + 200), DetonationProfile.craterOnly(3, 400, 1));
        job.waitForLoads();
        boolean[] forced = new boolean[1];
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(job.deferredChunks() >= 1, "the crater notes the chunk it could not reach");
            helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "and never loads it");
            level.setChunkForced(chunk.x, chunk.z, true);
            forced[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(forced[0] && job.finished(), "the crater finishes once the chunk loads");
            level.setChunkForced(chunk.x, chunk.z, false);
        });
    }

    /**
     * A crater out where nothing is loaded loads (or generates) the chunks it reaches itself, works them
     * through and lets them go.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_loads", timeoutTicks = 600)
    public static void theCraterLoadsTheChunksItReaches(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(6144, 0, 6144);
        ChunkPos chunk = new ChunkPos(far);
        helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "the far chunk starts unloaded");
        NuclearBlastManager.BlastJob job = NuclearBlastManager.start(level, far.atY(level.getMinBuildHeight() + 200), DetonationProfile.craterOnly(3, 400, 1));
        boolean[] held = new boolean[1];
        helper.onEachTick(() -> held[0] |= job.heldChunks() > 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(held[0], "the crater holds the chunks it is loading");
            helper.assertTrue(job.finished(), "and finishes with nobody near");
            helper.assertTrue(job.heldChunks() == 0, "then lets them go");
        });
    }

    /** A crater half dug is saved with its level and carries on where it got to. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_saved")
    public static void aCraterHalfDugIsSavedAndLoaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int before = NuclearBlastManager.pending(level);
        // Out where nothing is loaded, so the sweep moves on without digging.
        NuclearBlastManager.BlastJob job = NuclearBlastManager.start(level, nowhere(helper, 8192), small());
        int columns = PerformanceConfig.blastColumnsPerTick;
        try {
            PerformanceConfig.blastColumnsPerTick = 30;
            job.step();
        } finally {
            PerformanceConfig.blastColumnsPerTick = columns;
        }
        CompoundTag saved = BlastJobs.get(level).save(new CompoundTag(), level.registryAccess());
        NuclearBlastManager.cancelAll(level);
        BlastJobs loaded = BlastJobs.fromTag(saved, level.registryAccess());
        helper.assertTrue(loaded.jobs().size() == before + 1, "the job is saved");
        NuclearBlastManager.BlastJob back = loaded.jobs().get(loaded.jobs().size() - 1);
        helper.assertTrue(back.centre().equals(job.centre()) && back.profile().equals(job.profile()), "with its centre and zones");
        helper.assertTrue(back.ring() == job.ring() && back.index() == job.index() && back.ring() > 0, "and where its sweep got to");
        helper.succeed();
    }

    /** Every job of a level shares one budget per tick, however many there are. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_budget")
    public static void craterJobsShareOneBudget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int columns = PerformanceConfig.blastColumnsPerTick;
        try {
            PerformanceConfig.blastColumnsPerTick = 20;
            NuclearBlastManager.BlastJob first = NuclearBlastManager.start(level, nowhere(helper, 8192), small());
            NuclearBlastManager.BlastJob second = NuclearBlastManager.start(level, nowhere(helper, 12288), small());
            NuclearBlastManager.onLevelTick(new LevelTickEvent.Post(() -> true, level));
            int worked = first.lastColumns() + second.lastColumns();
            helper.assertTrue(worked <= 20, "two craters work 20 columns between them, not each: " + worked);
            helper.assertTrue(worked > 0, "but they do work");
        } finally {
            PerformanceConfig.blastColumnsPerTick = columns;
            NuclearBlastManager.cancelAll(level);
        }
        helper.succeed();
    }

    /** The flash burns a pig that sees the fireball, not one walled in. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_flash")
    public static void theFlashBurnsWhatSeesTheFireball(GameTestHelper helper) {
        for (int x = 8; x <= 12; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 8; z <= 12; z++) {
                    if (x == 8 || x == 12 || y == 1 || y == 5 || z == 8 || z == 12) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        Pig open = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 2));
        Pig walled = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(10, 2, 10));
        helper.runAfterDelay(2, () -> {
            NuclearFlash.strike(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.5, 2.5)), small());
            helper.assertTrue(open.getRemainingFireTicks() > 0, "the pig in the open burns");
            helper.assertTrue(open.getHealth() < open.getMaxHealth(), "and is hurt");
            helper.assertTrue(walled.getRemainingFireTicks() <= 0, "the walled-in pig does not");
            open.discard();
            walled.discard();
            helper.succeed();
        });
    }

    /** Prompt radiation doses whoever is near, much less through dense blocks. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "explosion_prompt")
    public static void promptRadiationIsCutByShielding(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            for (int z = 4; z <= 9; z++) {
                helper.setBlock(new BlockPos(6, y, z), AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get());
            }
        }
        Player open = helper.makeMockPlayer(GameType.SURVIVAL);
        Player shielded = helper.makeMockPlayer(GameType.SURVIVAL);
        open.setPos(helper.absoluteVec(new Vec3(2.5, 1, 9.5)));
        shielded.setPos(helper.absoluteVec(new Vec3(9.5, 1, 6.5)));
        open.setData(AllNuclearRadiation.DOSE, 0f);
        shielded.setData(AllNuclearRadiation.DOSE, 0f);
        Vec3 centre = helper.absoluteVec(new Vec3(2.5, 2.5, 6.5));
        helper.runAfterDelay(1, () -> {
            // Mock players are not in the level for the flash to find: dose them as it would.
            NuclearFlash.irradiate(helper.getLevel(), open, centre, open.getEyePosition().distanceTo(centre), small());
            NuclearFlash.irradiate(helper.getLevel(), shielded, centre, shielded.getEyePosition().distanceTo(centre), small());
            float inTheOpen = open.getData(AllNuclearRadiation.DOSE);
            float behind = shielded.getData(AllNuclearRadiation.DOSE);
            helper.assertTrue(inTheOpen > 600, "a dose that sickens in the open: " + inTheOpen);
            helper.assertTrue(behind < inTheOpen * 0.2f, "a fraction of it behind depleted uranium: " + behind);
            helper.succeed();
        });
    }

    /** Fallout carried by the wind lands downwind of ground zero, not upwind. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_falloutDriftsDownwind")
    public static void falloutDriftsDownwind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        ChunkPos origin = new ChunkPos(center);
        ContaminationData data = ContaminationData.get(level);
        try {
            ContaminationTests.loadAround(level, origin, 5);
            // Blowing towards +x: 64 blocks of fallout stretch to 128 downwind and 32 upwind.
            Contamination.fallout(level, center, 64, 10, 0, 1);
            float down = Contamination.get(level, new ChunkPos(origin.x + 5, origin.z));
            float up = Contamination.get(level, new ChunkPos(origin.x - 5, origin.z));
            helper.assertTrue(down > 0, "fallout lands five chunks downwind");
            helper.assertTrue(up == 0, "and none five chunks upwind, got " + up);
            data.events().removeLast();
        } finally {
            ContaminationTests.clean(helper, 5);
        }
        helper.succeed();
    }

    /** A spot far out in chunks that are not loaded, where a crater job only notes chunks and changes nothing. */
    private static BlockPos nowhere(GameTestHelper helper, int offset) {
        return helper.absolutePos(BlockPos.ZERO).offset(offset, 100, offset);
    }
}
