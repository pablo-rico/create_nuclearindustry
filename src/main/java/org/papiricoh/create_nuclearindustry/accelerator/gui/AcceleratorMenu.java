package org.papiricoh.create_nuclearindustry.accelerator.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * Accelerator control panel. It holds no slots: the screen reads the controller's synced
 * {@link org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings} and sends its buttons as
 * {@link AcceleratorControlPayload}s, which the server validates.
 */
public class AcceleratorMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public AcceleratorMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(AcceleratorRegistry.MENU.get(), containerId);
        this.pos = pos;
    }

    public static AcceleratorMenu read(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new AcceleratorMenu(containerId, inventory, buf.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public AcceleratorControllerBlockEntity controller(Level level) {
        return level.getBlockEntity(pos) instanceof AcceleratorControllerBlockEntity controller ? controller : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return ServerPayloadGuard.stillValid(controller(player.level()), player);
    }
}
