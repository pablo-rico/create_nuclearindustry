package org.papiricoh.create_nuclearindustry.controlroom;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Remote controls for a fission reactor: SCRAM and reset, the automatic SCRAM, and a rod target the
 * reactor's Redstone Ports in rod modes drive the pistons towards. Click the buttons on its face.
 */
public class ReactorControlPanelBlock extends HorizontalDirectionalBlock implements IBE<ReactorControlPanelBlockEntity> {
    public static final MapCodec<ReactorControlPanelBlock> CODEC = simpleCodec(ReactorControlPanelBlock::new);

    public ReactorControlPanelBlock(Properties properties) {
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
        return ControlRoomBlocks.linkerFirst(stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        double[] uv = ControlRoomBlocks.faceCoordinates(pos, state.getValue(FACING), hitResult);
        PanelButton button = uv == null ? null : PanelButton.at(uv[0], uv[1]);
        if (button == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, panel -> panel.press(button, player));
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ReactorControlPanelBlockEntity> getBlockEntityClass() {
        return ReactorControlPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactorControlPanelBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.REACTOR_CONTROL_PANEL.get();
    }
}
