package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Shared bits of the control room blocks. */
public final class ControlRoomBlocks {
    /** Wrenches open the setup screen of the instruments that have one. */
    public static final TagKey<Item> WRENCHES = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private ControlRoomBlocks() {}

    /**
     * Where a click landed on the front face of a block facing {@code facing}, in pixels as the viewer
     * sees it: {@code [0]} from the left edge, {@code [1]} up from the bottom, both 0-16. Null when the
     * click was on another face.
     */
    @org.jetbrains.annotations.Nullable
    public static float[] frontFacePixel(Direction facing, BlockPos pos,
                                         Vec3 hit) {
        return facePixel(facing, pos, hit, 0);
    }

    /**
     * As {@link #frontFacePixel}, for a face set {@code depth} pixels back from the block's front, like
     * a plate hung on the wall behind it.
     */
    @org.jetbrains.annotations.Nullable
    public static float[] facePixel(Direction facing, BlockPos pos,
                                    Vec3 hit, float depth) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        if (x * facing.getStepX() + z * facing.getStepZ() < 0.49 - depth / 16.0) {
            return null;
        }
        Direction right = facing.getCounterClockWise();
        double u = x * right.getStepX() + z * right.getStepZ() + 0.5;
        return new float[]{(float) (u * 16), (float) ((hit.y - pos.getY()) * 16)};
    }

    /** A linker in hand goes straight to the item, so clicking a device links it instead of pressing it. */
    public static ItemInteractionResult linkerFirst(ItemStack stack) {
        return stack.getItem() instanceof ControlRoomLinkerItem
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
