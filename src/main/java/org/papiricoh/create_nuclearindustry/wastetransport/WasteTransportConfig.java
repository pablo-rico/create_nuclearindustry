package org.papiricoh.create_nuclearindustry.wastetransport;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the radioactive waste cistern and the shielded waste cask, under
 * {@code wasteTransport.*}. Values are defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class WasteTransportConfig {
    private static ModConfigSpec.DoubleValue SHIELDED_TRANSMISSION;
    private static ModConfigSpec.DoubleValue BREACHED_TRANSMISSION;
    private static ModConfigSpec.DoubleValue MAX_EMISSION;
    private static ModConfigSpec.IntValue CASK_SLOTS_PER_BLOCK;
    private static ModConfigSpec.DoubleValue SPILL_FLUID_FRACTION;
    private static ModConfigSpec.DoubleValue SPILL_INTENSITY_PER_ACTIVITY;
    private static ModConfigSpec.IntValue SPILL_RADIUS_MAX;
    private static ModConfigSpec.DoubleValue BREACH_LEAK_FACTOR;
    private static ModConfigSpec.BooleanValue BREACH_ON_DERAIL;
    private static ModConfigSpec.BooleanValue BREACH_ON_EXPLOSION;

    /** Fraction of the cargo's radiation that gets through a sealed container. */
    public static double shieldedTransmission = 0.002;
    /** Fraction that gets through once the container is breached. */
    public static double breachedTransmission = 0.35;
    /** Highest dose rate (rad/s at the source) one container or one contraption gives off. */
    public static double maxEmission = 2000;
    /** Item slots each block of a shielded waste cask adds. */
    public static int caskSlotsPerBlock = 9;
    /** Share of a cistern's fluid lost to the ground when it is breached. */
    public static double spillFluidFraction = 0.5;
    /** Ground contamination (rad/s) at the centre of a spill per rad/s of cargo activity. */
    public static double spillIntensityPerActivity = 0.01;
    /** Largest radius, in blocks, of the contamination a spill leaves. */
    public static int spillRadiusMax = 16;
    /** How fast a breached container keeps leaking, as a multiple of its spill spread over a minute. */
    public static double breachLeakFactor = 2.0;
    /** Whether a derailed or crashed train breaches its waste containers. */
    public static boolean breachOnDerail = true;
    /** Whether explosions breach the waste containers they reach and do not destroy. */
    public static boolean breachOnExplosion = true;

    private WasteTransportConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        SHIELDED_TRANSMISSION = builder
                .comment("Fraction of the cargo's radiation that gets out of a sealed Radioactive Waste Cistern or Shielded Waste Cask.")
                .defineInRange("wasteTransport.shieldedTransmission", 0.002, 0.0, 1.0);
        BREACHED_TRANSMISSION = builder
                .comment("Fraction of the cargo's radiation that gets out once the container is breached (derailment, explosion).")
                .defineInRange("wasteTransport.breachedTransmission", 0.35, 0.0, 1.0);
        MAX_EMISSION = builder
                .comment("Highest dose rate, in rad/s at the source, that one container (or one moving contraption) gives off.")
                .defineInRange("wasteTransport.maxEmission", 2000.0, 0.0, 1_000_000.0);
        CASK_SLOTS_PER_BLOCK = builder
                .comment("Item slots each block of a Shielded Waste Cask adds. Changing it only affects casks placed afterwards.")
                .defineInRange("wasteTransport.caskSlotsPerBlock", 9, 1, 54);
        SPILL_FLUID_FRACTION = builder
                .comment("Share of a cistern's fluid that is lost to the ground when it is breached.")
                .defineInRange("wasteTransport.spillFluidFraction", 0.5, 0.0, 1.0);
        SPILL_INTENSITY_PER_ACTIVITY = builder
                .comment("Ground contamination, in rad/s at the centre of a spill, per rad/s of activity of the cargo.")
                .defineInRange("wasteTransport.spillIntensityPerActivity", 0.01, 0.0, 10.0);
        SPILL_RADIUS_MAX = builder
                .comment("Largest radius, in blocks, of the contamination a spill leaves.")
                .defineInRange("wasteTransport.spillRadiusMax", 16, 1, 128);
        BREACH_LEAK_FACTOR = builder
                .comment("How fast a breached container keeps leaking into the ground under it: this multiple of its spill",
                        "intensity is added over each minute. A breached cistern also loses this many thousandths of its fluid per second.")
                .defineInRange("wasteTransport.breachLeakFactor", 2.0, 0.0, 100.0);
        BREACH_ON_DERAIL = builder
                .comment("Whether a derailed or crashed Create train breaches the waste containers it carries.")
                .define("wasteTransport.breachOnDerail", true);
        BREACH_ON_EXPLOSION = builder
                .comment("Whether explosions breach the waste containers (placed or moving) they reach but do not destroy.")
                .define("wasteTransport.breachOnExplosion", true);
    }

    public static void bake() {
        shieldedTransmission = SHIELDED_TRANSMISSION.get();
        breachedTransmission = BREACHED_TRANSMISSION.get();
        maxEmission = MAX_EMISSION.get();
        caskSlotsPerBlock = CASK_SLOTS_PER_BLOCK.get();
        spillFluidFraction = SPILL_FLUID_FRACTION.get();
        spillIntensityPerActivity = SPILL_INTENSITY_PER_ACTIVITY.get();
        spillRadiusMax = SPILL_RADIUS_MAX.get();
        breachLeakFactor = BREACH_LEAK_FACTOR.get();
        breachOnDerail = BREACH_ON_DERAIL.get();
        breachOnExplosion = BREACH_ON_EXPLOSION.get();
    }
}
