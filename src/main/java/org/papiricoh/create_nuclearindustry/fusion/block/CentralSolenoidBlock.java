package org.papiricoh.create_nuclearindustry.fusion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The tokamak's central solenoid: a ribbed column that joins up with the solenoid blocks along its
 * axis, capped only at its two ends.
 */
public class CentralSolenoidBlock extends RotatedPillarBlock {
    public static final MapCodec<CentralSolenoidBlock> CODEC = simpleCodec(CentralSolenoidBlock::new);
    public static final EnumProperty<Segment> SEGMENT = EnumProperty.create("segment", Segment.class);

    public enum Segment implements StringRepresentable {
        SINGLE, TOP, MIDDLE, BOTTOM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public CentralSolenoidBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SEGMENT, Segment.SINGLE));
    }

    @Override
    public MapCodec<? extends CentralSolenoidBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SEGMENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withSegment(state, context.getLevel(), context.getClickedPos());
    }

    /** Blocks set without placement context (commands, structures) join up too. */
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        BlockState joined = withSegment(state, level, pos);
        if (joined != state) {
            level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis() == state.getValue(AXIS) ? withSegment(state, level, pos) : state;
    }

    /**
     * The model's top end, after the blockstate turns it to lie along the axis: up, north for Z
     * (rotated 90 about X) and east for X (also 90 about Y).
     */
    public static Direction ahead(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Z -> Direction.NORTH;
            default -> Direction.UP;
        };
    }

    public static BlockState withSegment(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction ahead = ahead(axis);
        boolean front = joins(level.getBlockState(pos.relative(ahead)), axis);
        boolean back = joins(level.getBlockState(pos.relative(ahead.getOpposite())), axis);
        Segment segment = front && back ? Segment.MIDDLE : back ? Segment.TOP : front ? Segment.BOTTOM : Segment.SINGLE;
        return state.setValue(SEGMENT, segment);
    }

    private static boolean joins(BlockState neighbor, Direction.Axis axis) {
        return neighbor.getBlock() instanceof CentralSolenoidBlock && neighbor.getValue(AXIS) == axis;
    }
}
