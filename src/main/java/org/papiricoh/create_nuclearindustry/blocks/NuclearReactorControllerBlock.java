package org.papiricoh.create_nuclearindustry.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.blockentity.ReactorBlockEntity;

public class NuclearReactorControllerBlock extends BaseEntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final MapCodec<NuclearReactorControllerBlock> CODEC = simpleCodec(NuclearReactorControllerBlock::new);

    // Temporary storage for menu opening (used to pass data to client)
    public static BlockPos lastReactorMenuPos = null;

    public NuclearReactorControllerBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(ON, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder builder) {
        builder.add(ON);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Only tick on server side
        if (level.isClientSide) {
            return null;
        }

        // Return the ticker if the block entity type matches
        return blockEntityType == AllNuclearEntities.REACTOR.get() ? (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof ReactorBlockEntity reactor) {
                ReactorBlockEntity.tick(lvl, pos, blockState, reactor);
            }
        } : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof ReactorBlockEntity reactor) {
            // Store the reactor position so client can access it
            lastReactorMenuPos = pos;

            // Force BlockEntity update to sync to client immediately
            reactor.setChanged();

            net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.literal("Reactor Control");
            var menuProvider = new org.papiricoh.create_nuclearindustry.gui.ReactorMenuProvider(pos, title);
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(menuProvider::createMenu, title));
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        return net.minecraft.world.InteractionResult.PASS;
    }
}
