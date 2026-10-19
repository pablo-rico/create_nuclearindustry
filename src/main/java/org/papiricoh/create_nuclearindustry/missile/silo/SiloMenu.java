package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

public class SiloMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public SiloMenu(int id, Inventory inv, BlockPos pos) { super(SiloRegistry.MENU.get(), id); this.pos = pos; }
    public static SiloMenu read(int id, Inventory inv, RegistryFriendlyByteBuf buf) { return new SiloMenu(id, inv, buf.readBlockPos()); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof SiloControllerEntity c && ServerPayloadGuard.stillValid(c, player);
    }
}
