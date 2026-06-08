package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.explosive.block.NuclearBombBlock;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;

public class NuclearBombMenu extends AbstractContainerMenu {
    private final NuclearBombBlockEntity bomb;
    private final BlockPos bombPos;

    public NuclearBombMenu(int containerId, Inventory playerInventory, NuclearBombBlockEntity bomb) {
        this(containerId, playerInventory, bomb, bomb != null ? bomb.getBlockPos() : null);
    }

    public NuclearBombMenu(int containerId, Inventory playerInventory, NuclearBombBlockEntity bomb, BlockPos bombPos) {
        super(AllNuclearGUIs.NUCLEAR_BOMB_MENU.get(), containerId);
        if (bomb == null && bombPos == null && net.minecraft.client.Minecraft.getInstance().level != null) {
            bombPos = NuclearBombBlock.lastBombMenuPos;
            if (bombPos != null && net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(bombPos) instanceof NuclearBombBlockEntity bombBlockEntity) {
                bomb = bombBlockEntity;
            }
        }
        this.bomb = bomb;
        this.bombPos = bombPos;
        if (this.bomb != null) {
            addBombSlots();
        }
        addPlayerInventorySlots(playerInventory);
    }

    private void addBombSlots() {
        for (int slot = 0; slot < NuclearBombBlockEntity.SLOT_COUNT; slot++) {
            addSlot(new UraniumSlot(bomb, slot, 62 + slot * 22, 62));
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
        int bombSlots = bomb != null ? NuclearBombBlockEntity.SLOT_COUNT : 0;
        if (index < bombSlots) {
            if (!moveItemStackTo(stack, bombSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (bomb != null && bomb.isValidUranium(stack)) {
            if (!moveItemStackTo(stack, 0, NuclearBombBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
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

    @Override
    public boolean stillValid(Player player) {
        return bombPos != null && player.blockPosition().closerThan(bombPos, 8.0);
    }

    public NuclearBombBlockEntity getBomb() {
        return bomb;
    }

    public BlockPos getBombPos() {
        return bombPos;
    }

    private static class UraniumSlot extends SlotItemHandler {
        private final NuclearBombBlockEntity bomb;

        private UraniumSlot(NuclearBombBlockEntity bomb, int slot, int x, int y) {
            super(bomb.getInventory(), slot, x, y);
            this.bomb = bomb;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return bomb.isValidUranium(stack);
        }
    }
}
