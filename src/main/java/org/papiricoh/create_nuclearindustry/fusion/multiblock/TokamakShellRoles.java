package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;

/** Looks up the {@link TokamakShellRole} of a block state, or null if it does not belong in the shell. */
public final class TokamakShellRoles {
    private TokamakShellRoles() {}

    @Nullable
    public static TokamakShellRole roleOf(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof TokamakShellBlock part) {
            return part.tokamakRole(state);
        }
        // Plain blocks with no class of their own.
        if (block == AllNuclearBlocks.FUSION_ELECTROMAGNET.get()) {
            return TokamakShellRole.COIL;
        }
        if (block == AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()) {
            return TokamakShellRole.BLANKET;
        }
        if (block == AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get()
                || block == AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get()
                || block == AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get()) {
            return TokamakShellRole.VESSEL;
        }
        return null;
    }
}
