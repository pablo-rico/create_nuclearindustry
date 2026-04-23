package org.papiricoh.create_nuclearindustry.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.blockentity.ReactorBlockEntity;

public class NuclearReactorControllerBlock extends BaseEntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final MapCodec<NuclearReactorControllerBlock> CODEC = simpleCodec(NuclearReactorControllerBlock::new);

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
}
