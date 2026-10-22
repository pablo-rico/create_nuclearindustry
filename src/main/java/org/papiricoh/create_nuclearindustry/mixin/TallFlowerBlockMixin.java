package org.papiricoh.create_nuclearindustry.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.Phytoremediation;
import org.spongepowered.asm.mixin.Mixin;

/** Gives sunflowers random ticks, so they draw contamination out of the ground (see {@link Phytoremediation}). */
@Mixin(TallFlowerBlock.class)
public abstract class TallFlowerBlockMixin extends DoublePlantBlock {
    private TallFlowerBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return Phytoremediation.absorbs(state) || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        Phytoremediation.randomTick(level, pos, state, random);
    }
}
