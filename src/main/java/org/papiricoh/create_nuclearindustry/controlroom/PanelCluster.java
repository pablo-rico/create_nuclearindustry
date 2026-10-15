package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Blocks of one kind, facing one way, that touch sideways or up and down in one wall's plane: the
 * monitors of one screen, the panels of one annunciator board. Their bottom-left one as seen from the
 * front is the origin. Only a full rectangle within the size limits makes one; any other shape
 * leaves each block on its own.
 *
 * @param members   every block found, the start included
 * @param origin    the bottom-left one
 * @param width     blocks across
 * @param height    blocks up
 * @param rectangle whether they make a full rectangle within the limits
 */
public record PanelCluster(Set<BlockPos> members, BlockPos origin, int width, int height, boolean rectangle) {

    /**
     * The cluster {@code start} belongs to: blocks for which {@code same} holds and that face
     * {@code facing}. The search stops a little past the biggest rectangle allowed.
     */
    public static PanelCluster find(Level level, BlockPos start, Direction facing, Predicate<BlockState> same, int maxWidth,
                                    int maxHeight) {
        Direction right = facing.getCounterClockWise();
        Set<BlockPos> members = connected(level, start, facing, right, same, maxWidth * maxHeight + 1);
        int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos member : members) {
            int r = along(member, right);
            minR = Math.min(minR, r);
            maxR = Math.max(maxR, r);
            minY = Math.min(minY, member.getY());
            maxY = Math.max(maxY, member.getY());
        }
        int width = maxR - minR + 1;
        int height = maxY - minY + 1;
        boolean rectangle = width <= maxWidth && height <= maxHeight && members.size() == width * height;
        BlockPos origin = start;
        for (BlockPos member : members) {
            if (along(member, right) == minR && member.getY() == minY) {
                origin = member;
            }
        }
        return new PanelCluster(members, origin, width, height, rectangle);
    }

    private static Set<BlockPos> connected(Level level, BlockPos start, Direction facing, Direction right, Predicate<BlockState> same,
                                           int limit) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        found.add(start);
        open.add(start);
        while (!open.isEmpty() && found.size() <= limit) {
            BlockPos current = open.poll();
            for (BlockPos next : new BlockPos[]{current.relative(right), current.relative(right.getOpposite()), current.above(), current.below()}) {
                if (found.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (same.test(state) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                    found.add(next);
                    open.add(next);
                }
            }
        }
        return found;
    }

    /** How far along {@code axis} (a horizontal direction) {@code pos} lies. */
    public static int along(BlockPos pos, Direction axis) {
        return pos.getX() * axis.getStepX() + pos.getZ() * axis.getStepZ();
    }
}
