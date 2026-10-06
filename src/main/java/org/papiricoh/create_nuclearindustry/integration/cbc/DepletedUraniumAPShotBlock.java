package org.papiricoh.create_nuclearindustry.integration.cbc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.ap_shot.APShotBlock;

/** A Create Big Cannons armour-piercing shot with a depleted uranium penetrator. */
public class DepletedUraniumAPShotBlock extends APShotBlock {
    public static final MapCodec<DepletedUraniumAPShotBlock> CODEC = simpleCodec(DepletedUraniumAPShotBlock::new);

    public DepletedUraniumAPShotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public EntityType<? extends AbstractBigCannonProjectile> getAssociatedEntityType() {
        return CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_PROJECTILE.get();
    }

    /** CBC's own projectile block entity type does not list this block, so it has its own. */
    @Override
    public BlockEntityType<? extends BigCannonProjectileBlockEntity> getBlockEntityType() {
        return CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_BLOCK_ENTITY.get();
    }
}
