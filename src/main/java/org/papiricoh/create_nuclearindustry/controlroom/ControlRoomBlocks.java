package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;

/** Shared bits of the control room blocks. */
public final class ControlRoomBlocks {
    private ControlRoomBlocks() {}

    /** A linker in hand goes straight to the item, so clicking a device links it instead of pressing it. */
    public static ItemInteractionResult linkerFirst(ItemStack stack) {
        return stack.getItem() instanceof ControlRoomLinkerItem
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
