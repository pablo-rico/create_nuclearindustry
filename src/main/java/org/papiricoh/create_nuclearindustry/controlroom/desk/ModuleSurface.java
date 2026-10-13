package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of block that hold control modules: the sloping desk, the flat table and the plate on a
 * wall. Every kind numbers its slots the same way (see {@link SurfaceGeometry}): left to right as the
 * operator sees them, the near or lower row 0 and 1, then 2 and 3. Where the slots are drawn is
 * {@link DeskGeometry#toSurfaceSlot}.
 */
public enum ModuleSurface {
    DESK,
    TABLE,
    WALL;

    /** Clicks below this height on a desk land on the cabinet, not on a module. */
    private static final double SLOPE_MIN_Y = 0.55;

    @Nullable
    public static ModuleSurface of(BlockState state) {
        return state.getBlock() instanceof ModuleSurfaceBlock block ? block.surface() : null;
    }

    public static int row(int slot) {
        return slot / 2;
    }

    public static int column(int slot) {
        return slot % 2;
    }

    public static int slot(int row, int column) {
        return row * 2 + column;
    }

    /** Whether the block's shape has room for this slot: a desk's corners hold two modules. */
    public boolean hasSlot(BlockState state, int slot) {
        if (this == DESK && state.hasProperty(ControlDeskBlock.SHAPE)) {
            return state.getValue(ControlDeskBlock.SHAPE).hasSlot(slot);
        }
        return slot >= 0 && slot < ControlDeskBlockEntity.SLOTS;
    }

    /** Modules stand upright from the surface (a wall) rather than on it. */
    public boolean vertical() {
        return this == WALL;
    }

    /** Whether the rows carry on into the next block to the side: not round a desk's corner. */
    public boolean straightRows(BlockState state) {
        return this != DESK || !state.hasProperty(ControlDeskBlock.SHAPE) || state.getValue(ControlDeskBlock.SHAPE) == DeskShape.STRAIGHT;
    }

    /** The world direction of the operator's left, along the rows. */
    public static Direction leftOf(BlockState state) {
        return DeskShape.STRAIGHT.leftEnd(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /** The world direction of the operator's right, along the rows. */
    public static Direction rightOf(BlockState state) {
        return DeskShape.STRAIGHT.rightEnd(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /** The slot a click at {@code hit} lands on, or -1 off the module surface. */
    public int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        return switch (this) {
            case DESK -> deskSlot(state, local);
            case TABLE -> {
                double[] n = SurfaceGeometry.toNative(local.x, local.z, DeskGeometry.modelYaw(state));
                yield SurfaceGeometry.tableSlot(n[0], local.y, n[1]);
            }
            case WALL -> {
                double[] n = SurfaceGeometry.toNative(local.x, local.z, DeskGeometry.modelYaw(state));
                yield SurfaceGeometry.wallSlot(n[0], local.y, n[1]);
            }
        };
    }

    /**
     * On a straight desk slots go by quarters of the top; on a corner each half of the top, either
     * side of the diagonal, is its arm's one slot.
     */
    private static int deskSlot(BlockState state, Vec3 local) {
        if (local.y < SLOPE_MIN_Y) {
            return -1;
        }
        double[] n = SurfaceGeometry.toNative(local.x, local.z, DeskGeometry.bodyYaw(state));
        DeskShape shape = state.getValue(ControlDeskBlock.SHAPE);
        if (shape == DeskShape.STRAIGHT) {
            return SurfaceGeometry.topSlot(n[0], n[1]);
        }
        // The half north-east of the diagonal: an inner corner's arm carrying on north (operator west),
        // an outer corner's arm carrying on east (operator north).
        boolean northEast = n[1] <= n[0];
        return shape.isInner() ? (northEast ? 2 : 3) : (northEast ? 0 : 1);
    }
}
