package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;

import java.util.Map;

/**
 * A control desk: a cabinet with a sloping top that holds four control modules, or two on a corner,
 * one on each arm (see {@link DeskShape#hasSlot}). FACING is the side the operator stands on; the
 * top rises away from it. Desks side by side join (LEFT and RIGHT drop the end panels), and desks
 * meeting at right angles turn a corner like stairs do. Slots and consoles work as on every
 * {@link ModuleSurfaceBlock}.
 */
public class ControlDeskBlock extends ModuleSurfaceBlock {
    public static final MapCodec<ControlDeskBlock> CODEC = simpleCodec(ControlDeskBlock::new);
    public static final EnumProperty<DeskShape> SHAPE = EnumProperty.create("shape", DeskShape.class);
    // Native (operator to the north) outlines: the slope as steps that hug the surface.
    private static final VoxelShape STRAIGHT_NATIVE = Shapes.or(Block.box(0, 0, 0, 16, 9.5, 16),
            Block.box(0, 9.5, 4, 16, 11, 16), Block.box(0, 11, 7.5, 16, 12.5, 16),
            Block.box(0, 12.5, 11, 16, 14, 16), Block.box(0, 9, 13.75, 16, 16, 16));
    private static final VoxelShape INNER_NATIVE = Shapes.or(STRAIGHT_NATIVE,
            Block.box(4, 9.5, 0, 16, 11, 16), Block.box(7.5, 11, 0, 16, 12.5, 16),
            Block.box(11, 12.5, 0, 16, 14, 16), Block.box(13.75, 9, 0, 16, 16, 16));
    private static final VoxelShape OUTER_NATIVE = Shapes.or(Block.box(0, 0, 0, 16, 9.5, 16),
            Block.box(4, 9.5, 4, 16, 11, 16), Block.box(7.5, 11, 7.5, 16, 12.5, 16),
            Block.box(11, 12.5, 11, 16, 14, 16), Block.box(13.75, 9, 13.75, 16, 16, 16));
    private static final Map<BlockState, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    public ControlDeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, DeskShape.STRAIGHT)
                .setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, LEFT, RIGHT);
    }

    @Override
    public ModuleSurface surface() {
        return ModuleSurface.DESK;
    }

    @Override
    public BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return withConnections(state, level, pos);
    }

    // ------------------------------------------------------------------ shape

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? withConnections(state, level, pos) : state;
    }

    /** Works out SHAPE, then whether each end is closed by a neighbour continuing the console. */
    public static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        DeskShape shape = shapeOf(state, level, pos);
        Direction facing = state.getValue(FACING);
        return state.setValue(SHAPE, shape)
                .setValue(LEFT, joined(level, pos, shape.leftEnd(facing), shape.leftOperator(facing)))
                .setValue(RIGHT, joined(level, pos, shape.rightEnd(facing), shape.rightOperator(facing)));
    }

    /** The end towards {@code side} is joined when the desk there has an end facing back, operated from the same side. */
    private static boolean joined(BlockGetter level, BlockPos pos, Direction side, Direction operator) {
        BlockState neighbor = level.getBlockState(pos.relative(side));
        if (!isDesk(neighbor)) {
            return false;
        }
        DeskShape shape = neighbor.getValue(SHAPE);
        Direction facing = neighbor.getValue(FACING);
        return (shape.leftEnd(facing) == side.getOpposite() && shape.leftOperator(facing) == operator)
                || (shape.rightEnd(facing) == side.getOpposite() && shape.rightOperator(facing) == operator);
    }

    /** As stairs: a desk behind or in front facing across turns this one into an outer or inner corner. */
    static DeskShape shapeOf(BlockState state, BlockGetter level, BlockPos pos) {
        Direction back = state.getValue(FACING).getOpposite();
        BlockState behind = level.getBlockState(pos.relative(back));
        if (isDesk(behind)) {
            Direction behindBack = behind.getValue(FACING).getOpposite();
            if (behindBack.getAxis() != back.getAxis() && canTakeShape(state, level, pos, behindBack.getOpposite())) {
                return behindBack == back.getCounterClockWise() ? DeskShape.OUTER_LEFT : DeskShape.OUTER_RIGHT;
            }
        }
        BlockState front = level.getBlockState(pos.relative(back.getOpposite()));
        if (isDesk(front)) {
            Direction frontBack = front.getValue(FACING).getOpposite();
            if (frontBack.getAxis() != back.getAxis() && canTakeShape(state, level, pos, frontBack)) {
                return frontBack == back.getCounterClockWise() ? DeskShape.INNER_LEFT : DeskShape.INNER_RIGHT;
            }
        }
        return DeskShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        BlockState neighbor = level.getBlockState(pos.relative(side));
        return !isDesk(neighbor) || neighbor.getValue(FACING) != state.getValue(FACING);
    }

    static boolean isDesk(BlockState state) {
        return state.getBlock() instanceof ControlDeskBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.computeIfAbsent(state, ControlDeskBlock::shapeFor);
    }

    private static VoxelShape shapeFor(BlockState state) {
        DeskShape shape = state.getValue(SHAPE);
        VoxelShape nativeShape = shape == DeskShape.STRAIGHT ? STRAIGHT_NATIVE : shape.isInner() ? INNER_NATIVE : OUTER_NATIVE;
        int yaw = (DeskGeometry.modelYaw(state) + shape.bodyExtraRotation()) % 360;
        return rotateClockwise(nativeShape, yaw / 90);
    }

        @Override
    public BlockEntityType<? extends ControlDeskBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.REACTOR_CONTROL_PANEL.get();
    }
}
