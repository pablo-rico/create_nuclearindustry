package org.papiricoh.create_nuclearindustry.turbine.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;

/** Hides the rotor blocks of a formed turbine (its renderer draws one big rotor instead). */
public final class TurbineRotorFormed {

    private TurbineRotorFormed() {}

    public static void apply(Level level, TurbineStructure structure, boolean formed) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos pos : structure.rotors) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TurbineRotorBlock && state.getValue(TurbineRotorBlock.FORMED) != formed) {
                // Clients only: no neighbour updates, so the multiblock does not revalidate itself.
                level.setBlock(pos, state.setValue(TurbineRotorBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }
}
