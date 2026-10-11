package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block that can sit on the accelerator's beamline. */
public interface BeamElementBlock {

    BeamElementRole beamRole(BlockState state);

    /** The axis the beam runs along through this block, or null if it takes the beam from any side. */
    @Nullable
    default Direction.Axis beamAxis(BlockState state) {
        return null;
    }

    /** Where a bend sends the beam; only asked of {@link BeamElementRole#BEND}. */
    default Direction beamExit(BlockState state) {
        return Direction.NORTH;
    }
}
