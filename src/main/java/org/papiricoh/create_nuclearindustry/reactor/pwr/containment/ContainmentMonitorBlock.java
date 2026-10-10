package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/**
 * Stands anywhere inside a containment building and measures it. Right-click to measure again
 * (and to clear a burst wall once it has been repaired); comparators read the pressure.
 */
public class ContainmentMonitorBlock extends HorizontalDirectionalBlock implements IBE<ContainmentMonitorBlockEntity> {
    public static final MapCodec<ContainmentMonitorBlock> CODEC = simpleCodec(ContainmentMonitorBlock::new);

    public ContainmentMonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, monitor -> monitor.inspect(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ContainmentMonitorBlockEntity monitor ? monitor.comparatorSignal() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ContainmentMonitorBlockEntity> getBlockEntityClass() {
        return ContainmentMonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ContainmentMonitorBlockEntity> getBlockEntityType() {
        return PwrRegistry.CONTAINMENT_MONITOR_ENTITY.get();
    }
}
