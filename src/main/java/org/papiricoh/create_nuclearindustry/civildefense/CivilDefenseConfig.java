package org.papiricoh.create_nuclearindustry.civildefense;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for sirens, the seismic detector, blast doors, the blast wave and shelters, under {@code civilDefense.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class CivilDefenseConfig {
    public static int sirenThreatRadius = 96;
    public static int sirenAudibleRange = 128;
    public static int crankSirenMinSpeed = 32;
    public static double crankSirenStress = 2.0;
    public static int seismicMemorySeconds = 300;
    public static double seismicFalloff = 256.0;
    public static double seismicFullScale = 100.0;
    public static double blastDoorBlocksPerRevolution = 2.0;
    public static int blastDoorRedstoneTicksPerBlock = 10;
    public static double blastDoorStress = 4.0;

    private static ModConfigSpec.IntValue sirenThreatRadiusValue;
    private static ModConfigSpec.IntValue sirenAudibleRangeValue;
    private static ModConfigSpec.IntValue crankSirenMinSpeedValue;
    private static ModConfigSpec.DoubleValue crankSirenStressValue;
    private static ModConfigSpec.IntValue seismicMemorySecondsValue;
    private static ModConfigSpec.DoubleValue seismicFalloffValue;
    private static ModConfigSpec.DoubleValue seismicFullScaleValue;
    private static ModConfigSpec.DoubleValue blastDoorBlocksPerRevolutionValue;
    private static ModConfigSpec.IntValue blastDoorRedstoneTicksPerBlockValue;
    private static ModConfigSpec.DoubleValue blastDoorStressValue;

    private CivilDefenseConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        sirenThreatRadiusValue = builder
                .comment("Air raid sirens sound on their own when a missile, bomb or launch countdown is aimed this many blocks",
                        "from them, plus the radius the threat itself reaches (its fallout radius).")
                .defineInRange("civilDefense.siren.threatRadius", 96, 0, 4096);
        sirenAudibleRangeValue = builder
                .comment("How far away a sounding siren can be heard, in blocks.")
                .defineInRange("civilDefense.siren.audibleRange", 128, 16, 512);
        crankSirenMinSpeedValue = builder
                .comment("Slowest a hand-crank siren can turn and still sound, in RPM.")
                .defineInRange("civilDefense.siren.crankMinSpeed", 32, 1, 256);
        crankSirenStressValue = builder
                .comment("Stress impact of a hand-crank siren, in SU per RPM.")
                .defineInRange("civilDefense.siren.crankStress", 2.0, 0.0, 1.0e6);
        seismicMemorySecondsValue = builder
                .comment("How long a detonation keeps a seismic detector's comparator output up, in seconds.")
                .defineInRange("civilDefense.seismic.memorySeconds", 300, 1, 86400);
        seismicFalloffValue = builder
                .comment("Distance in blocks at which a seismic detector feels half the power of a detonation;",
                        "beyond it the ground motion falls with the square of the distance.")
                .defineInRange("civilDefense.seismic.falloff", 256.0, 1.0, 1.0e6);
        seismicFullScaleValue = builder
                .comment("Ground motion that gives a full comparator signal of 15 (a full nuclear warhead has a power of 160).")
                .defineInRange("civilDefense.seismic.fullScale", 100.0, 0.01, 1.0e6);
        blastDoorBlocksPerRevolutionValue = builder
                .comment("How far a blast door slides for every turn of its controller's shaft, in blocks.")
                .defineInRange("civilDefense.blastDoor.blocksPerRevolution", 2.0, 0.01, 64.0);
        blastDoorRedstoneTicksPerBlockValue = builder
                .comment("Ticks a blast door moved by redstone takes to slide one block.")
                .defineInRange("civilDefense.blastDoor.redstoneTicksPerBlock", 10, 1, 1200);
        blastDoorStressValue = builder
                .comment("Stress impact of a blast door controller, in SU per RPM.")
                .defineInRange("civilDefense.blastDoor.stress", 4.0, 0.0, 1.0e6);
    }

    public static void bake() {
        sirenThreatRadius = sirenThreatRadiusValue.get();
        sirenAudibleRange = sirenAudibleRangeValue.get();
        crankSirenMinSpeed = crankSirenMinSpeedValue.get();
        crankSirenStress = crankSirenStressValue.get();
        seismicMemorySeconds = seismicMemorySecondsValue.get();
        seismicFalloff = seismicFalloffValue.get();
        seismicFullScale = seismicFullScaleValue.get();
        blastDoorBlocksPerRevolution = blastDoorBlocksPerRevolutionValue.get();
        blastDoorRedstoneTicksPerBlock = blastDoorRedstoneTicksPerBlockValue.get();
        blastDoorStress = blastDoorStressValue.get();
    }
}
