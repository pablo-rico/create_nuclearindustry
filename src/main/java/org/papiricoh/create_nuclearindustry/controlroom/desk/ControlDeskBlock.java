package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;

import java.util.Map;

/**
 * A control desk: a cabinet with a sloping top that holds four control modules, or two on a corner,
 * one on each arm (see {@link DeskShape#hasSlot}). FACING is the side the operator stands on; the
 * top rises away from it. Desks side by side join into one
 * console (LEFT and RIGHT drop the end panels), and desks meeting at right angles turn a corner
 * like stairs do. Fit a module by clicking a slot with it; wrench a module to set it up,
 * sneak-wrench to take it out; click it with an empty hand to work it.
 */
public class ControlDeskBlock extends HorizontalDirectionalBlock implements IBE<ControlDeskBlockEntity> {
    public static final MapCodec<ControlDeskBlock> CODEC = simpleCodec(ControlDeskBlock::new);
    public static final EnumProperty<DeskShape> SHAPE = EnumProperty.create("shape", DeskShape.class);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WRENCHES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    /** Clicks below this height land on the cabinet, not on a module. */
    private static final double SLOPE_MIN_Y = 0.55;
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

    /** Turns a shape a number of quarter turns clockwise seen from above, like a blockstate y rotation. */
    private static VoxelShape rotateClockwise(VoxelShape shape, int quarterTurns) {
        VoxelShape turned = shape;
        for (int i = 0; i < quarterTurns; i++) {
            VoxelShape[] built = {Shapes.empty()};
            turned.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                    built[0] = Shapes.or(built[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            turned = built[0];
        }
        return turned;
    }

    // ------------------------------------------------------------------ slots and interaction

    /**
     * The slot a click lands on, 0-3, or -1 off the top. On a straight desk slots go left to right as
     * the operator sees them, the near row (0, 1) then the far row (2, 3); on a corner each half of
     * the top, either side of the diagonal, is its arm's one slot.
     */
    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        if (!(state.getBlock() instanceof ControlDeskBlock)) {
            return -1;
        }
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        if (local.y < SLOPE_MIN_Y) {
            return -1;
        }
        // Into native model space: undo the body's clockwise turn a quarter at a time.
        double x = local.x;
        double z = local.z;
        for (int i = 0; i < DeskGeometry.bodyYaw(state) / 90; i++) {
            double turned = z;
            z = 1 - x;
            x = turned;
        }
        DeskShape shape = state.getValue(SHAPE);
        if (shape == DeskShape.STRAIGHT) {
            // The operator stands at z = 0 with +x on their left.
            int column = x >= 0.5 ? 0 : 1;
            int row = z < 0.5 ? 0 : 1;
            return row * 2 + column;
        }
        // The half north-east of the diagonal: an inner corner's arm carrying on north (operator west),
        // an outer corner's arm carrying on east (operator north).
        boolean northEast = z <= x;
        return shape.isInner() ? (northEast ? 2 : 3) : (northEast ? 0 : 1);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        int slot = slotAt(state, pos, hitResult.getLocation());
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ControlModule module = desk.module(slot);
        if (stack.getItem() instanceof ControlModuleItem moduleItem) {
            if (module != null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide && desk.insert(slot, moduleItem.type())) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(WRENCHES) && module != null) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    ModuleType removed = desk.remove(slot);
                    if (removed != null) {
                        player.getInventory().placeItemBackInInventory(ControlModuleItem.stackOf(removed));
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1.0f);
                    }
                } else if (player instanceof ServerPlayer serverPlayer) {
                    ControlModuleMenu.open(serverPlayer, pos, slot);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int slot = slotAt(state, pos, hitResult.getLocation());
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk)) {
            return InteractionResult.PASS;
        }
        ControlModule module = desk.module(slot);
        if (module == null || !module.type().interactive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (desk.press(slot, player.isShiftKeyDown())) {
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.2f);
            } else {
                player.displayClientMessage(Component.translatable("create_nuclearindustry.control_module.inactive")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ControlDeskBlockEntity::requestClusterUpdate);
        }
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Out of the bottom and the back, for modules set to put their state out below. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction != Direction.UP && direction != state.getValue(FACING)) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk ? desk.belowSignal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk) {
            for (ItemStack drop : desk.drops()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, drop);
            }
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ControlDeskBlockEntity> getBlockEntityClass() {
        return ControlDeskBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ControlDeskBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.REACTOR_CONTROL_PANEL.get();
    }
}
