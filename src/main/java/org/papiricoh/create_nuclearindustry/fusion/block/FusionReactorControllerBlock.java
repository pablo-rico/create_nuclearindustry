package org.papiricoh.create_nuclearindustry.fusion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

public class FusionReactorControllerBlock extends BaseEntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final BooleanProperty IGNITED = BooleanProperty.create("ignited");
    public static final MapCodec<FusionReactorControllerBlock> CODEC = simpleCodec(FusionReactorControllerBlock::new);

    public FusionReactorControllerBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(ON, false).setValue(IGNITED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON, IGNITED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == AllNuclearEntities.FUSION_CONTROLLER.get() ? (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof FusionReactorBlockEntity reactor) {
                FusionReactorBlockEntity.tick(lvl, pos, blockState, reactor);
            }
        } : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FusionReactorBlockEntity reactor) {
            reactor.attemptFormation(player);
            reactor.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
