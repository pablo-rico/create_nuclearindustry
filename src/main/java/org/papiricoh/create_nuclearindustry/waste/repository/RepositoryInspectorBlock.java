package org.papiricoh.create_nuclearindustry.waste.repository;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/** A survey panel for a waste repository; its screen faces whoever placed it. */
public class RepositoryInspectorBlock extends HorizontalDirectionalBlock implements IBE<RepositoryInspectorBlockEntity> {
    public static final MapCodec<RepositoryInspectorBlock> CODEC = simpleCodec(RepositoryInspectorBlock::new);

    public RepositoryInspectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        RepositoryInspectorBlockEntity inspector = getBlockEntity(level, pos);
        return inspector == null ? 0 : inspector.signal();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<RepositoryInspectorBlockEntity> getBlockEntityClass() {
        return RepositoryInspectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RepositoryInspectorBlockEntity> getBlockEntityType() {
        return WasteRegistry.REPOSITORY_INSPECTOR_ENTITY.get();
    }
}
