package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

/**
 * Helium compressor in the wall of a cryogenic plant: a shaft enters through its outward face
 * ({@link #FACING}) and turns the impeller behind the grille.
 */
public class CryoplantCompressorBlock extends KineticBlock implements IBE<CryoplantCompressorBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final MapCodec<CryoplantCompressorBlock> CODEC = simpleCodec(CryoplantCompressorBlock::new);

    public CryoplantCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends CryoplantCompressorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    /** The shaft side faces the player, who places it from outside the plant. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    /**
     * Turning a compressor with the wrench may point its shaft into the plant: the plant checks
     * itself again on its next tick, once the new state is in place.
     */
    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            for (CryoplantControllerBlockEntity controller : MultiblockRegistry.affecting(context.getLevel(), context.getClickedPos(),
                    CryoplantControllerBlockEntity.class)) {
                controller.requestStructureRevalidation(context.getPlayer());
            }
        }
        return Block.updateFromNeighbourShapes(newState, context.getLevel(), context.getClickedPos());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<CryoplantCompressorBlockEntity> getBlockEntityClass() {
        return CryoplantCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CryoplantCompressorBlockEntity> getBlockEntityType() {
        return CryoRegistry.CRYOPLANT_COMPRESSOR_ENTITY.get();
    }
}
