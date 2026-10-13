package org.papiricoh.create_nuclearindustry.spentfuel.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

/**
 * Spent fuel pool rack: sits at the bottom of a pool, waterlogged, holding nine assemblies.
 * Fresh fuel under water lights it up blue.
 */
public class SpentFuelRackBlock extends SpentFuelStorageBlock<SpentFuelRackBlockEntity> implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<RackLoad> LOAD = EnumProperty.create("load", RackLoad.class);
    public static final MapCodec<SpentFuelRackBlock> CODEC = simpleCodec(SpentFuelRackBlock::new);

    public SpentFuelRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false).setValue(LOAD, RackLoad.EMPTY));
    }

    /** Light level for {@link Properties#lightLevel}: the Cherenkov glow. */
    public static int lightLevel(BlockState state) {
        return state.getValue(LOAD) == RackLoad.GLOWING ? 9 : 0;
    }

    @Override
    protected MapCodec<? extends SpentFuelRackBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, LOAD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, water);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public Class<SpentFuelRackBlockEntity> getBlockEntityClass() {
        return SpentFuelRackBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpentFuelRackBlockEntity> getBlockEntityType() {
        return SpentFuelRegistry.SPENT_FUEL_RACK_ENTITY.get();
    }
}
