package org.papiricoh.create_nuclearindustry.dyson;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the Dyson swarm, its EM rail ejectors and ray receivers, under {@code dyson.*}. Values are defined on the shared
 * builder by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class DysonConfig {
    public static double sailLifetimeDays = 20;
    public static int fePerSail = 64;
    public static int receiverMaxFe = 32768;
    public static int receiverBufferSeconds = 1;
    public static int maxSails = 20000;
    public static int ejectorMinRpm = 64;
    public static double ejectorStress = 16;
    public static int ejectorTicksPerSail = 40;
    public static double rainFactor = 0.4;
    public static double thunderFactor = 0.1;
    public static double minSunElevation = 5;
    public static int syncSeconds = 10;

    private static ModConfigSpec.DoubleValue sailLifetimeDaysValue;
    private static ModConfigSpec.IntValue fePerSailValue;
    private static ModConfigSpec.IntValue receiverMaxFeValue;
    private static ModConfigSpec.IntValue receiverBufferSecondsValue;
    private static ModConfigSpec.IntValue maxSailsValue;
    private static ModConfigSpec.IntValue ejectorMinRpmValue;
    private static ModConfigSpec.DoubleValue ejectorStressValue;
    private static ModConfigSpec.IntValue ejectorTicksPerSailValue;
    private static ModConfigSpec.DoubleValue rainFactorValue;
    private static ModConfigSpec.DoubleValue thunderFactorValue;
    private static ModConfigSpec.DoubleValue minSunElevationValue;
    private static ModConfigSpec.IntValue syncSecondsValue;

    private DysonConfig() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        sailLifetimeDaysValue = builder
                .comment("In-game days a solar sail lasts in orbit before it wears out and is lost.")
                .defineInRange("dyson.sailLifetimeDays", 20.0, 0.05, 3650.0);
        fePerSailValue = builder
                .comment("FE per tick each sail in the swarm sends down with the sun straight overhead and a clear sky.")
                .defineInRange("dyson.fePerSail", 64, 1, 1 << 20);
        receiverMaxFeValue = builder
                .comment("Most FE per tick one ray receiver takes from the swarm.")
                .defineInRange("dyson.receiverMaxFe", 32768, 1, Integer.MAX_VALUE / 64);
        receiverBufferSecondsValue = builder
                .comment("Seconds of its full output a ray receiver can hold when nothing takes its energy.")
                .defineInRange("dyson.receiverBufferSeconds", 1, 1, 60);
        maxSailsValue = builder
                .comment("Most sails the swarm holds, in all its orbits; ejectors stop while it is full.")
                .defineInRange("dyson.maxSails", 20000, 1, 1_000_000);
        ejectorMinRpmValue = builder
                .comment("Slowest an EM rail ejector can turn and still launch sails, in RPM.")
                .defineInRange("dyson.ejector.minRpm", 64, 1, 256);
        ejectorStressValue = builder
                .comment("Stress impact of an EM rail ejector, in SU per RPM.")
                .defineInRange("dyson.ejector.stress", 16.0, 0.0, 1.0e6);
        ejectorTicksPerSailValue = builder
                .comment("Ticks an EM rail ejector takes to charge and launch a sail at 256 RPM; slower in proportion below that.")
                .defineInRange("dyson.ejector.ticksPerSail", 40, 1, 72000);
        rainFactorValue = builder
                .comment("Share of the swarm's light that reaches the receivers in full rain.")
                .defineInRange("dyson.rainFactor", 0.4, 0.0, 1.0);
        thunderFactorValue = builder
                .comment("Share of the swarm's light that reaches the receivers in a full thunderstorm.")
                .defineInRange("dyson.thunderFactor", 0.1, 0.0, 1.0);
        minSunElevationValue = builder
                .comment("Degrees the sun must be over the horizon for ejectors to launch and receivers to take power.")
                .defineInRange("dyson.minSunElevation", 5.0, 0.0, 60.0);
        syncSecondsValue = builder
                .comment("Seconds between refreshes of the swarm sent to the players.")
                .defineInRange("dyson.syncSeconds", 10, 1, 600);
    }

    public static void bake() {
        sailLifetimeDays = sailLifetimeDaysValue.get();
        fePerSail = fePerSailValue.get();
        receiverMaxFe = receiverMaxFeValue.get();
        receiverBufferSeconds = receiverBufferSecondsValue.get();
        maxSails = maxSailsValue.get();
        ejectorMinRpm = ejectorMinRpmValue.get();
        ejectorStress = ejectorStressValue.get();
        ejectorTicksPerSail = ejectorTicksPerSailValue.get();
        rainFactor = rainFactorValue.get();
        thunderFactor = thunderFactorValue.get();
        minSunElevation = minSunElevationValue.get();
        syncSeconds = syncSecondsValue.get();
    }

    /** Ticks a sail lasts in orbit. */
    public static long sailLifetimeTicks() {
        return Math.max(1, Math.round(sailLifetimeDays * 24000));
    }

    /** The sine of {@link #minSunElevation}. */
    public static double minSinElevation() {
        return Math.sin(Math.toRadians(minSunElevation));
    }
}
