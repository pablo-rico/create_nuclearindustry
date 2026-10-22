package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

/** The cascade header drawn between centrifuges standing side by side, and the machines' outline. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CentrifugeCascadeTests {
    private static final BlockPos WEST = new BlockPos(1, 1, 2);
    private static final BlockPos MIDDLE = new BlockPos(2, 1, 2);
    private static final BlockPos EAST = new BlockPos(3, 1, 2);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void sideBySideCentrifugesJoinTheirHeaders(GameTestHelper helper) {
        place(helper, WEST, AllNuclearBlocks.CENTRIFUGE.get(), Direction.NORTH);
        place(helper, MIDDLE, AllNuclearBlocks.CENTRIFUGE.get(), Direction.NORTH);
        place(helper, EAST, AllNuclearBlocks.CENTRIFUGE.get(), Direction.NORTH);
        // Facing north, the left side is west and the right side east.
        assertJoints(helper, WEST, false, true);
        assertJoints(helper, MIDDLE, true, true);
        assertJoints(helper, EAST, true, false);

        helper.setBlock(EAST, Blocks.AIR);
        assertJoints(helper, MIDDLE, true, false);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void headersFollowTheMachinesFacing(GameTestHelper helper) {
        BlockPos north = new BlockPos(2, 1, 1);
        BlockPos south = new BlockPos(2, 1, 3);
        // Facing east, the left side is north and the right side south.
        place(helper, MIDDLE, AllNuclearBlocks.CENTRIFUGE.get(), Direction.EAST);
        place(helper, north, AllNuclearBlocks.ELECTROLYZER.get(), Direction.EAST);
        place(helper, south, AllNuclearBlocks.CENTRIFUGE.get(), Direction.NORTH);
        assertJoints(helper, MIDDLE, false, false);

        place(helper, north, AllNuclearBlocks.CENTRIFUGE.get(), Direction.EAST);
        assertJoints(helper, MIDDLE, true, false);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void machineOutlineFitsItsBlock(GameTestHelper helper) {
        for (Block block : new Block[]{AllNuclearBlocks.CENTRIFUGE.get(), AllNuclearBlocks.ELECTROLYZER.get()}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = block.defaultBlockState().setValue(KineticProcessingBlock.FACING, facing);
                VoxelShape shape = state.getShape(helper.getLevel(), helper.absolutePos(MIDDLE), CollisionContext.empty());
                helper.assertTrue(!shape.isEmpty(), block + " facing " + facing + " should have an outline");
                AABB bounds = shape.bounds();
                helper.assertTrue(bounds.minX >= 0 && bounds.minY >= 0 && bounds.minZ >= 0
                                && bounds.maxX <= 1 && bounds.maxY <= 1 && bounds.maxZ <= 1,
                        block + " facing " + facing + " pokes out of its block: " + bounds);
            }
        }
        helper.succeed();
    }

    /** The Ponder cascade (tools/structgen/ponder_centrifuge.py) joins exactly as placed centrifuges would. */
    @GameTest(template = "gametest/centrifuge_cascade_ponder", batch = "centrifuge_ponder")
    public static void ponderCascadeJoinsLikePlacedCentrifuges(GameTestHelper helper) {
        // Templates sit one block above the test origin.
        for (int x = 2; x <= 4; x++) {
            BlockPos pos = new BlockPos(x, 3, 2);
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.getBlock() instanceof CentrifugeBlock, "expected a centrifuge at " + pos);
            BlockState expected = CentrifugeBlock.withCascade(state, helper.getLevel(), helper.absolutePos(pos));
            helper.assertTrue(state == expected, "the schematic's centrifuge at " + pos + " is " + state + ", expected " + expected);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/electrolyzer_ponder", batch = "centrifuge_ponder")
    public static void ponderElectrolyzerStandsWhereTheSceneLooks(GameTestHelper helper) {
        helper.assertBlockPresent(AllNuclearBlocks.ELECTROLYZER.get(), new BlockPos(2, 3, 2));
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 3, 2)).getValue(KineticProcessingBlock.FACING) == Direction.NORTH,
                "the electrolyzer faces the camera");
        helper.succeed();
    }

    /** Places a machine as a player would: its own joints worked out from its neighbours. */
    private static void place(GameTestHelper helper, BlockPos pos, Block block, Direction facing) {
        BlockState state = block.defaultBlockState().setValue(KineticProcessingBlock.FACING, facing);
        helper.setBlock(pos, CentrifugeBlock.withCascade(state, helper.getLevel(), helper.absolutePos(pos)));
    }

    private static void assertJoints(GameTestHelper helper, BlockPos pos, boolean left, boolean right) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getBlock() instanceof CentrifugeBlock, "expected a centrifuge at " + pos);
        helper.assertTrue(state.getValue(CentrifugeBlock.LEFT) == left && state.getValue(CentrifugeBlock.RIGHT) == right,
                "centrifuge at " + pos + " should have left=" + left + " right=" + right + ", got " + state);
    }
}
