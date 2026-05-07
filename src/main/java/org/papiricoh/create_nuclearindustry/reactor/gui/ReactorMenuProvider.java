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

        System.out.println("[ReactorMenuProvider] Creating menu at " + reactorPos + ", isClient: " + (level != null && level.isClientSide));

        if (level != null) {
            var blockEntity = level.getBlockEntity(reactorPos);
            System.out.println("[ReactorMenuProvider] BlockEntity type: " + (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null"));

            if (blockEntity instanceof ReactorBlockEntity reactorBE) {
                reactor = reactorBE;
                System.out.println("[ReactorMenuProvider] Reactor found! isFormed: " + reactorBE.isFormed());
            } else {
                System.out.println("[ReactorMenuProvider] BlockEntity is not ReactorBlockEntity!");
            }
        } else {
            System.out.println("[ReactorMenuProvider] Level is null!");
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
