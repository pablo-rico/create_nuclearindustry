package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;

/**
 * A quartz window in the tokamak shell. It seals the chamber like any shell block, lets the player
 * watch the plasma, and gives off its light: {@link #GLOW} follows the plasma (see
 * {@link ViewportGlow}) and is the block's light level. Like glass, it hides the faces it shares
 * with another viewport but never the one facing the dark chamber.
 */
public class FusionViewportBlock extends TransparentBlock implements EntityBlock, TokamakShellBlock {
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, ViewportGlow.MAX);
    public static final MapCodec<FusionViewportBlock> CODEC = simpleCodec(FusionViewportBlock::new);

    public FusionViewportBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLOW, 0));
    }

    @Override
    protected MapCodec<? extends FusionViewportBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    /** Light level for the block properties. */
    public static int lightLevel(BlockState state) {
        return state.getValue(GLOW);
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return TokamakShellRole.VIEWPORT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionViewportBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != PlasmaHeatingRegistry.VIEWPORT_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> ((FusionViewportBlockEntity) blockEntity).serverTick();
    }
}
