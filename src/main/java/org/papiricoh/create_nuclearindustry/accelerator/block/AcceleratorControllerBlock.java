package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.gui.AcceleratorMenu;

/**
 * Heads a particle accelerator. {@code FACING} is the way the beam leaves it, towards the ion
 * source; the operator's panel is on the opposite face. Right-click to check the structure and open
 * the control panel; a redstone signal starts the beam and taking it away stops it.
 */
public class AcceleratorControllerBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final MapCodec<AcceleratorControllerBlock> CODEC = simpleCodec(AcceleratorControllerBlock::new);

    public AcceleratorControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON);
    }

    /** Placed with its beam face away from the player, so the panel faces them. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorControllerBlockEntity(AcceleratorRegistry.CONTROLLER_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == AcceleratorRegistry.CONTROLLER_ENTITY.get() ? (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof AcceleratorControllerBlockEntity controller) {
                AcceleratorControllerBlockEntity.tick(lvl, pos, blockState, controller);
            }
        } : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AcceleratorControllerBlockEntity controller) {
            controller.onRedstoneSignal(level.hasNeighborSignal(pos));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AcceleratorControllerBlockEntity controller) {
            controller.attemptFormation(player);
            Component title = Component.translatable("gui.create_nuclearindustry.accelerator.title");
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AcceleratorMenu(id, inventory, controller.getBlockPos()), title),
                    buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
