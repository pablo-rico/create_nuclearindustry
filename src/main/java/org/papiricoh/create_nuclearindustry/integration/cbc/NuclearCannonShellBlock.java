package org.papiricoh.create_nuclearindustry.integration.cbc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.SimpleShellBlock;

public class NuclearCannonShellBlock extends SimpleShellBlock {
    public static final MapCodec<NuclearCannonShellBlock> CODEC = simpleCodec(NuclearCannonShellBlock::new);

    public NuclearCannonShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NuclearCannonShellBlock> codec() {
        return CODEC;
    }

    @Override
    public EntityType<?> getAssociatedEntityType() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_PROJECTILE.get();
    }

    @Override
    public BlockEntityType<? extends FuzedBlockEntity> getBlockEntityType() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_BLOCK_ENTITY.get();
    }

    @Override
    public boolean isBaseFuze() {
        return false;
    }
}
