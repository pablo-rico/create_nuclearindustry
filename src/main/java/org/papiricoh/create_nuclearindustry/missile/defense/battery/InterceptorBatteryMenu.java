package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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
    }

    public static InterceptorBatteryMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new InterceptorBatteryMenu(id, inventory,
                inventory.player.level().getBlockEntity(pos) instanceof InterceptorBatteryEntity battery ? battery : null, pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return battery != null && ServerPayloadGuard.stillValid(battery, player);
    }
}
