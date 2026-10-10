package org.papiricoh.create_nuclearindustry.waste;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;

/**
 * A placed waste package: a barrel, drum or canister. It leaks into its chunk's ground on
 * vanilla random ticks, so a stockpile costs nothing to simulate; how much depends on its
 * {@link WastePackage} data and on the repository around it ({@link GeologicalStorage#leak}).
 */
public class WastePackageBlock extends Block {
    public WastePackageBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        double leak = GeologicalStorage.leak(level, state, pos);
        if (leak > 0) {
            Contamination.add(level.getChunkAt(pos), (float) leak);
        }
    }
}
