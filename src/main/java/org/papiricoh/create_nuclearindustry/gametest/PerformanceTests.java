package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationStats;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.WasteConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * What the server saves without changing what players get: the radiation scan skips chunk sections
 * with nothing radioactive and reuses rays until a block changes, emitters are found through a
 * chunk index, the crater keeps to its configured budget, and a reactor being filled sends its
 * clients an update every ten ticks at most, without its structure once they have it.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceTests {
    private static final BlockPos PLAYER = new BlockPos(6, 2, 6);
    private static final BlockPos BARREL = new BlockPos(6, 2, 8);

    private static Player playerAt(GameTestHelper helper, BlockPos relative) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(relative)));
        return player;
    }

    /**
     * All in one tick, so the barrel never takes a random tick (and leaks) and nothing expires: every
     * change shows at once because block changes make the cached rays across them stale.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aBarrelPlacedOrTakenAwayChangesTheDoseAtOnce(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Player player = playerAt(helper, PLAYER);
            BlockPos barrel = helper.absolutePos(BARREL);
            int storage = Config.geologicalStorageMaxY;
            boolean legacy = WasteConfig.legacyDepthRule;
            try {
                // A barrel above the storage depth irradiates in full.
                WasteConfig.legacyDepthRule = true;
                Config.geologicalStorageMaxY = barrel.getY() - 1;

                double clean = RadiationHandler.exposureRate(player);
                RadiationStats empty = RadiationStats.last();
                helper.assertTrue(empty.blockLookups == 0 && empty.sectionsScanned == 0 && empty.sectionsSkipped > 0,
                        "with nothing radioactive around, every section is skipped by its palette: " + empty);

                helper.setBlock(BARREL, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get());
                double hot = RadiationHandler.exposureRate(player);
                RadiationStats first = RadiationStats.last();
                helper.assertTrue(hot > clean + 0.1, "a barrel two blocks off raises the dose, " + clean + " -> " + hot);
                helper.assertTrue(first.sectionsScanned >= 1 && first.blockLookups > 0 && first.blockLookups <= 7 * 7 * 7,
                        "only the section holding it is read, no more than the 7x7x7 around: " + first);
                helper.assertTrue(first.raysCast >= 1, "its ray is cast: " + first);

                double again = RadiationHandler.exposureRate(player);
                RadiationStats second = RadiationStats.last();
                helper.assertTrue(again == hot, "standing still the dose is the same, " + hot + " -> " + again);
                helper.assertTrue(second.raysCast == 0 && second.raysReused == first.raysCast + first.raysReused,
                        "and every ray is reused: " + second);

                helper.setBlock(BARREL.north(), Blocks.STONE);
                helper.setBlock(BARREL.north().above(), Blocks.STONE);
                double shielded = RadiationHandler.exposureRate(player);
                helper.assertTrue(shielded < hot, "a wall built in between counts at once, " + hot + " -> " + shielded);
                helper.assertTrue(RadiationStats.last().raysCast >= 1, "its ray is cast again: " + RadiationStats.last());

                helper.setBlock(BARREL.north(), Blocks.AIR);
                helper.setBlock(BARREL.north().above(), Blocks.AIR);
                double open = RadiationHandler.exposureRate(player);
                helper.assertTrue(Math.abs(open - hot) < 1e-9, "and so does taking it down, " + hot + " -> " + open);

                helper.setBlock(BARREL, Blocks.AIR);
                double gone = RadiationHandler.exposureRate(player);
                helper.assertTrue(Math.abs(gone - clean) < 1e-9, "taking the barrel away lowers the dose again, " + clean + " -> " + gone);
            } finally {
                Config.geologicalStorageMaxY = storage;
                WasteConfig.legacyDepthRule = legacy;
                helper.setBlock(BARREL, Blocks.AIR);
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void withNoCacheEveryRayIsCast(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Player player = playerAt(helper, PLAYER);
            List<RadiationEmitter> emitters = List.of(PerformanceBenchmarks.emitter(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.5, 2.5))));
            int ttl = PerformanceConfig.radiationRayCacheTicks;
            RadiationStats cached;
            RadiationStats uncached;
            try {
                emitters.forEach(RadiationEmitters::register);
                RadiationHandler.exposureRate(player);
                RadiationHandler.exposureRate(player);
                cached = RadiationStats.last();
                PerformanceConfig.radiationRayCacheTicks = 0;
                RadiationHandler.exposureRate(player);
                uncached = RadiationStats.last();
            } finally {
                PerformanceConfig.radiationRayCacheTicks = ttl;
                emitters.forEach(RadiationEmitters::unregister);
            }
            helper.assertTrue(cached.raysCast == 0 && cached.raysReused >= 1, "the emitter's ray is reused: " + cached);
            helper.assertTrue(uncached.raysReused == 0 && uncached.raysCast >= 1, "with a cache of 0 ticks it is cast again: " + uncached);
            helper.succeed();
        });
    }

    /** An emitter whose origin can be moved, as a reactor's moves to the middle of its core when it forms. */
    private static final class MovableEmitter implements RadiationEmitter {
        private final Level level;
        private Vec3 origin;

        MovableEmitter(Level level, Vec3 origin) {
            this.level = level;
            this.origin = origin;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public Vec3 radiationOrigin() {
            return origin;
        }

        @Override
        public double radiationStrength() {
            return 1.0;
        }
    }

    /** Registered, queried and unregistered within one tick, so no other test's players meet them. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theEmitterIndexFindsEmittersByChunk(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Level level = helper.getLevel();
            Vec3 at = helper.absoluteVec(new Vec3(6.5, 2.5, 6.5));
            MovableEmitter near = new MovableEmitter(level, at.add(5, 0, 0));
            MovableEmitter mid = new MovableEmitter(level, at.add(0, 0, 20));
            MovableEmitter far = new MovableEmitter(level, at.add(-200, 0, 0));
            List<MovableEmitter> all = List.of(near, mid, far);
            int before = RadiationEmitters.count(level);
            List<RadiationEmitter> found;
            List<RadiationEmitter> limited;
            List<RadiationEmitter> moved;
            int chunks;
            try {
                all.forEach(RadiationEmitters::register);
                RadiationEmitters.reindex(level);
                found = new ArrayList<>(RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, RadiationHandler.EMITTER_LIMIT));
                chunks = RadiationStats.last().emitterChunks;
                limited = new ArrayList<>(RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, 1));
                // The far one comes over, beyond its old chunk.
                far.origin = at.add(-10, 0, 0);
                RadiationEmitters.reindex(level);
                moved = new ArrayList<>(RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, RadiationHandler.EMITTER_LIMIT));
            } finally {
                all.forEach(RadiationEmitters::unregister);
            }
            helper.assertTrue(found.contains(near) && found.contains(mid) && !found.contains(far),
                    "the two within range are found and the one 200 blocks off is not, found " + found.size());
            helper.assertTrue(found.indexOf(near) < found.indexOf(mid), "nearest first");
            helper.assertTrue(limited.size() == 1 && limited.get(0) == near, "a limit of one keeps the nearest");
            helper.assertTrue(chunks >= 1 && chunks <= 9 * 9, "only the chunks in range are searched, " + chunks);
            helper.assertTrue(moved.contains(far) && moved.indexOf(far) < moved.indexOf(mid),
                    "an emitter that moved is found where it is now");
            helper.assertTrue(RadiationEmitters.count(level) == before, "and unregistered they are gone from the index");
            helper.succeed();
        });
    }

    /** A crater keeps to the columns and blocks per tick of {@code explosive.*} in the performance config. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theCraterKeepsToItsBudget(GameTestHelper helper) {
        BlockPos center = new BlockPos(6, 12, 6);
        helper.setBlock(center.below(2), Blocks.STONE);
        helper.runAfterDelay(1, () -> {
            int columns = PerformanceConfig.blastColumnsPerTick;
            int blocks = PerformanceConfig.blastBlocksPerTick;
            NuclearBlastManager.BlastJob byColumns;
            NuclearBlastManager.BlastJob byBlocks;
            try {
                PerformanceConfig.blastColumnsPerTick = 7;
                PerformanceConfig.blastBlocksPerTick = 1_000_000;
                // Up in the air: nothing but air above the stone in the middle.
                byColumns = NuclearBlastManager.create(helper.getLevel(), helper.absolutePos(center).above(20), 3, 3, 1.0f);
                helper.assertTrue(byColumns != null, "a crater is made");
                byColumns.step();

                PerformanceConfig.blastColumnsPerTick = 600;
                PerformanceConfig.blastBlocksPerTick = 1;
                byBlocks = NuclearBlastManager.create(helper.getLevel(), helper.absolutePos(center), 3, 3, 1.0f);
                helper.assertTrue(byBlocks != null, "a crater is made");
                byBlocks.step();
            } finally {
                PerformanceConfig.blastColumnsPerTick = columns;
                PerformanceConfig.blastBlocksPerTick = blocks;
            }
            helper.assertTrue(byColumns.lastColumns() == 7, "seven columns a tick, dug " + byColumns.lastColumns());
            helper.assertTrue(byBlocks.lastColumns() == 1 && byBlocks.lastBlocks() == 1,
                    "one block a tick stops after the first column that removed one, dug " + byBlocks.lastColumns()
                            + " columns and " + byBlocks.lastBlocks() + " blocks");
            helper.assertTrue(helper.getBlockState(center.below(2)).isAir(), "the stone in the middle is gone");
            helper.succeed();
        });
    }

    /**
     * Coolant pumped in every tick used to send every watching client the whole reactor every tick;
     * now contents go out with an update at most every {@link ReactorBlockEntity#SYNC_INTERVAL} ticks,
     * a SCRAM at once, and the structure only while the clients do not have it yet.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aReactorBeingFilledUpdatesItsClientsEveryTenTicksAtMost(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        int[] pumped = {0};
        int[] syncsBefore = {0};
        helper.onEachTick(() -> {
            if (reactor == null || !reactor.isFormed() || pumped[0] >= 40) {
                return;
            }
            if (pumped[0] == 0) {
                syncsBefore[0] = reactor.clientSyncCount();
            }
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
            pumped[0]++;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor != null && pumped[0] >= 40, "coolant is pumped in for 40 ticks");
            int sent = reactor.clientSyncCount() - syncsBefore[0];
            helper.assertTrue(sent >= 2 && sent <= 40 / ReactorBlockEntity.SYNC_INTERVAL + 1,
                    "40 ticks of pumping send a few updates, not one a tick: " + sent);
            helper.assertTrue(reactor.getCoolantAmount() == 4_000, "and every drop got in, " + reactor.getCoolantAmount());

            int beforeScram = reactor.clientSyncCount();
            reactor.setManualScram(true);
            helper.assertTrue(reactor.clientSyncCount() == beforeScram + 1, "a SCRAM goes out at once");

            var registries = helper.getLevel().registryAccess();
            CompoundTag full = reactor.getUpdateTag(registries);
            var firstPacket = reactor.getUpdatePacket();
            var secondPacket = reactor.getUpdatePacket();
            helper.assertTrue(firstPacket != null && secondPacket != null, "update packets are made");
            CompoundTag first = firstPacket.getTag();
            CompoundTag second = secondPacket.getTag();
            helper.assertTrue(full.contains("structure") && first.contains("structure"),
                    "the chunk data and the first packet carry the structure");
            helper.assertTrue(!second.contains("structure") && second.getBoolean("structureUnchanged"),
                    "a later packet leaves it out");
            helper.assertTrue(second.sizeInBytes() < full.sizeInBytes(), "and is smaller, " + second.sizeInBytes() + " B against "
                    + full.sizeInBytes() + " B");

            ReactorBlockEntity client = new ReactorBlockEntity(reactor.getBlockPos(), reactor.getBlockState());
            client.handleUpdateTag(full, registries);
            client.handleUpdateTag(second, registries);
            helper.assertTrue(client.isFormed() && client.getCoolantAmount() == 4_000,
                    "a client that got the chunk keeps the structure through later packets");
        });
    }
}
