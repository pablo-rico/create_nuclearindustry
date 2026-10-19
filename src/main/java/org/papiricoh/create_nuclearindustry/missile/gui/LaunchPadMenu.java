package org.papiricoh.create_nuclearindustry.missile.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

/**
 * The launch pad's loading screen. On the client the pad is found from the position sent with the
 * menu ({@link #fromNetwork}), so two players opening two pads each get their own.
 */
public class LaunchPadMenu extends AbstractContainerMenu {
    private final LaunchPadBlockEntity pad;
    private final BlockPos padPos;

    public LaunchPadMenu(int containerId, Inventory playerInventory, LaunchPadBlockEntity pad) {
        this(containerId, playerInventory, pad, pad != null ? pad.getBlockPos() : null);
    }

    /** The menu on the receiving side: the pad is the one at the position the server sent. */
    public static LaunchPadMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        LaunchPadBlockEntity pad = playerInventory.player.level().getBlockEntity(pos) instanceof LaunchPadBlockEntity found
                ? found : null;
        return new LaunchPadMenu(containerId, playerInventory, pad, pos);
    }

    public LaunchPadMenu(int containerId, Inventory playerInventory, LaunchPadBlockEntity pad, BlockPos padPos) {
        super(MissileRegistry.LAUNCH_PAD_MENU.get(), containerId);
        this.pad = pad;
        this.padPos = padPos;
        if (this.pad != null) {
            addPadSlots();
        }
        addPlayerInventorySlots(playerInventory);
    }

    private void addPadSlots() {
        // Missile and designator at the top, warhead slots to the right.
        addSlot(new FilteredSlot(pad, LaunchPadBlockEntity.MISSILE_SLOT, 26, 35));
        addSlot(new FilteredSlot(pad, LaunchPadBlockEntity.DESIGNATOR_SLOT, 26, 62));
        for (int i = 0; i < LaunchPadBlockEntity.URANIUM_SLOTS; i++) {
            addSlot(new FilteredSlot(pad, LaunchPadBlockEntity.FIRST_URANIUM_SLOT + i, 80 + i * 22, 62));
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startY = 128;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, startY + 58));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        // Whoever loads the pad answers for its launch (missile.launchPermissionLevel, missile.launchCooldownSeconds).
        boolean padSlot = slotId >= 0 && slotId < LaunchPadBlockEntity.SLOT_COUNT;
        if (pad != null && !player.level().isClientSide && (padSlot || clickType == ClickType.QUICK_MOVE)) {
            pad.setOperator(player.getUUID());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int padSlots = pad != null ? LaunchPadBlockEntity.SLOT_COUNT : 0;
        if (index < padSlots) {
            if (!moveItemStackTo(stack, padSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (false) {
            // movido
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    private boolean moveIntoPad(ItemStack stack) {
        for (int slot = 0; slot < LaunchPadBlockEntity.SLOT_COUNT; slot++) {
            if (pad.isItemValid(slot, stack)) {
                if (moveItemStackTo(stack, slot, slot + 1, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard.stillValid(pad, player);
    }

    public LaunchPadBlockEntity getPad() {
        return pad;
    }

    public BlockPos getPadPos() {
        return padPos;
    }

    private static class FilteredSlot extends SlotItemHandler {
        private final LaunchPadBlockEntity pad;
        private final int slotIndex;

        private FilteredSlot(LaunchPadBlockEntity pad, int slot, int x, int y) {
            super(pad.getInventory(), slot, x, y);
            this.pad = pad;
            this.slotIndex = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Retired platform: extraction only.
        }
    }
}
