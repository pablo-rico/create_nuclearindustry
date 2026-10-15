package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.PanelCluster;

/**
 * The wall-plane search shared by monitors and annunciator boards, run on plain furnaces so only the
 * search itself is under test: rectangles join, other shapes and other facings do not.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PanelClusterTests {

    private static void wall(GameTestHelper helper, Direction facing, int[][] cells) {
        BlockState state = Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        for (int[] cell : cells) {
            helper.setBlock(new BlockPos(cell[0], cell[1], 6), state);
        }
    }

    private static PanelCluster find(GameTestHelper helper, int x, int y) {
        return PanelCluster.find(helper.getLevel(), helper.absolutePos(new BlockPos(x, y, 6)), Direction.NORTH,
                s -> s.is(Blocks.FURNACE), 4, 3);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aFullRectangleJoinsWithItsBottomLeftAsOrigin(GameTestHelper helper) {
        wall(helper, Direction.NORTH, new int[][]{{3, 2}, {4, 2}, {5, 2}, {3, 3}, {4, 3}, {5, 3}});
        PanelCluster cluster = find(helper, 4, 3);
        helper.assertTrue(cluster.rectangle(), "a 3x2 wall should be a rectangle");
        helper.assertTrue(cluster.width() == 3 && cluster.height() == 2, "expected 3x2, got " + cluster.width() + "x" + cluster.height());
        helper.assertTrue(cluster.members().size() == 6, "expected 6 members, got " + cluster.members().size());
        // Facing north, the viewer stands north looking south: their left is east, the highest x.
        BlockPos origin = helper.absolutePos(new BlockPos(5, 2, 6));
        helper.assertTrue(cluster.origin().equals(origin), "origin should be the bottom-left seen from the front, got " + cluster.origin());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anLShapeIsNotARectangle(GameTestHelper helper) {
        wall(helper, Direction.NORTH, new int[][]{{3, 2}, {4, 2}, {3, 3}});
        PanelCluster cluster = find(helper, 3, 2);
        helper.assertTrue(!cluster.rectangle(), "an L shape should not make a rectangle");
        helper.assertTrue(cluster.members().size() == 3, "all three touching blocks should be found");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anotherFacingAndTheSizeLimitSplitTheWall(GameTestHelper helper) {
        wall(helper, Direction.NORTH, new int[][]{{2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}});
        wall(helper, Direction.SOUTH, new int[][]{{4, 3}});
        PanelCluster cluster = find(helper, 4, 2);
        helper.assertTrue(cluster.members().size() == 5, "the block facing south should not join, got " + cluster.members().size());
        helper.assertTrue(!cluster.rectangle(), "five across is past the four-wide limit");
        helper.succeed();
    }
}
