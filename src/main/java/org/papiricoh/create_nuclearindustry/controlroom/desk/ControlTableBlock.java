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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;

/**
 * A control table: a cabinet with a flat top, flush with the back rail of a desk, holding four
 * modules in two rows. FACING is the side the operator stands on. Tables side by side join (LEFT and
 * RIGHT drop the end panels); next to a desk or wall panel they share its console but keep their ends.
 */
public class ControlTableBlock extends ModuleSurfaceBlock {
    public static final MapCodec<ControlTableBlock> CODEC = simpleCodec(ControlTableBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, SurfaceGeometry.TABLE_TOP, 16);

    public ControlTableBlock(Properties properties) {
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
        return ModuleSurface.TABLE;
    }

    @Override
    public BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(LEFT, sameRow(state, level, pos, true)).setValue(RIGHT, sameRow(state, level, pos, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return connect(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntityType<? extends ControlDeskBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.MODULE_SURFACE_ENTITY.get();
    }
}
