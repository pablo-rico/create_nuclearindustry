package org.papiricoh.create_nuclearindustry.antimatter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.multiblock.MasterProxy;

/**
 * Finds the {@link AntimatterHolder} at a position: its own block entity, or the master of the
 * multi-block trap it is a part of.
 */
public final class AntimatterHolders {
    private AntimatterHolders() {}

    @Nullable
    public static AntimatterHolder at(BlockGetter level, BlockPos pos) {
        BlockPos at = master(level, pos);
        return level.getBlockEntity(at) instanceof AntimatterHolder holder ? holder : null;
    }

    /** The position that answers for {@code pos}: the master of a multi-block, or {@code pos} itself. */
    public static BlockPos master(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MasterProxy proxy ? proxy.masterPos(state, pos) : pos;
    }

    /** Whether a conduit beside {@code pos} would find a holder there. */
    public static boolean isHolder(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MasterProxy || level.getBlockEntity(pos) instanceof AntimatterHolder;
    }
}
