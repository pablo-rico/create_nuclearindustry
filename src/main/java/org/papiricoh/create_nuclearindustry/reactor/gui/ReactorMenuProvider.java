package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

public class ReactorMenuProvider {
    private final BlockPos reactorPos;
    private final Component displayName;

    public ReactorMenuProvider(BlockPos reactorPos, Component displayName) {
        this.reactorPos = reactorPos;
        this.displayName = displayName;
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Level level = player.level();
        ReactorBlockEntity reactor = null;

        if (level != null) {
            var blockEntity = level.getBlockEntity(reactorPos);

            if (blockEntity instanceof ReactorBlockEntity reactorBE) {
                reactor = reactorBE;
            }
        }

        return new ReactorControlMenu(containerId, playerInventory, reactor, reactorPos);
    }

    public Component getDisplayName() {
        return displayName;
    }

    public BlockPos getReactorPos() {
        return reactorPos;
    }
}
