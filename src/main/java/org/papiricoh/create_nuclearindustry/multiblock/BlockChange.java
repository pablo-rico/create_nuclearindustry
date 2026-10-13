package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block change near a multiblock.
 *
 * @param state   the state now at {@code pos}, or the state about to be removed when {@code removed}
 * @param removed the block is being broken or blown up and has not been replaced yet
 */
public record BlockChange(BlockPos pos, BlockState state, boolean removed) {

    public static BlockChange placed(BlockPos pos, BlockState state) {
        return new BlockChange(pos, state, false);
    }

    public static BlockChange removed(BlockPos pos, BlockState state) {
        return new BlockChange(pos, state, true);
    }

    public boolean isMultiblockPart() {
        return state.is(MultiblockTags.MULTIBLOCK_PARTS);
    }
}
