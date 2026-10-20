package org.papiricoh.create_nuclearindustry.missile.radar;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A radar scope: a screen that faces the player who places it, and joins its neighbours into one bigger screen. */
public class RadarScopeBlock extends HorizontalDirectionalBlock implements IBE<RadarScopeBlockEntity> {
    public static final MapCodec<RadarScopeBlock> CODEC = simpleCodec(RadarScopeBlock::new);

    public RadarScopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
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
    public Class<RadarScopeBlockEntity> getBlockEntityClass() {
        return RadarScopeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RadarScopeBlockEntity> getBlockEntityType() {
        return RadarRegistry.SCOPE_ENTITY.get();
    }
}
