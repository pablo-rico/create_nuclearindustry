package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.world.level.block.state.BlockState;

/** A block that may sit in the tokamak shell, and what it does there. */
public interface TokamakShellBlock {
    TokamakShellRole tokamakRole(BlockState state);
}
