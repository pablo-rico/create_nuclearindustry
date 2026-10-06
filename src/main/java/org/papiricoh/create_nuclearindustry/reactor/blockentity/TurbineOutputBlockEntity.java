package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** Steam turbine controller: expands steam or heavy steam from a fission reactor. */
public class TurbineOutputBlockEntity extends AbstractTurbineControllerBlockEntity {

    public TurbineOutputBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.TURBINE_OUTPUT.get(), pos, state);
    }

    @Override
    public TurbineTier tier() {
        return TurbineTier.STEAM;
    }
}
