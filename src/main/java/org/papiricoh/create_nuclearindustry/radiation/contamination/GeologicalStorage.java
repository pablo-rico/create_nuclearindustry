package org.papiricoh.create_nuclearindustry.radiation.contamination;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.waste.WasteConfig;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryProbe;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryQuality;

/**
 * Whether placed waste is safely stored, and how much it leaks and irradiates when it is not.
 * Waste packages (the {@code waste_package} data map) are judged by the repository around them
 * ({@link RepositoryQuality}): depth, host rock, backfill, their own form and groundwater. With
 * {@code waste.repository.legacyDepthRule} the old rule applies instead: tagged waste at or
 * below {@code contamination.geologicalStorageMaxY} is stored, anything else leaks in full.
 */
public final class GeologicalStorage {
    private GeologicalStorage() {}

    public static boolean isStored(Level level, BlockState state, BlockPos pos) {
        WastePackage pack = WastePackage.of(state);
        if (WasteConfig.legacyDepthRule || pack == null) {
            return isDeepEnough(pos) && state.is(AllNuclearTags.Blocks.GEOLOGICALLY_STORABLE);
        }
        return RepositoryQuality.isStored(score(level, state, pos), WasteConfig.minScore);
    }

    /** The repository score of the package at {@code pos}; 0 for anything that is not a package. */
    public static double score(Level level, BlockState state, BlockPos pos) {
        WastePackage pack = WastePackage.of(state);
        return pack == null ? 0 : RepositoryProbe.assess(level, pos, pack).score();
    }

    /** The old rule: at or below the configured storage height. */
    public static boolean isDeepEnough(BlockPos pos) {
        return pos.getY() <= Config.geologicalStorageMaxY;
    }

    /** Dose rate a placed radioactive block of carried activity {@code perSecond} gives off. */
    public static double emission(Level level, BlockState state, BlockPos pos, double perSecond) {
        WastePackage pack = WastePackage.of(state);
        if (pack == null) {
            return isStored(level, state, pos) ? 0 : perSecond;
        }
        if (WasteConfig.legacyDepthRule) {
            return isStored(level, state, pos) ? 0 : perSecond * pack.emissionFactor();
        }
        return RepositoryQuality.emission(perSecond, pack.emissionFactor(), score(level, state, pos), WasteConfig.minScore);
    }

    /** Contamination a waste package adds to its chunk on one random tick. */
    public static double leak(Level level, BlockState state, BlockPos pos) {
        WastePackage pack = WastePackage.of(state);
        if (pack == null || !Config.contaminationEnabled || Config.barrelLeak <= 0) {
            return 0;
        }
        if (WasteConfig.legacyDepthRule) {
            return isStored(level, state, pos) ? 0 : Config.barrelLeak * pack.leakFactor();
        }
        return RepositoryQuality.leak(Config.barrelLeak, pack.leakFactor(), score(level, state, pos), WasteConfig.minScore);
    }
}
