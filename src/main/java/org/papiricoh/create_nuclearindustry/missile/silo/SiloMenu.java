package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * The launch controller's console. It reaches up the rocket standing on the base: the player picks a section in the
 * cutaway ({@link SiloControlPayload#SELECT}) and its slots show as menu slots {@code 0…SECTION_SLOTS-1}, laid out as
 * {@code SiloLayout.SLOT_X/SLOT_Y} and typed by {@link WarheadBay}; the player's inventory follows from {@link #INV_START}.
 * Nothing can be moved in or out while the controller is armed, counting down or launched.
 */
public class SiloMenu extends AbstractContainerMenu {
    public static final int SECTION_SLOTS = WarheadBay.SLOTS;
    public static final int INV_START = SECTION_SLOTS, INV_END = INV_START + 36;
    public final BlockPos pos;
    /** Client only: whether the player's inventory slots show (the rocket tab) or hide (the launch tab). */
    public boolean inventoryVisible = true;

    public SiloMenu(int id, Inventory inv, BlockPos pos) { super(SiloRegistry.MENU.get(), id); this.pos = pos; }
    public static SiloMenu read(int id, Inventory inv, RegistryFriendlyByteBuf buf) { return new SiloMenu(id, inv, buf.readBlockPos()); }

    /** Server: picks the section {@code index} cells up the column, or none for -1; false if there is no section there. */
    public boolean select(int index) { return false; }
    /** The picked section's index up the column, or -1. */
    public int selected() { return -1; }
    /** What the picked section is, or null. */
    public @Nullable RocketComposition.Section selectedSection() { return null; }
    /** Whether the controller is armed, counting down or launched, so nothing moves. */
    public boolean locked() { return false; }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof SiloControllerEntity c && ServerPayloadGuard.stillValid(c, player);
    }
}
