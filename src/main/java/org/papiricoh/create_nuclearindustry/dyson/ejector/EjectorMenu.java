package org.papiricoh.create_nuclearindustry.dyson.ejector;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.gui.EjectorLayout;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** An EM rail ejector's magazine and orbit picker, for the player standing beside it. */
public class EjectorMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    @Nullable
    public final EmRailEjectorBlockEntity ejector;

    public EjectorMenu(int id, Inventory inventory, @Nullable EmRailEjectorBlockEntity ejector, BlockPos pos) {
        super(DysonRegistry.EJECTOR_MENU.get(), id);
        this.pos = pos;
        this.ejector = ejector;
        IItemHandler magazine = ejector != null ? ejector.magazine() : new ItemStackHandler(EmRailEjectorBlockEntity.SLOTS);
        for (int i = 0; i < EmRailEjectorBlockEntity.SLOTS; i++)
            addSlot(new SlotItemHandler(magazine, i, EjectorLayout.SAIL_X + (i % 8) * 18, EjectorLayout.SAIL_Y + (i / 8) * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, EjectorLayout.INV_X + col * 18, EjectorLayout.INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, EjectorLayout.INV_X + col * 18, EjectorLayout.HOTBAR_Y));
    }

    public static EjectorMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new EjectorMenu(id, inventory,
                inventory.player.level().getBlockEntity(pos) instanceof EmRailEjectorBlockEntity found ? found : null, pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return ServerPayloadGuard.stillValid(ejector, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int magazine = EmRailEjectorBlockEntity.SLOTS;
        if (index < magazine) {
            if (!moveItemStackTo(stack, magazine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!stack.is(DysonRegistry.SOLAR_SAIL.get()) || !moveItemStackTo(stack, 0, magazine, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
