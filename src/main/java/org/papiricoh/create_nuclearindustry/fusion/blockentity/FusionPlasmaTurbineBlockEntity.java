package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** Plasma turbine controller: expands plasma steam from a tokamak. */
public class FusionPlasmaTurbineBlockEntity extends AbstractTurbineControllerBlockEntity {

    public FusionPlasmaTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_PLASMA_TURBINE.get(), pos, state);
    }

    @Override
    public TurbineTier tier() {
        return TurbineTier.PLASMA;
    }
}
