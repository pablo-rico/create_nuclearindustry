package org.papiricoh.create_nuclearindustry.turbine.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Reinforced glass for turbine walls. Panes join into one window through Create's connected
 * textures (registered client-side in {@code NuclearModelSwaps}); in a formed turbine the pane
 * thins to a sheet on its outer face ({@link TurbineFormedLook}).
 */
public class TurbineGlassBlock extends TransparentBlock {
    public static final MapCodec<TurbineGlassBlock> CODEC = simpleCodec(TurbineGlassBlock::new);

    public TurbineGlassBlock(Properties properties) {
        super(properties);
        registerDefaultState(TurbineFormedLook.withoutPanels(defaultBlockState()));
    }

    @Override
    protected MapCodec<? extends TurbineGlassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        TurbineFormedLook.addPanels(builder);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return TurbineFormedLook.isShaped(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TurbineFormedLook.releaseIfOrphaned(state, level, pos);
    }
}
