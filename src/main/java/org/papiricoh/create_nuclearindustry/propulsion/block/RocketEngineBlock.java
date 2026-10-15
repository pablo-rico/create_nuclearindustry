package org.papiricoh.create_nuclearindustry.propulsion.block;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.RocketEngineBlockEntity;

/**
 * The controller at the front of a nuclear engine. {@code FACING} points along the engine towards its
 * nozzle, the way the exhaust leaves: placed facing away from the player, who then lays the segments
 * and the nozzle behind it. Redstone sets the throttle; comparators read the thrust.
 */
public abstract class RocketEngineBlock<T extends RocketEngineBlockEntity> extends DirectionalBlock implements IBE<T> {
    protected RocketEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, RocketEngineBlockEntity::requestScan);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(RocketEngineBlockEntity::comparatorSignal).orElse(0);
    }
}
