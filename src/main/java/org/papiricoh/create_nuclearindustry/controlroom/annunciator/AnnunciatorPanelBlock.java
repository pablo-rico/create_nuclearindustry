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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;

/**
 * An annunciator panel: six alarm windows over an ACK and a RESET button. Click a window with a
 * Control Room Linker to link it (or the frame to link them all), wrench a window to set it up, and
 * press the buttons, or pulse redstone into the left (ACK) and right (RESET) sides as seen from the
 * front. The back and bottom give full power while an alarm waits for an acknowledgement; a
 * comparator reads how many windows are lit.
 */
public class AnnunciatorPanelBlock extends HorizontalDirectionalBlock implements IBE<AnnunciatorPanelBlockEntity> {
    public static final MapCodec<AnnunciatorPanelBlock> CODEC = simpleCodec(AnnunciatorPanelBlock::new);

    public AnnunciatorPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    private static float[] pixel(BlockState state, BlockPos pos, BlockHitResult hit) {
        return ControlRoomBlocks.frontFacePixel(state.getValue(FACING), pos, hit.getLocation());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        if (stack.is(ControlRoomBlocks.WRENCHES) && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                int window = Math.max(0, AnnunciatorPanelBlockEntity.windowAt(pixel(state, pos, hitResult)));
                DeviceMenu.open(serverPlayer, ControlRoom2Registry.ANNUNCIATOR_MENU, pos, window, state.getBlock().getName());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        float[] pixel = pixel(state, pos, hitResult);
        int button = AnnunciatorPanelBlockEntity.buttonAt(pixel);
        int window = AnnunciatorPanelBlockEntity.windowAt(pixel);
        if ((button == 0 && window < 0) || !(level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (button != 0) {
            boolean acted = button == 1 ? panel.acknowledge() : panel.reset();
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, acted ? 1.2f : 0.8f);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(describe(panel, window), true);
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
                window.metric().format(window.setting()), Component.translatable(key + "state." + state.name().toLowerCase(java.util.Locale.ROOT)))
                .withStyle(state == AnnunciatorLogic.State.NORMAL ? ChatFormatting.GRAY : ChatFormatting.RED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, AnnunciatorPanelBlockEntity::redstoneChanged);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Out of the back and the bottom, never the ACK and RESET sides, so wiring them cannot loop. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction != Direction.UP && direction != state.getValue(FACING)) {
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
