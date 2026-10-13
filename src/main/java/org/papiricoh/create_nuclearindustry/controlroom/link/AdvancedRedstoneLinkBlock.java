package org.papiricoh.create_nuclearindustry.controlroom.link;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;

import java.util.EnumMap;
import java.util.Map;

/**
 * An Advanced Redstone Link: a Create Redstone Link tuned by a four-character code instead of two
 * items. Placed on a block face, FACING away from it. A transmitter sends the strongest redstone
 * signal around it on its code; a receiver puts out the strongest signal sent on its code within
 * Create's link range, into the block it sits on as well. Click it to type the code; sneak-click
 * with an empty hand to switch between transmitting and receiving, as on Create's link.
 */
public class AdvancedRedstoneLinkBlock extends DirectionalBlock implements IBE<AdvancedRedstoneLinkBlockEntity> {
    public static final MapCodec<AdvancedRedstoneLinkBlock> CODEC = simpleCodec(AdvancedRedstoneLinkBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, Block.box(2, 0, 2, 14, 3, 14));
        SHAPES.put(Direction.DOWN, Block.box(2, 13, 2, 14, 16, 14));
        SHAPES.put(Direction.NORTH, Block.box(2, 2, 13, 14, 14, 16));
        SHAPES.put(Direction.SOUTH, Block.box(2, 2, 0, 14, 14, 3));
        SHAPES.put(Direction.EAST, Block.box(0, 2, 2, 3, 14, 14));
        SHAPES.put(Direction.WEST, Block.box(13, 2, 2, 16, 14, 14));
    }

    public AdvancedRedstoneLinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(RECEIVER, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, RECEIVER);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** Sits on a face that can hold it, as Create's link does. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, AdvancedRedstoneLinkBlockEntity::toggleMode);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            org.papiricoh.create_nuclearindustry.controlroom.link.client.AdvancedLinkClient.openScreen(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide && !state.getValue(RECEIVER)) {
            withBlockEntityDo(level, pos, be -> be.transmit(powerAround(level, pos)));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && !state.getValue(RECEIVER) && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(RECEIVER)) {
            withBlockEntityDo(level, pos, be -> be.transmit(powerAround(level, pos)));
        }
    }

    /** The strongest redstone signal reaching the link from any side, as Create's transmitter reads it. */
    static int powerAround(Level level, BlockPos pos) {
        int power = 0;
        for (Direction direction : Direction.values()) {
            power = Math.max(power, level.getSignal(pos.relative(direction), direction));
        }
        return power;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return state.getValue(RECEIVER);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(RECEIVER)) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof AdvancedRedstoneLinkBlockEntity link ? link.receivedSignal() : 0;
    }

    /** A receiver powers the block it sits on, so it can drive whatever is around that block. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING) ? getSignal(state, level, pos, direction) : 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        IBE.onRemove(state, level, pos, newState);
        if (!state.is(newState.getBlock()) && state.getValue(RECEIVER)) {
            level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
        }
    }

    @Override
    public Class<AdvancedRedstoneLinkBlockEntity> getBlockEntityClass() {
        return AdvancedRedstoneLinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AdvancedRedstoneLinkBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.ADVANCED_LINK_ENTITY.get();
    }
}
