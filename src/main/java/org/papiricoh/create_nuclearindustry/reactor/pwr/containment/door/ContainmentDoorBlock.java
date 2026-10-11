package org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.OptionalDouble;

/**
 * A two-block-high steel door for the containment wall. Shut, each half is a full block, so the
 * containment counts it as wall; open, only its edge is left in the doorway, to click it shut by,
 * and the containment is open. It opens like Create's sliding doors
 * (by hand, by redstone, two side by side as a pair) but heavier: it pulls out of its frame and
 * slides sideways along the wall ({@link ContainmentDoorMotion}), and it stays shut while the
 * containment behind it is under pressure ({@link ContainmentDoorInterlock}).
 *
 * <p>While it moves the block model is hidden ({@link #VISIBLE} false) and the block entity of the
 * lower half draws the door; the logic follows Create's {@code SlidingDoorBlock}.
 */
public class ContainmentDoorBlock extends DoorBlock implements IBE<ContainmentDoorBlockEntity>, IWrenchable, RadiationRay.Passable {
    public static final BooleanProperty VISIBLE = BooleanProperty.create("visible");
    public static final BlockSetType SET_TYPE = new BlockSetType("create_nuclearindustry:containment", false, false, false,
            BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.NETHERITE_BLOCK, SoundEvents.IRON_DOOR_CLOSE,
            SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    public ContainmentDoorBlock(Properties properties) {
        super(SET_TYPE, properties);
        registerDefaultState(defaultBlockState().setValue(VISIBLE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(VISIBLE));
    }

    // ------------------------------------------------------------------ shape: a full block when shut, an edge when open

    /** Open: the edge left in the doorway, sticking out of the wall, by [facing][hinge]. */
    private static final VoxelShape[][] OPEN_SHAPES = new VoxelShape[4][2];
    /** The same edge cut to its own block, which is what entities bump into. */
    private static final VoxelShape[][] OPEN_COLLISION = new VoxelShape[4][2];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                Direction slide = slideDirection(facing, hinge);
                float[] across = ContainmentDoorMotion.edgeAcross(slide.getAxisDirection().getStep());
                float[] through = ContainmentDoorMotion.edgeThrough(facing.getOpposite().getAxisDirection().getStep());
                OPEN_SHAPES[facing.get2DDataValue()][hinge.ordinal()] = box(slide.getAxis(), across, through);
                OPEN_COLLISION[facing.get2DDataValue()][hinge.ordinal()] = box(slide.getAxis(), across,
                        new float[]{Math.max(0, through[0]), Math.min(1, through[1])});
            }
        }
    }

    private static VoxelShape box(Direction.Axis slideAxis, float[] across, float[] through) {
        return slideAxis == Direction.Axis.X
                ? Shapes.box(across[0], 0, through[0], across[1], 1, through[1])
                : Shapes.box(through[0], 0, across[0], through[1], 1, across[1]);
    }

    /** Which way the door slides as it opens: along the wall, away from where a partner door would stand. */
    public static Direction slideDirection(Direction facing, DoorHingeSide hinge) {
        Direction slide = facing.getClockWise();
        return hinge == DoorHingeSide.LEFT ? slide.getOpposite() : slide;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN)
                ? OPEN_SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(HINGE).ordinal()]
                : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN)
                ? OPEN_COLLISION[state.getValue(FACING).get2DDataValue()][state.getValue(HINGE).ordinal()]
                : Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND && state.getValue(OPEN);
    }

    @Override
    public boolean letsRadiationThrough(BlockState state) {
        return state.getValue(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(VISIBLE) ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // ------------------------------------------------------------------ placing and keeping the two halves together

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OPEN, false).setValue(POWERED, false).setValue(VISIBLE, true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || level.getBlockState(pos.below()).is(this);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            withBlockEntityDo(level, state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below(), door -> door.deferUpdate = true);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
        if (updated.isAir()) {
            return updated;
        }
        DoubleBlockHalf half = updated.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighbour.is(this) && neighbour.getValue(HALF) != half
                    ? updated.setValue(VISIBLE, neighbour.getValue(VISIBLE))
                    : Blocks.AIR.defaultBlockState();
        }
        return updated;
    }

    // ------------------------------------------------------------------ opening

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean open = !state.getValue(OPEN);
        if (open && refuse(level, pos, state, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        move(player, level, state, pos, open, true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        if (!state.is(this) || state.getValue(OPEN) == open) {
            return;
        }
        if (open && refuse(level, pos, state, entity instanceof Player player ? player : null)) {
            return;
        }
        move(entity, level, state, pos, open, true);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (defaultBlockState().is(block)) {
            return;
        }
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        boolean powered = isDoorPowered(level, pos, state);
        if (powered == state.getValue(POWERED)) {
            return;
        }
        ContainmentDoorBlockEntity door = getBlockEntity(level, lower ? pos : pos.below());
        if (door != null && door.deferUpdate) {
            return;
        }
        BlockState changed = state.setValue(POWERED, powered);
        if (powered != state.getValue(OPEN) && !(powered && refuse(level, pos, state, null))) {
            move(null, level, changed, pos, powered, true);
            return;
        }
        level.setBlock(pos, changed, Block.UPDATE_CLIENTS);
    }

    /** Opens or shuts this door, and its partner if it stands beside it as a double door. */
    private void move(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open, boolean withPartner) {
        BlockState changed = state.setValue(OPEN, open);
        if (open) {
            changed = changed.setValue(VISIBLE, false);
        }
        // Neighbours are told too: a containment around the door hears of it through those updates.
        level.setBlock(pos, changed, UPDATE_ALL | UPDATE_IMMEDIATE);
        level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        if (withPartner) {
            BlockPos partnerPos = partner(changed, pos);
            BlockState partner = level.getBlockState(partnerPos);
            if (isDoubleDoor(changed, partner)) {
                move(entity, level, partner, partnerPos, open, false);
            }
            playSound(level, pos, open);
        }
    }

    /** True, with a message for the player, when the containment behind the door is pressurized. */
    private boolean refuse(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        OptionalDouble pressure = ContainmentDoorInterlock.lockingPressure(level, lower);
        if (pressure.isEmpty()) {
            return false;
        }
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.5f);
        if (player != null) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.containment_door.locked",
                    String.format("%.2f", pressure.getAsDouble())), true);
        }
        return true;
    }

    private static BlockPos partner(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return pos.relative(state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
    }

    /** Two doors side by side, hinged on opposite sides: they open and shut together, sliding apart. */
    private boolean isDoubleDoor(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(HINGE) != state.getValue(HINGE) && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(OPEN) != state.getValue(OPEN) && other.getValue(HALF) == state.getValue(HALF);
    }

    private boolean isDoorPowered(Level level, BlockPos pos, BlockState state) {
        Direction otherHalf = state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        BlockPos partnerPos = partner(state, pos);
        if (isDoubleDoor(state.cycle(OPEN), level.getBlockState(partnerPos))
                && (level.hasNeighborSignal(partnerPos) || level.hasNeighborSignal(partnerPos.relative(otherHalf)))) {
            return true;
        }
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.relative(otherHalf));
    }

    private static void playSound(Level level, BlockPos pos, boolean opening) {
        // The seal lets go with a hiss, then the drive grinds the heavy door along.
        if (opening) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
        }
        level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.7f, opening ? 0.55f : 0.5f);
    }

    // ------------------------------------------------------------------ block entity (lower half only)

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : IBE.super.newBlockEntity(pos, state);
    }

    @Override
    public Class<ContainmentDoorBlockEntity> getBlockEntityClass() {
        return ContainmentDoorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ContainmentDoorBlockEntity> getBlockEntityType() {
        return PwrRegistry.CONTAINMENT_DOOR_ENTITY.get();
    }
}
