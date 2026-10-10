package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;

/**
 * Corium that has frozen: a glassy, intensely radioactive slag of fuel, cladding and whatever it
 * melted through. It irradiates like the waste it is and, like a waste barrel left above the
 * geological storage depth, slowly leaks into its chunk's ground on random ticks.
 */
public class SolidifiedCoriumBlock extends Block {
    /** It leaks more than a sealed waste barrel: nothing contains it. */
    private static final double LEAK_FACTOR = 2.0;

    public SolidifiedCoriumBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Config.contaminationEnabled && Config.barrelLeak > 0 && !GeologicalStorage.isDeepEnough(pos)) {
            Contamination.add(level.getChunkAt(pos), (float) (Config.barrelLeak * LEAK_FACTOR));
        }
    }
}
