package org.papiricoh.create_nuclearindustry.explosive.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.papiricoh.create_nuclearindustry.missile.silo.WarheadBay;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombMenuProvider;

public class NuclearBombBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final MapCodec<NuclearBombBlock> CODEC = simpleCodec(NuclearBombBlock::new);

    public NuclearBombBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var items = stack.getOrDefault(DataComponents.CONTAINER,
                ItemContainerContents.EMPTY);
        int count = 0;
        for (var item : items.nonEmptyItems()) count += item.getCount();
        tooltip.add(Component.translatable("silo.create_nuclearindustry.bomb.contents", count,
                Component.translatable("silo.create_nuclearindustry.warhead." +
                        WarheadBay.fromBomb(stack).kind().name().toLowerCase(Locale.ROOT))));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb) {
            bomb.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            MenuProvider provider = new SimpleMenuProvider(menu(pos), Component.translatable(state.getBlock().getDescriptionId()));
            // The position travels with the menu, so each player's screen finds its own block.
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** The loading screen this bomb opens. */
    protected MenuConstructor menu(BlockPos pos) {
        return new NuclearBombMenuProvider(pos)::createMenu;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuclearBombBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof NuclearBombBlockEntity bomb) {
                bomb.tick();
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
