package org.papiricoh.create_nuclearindustry;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for how much work the mod does per tick, for servers to tune: radiation caching
 * and the nuclear crater's budget, under {@code performance.*} and {@code explosive.*}. Values are
 * defined on the shared builder by {@link Config} and copied into public statics on load.
 */
public final class PerformanceConfig {
    private static ModConfigSpec.IntValue RADIATION_RAY_CACHE_TICKS;
    private static ModConfigSpec.IntValue RADIATION_RAY_MAX_AGE_TICKS;
    private static ModConfigSpec.IntValue BLAST_COLUMNS_PER_TICK;
    private static ModConfigSpec.IntValue BLAST_BLOCKS_PER_TICK;
    private static ModConfigSpec.IntValue FORCED_SYNC_TICKS;
    private static ModConfigSpec.IntValue SCREEN_SYNC_TICKS;

    public static int radiationRayCacheTicks = 100;
    public static int radiationRayMaxAgeTicks = 1200;
    public static int blastColumnsPerTick = 600;
    public static int blastBlocksPerTick = 3500;
    /** Most ticks a controller or control room screen goes without updating its clients. */
    public static int forcedSyncTicks = 100;
    /** Least ticks between two updates of a control room monitor or chart recorder. */
    public static int screenSyncTicks = 20;

    private PerformanceConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        RADIATION_RAY_CACHE_TICKS = builder
                .comment("Ticks a radiation ray (from a source to a player or a monitored area) may go unused and still be",
                        "reused, as long as neither end has moved and no block changed along it. Players are updated every",
                        "20 ticks, so 20 or more keeps a standing player's rays. 0 casts every ray every time.")
                .defineInRange("performance.radiationRayCacheTicks", 100, 0, 1200);
        RADIATION_RAY_MAX_AGE_TICKS = builder
                .comment("Ticks after which a cached radiation ray is cast again even if it is still in use, so a block",
                        "changed without notifying its neighbours is noticed at last. 0 never recasts a ray in use.")
                .defineInRange("performance.radiationRayMaxAgeTicks", 1200, 0, 72000);
        BLAST_COLUMNS_PER_TICK = builder
                .comment("Columns of the crater a nuclear detonation digs per tick. Lower spreads the work over more ticks.")
                .defineInRange("explosive.blastColumnsPerTick", 600, 1, 100000);
        BLAST_BLOCKS_PER_TICK = builder
                .comment("Blocks a nuclear detonation removes or changes per tick at most.")
                .defineInRange("explosive.blastBlocksPerTick", 3500, 1, 1000000);
        FORCED_SYNC_TICKS = builder
                .comment("Most ticks a machine controller (steam generator, cryogenic plant, tokamak, accelerator) or a",
                        "control room monitor goes without sending its clients an update. Between them, updates go out",
                        "only when something they show changed by a visible step.")
                .defineInRange("performance.forcedSyncTicks", 100, 10, 1200);
        SCREEN_SYNC_TICKS = builder
                .comment("Least ticks between two updates a control room monitor or chart recorder sends its clients;",
                        "a chart recorder sends the samples drawn in between together.")
                .defineInRange("performance.screenSyncTicks", 20, 1, 200);
    }

    public static void bake() {
        radiationRayCacheTicks = RADIATION_RAY_CACHE_TICKS.get();
        radiationRayMaxAgeTicks = RADIATION_RAY_MAX_AGE_TICKS.get();
        blastColumnsPerTick = BLAST_COLUMNS_PER_TICK.get();
        blastBlocksPerTick = BLAST_BLOCKS_PER_TICK.get();
        forcedSyncTicks = FORCED_SYNC_TICKS.get();
        screenSyncTicks = SCREEN_SYNC_TICKS.get();
    }
}
