package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.turbine.TurbinePortBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

public class TurbineFluidPortBlockEntity extends TurbinePortBlockEntity {

    public TurbineFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.TURBINE_FLUID_PORT.get(), pos, state);
    }

    @Override
    protected TurbineTier tier() {
        return TurbineTier.STEAM;
    }
}
