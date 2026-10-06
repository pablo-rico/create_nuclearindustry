package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.ap_shot.APShotProjectile;

/** Flies like CBC's AP shot with its own, heavier munition properties. */
public class DepletedUraniumAPShotProjectile extends APShotProjectile {
    public DepletedUraniumAPShotProjectile(EntityType<? extends DepletedUraniumAPShotProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public BlockState getRenderedBlockState() {
        return CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH);
    }
}
