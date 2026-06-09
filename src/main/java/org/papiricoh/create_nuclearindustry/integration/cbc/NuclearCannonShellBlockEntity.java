package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBlockEntity;

public class NuclearCannonShellBlockEntity extends FuzedBlockEntity {
    public NuclearCannonShellBlockEntity(BlockPos pos, BlockState state) {
        super(CBCNuclearContent.NUCLEAR_CANNON_SHELL_BLOCK_ENTITY.get(), pos, state);
    }
}
