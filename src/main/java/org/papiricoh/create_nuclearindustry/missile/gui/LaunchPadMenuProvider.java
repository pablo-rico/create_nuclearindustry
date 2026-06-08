package org.papiricoh.create_nuclearindustry.missile.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

public class LaunchPadMenuProvider {
    private final BlockPos padPos;

    public LaunchPadMenuProvider(BlockPos padPos) {
        this.padPos = padPos;
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Level level = player.level();
        LaunchPadBlockEntity pad = null;
        if (level.getBlockEntity(padPos) instanceof LaunchPadBlockEntity padBlockEntity) {
            pad = padBlockEntity;
        }
        return new LaunchPadMenu(containerId, playerInventory, pad, padPos);
    }
}
