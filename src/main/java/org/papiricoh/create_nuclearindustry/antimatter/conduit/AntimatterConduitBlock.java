package org.papiricoh.create_nuclearindustry.antimatter.conduit;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolders;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Antimatter Conduit: a thin evacuated glass tube ringed with magnets. It joins the conduits
 * and antimatter holders (traps, target stations) on its six sides; one boolean per side says
 * which, for the multipart model. A change next to it makes its network look again.
 */
public class AntimatterConduitBlock extends Block implements IBE<AntimatterConduitBlockEntity>, IWrenchable {
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CORE = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final Map<Integer, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        ARMS.put(Direction.NORTH, Block.box(5.5, 5.5, 0, 10.5, 10.5, 5.5));
        ARMS.put(Direction.SOUTH, Block.box(5.5, 5.5, 10.5, 10.5, 10.5, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5.5, 5.5, 5.5, 10.5, 10.5));
        ARMS.put(Direction.EAST, Block.box(10.5, 5.5, 5.5, 16, 10.5, 10.5));
        ARMS.put(Direction.DOWN, Block.box(5.5, 0, 5.5, 10.5, 5.5, 10.5));
        ARMS.put(Direction.UP, Block.box(5.5, 10.5, 5.5, 10.5, 16, 10.5));
    }

    public AntimatterConduitBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    /** Whether a conduit at {@code pos} joins what is on {@code side}. */
    public static boolean connects(BlockGetter level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        return level.getBlockState(next).getBlock() instanceof AntimatterConduitBlock || AntimatterHolders.isHolder(level, next);
    }

    public BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) {
            state = state.setValue(SIDES.get(side), connects(level, pos, side));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(SIDES.get(direction), connects(level, pos, direction));
    }

    /** Anything changing beside the conduit (a trap placed, a target broken) makes its network look again. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AntimatterConduitBlockEntity conduit) {
            conduit.invalidateNetwork();
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            ConduitNetwork.invalidateAround(level, pos);
            // Set by code (schematics, Ponder, tests) rather than placed: join what is already there.
            BlockState joined = withConnections(level, pos, state);
            if (joined != state) {
                level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ConduitNetwork.invalidateAround(level, pos);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            if (state.getValue(SIDES.get(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return SHAPES.computeIfAbsent(mask, AntimatterConduitBlock::shape);
    }

    private static VoxelShape shape(int mask) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if ((mask & 1 << side.ordinal()) != 0) {
                shape = Shapes.or(shape, ARMS.get(side));
            }
        }
        return shape;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public Class<AntimatterConduitBlockEntity> getBlockEntityClass() {
        return AntimatterConduitBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AntimatterConduitBlockEntity> getBlockEntityType() {
        return AntimatterRegistry.CONDUIT_ENTITY.get();
    }
}
