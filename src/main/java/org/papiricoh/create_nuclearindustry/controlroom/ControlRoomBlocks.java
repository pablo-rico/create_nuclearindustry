package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared bits of the control room blocks. */
public final class ControlRoomBlocks {
    private ControlRoomBlocks() {}

    /** A linker in hand goes straight to the item, so clicking a device links it instead of pressing it. */
    public static ItemInteractionResult linkerFirst(ItemStack stack) {
        return stack.getItem() instanceof ControlRoomLinkerItem
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Where a click landed on the front face of a block facing {@code facing}, as (u, v): u to the
     * viewer's right and v up, both 0-1. Null for a click on another face.
     */
    public static double[] faceCoordinates(BlockPos pos, Direction facing, BlockHitResult hit) {
        if (hit.getDirection() != facing) {
            return null;
        }
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double u = switch (facing.getCounterClockWise()) {
            case EAST -> local.x;
            case WEST -> 1 - local.x;
            case SOUTH -> local.z;
            default -> 1 - local.z;
        };
        return new double[]{u, local.y};
    }
}
