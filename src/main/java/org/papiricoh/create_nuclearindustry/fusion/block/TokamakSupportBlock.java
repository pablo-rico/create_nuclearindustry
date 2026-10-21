package org.papiricoh.create_nuclearindustry.fusion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;

/**
 * A girder that holds a tokamak up off the floor. Purely structural, never part of the multiblock:
 * stacked supports form one pillar, with a foot on the lowest and a saddle on one that meets the ring.
 */
public class TokamakSupportBlock extends Block {
    public static final MapCodec<TokamakSupportBlock> CODEC = simpleCodec(TokamakSupportBlock::new);
    public static final BooleanProperty FOOT = BooleanProperty.create("foot");
    public static final BooleanProperty CRADLE = BooleanProperty.create("cradle");
    private static final VoxelShape PILLAR = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape FOOT_SHAPE = Block.box(1, 0, 1, 15, 2, 15);
    private static final VoxelShape CRADLE_SHAPE = Block.box(1, 14, 1, 15, 16, 15);

    public TokamakSupportBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FOOT, true).setValue(CRADLE, false));
    }

    @Override
    protected MapCodec<? extends TokamakSupportBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FOOT, CRADLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** Blocks set without placement context (commands, structures) join up too. */
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        BlockState joined = connect(state, level, pos);
        if (joined != state) {
            level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis() == Direction.Axis.Y ? connect(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = PILLAR;
        if (state.getValue(FOOT)) {
            shape = Shapes.or(shape, FOOT_SHAPE);
        }
        if (state.getValue(CRADLE)) {
            shape = Shapes.or(shape, CRADLE_SHAPE);
        }
        return shape;
    }

    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        boolean foot = !(level.getBlockState(pos.below()).getBlock() instanceof TokamakSupportBlock);
        return state.setValue(FOOT, foot).setValue(CRADLE, carries(level.getBlockState(pos.above())));
    }

    /** Whether the block above is part of a tokamak's ring. */
    private static boolean carries(BlockState above) {
        Block block = above.getBlock();
        return block instanceof TokamakHullBlock || block instanceof TokamakShellBlock || block instanceof FusionReactorControllerBlock;
    }
}
