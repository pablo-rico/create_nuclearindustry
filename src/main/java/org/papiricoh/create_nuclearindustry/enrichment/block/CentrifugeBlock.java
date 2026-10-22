package org.papiricoh.create_nuclearindustry.enrichment.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

import java.util.function.Supplier;

/**
 * One stage of a gas centrifuge cascade. Centrifuges standing side by side with the same facing
 * draw the cascade header between them: LEFT is the side counter-clockwise from the front (west
 * when facing north), RIGHT the clockwise one. Only looks; the stages still trade through pipes.
 */
public class CentrifugeBlock extends KineticProcessingBlock {
    public static final MapCodec<CentrifugeBlock> CODEC = simpleCodec(CentrifugeBlock::new);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    public CentrifugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends CentrifugeBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<? extends BlockEntityType<CentrifugeBlockEntity>> blockEntityType() {
        return AllNuclearEntities.CENTRIFUGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEFT, RIGHT);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withCascade(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return direction.getAxis().isHorizontal() ? withCascade(updated, level, pos) : updated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored
                : mirrored.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    /** The state with its header joints recomputed from the two side neighbours. */
    public static BlockState withCascade(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof CentrifugeBlock)) {
            return state;
        }
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, joins(level, pos, facing, facing.getCounterClockWise()))
                .setValue(RIGHT, joins(level, pos, facing, facing.getClockWise()));
    }

    private static boolean joins(BlockGetter level, BlockPos pos, Direction facing, Direction side) {
        BlockState neighbour = level.getBlockState(pos.relative(side));
        return neighbour.getBlock() instanceof CentrifugeBlock && neighbour.getValue(FACING) == facing;
    }
}
