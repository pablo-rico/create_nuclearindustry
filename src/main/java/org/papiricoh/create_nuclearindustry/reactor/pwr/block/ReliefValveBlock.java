package org.papiricoh.create_nuclearindustry.reactor.pwr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The relief valve on top of a pressurizer. The reactor opens it past the relief pressure and shows
 * it as {@link #OPEN}; a redstone signal shuts the block valve under it, which stops a stuck valve
 * from draining the loop, and also stops it relieving at all.
 */
public class ReliefValveBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public ReliefValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    /** A redstone signal shuts the block valve under the relief valve. */
    public static boolean isolated(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparators read 15 while the valve is open. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(OPEN) ? 15 : 0;
    }
}
