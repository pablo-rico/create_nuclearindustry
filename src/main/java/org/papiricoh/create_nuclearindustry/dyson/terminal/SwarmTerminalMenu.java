package org.papiricoh.create_nuclearindustry.dyson.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** The swarm terminal's screen, for the player standing at it; the swarm itself comes to the client by its own payload. */
public class SwarmTerminalMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    @Nullable
    public final SwarmTerminalBlockEntity terminal;

    public SwarmTerminalMenu(int id, Inventory inventory, @Nullable SwarmTerminalBlockEntity terminal, BlockPos pos) {
        super(DysonRegistry.TERMINAL_MENU.get(), id);
        this.pos = pos;
        this.terminal = terminal;
    }

    public static SwarmTerminalMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new SwarmTerminalMenu(id, inventory,
                inventory.player.level().getBlockEntity(pos) instanceof SwarmTerminalBlockEntity found ? found : null, pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return ServerPayloadGuard.stillValid(terminal, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
