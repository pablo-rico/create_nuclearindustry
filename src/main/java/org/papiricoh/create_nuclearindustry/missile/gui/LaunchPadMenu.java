package org.papiricoh.create_nuclearindustry.missile.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.missile.block.LaunchPadBlock;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

public class LaunchPadMenu extends AbstractContainerMenu {
    private final LaunchPadBlockEntity pad;
    private final BlockPos padPos;

    public LaunchPadMenu(int containerId, Inventory playerInventory, LaunchPadBlockEntity pad) {
        this(containerId, playerInventory, pad, pad != null ? pad.getBlockPos() : null);
    }

    public LaunchPadMenu(int containerId, Inventory playerInventory, LaunchPadBlockEntity pad, BlockPos padPos) {
        super(AllNuclearGUIs.LAUNCH_PAD_MENU.get(), containerId);
        if (pad == null && padPos == null && net.minecraft.client.Minecraft.getInstance().level != null) {
            padPos = LaunchPadBlock.lastMenuPos;
            if (padPos != null && net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(padPos) instanceof LaunchPadBlockEntity padBlockEntity) {
                pad = padBlockEntity;
            }
        }
        this.pad = pad;
        this.padPos = padPos;
        if (this.pad != null) {
            addPadSlots();
        }
        addPlayerInventorySlots(playerInventory);
    }

    private void addPadSlots() {
        // Cohete y designador arriba, slots de uranio a la derecha.
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
        } else if (pad != null && moveIntoPad(stack)) {
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
        return padPos != null && player.blockPosition().closerThan(padPos, 8.0);
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
            return pad.isItemValid(slotIndex, stack);
        }
    }
}
