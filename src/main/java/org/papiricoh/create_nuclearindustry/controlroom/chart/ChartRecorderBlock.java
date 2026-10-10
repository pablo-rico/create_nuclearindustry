package org.papiricoh.create_nuclearindustry.controlroom.chart;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;

/**
 * A chart recorder. Feed it paper by hand or with a hopper, link it with a Control Room Linker and
 * wrench it to choose what its pens draw and how fast the paper runs. Sneak-click it with an empty
 * hand to tear the chart off as a Chart Roll; a click shows how much paper is left.
 */
public class ChartRecorderBlock extends HorizontalDirectionalBlock implements IBE<ChartRecorderBlockEntity> {
    public static final MapCodec<ChartRecorderBlock> CODEC = simpleCodec(ChartRecorderBlock::new);
    private static final String KEY = "create_nuclearindustry.chart_recorder.";

    public ChartRecorderBlock(Properties properties) {
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        if (!(level.getBlockEntity(pos) instanceof ChartRecorderBlockEntity recorder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (recorder.insertPaper(stack, true) > 0) {
            if (!level.isClientSide) {
                stack.consume(recorder.insertPaper(stack, false), player);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ControlRoomBlocks.WRENCHES) && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                DeviceMenu.open(serverPlayer, ControlRoom2Registry.CHART_RECORDER_MENU, pos, 0, state.getBlock().getName());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChartRecorderBlockEntity recorder)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack roll = recorder.tearOff();
            if (roll.isEmpty()) {
                player.displayClientMessage(Component.translatable(KEY + "blank").withStyle(ChatFormatting.YELLOW), true);
            } else {
                player.getInventory().placeItemBackInInventory(roll);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            return InteractionResult.SUCCESS;
        }
        Component status = recorder.running()
                ? Component.translatable(KEY + "status", recorder.paper(), recorder.ticksPerSample())
                : Component.translatable(KEY + "no_paper");
        player.displayClientMessage(status.copy().withStyle(recorder.running() ? ChatFormatting.GRAY : ChatFormatting.RED), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full the paper tray is, so a refill can be automated. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChartRecorderBlockEntity recorder ? recorder.comparatorSignal() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChartRecorderBlockEntity recorder) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, recorder.paperDrop());
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ChartRecorderBlockEntity> getBlockEntityClass() {
        return ChartRecorderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChartRecorderBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.CHART_RECORDER_ENTITY.get();
    }
}
