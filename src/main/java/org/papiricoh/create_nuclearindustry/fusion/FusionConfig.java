package org.papiricoh.create_nuclearindustry.fusion;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the tokamak, under {@code fusion.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 * The keys predate this class, so existing config files keep their values.
 */
public final class FusionConfig {
    private static ModConfigSpec.DoubleValue DT_BURN_RATE;
    private static ModConfigSpec.IntValue MAX_MAJOR_RADIUS;
    private static ModConfigSpec.IntValue COILS_PER_SECTOR;
    private static ModConfigSpec.DoubleValue MAGNET_STRESS;
    private static ModConfigSpec.BooleanValue DISRUPTION_EXPLOSIONS;

    public static double deuteriumTritiumBurnRate = 0.0011;
    public static int maxMajorRadius = 16;
    public static int coilsPerSector = 4;
    public static double magnetInputStress = 1024.0;
    public static boolean disruptionExplosions = true;

    private FusionConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        DT_BURN_RATE = builder
                .comment("mB of deuterium (and of tritium) burnt per plasma cell per tick at full temperature.",
                        "A tokamak of major radius R has about 56*R plasma cells. Fusion power is 1000 MW per mB/t burnt.")
                .defineInRange("fusion.deuteriumTritiumBurnRate", 0.0011, 0.0, 1.0);
        MAX_MAJOR_RADIUS = builder
                .comment("Largest tokamak major radius (distance from the central solenoid to the plasma centre).")
                .defineInRange("fusion.maxMajorRadius", 16, 4, 32);
        COILS_PER_SECTOR = builder
                .comment("Electromagnets a toroidal sector needs for full field coverage. A tokamak has 4*R sectors.")
                .defineInRange("fusion.coilsPerSector", 4, 1, 25);
        MAGNET_STRESS = builder
                .comment("Stress impact (SU per RPM) of each confinement magnet drive.")
                .defineInRange("fusion.magnetInputStress", 1024.0, 0.0, 1.0e6);
        DISRUPTION_EXPLOSIONS = builder
                .comment("Whether a plasma disruption blows holes in the tokamak. If false, the plasma just quenches.")
                .define("fusion.disruptionExplosions", true);
    }

    public static void bake() {
        deuteriumTritiumBurnRate = DT_BURN_RATE.get();
        maxMajorRadius = MAX_MAJOR_RADIUS.get();
        coilsPerSector = COILS_PER_SECTOR.get();
        magnetInputStress = MAGNET_STRESS.get();
        disruptionExplosions = DISRUPTION_EXPLOSIONS.get();
    }
}
