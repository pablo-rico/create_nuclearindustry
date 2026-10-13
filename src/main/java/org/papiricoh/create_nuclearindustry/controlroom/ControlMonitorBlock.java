package org.papiricoh.create_nuclearindustry.controlroom;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;

/**
 * A control room screen. Monitors side by side facing the same way join into one rectangular
 * screen of up to 8x8. Right-click with an empty hand to change page, with a wrench to set the
 * screen up (title, layout, rows and graph); a comparator reads the main value.
 */
public class ControlMonitorBlock extends HorizontalDirectionalBlock implements IBE<ControlMonitorBlockEntity> {
    public static final MapCodec<ControlMonitorBlock> CODEC = simpleCodec(ControlMonitorBlock::new);

    public ControlMonitorBlock(Properties properties) {
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

    /** The screen faces the player who places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * The linker links; a wrench opens the setup screen on the clicked monitor, which hands the setup
     * to its screen's origin, so a wall of monitors can be set up from any block within reach.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        if (stack.is(ControlRoomBlocks.WRENCHES) && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                DeviceMenu.open(serverPlayer, ControlRoom2Registry.MONITOR_MENU, pos, 0, state.getBlock().getName());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, monitor -> monitor.origin().ifPresent(ControlMonitorBlockEntity::nextPage));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ControlMonitorBlockEntity::requestClusterUpdate);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ControlMonitorBlockEntity monitor
                ? monitor.origin().map(origin -> origin.getReading().comparatorSignal()).orElse(0)
                : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ControlMonitorBlockEntity> getBlockEntityClass() {
        return ControlMonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ControlMonitorBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.CONTROL_MONITOR.get();
    }
}
