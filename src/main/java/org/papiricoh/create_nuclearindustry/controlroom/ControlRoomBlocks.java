package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;

/** Shared bits of the control room blocks. */
public final class ControlRoomBlocks {
    /** Wrenches open the setup screen of the instruments that have one. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WRENCHES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private ControlRoomBlocks() {}

    /**
     * Where a click landed on the front face of a block facing {@code facing}, in pixels as the viewer
     * sees it: {@code [0]} from the left edge, {@code [1]} up from the bottom, both 0-16. Null when the
     * click was on another face.
     */
    @org.jetbrains.annotations.Nullable
    public static float[] frontFacePixel(net.minecraft.core.Direction facing, net.minecraft.core.BlockPos pos,
                                         net.minecraft.world.phys.Vec3 hit) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        if (x * facing.getStepX() + z * facing.getStepZ() < 0.49) {
            return null;
        }
        net.minecraft.core.Direction right = facing.getCounterClockWise();
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
