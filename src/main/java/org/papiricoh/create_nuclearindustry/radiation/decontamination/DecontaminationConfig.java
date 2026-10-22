package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the Decontamination Sprinkler and phytoremediation with sunflowers, under
 * {@code decontamination.*}. Values are defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class DecontaminationConfig {
    public static double sprinklerRate = 1.0;
    public static int sprinklerRadius = 1;
    public static int sprinklerWaterPerSecond = 50;
    public static double sprinklerContaminatedPerRad = 100.0;
    public static double sprinklerShowerPerSecond = 5.0;
    public static double sprinklerStress = 4.0;
    public static double sunflowerUptake = 0.05;
    public static int sunflowerAbsorptions = 20;

    private static ModConfigSpec.DoubleValue sprinklerRateValue;
    private static ModConfigSpec.IntValue sprinklerRadiusValue;
    private static ModConfigSpec.IntValue sprinklerWaterPerSecondValue;
    private static ModConfigSpec.DoubleValue sprinklerContaminatedPerRadValue;
    private static ModConfigSpec.DoubleValue sprinklerShowerPerSecondValue;
    private static ModConfigSpec.DoubleValue sprinklerStressValue;
    private static ModConfigSpec.DoubleValue sunflowerUptakeValue;
    private static ModConfigSpec.IntValue sunflowerAbsorptionsValue;

    private DecontaminationConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        sprinklerRateValue = builder
                .comment("Contamination (rad/s) a Decontamination Sprinkler at 256 RPM or more washes off each chunk it reaches every second.")
                .defineInRange("decontamination.sprinklerRate", 1.0, 0.0, 1000.0);
        sprinklerRadiusValue = builder
                .comment("Chunks around its own a Decontamination Sprinkler reaches: 1 is a 3x3 area.")
                .defineInRange("decontamination.sprinklerRadius", 1, 0, 4);
        sprinklerWaterPerSecondValue = builder
                .comment("Water (mB) a Decontamination Sprinkler sprays each second at 256 RPM or more.")
                .defineInRange("decontamination.sprinklerWaterPerSecond", 50, 1, 10000);
        sprinklerContaminatedPerRadValue = builder
                .comment("Contaminated water (mB) a Decontamination Sprinkler collects for each rad/s it washes off the ground.")
                .defineInRange("decontamination.sprinklerContaminatedPerRad", 100.0, 0.0, 10000.0);
        sprinklerShowerPerSecondValue = builder
                .comment("Dose (rad) washed off each second from a player standing in a sprinkler's spray at 256 RPM or more.")
                .defineInRange("decontamination.sprinklerShowerPerSecond", 5.0, 0.0, 10000.0);
        sprinklerStressValue = builder
                .comment("Stress impact of a Decontamination Sprinkler, in SU per RPM.")
                .defineInRange("decontamination.sprinklerStress", 4.0, 0.0, 1.0e6);
        sunflowerUptakeValue = builder
                .comment("Contamination (rad/s) a sunflower draws out of its chunk on each random tick (about every 68 seconds).")
                .defineInRange("decontamination.sunflowerUptake", 0.05, 0.0, 100.0);
        sunflowerAbsorptionsValue = builder
                .comment("Absorptions a sunflower makes on average before it wilts into an Irradiated Sunflower. 0 never wilts.")
                .defineInRange("decontamination.sunflowerAbsorptions", 20, 0, 10000);
    }

    public static void bake() {
        sprinklerRate = sprinklerRateValue.get();
        sprinklerRadius = sprinklerRadiusValue.get();
        sprinklerWaterPerSecond = sprinklerWaterPerSecondValue.get();
        sprinklerContaminatedPerRad = sprinklerContaminatedPerRadValue.get();
        sprinklerShowerPerSecond = sprinklerShowerPerSecondValue.get();
        sprinklerStress = sprinklerStressValue.get();
        sunflowerUptake = sunflowerUptakeValue.get();
        sunflowerAbsorptions = sunflowerAbsorptionsValue.get();
    }
}
