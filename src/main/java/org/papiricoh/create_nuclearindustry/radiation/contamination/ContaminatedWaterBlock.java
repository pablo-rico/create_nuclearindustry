package org.papiricoh.create_nuclearindustry.radiation.contamination;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Contaminated water in the world. Its radiation is the contamination of the chunk it lies in, so
 * the block itself is no source (no rays per block). Its source blocks tick randomly and turn
 * back into clean water once their chunk has decayed or been cleaned below
 * {@link WaterContamination#CLEAN_SEVERITY}; water poured out of a bucket or a pipe puts what it
 * washed off back into the ground (see {@link WaterContamination#spilled}).
 */
public class ContaminatedWaterBlock extends LiquidBlock {
    public ContaminatedWaterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getFluidState().isSource();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WaterContamination.settle(level, pos, state, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this) && state.getFluidState().isSource()) {
            WaterContamination.spilled(server, pos);
        }
    }
}
