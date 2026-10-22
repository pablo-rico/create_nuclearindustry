package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

/**
 * Sunflowers draw caesium and strontium out of the soil, as they did around Chernobyl and
 * Fukushima: a sunflower in a contaminated chunk takes {@code decontamination.sunflowerUptake}
 * off it on each random tick and, after about {@code decontamination.sunflowerAbsorptions} of
 * them, wilts into an Irradiated Sunflower that absorbs no more. Nothing is kept per plant, and
 * clean chunks cost a single read.
 */
public final class Phytoremediation {
    private Phytoremediation() {}

    /** Whether the state takes part: the lower half of a sunflower. */
    public static boolean absorbs(BlockState state) {
        return state.is(Blocks.SUNFLOWER) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    /** A random tick of the sunflower at {@code pos}; returns the contamination it took. */
    public static float randomTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!Config.contaminationEnabled || !absorbs(state) || DecontaminationConfig.sunflowerUptake <= 0) {
            return 0;
        }
        LevelChunk chunk = Contamination.loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return 0;
        }
        float value = Contamination.read(chunk);
        if (value < Config.radiationBackgroundRate) {
            return 0;
        }
        float taken = Math.min(value, (float) DecontaminationConfig.sunflowerUptake);
        Contamination.set(chunk, value - taken);
        int absorptions = DecontaminationConfig.sunflowerAbsorptions;
        if (absorptions > 0 && random.nextInt(absorptions) == 0) {
            wilt(level, pos);
        }
        return taken;
    }

    /** Replaces both halves of the sunflower at {@code lower} with an Irradiated Sunflower. */
    public static void wilt(ServerLevel level, BlockPos lower) {
        // Both halves are swapped without neighbour or shape updates, or each would see the other gone and break.
        DoublePlantBlock.placeAt(level, DecontaminationRegistry.IRRADIATED_SUNFLOWER.get().defaultBlockState(), lower,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        RadiationCaches.blockChanged(level, lower);
    }
}
