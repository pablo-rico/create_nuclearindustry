package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.turbine.TurbinePortBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

public class FusionTurbineFluidPortBlockEntity extends TurbinePortBlockEntity {

    public FusionTurbineFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_TURBINE_FLUID_PORT.get(), pos, state);
    }

    @Override
    protected TurbineTier tier() {
        return TurbineTier.PLASMA;
    }
}
