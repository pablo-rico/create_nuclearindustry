package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A filtered containment vent, built into the wall: a scrubber and filter bed behind a valve. With a
 * redstone signal the valve opens and the building's overpressure bleeds out through the filters,
 * which keep all but a small share of the radioactivity. Shut, it is as strong as concrete.
 */
public class FilteredVentBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public FilteredVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(OPEN, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(OPEN)) {
            level.setBlock(pos, state.setValue(OPEN, powered), UPDATE_CLIENTS);
        }
    }
}
