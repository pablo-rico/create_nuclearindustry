package org.papiricoh.create_nuclearindustry.fusion.cryo.well;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;

/**
 * The helium well: a wellhead that draws traces of helium gas out of the rock while a shaft turns
 * it. Its gauge front ({@link #FACING}) looks at the player who placed it; the shaft goes in
 * through the back, so a motor or shaft line can sit behind it while tanks or pipes take the gas
 * from any other side.
 */
public class HeliumWellBlock extends KineticBlock implements IBE<HeliumWellBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<HeliumWellBlock> CODEC = simpleCodec(HeliumWellBlock::new);

    public HeliumWellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HeliumWellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The face the shaft enters through: the back, opposite the gauge. */
    public static Direction shaftSide(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING).getOpposite() : Direction.SOUTH;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == shaftSide(state);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Turned with a wrench: the shaft side moves, so cached fluid handlers on the old sides are dropped. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this) && shaftSide(oldState) != shaftSide(state)) {
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    public Class<HeliumWellBlockEntity> getBlockEntityClass() {
        return HeliumWellBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeliumWellBlockEntity> getBlockEntityType() {
        return CryoRegistry.HELIUM_WELL_ENTITY.get();
    }
}
