package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;

import java.util.EnumMap;
import java.util.Map;

/**
 * A wall control panel: a plate hung on the face of a solid block, holding four modules upright in
 * two rows. FACING is the side the operator stands on, away from the wall. Panels side by side join
 * (LEFT and RIGHT drop the end trims), and panels on one wall, beside or above each other, make one
 * console. A panel whose wall goes falls off with its modules.
 */
public class WallControlPanelBlock extends ModuleSurfaceBlock {
    public static final MapCodec<WallControlPanelBlock> CODEC = simpleCodec(WallControlPanelBlock::new);
    private static final VoxelShape NATIVE = Block.box(0, 0, SurfaceGeometry.WALL_FACE, 16, 16, 16);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int yaw = ((int) facing.toYRot() + 180) % 360;
            SHAPES.put(facing, rotateClockwise(NATIVE, yaw / 90));
        }
    }

    public WallControlPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    @Override
    public ModuleSurface surface() {
        return ModuleSurface.WALL;
    }

    @Override
    public BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(LEFT, sameRow(state, level, pos, true)).setValue(RIGHT, sameRow(state, level, pos, false));
    }

    /** Hangs on a block face that can hold it, like a wall sign. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction looking : context.getNearestLookingDirections()) {
            if (looking.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return connect(state, context.getLevel(), context.getClickedPos());
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return direction.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntityType<? extends ControlDeskBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.MODULE_SURFACE_ENTITY.get();
    }
}
