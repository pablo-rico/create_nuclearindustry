package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * A formed blast door: a rectangle of leaves, 2x2 to 4x4, in the wall plane across the controller's
 * shaft, with a ring of frame blocks around it that the controller is part of. The panel slides
 * towards the controller when it sits beside the door, or along +X/+Z when it sits above or below.
 *
 * @param far    bottom leaf of the column farthest from the pocket the panel slides into
 * @param slide  direction the panel slides when opening
 * @param width  columns, along {@code slide}
 * @param height rows
 */
public record BlastDoorLayout(BlockPos far, Direction slide, int width, int height) {
    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 4;

    /** Leaf at column {@code column} (0 farthest from the pocket) and row {@code row} (0 at the bottom). */
    public BlockPos cell(int column, int row) {
        return far.relative(slide, column).above(row);
    }

    /** Columns between this column and the pocket side, as {@link BlastDoorTravel#cell} counts them. */
    public int fromPocket(int column) {
        return width - 1 - column;
    }

    public boolean contains(BlockPos pos) {
        int row = pos.getY() - far.getY();
        int column = (pos.getX() - far.getX()) * slide.getStepX() + (pos.getZ() - far.getZ()) * slide.getStepZ();
        BlockPos expected = cell(column, row);
        return row >= 0 && row < height && column >= 0 && column < width && expected.equals(pos);
    }

    /**
     * The door a controller at {@code controller} whose shaft runs along {@code shaftAxis} closes, or
     * {@code null} when there is none, it is too small or large, it is not a full rectangle, or its
     * frame has a gap.
     */
    @Nullable
    public static BlastDoorLayout find(BlockGetter level, BlockPos controller, Direction.Axis shaftAxis) {
        if (shaftAxis == Direction.Axis.Y) {
            return null;
        }
        Direction.Axis planeAxis = shaftAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction[] inPlane = {Direction.UP, Direction.DOWN,
                Direction.get(Direction.AxisDirection.POSITIVE, planeAxis), Direction.get(Direction.AxisDirection.NEGATIVE, planeAxis)};
        for (Direction toDoor : inPlane) {
            BlastDoorLayout layout = tryFrom(level, controller, controller.relative(toDoor), toDoor, planeAxis, inPlane);
            if (layout != null) {
                return layout;
            }
        }
        return null;
    }

    @Nullable
    private static BlastDoorLayout tryFrom(BlockGetter level, BlockPos controller, BlockPos start, Direction toDoor,
                                           Direction.Axis planeAxis, Direction[] inPlane) {
        if (!isLeaf(level.getBlockState(start))) {
            return null;
        }
        // Flood the leaves in the wall plane, giving up past the largest door.
        int maxCells = MAX_SIZE * MAX_SIZE;
        Set<BlockPos> leaves = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        leaves.add(start);
        queue.add(start);
        int minU = Integer.MAX_VALUE, maxU = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int u = pos.get(planeAxis);
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            for (Direction direction : inPlane) {
                BlockPos next = pos.relative(direction);
                if (!leaves.contains(next) && isLeaf(level.getBlockState(next))) {
                    if (leaves.size() >= maxCells) {
                        return null;
                    }
                    leaves.add(next);
                    queue.add(next);
                }
            }
        }
        int width = maxU - minU + 1;
        int height = maxY - minY + 1;
        if (width < MIN_SIZE || width > MAX_SIZE || height < MIN_SIZE || height > MAX_SIZE || leaves.size() != width * height) {
            return null;
        }
        // Every block of the ring around the leaves is frame, except the controller itself.
        for (int u = minU - 1; u <= maxU + 1; u++) {
            for (int y = minY - 1; y <= maxY + 1; y++) {
                boolean border = u == minU - 1 || u == maxU + 1 || y == minY - 1 || y == maxY + 1;
                if (!border) {
                    continue;
                }
                BlockPos ring = at(controller, planeAxis, u, y);
                if (!ring.equals(controller) && !level.getBlockState(ring).is(CivilDefenseRegistry.BLAST_DOOR_FRAME.get())) {
                    return null;
                }
            }
        }
        Direction slide = toDoor.getAxis().isHorizontal() ? toDoor.getOpposite()
                : Direction.get(Direction.AxisDirection.POSITIVE, planeAxis);
        int farU = slide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? minU : maxU;
        return new BlastDoorLayout(at(controller, planeAxis, farU, minY), slide, width, height);
    }

    private static BlockPos at(BlockPos controller, Direction.Axis planeAxis, int u, int y) {
        return planeAxis == Direction.Axis.X ? new BlockPos(u, y, controller.getZ()) : new BlockPos(controller.getX(), y, u);
    }

    private static boolean isLeaf(BlockState state) {
        return state.getBlock() instanceof BlastDoorBlock;
    }
}
