package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** An interceptor battery's screen: its four tubes, the player's inventory, and its network, automatic mode and zone. */
public class InterceptorBatteryMenu extends AbstractContainerMenu {
    @Nullable
    public final InterceptorBatteryEntity battery;
    public final BlockPos pos;

    public InterceptorBatteryMenu(int id, Inventory inventory, @Nullable InterceptorBatteryEntity battery, BlockPos pos) {
        super(DefenseRegistry.BATTERY_MENU.get(), id);
        this.battery = battery;
        this.pos = pos;
        if (battery != null) for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++)
            addSlot(new net.neoforged.neoforge.items.SlotItemHandler(battery.tubeInventory(), i,
                    org.papiricoh.create_nuclearindustry.missile.gui.BatteryLayout.SLOT_X[i], org.papiricoh.create_nuclearindustry.missile.gui.BatteryLayout.SLOT_Y[i]));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 36 + col * 18, 154 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 36 + col * 18, 212));
    }

    public static InterceptorBatteryMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new InterceptorBatteryMenu(id, inventory,
                inventory.player.level().getBlockEntity(pos) instanceof InterceptorBatteryEntity battery ? battery : null, pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int tubes = InterceptorBatteryEntity.TUBES;
        if (index < tubes) {
            if (!moveItemStackTo(stack, tubes, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, tubes, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return battery != null && ServerPayloadGuard.stillValid(battery, player);
    }
}
