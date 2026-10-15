package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * An annunciator panel: a plate hung on the face of a solid block, like a wall sign, with six alarm
 * windows over a strip. Panels hung side by side and one above the other on one wall make one board
 * (see {@link AnnunciatorPanelBlockEntity}), with a single set of TEST, SILENCE, ACK and RESET buttons
 * at the right of its bottom strip; LEFT, RIGHT, UP and DOWN drop the frame between joined panels.
 * <p>
 * Click a window with a Control Room Linker to link it (or the frame to link all six of that panel),
 * wrench one to set it up, and press the buttons. Redstone works the whole board: a rising edge into
 * the left side acknowledges, into the right side resets and from above silences the horn, and power
 * from below holds the lamp test. While an alarm waits for an acknowledgement every panel powers the
 * wall it hangs on; a comparator reads how many windows are lit.
 * <p>
 * MOUNTED is set when a panel is hung: panels placed before they hung on walls stand where they were,
 * and only a hung panel falls when its wall goes.
 */
public class AnnunciatorPanelBlock extends HorizontalDirectionalBlock implements IBE<AnnunciatorPanelBlockEntity> {
    public static final MapCodec<AnnunciatorPanelBlock> CODEC = simpleCodec(AnnunciatorPanelBlock::new);
    public static final BooleanProperty MOUNTED = BooleanProperty.create("mounted");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    /** How far the plate's face sits behind the block's front, in pixels. */
    public static final float FACE_DEPTH = 13;
    private static final VoxelShape NATIVE = Block.box(0, 0, FACE_DEPTH, 16, 16, 16);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int yaw = ((int) facing.toYRot() + 180) % 360;
            SHAPES.put(facing, ModuleSurfaceBlock.rotateClockwise(NATIVE, yaw / 90));
        }
    }

    public AnnunciatorPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MOUNTED, false)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MOUNTED, LEFT, RIGHT, UP, DOWN);
    }

    // ------------------------------------------------------------------ hanging and joining

    /** Hangs on a block face that can hold it. */
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
                BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite()).setValue(MOUNTED, true);
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return connect(state, context.getLevel(), context.getClickedPos());
                }
            }
        }
        return null;
    }

    /** Sets which sides another panel of the same wall carries the board on from. */
    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, joins(state, level, pos.relative(facing.getClockWise())))
                .setValue(RIGHT, joins(state, level, pos.relative(facing.getCounterClockWise())))
                .setValue(UP, joins(state, level, pos.above()))
                .setValue(DOWN, joins(state, level, pos.below()));
    }

    private static boolean joins(BlockState state, BlockGetter level, BlockPos neighbor) {
        BlockState other = level.getBlockState(neighbor);
        return other.getBlock() == state.getBlock() && other.getValue(FACING) == state.getValue(FACING);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && state.getValue(MOUNTED) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return connect(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    // ------------------------------------------------------------------ interaction

    /** Where a click landed on the plate's face, in pixels from the bottom-left as the viewer sees it; null elsewhere. */
    @Nullable
    public static float[] pixel(BlockState state, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        return ControlRoomBlocks.facePixel(state.getValue(FACING), pos, hit, FACE_DEPTH);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        if (stack.is(ControlRoomBlocks.WRENCHES) && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel) {
                AnnunciatorBoardLayout.Hit hit = panel.hitAt(pixel(state, pos, hitResult.getLocation()));
                int window = hit.kind() == AnnunciatorBoardLayout.HitKind.WINDOW ? hit.index() : 0;
                DeviceMenu.open(serverPlayer, ControlRoom2Registry.ANNUNCIATOR_MENU, pos, window, state.getBlock().getName());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel)) {
            return InteractionResult.PASS;
        }
        AnnunciatorBoardLayout.Hit hit = panel.hitAt(pixel(state, pos, hitResult.getLocation()));
        if (hit.kind() != AnnunciatorBoardLayout.HitKind.BUTTON && hit.kind() != AnnunciatorBoardLayout.HitKind.WINDOW) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hit.kind() == AnnunciatorBoardLayout.HitKind.BUTTON) {
            boolean acted = panel.press(hit.button());
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, acted ? 1.2f : 0.8f);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(describe(panel, hit.index()), true);
        return InteractionResult.SUCCESS;
    }

    /** "Reactor temperature above 2800°C: ALERT" for a window, or how to link it. */
    private static Component describe(AnnunciatorPanelBlockEntity panel, int index) {
        String key = "create_nuclearindustry.annunciator.";
        AnnunciatorPanelBlockEntity.Window window = panel.window(index);
        if (window.target() == null) {
            return Component.translatable(key + "unlinked", index + 1).withStyle(ChatFormatting.YELLOW);
        }
        AnnunciatorLogic.State state = panel.logic().state(index);
        return Component.translatable(key + "describe", index + 1, Component.translatable(window.metric().getTranslationKey()),
                window.metric().format(window.setting()), Component.translatable(key + "state." + state.name().toLowerCase(Locale.ROOT)))
                .withStyle(state == AnnunciatorLogic.State.NORMAL ? ChatFormatting.GRAY : ChatFormatting.RED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, panel -> {
                panel.requestClusterUpdate();
                panel.redstoneChanged();
            });
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            withBlockEntityDo(level, pos, AnnunciatorPanelBlockEntity::requestClusterUpdate);
        }
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Into the wall only, never the sides that work the buttons, so wiring them cannot loop. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction != state.getValue(FACING)) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel && panel.outputOn() ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel ? panel.comparatorSignal() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<AnnunciatorPanelBlockEntity> getBlockEntityClass() {
        return AnnunciatorPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AnnunciatorPanelBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.ANNUNCIATOR_ENTITY.get();
    }
}
