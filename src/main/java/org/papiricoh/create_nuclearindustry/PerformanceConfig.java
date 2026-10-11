package org.papiricoh.create_nuclearindustry;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for how much work the mod does per tick, for servers to tune: radiation caching
 * and the nuclear crater's budget, under {@code performance.*} and {@code explosive.*}. Values are
 * defined on the shared builder by {@link Config} and copied into public statics on load.
 */
public final class PerformanceConfig {
    private static ModConfigSpec.IntValue RADIATION_RAY_CACHE_TICKS;
    private static ModConfigSpec.IntValue BLAST_COLUMNS_PER_TICK;
    private static ModConfigSpec.IntValue BLAST_BLOCKS_PER_TICK;

    public static int radiationRayCacheTicks = 20;
    public static int blastColumnsPerTick = 600;
    public static int blastBlocksPerTick = 3500;

    private PerformanceConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        RADIATION_RAY_CACHE_TICKS = builder
                .comment("Ticks a player's radiation ray to a source is reused before it is cast again, as long as",
                        "neither has moved. 0 casts every ray every time.")
                .defineInRange("performance.radiationRayCacheTicks", 20, 0, 1200);
        BLAST_COLUMNS_PER_TICK = builder
                .comment("Columns of the crater a nuclear detonation digs per tick. Lower spreads the work over more ticks.")
                .defineInRange("explosive.blastColumnsPerTick", 600, 1, 100000);
        BLAST_BLOCKS_PER_TICK = builder
                .comment("Blocks a nuclear detonation removes or changes per tick at most.")
                .defineInRange("explosive.blastBlocksPerTick", 3500, 1, 1000000);
    }

    public static void bake() {
        radiationRayCacheTicks = RADIATION_RAY_CACHE_TICKS.get();
        blastColumnsPerTick = BLAST_COLUMNS_PER_TICK.get();
        blastBlocksPerTick = BLAST_BLOCKS_PER_TICK.get();
    }
}
