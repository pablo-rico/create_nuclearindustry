package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.ThermonuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * The thermonuclear bomb's loading screen: the primary's slot, the secondary's four and the tamper's two,
 * at the positions {@link ThermonuclearBombScreen} draws their wells. Shift-clicking a part sends it to
 * its own slots.
 */
public class ThermonuclearBombMenu extends AbstractContainerMenu {
    public static final int SLOT_Y = 104;
    public static final int PRIMARY_X = 14;
    public static final int SECONDARY_X = 44;
    public static final int TAMPER_X = 128;
    public static final int INVENTORY_Y = 144;
    public static final int HOTBAR_Y = 202;

    private final ThermonuclearBombBlockEntity bomb;
    private final BlockPos bombPos;

    public static ThermonuclearBombMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ThermonuclearBombBlockEntity bomb = playerInventory.player.level().getBlockEntity(pos) instanceof ThermonuclearBombBlockEntity found
                ? found : null;
        return new ThermonuclearBombMenu(containerId, playerInventory, bomb, pos);
    }

    public ThermonuclearBombMenu(int containerId, Inventory playerInventory, ThermonuclearBombBlockEntity bomb, BlockPos bombPos) {
        super(ExplosiveRegistry.THERMONUCLEAR_BOMB_MENU.get(), containerId);
        this.bomb = bomb;
        this.bombPos = bombPos;
        if (bomb != null) {
            for (int slot = 0; slot < ThermonuclearBombBlockEntity.SLOTS; slot++) {
                addSlot(new PartSlot(bomb, slot, slotX(slot), SLOT_Y));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    /** Where the bomb's {@code slot} sits on the screen. */
    public static int slotX(int slot) {
        return switch (ThermonuclearBombBlockEntity.part(slot)) {
            case PRIMARY -> PRIMARY_X;
            case SECONDARY -> SECONDARY_X + (slot - ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT) * 18;
            case TAMPER -> TAMPER_X + (slot - ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT) * 18;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int bombSlots = bomb != null ? ThermonuclearBombBlockEntity.SLOTS : 0;
        if (index < bombSlots) {
            if (!moveItemStackTo(stack, bombSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (bomb != null && bomb.isValidCharge(stack)) {
            boolean moved = false;
            for (int target = 0; target < bombSlots && !stack.isEmpty(); target++) {
                if (bomb.isValidInSlot(target, stack)) {
                    moved |= moveItemStackTo(stack, target, target + 1, false);
                }
            }
            if (!moved) {
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
        return ServerPayloadGuard.stillValid(bomb, player);
    }

    public ThermonuclearBombBlockEntity getBomb() {
        return bomb;
    }

    public BlockPos getBombPos() {
        return bombPos;
    }

    private static class PartSlot extends SlotItemHandler {
        private final ThermonuclearBombBlockEntity bomb;
        private final int index;

        private PartSlot(ThermonuclearBombBlockEntity bomb, int index, int x, int y) {
            super(bomb.getInventory(), index, x, y);
            this.bomb = bomb;
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return bomb.isValidInSlot(index, stack);
        }
    }
}
