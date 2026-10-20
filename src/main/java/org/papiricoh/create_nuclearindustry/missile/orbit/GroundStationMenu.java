package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** A code editor for the station the player is standing beside. */
public class GroundStationMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final GroundStationBlockEntity station;
    public GroundStationMenu(int id, Inventory inventory, GroundStationBlockEntity station, BlockPos pos) {
        super(OrbitRegistry.MENU.get(), id);
        this.pos = pos; this.station = station;
    }
    public static GroundStationMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new GroundStationMenu(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof GroundStationBlockEntity station ? station : null, pos);
    }
    @Override public boolean stillValid(Player player) { return ServerPayloadGuard.stillValid(station, player) && ServerPayloadGuard.check(player, pos); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
