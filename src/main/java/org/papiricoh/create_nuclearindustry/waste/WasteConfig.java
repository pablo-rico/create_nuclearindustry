package org.papiricoh.create_nuclearindustry.waste;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryQuality;

/**
 * Common config for waste classes, treatments and the geological repository, under {@code waste.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class WasteConfig {
    private static ModConfigSpec.BooleanValue LEGACY_DEPTH_RULE;
    private static ModConfigSpec.DoubleValue MIN_SCORE;
    private static ModConfigSpec.IntValue DEPTH_SPAN;
    private static ModConfigSpec.DoubleValue DEPTH_WEIGHT;
    private static ModConfigSpec.DoubleValue HOST_ROCK_WEIGHT;
    private static ModConfigSpec.DoubleValue BACKFILL_WEIGHT;
    private static ModConfigSpec.DoubleValue FORM_WEIGHT;
    private static ModConfigSpec.DoubleValue FLUID_PENALTY;
    private static ModConfigSpec.IntValue PROBE_CACHE_TICKS;

    /** Keep the old rule: tagged waste at or below {@code contamination.geologicalStorageMaxY} is stored, whatever is around it. */
    public static boolean legacyDepthRule = false;
    /** Repository score from which a package counts as stored (no leak, no radiation). */
    public static double minScore = 0.8;
    /** Blocks below the surface for the full depth score. */
    public static int depthSpan = 64;
    public static RepositoryQuality.Weights weights = RepositoryQuality.Weights.DEFAULT;
    /** Ticks a sampled repository site is trusted before the blocks around it are looked at again. */
    public static int probeCacheTicks = 200;

    private WasteConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        LEGACY_DEPTH_RULE = builder
                .comment("Use the old storage rule instead of the repository score: waste barrels and packages at or below",
                        "contamination.geologicalStorageMaxY are stored (no leak, no radiation) whatever surrounds them.")
                .define("waste.repository.legacyDepthRule", false);
        MIN_SCORE = builder
                .comment("Repository score (0-1) from which a waste package counts as geologically stored: it neither leaks nor irradiates.",
                        "Below it, leaks and radiation shrink as the score grows.")
                .defineInRange("waste.repository.minScore", 0.8, 0.0, 1.0);
        DEPTH_SPAN = builder
                .comment("Blocks below the surface (the highest solid block of the column) for the full depth score.")
                .defineInRange("waste.repository.depthSpan", 64, 1, 4096);
        RepositoryQuality.Weights defaults = RepositoryQuality.Weights.DEFAULT;
        DEPTH_WEIGHT = builder
                .comment("Share of the repository score that comes from depth.")
                .defineInRange("waste.repository.depthWeight", defaults.depth(), 0.0, 1.0);
        HOST_ROCK_WEIGHT = builder
                .comment("Share of the repository score from host rock (deepslate, tuff, basalt...) within 3 blocks.")
                .defineInRange("waste.repository.hostRockWeight", defaults.hostRock(), 0.0, 1.0);
        BACKFILL_WEIGHT = builder
                .comment("Share of the repository score from backfill: faces covered by Bentonite or other waste packages.")
                .defineInRange("waste.repository.backfillWeight", defaults.backfill(), 0.0, 1.0);
        FORM_WEIGHT = builder
                .comment("Share of the repository score from the waste form (raw 0.3, compacted or cemented 0.7, vitrified 1).")
                .defineInRange("waste.repository.formWeight", defaults.form(), 0.0, 1.0);
        FLUID_PENALTY = builder
                .comment("Score taken away when a fluid source (water, lava...) is within 4 blocks of the package.")
                .defineInRange("waste.repository.fluidPenalty", defaults.fluidPenalty(), 0.0, 1.0);
        PROBE_CACHE_TICKS = builder
                .comment("Ticks a repository assessment is reused before the blocks around the package are sampled again.")
                .defineInRange("waste.repository.probeCacheTicks", 200, 1, 72000);
    }

    public static void bake() {
        legacyDepthRule = LEGACY_DEPTH_RULE.get();
        minScore = MIN_SCORE.get();
        depthSpan = DEPTH_SPAN.get();
        weights = new RepositoryQuality.Weights(DEPTH_WEIGHT.get(), HOST_ROCK_WEIGHT.get(), BACKFILL_WEIGHT.get(),
                FORM_WEIGHT.get(), FLUID_PENALTY.get());
        probeCacheTicks = PROBE_CACHE_TICKS.get();
    }
}
