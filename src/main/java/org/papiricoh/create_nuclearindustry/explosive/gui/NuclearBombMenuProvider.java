package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;

public class NuclearBombMenuProvider {
    private final BlockPos bombPos;

    public NuclearBombMenuProvider(BlockPos bombPos) {
        this.bombPos = bombPos;
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Level level = player.level();
        NuclearBombBlockEntity bomb = null;
        if (level.getBlockEntity(bombPos) instanceof NuclearBombBlockEntity bombBlockEntity) {
            bomb = bombBlockEntity;
        }
        return new NuclearBombMenu(containerId, playerInventory, bomb, bombPos);
    }
}
