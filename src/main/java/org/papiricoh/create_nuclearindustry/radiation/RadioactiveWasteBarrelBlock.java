package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;

/**
 * A drum of waste. Above the geological storage depth it slowly leaks into its chunk's ground,
 * on vanilla random ticks, so a stockpile costs nothing to simulate; buried deep it is inert.
 */
public class RadioactiveWasteBarrelBlock extends Block {
    public RadioactiveWasteBarrelBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Config.contaminationEnabled && Config.barrelLeak > 0 && !GeologicalStorage.isDeepEnough(pos)) {
            Contamination.add(level.getChunkAt(pos), (float) Config.barrelLeak);
        }
    }
}
