package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block placed as a unit of several blocks (a medium or large Penning trap) whose parts all answer
 * through one master block, the only one with a block entity: capabilities, goggles, linkers and
 * conduits look the master up with {@link #masterPos}.
 */
public interface MasterProxy {

    /** The master of the part {@code state} at {@code pos}; {@code pos} itself for the master. */
    BlockPos masterPos(BlockState state, BlockPos pos);
}
