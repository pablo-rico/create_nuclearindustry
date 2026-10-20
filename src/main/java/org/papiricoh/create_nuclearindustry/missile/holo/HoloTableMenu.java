package org.papiricoh.create_nuclearindustry.missile.holo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** A command table's screen: open while its player stands at the whole table. */
public class HoloTableMenu extends AbstractContainerMenu {
    public final BlockPos centre;

    public HoloTableMenu(int id, Inventory inventory, BlockPos centre) {
        super(HoloRegistry.MENU.get(), id);
        this.centre = centre;
    }

    public static HoloTableMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new HoloTableMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(centre) instanceof HoloTableBlockEntity table && table.near(player) && HoloTable.intact(player.level(), centre);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
