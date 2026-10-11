package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

import java.util.ArrayList;
import java.util.List;

/**
 * The multiblock index answers block changes from the bounds it stored when it indexed each
 * controller, without asking the controllers again; a chunk unload only looks at the controllers
 * indexed under that chunk; and unformed turbines and cooling towers keep their (large) bounds
 * instead of building them anew. The fake controllers live 200 000 blocks away and only within one
 * tick, so no other test meets them.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceMultiblockRegistryTests {
    private static final int FAR = 200_000;

    /** A controller that only counts how often it is asked for its bounds. */
    private static final class CountingController implements MultiblockController {
        private final Level level;
        private final BlockPos pos;
        private final BoundingBox bounds;
        int boundsCalls;

        CountingController(Level level, BlockPos pos, int reach) {
            this.level = level;
            this.pos = pos.immutable();
            this.bounds = new BoundingBox(pos).inflatedBy(reach);
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public BlockPos getBlockPos() {
            return pos;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public BoundingBox interestBounds() {
            boundsCalls++;
            return bounds;
        }

        @Override
        public boolean shouldReactToBlockChange(BlockChange change) {
            return false;
        }

        @Override
        public void requestStructureRevalidation(@Nullable Player player) {
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void lookupsUseTheBoundsStoredWhenIndexed(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Level level = helper.getLevel();
            BlockPos at = helper.absolutePos(BlockPos.ZERO).offset(FAR, 0, FAR);
            CountingController controller = new CountingController(level, at, 4);
            int calls;
            boolean inside = true;
            boolean outside = false;
            try {
                MultiblockRegistry.register(controller);
                for (int i = 0; i < 100; i++) {
                    inside &= MultiblockRegistry.affecting(level, at.offset(i % 9 - 4, 0, 3)).contains(controller);
                    outside |= MultiblockRegistry.affecting(level, at.offset(5, 0, i % 9 - 4)).contains(controller);
                }
                calls = controller.boundsCalls;
                MultiblockRegistry.reindex(controller);
            } finally {
                MultiblockRegistry.unregister(controller);
            }
            helper.assertTrue(inside, "every position inside its bounds finds it");
            helper.assertTrue(!outside, "no position just outside does");
            helper.assertTrue(calls == 1, "its bounds were read once, when it was indexed, not per lookup: " + calls);
            helper.assertTrue(controller.boundsCalls == 2, "and once more when reindexed: " + controller.boundsCalls);
            helper.assertTrue(MultiblockRegistry.affecting(level, at).isEmpty(), "unregistered, it is gone");
            helper.succeed();
        });
    }

    /** A real reactor: block changes around it read no bounds, and still find it. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void blockChangesAroundAReactorReadNoBounds(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(5, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor forms");
            BlockPos shell = helper.absolutePos(FissionTestBuilder.WALL_CASING);
            BlockPos beside = FissionTestBuilder.ORIGIN.offset(FissionTestBuilder.SIZE + 1, 0, 0);
            long before = MultiblockRegistry.boundsReads();
            boolean found = MultiblockRegistry.affecting(helper.getLevel(), shell, ReactorBlockEntity.class).contains(reactor);
            for (int i = 0; i < 10; i++) {
                helper.setBlock(beside, i % 2 == 0 ? Blocks.STONE : Blocks.AIR);
            }
            long reads = MultiblockRegistry.boundsReads() - before;
            helper.assertTrue(found, "a shell block resolves to its controller");
            helper.assertTrue(reads == 0, "ten block changes beside it read no bounds: " + reads);
            helper.succeed();
        });
    }

    /**
     * Forty controllers in forty chunks, one in the chunk that unloads and one beside it whose bounds
     * reach into it: the unload looks at those two only, drops the one standing there and keeps the other.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aChunkUnloadOnlyLooksAtThatChunk(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Level level = helper.getLevel();
            BlockPos base = new BlockPos(SectionCorner.of(helper.absolutePos(BlockPos.ZERO).getX() + FAR), 64,
                    SectionCorner.of(helper.absolutePos(BlockPos.ZERO).getZ() - FAR));
            List<CountingController> others = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                others.add(new CountingController(level, base.offset(64 + 32 * i, 0, 64), 2));
            }
            CountingController inChunk = new CountingController(level, base.offset(8, 0, 8), 2);
            CountingController beside = new CountingController(level, base.offset(-2, 0, 8), 4);
            int visited;
            boolean droppedInChunk;
            boolean keptBeside;
            try {
                others.forEach(MultiblockRegistry::register);
                MultiblockRegistry.register(inChunk);
                MultiblockRegistry.register(beside);
                MultiblockRegistry.unregisterChunk(level, new ChunkPos(base));
                visited = MultiblockRegistry.lastUnloadVisited();
                droppedInChunk = !MultiblockRegistry.affecting(level, inChunk.getBlockPos()).contains(inChunk);
                keptBeside = MultiblockRegistry.affecting(level, beside.getBlockPos()).contains(beside);
            } finally {
                others.forEach(MultiblockRegistry::unregister);
                MultiblockRegistry.unregister(inChunk);
                MultiblockRegistry.unregister(beside);
            }
            helper.assertTrue(visited == 2, "only the two controllers indexed under the chunk are looked at: " + visited);
            helper.assertTrue(droppedInChunk, "the one standing in it is dropped");
            helper.assertTrue(keptBeside, "the one beside it stays");
            helper.succeed();
        });
    }

    /** The corner of the chunk a block coordinate is in. */
    private static final class SectionCorner {
        static int of(int block) {
            return Math.floorDiv(block, 16) * 16;
        }
    }

    /** Unformed, a turbine and a cooling tower hand out the same box each time instead of building one. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void unformedTurbinesAndCoolingTowersKeepTheirBounds(GameTestHelper helper) {
        BlockPos turbinePos = new BlockPos(3, 2, 3);
        BlockPos towerPos = new BlockPos(9, 2, 9);
        helper.setBlock(turbinePos, TurbineTestBuilder.controller(TurbineTier.STEAM).defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(towerPos, AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get());
        helper.runAfterDelay(2, () -> {
            AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(turbinePos);
            CoolingTowerControllerBlockEntity tower = helper.getBlockEntity(towerPos);
            helper.assertTrue(turbine != null && tower != null, "both controllers have their block entities");
            BoundingBox turbineBounds = turbine.interestBounds();
            BoundingBox towerBounds = tower.interestBounds();
            helper.assertTrue(turbine.interestBounds() == turbineBounds, "the turbine's box is kept");
            helper.assertTrue(tower.interestBounds() == towerBounds, "the cooling tower's box is kept");
            helper.assertTrue(turbineBounds.isInside(helper.absolutePos(turbinePos)) && towerBounds.isInside(helper.absolutePos(towerPos)),
                    "and each holds its controller");
            helper.assertTrue(turbineBounds.equals(MultiblockRegistry.indexedBounds(turbine)),
                    "the index holds the same box the turbine hands out");
            helper.setBlock(turbinePos, turbine.getBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
            helper.succeedWhen(() -> {
                BoundingBox turned = turbine.interestBounds();
                helper.assertTrue(!turned.equals(turbineBounds), "turned east, the turbine reaches the other way");
                helper.assertTrue(turned.equals(MultiblockRegistry.indexedBounds(turbine)), "and the index follows it");
                helper.setBlock(turbinePos, Blocks.AIR);
                helper.setBlock(towerPos, Blocks.AIR);
            });
        });
    }
}
