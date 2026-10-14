package org.papiricoh.create_nuclearindustry.civildefense;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for sirens, the seismic detector, blast doors, the blast wave and shelters, under {@code civilDefense.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class CivilDefenseConfig {
    public static int sirenThreatRadius = 96;
    public static int sirenAudibleRange = 128;
    /** How long a plant accident (meltdown, vessel breach, steam explosion, containment burst, hydrogen burn) sounds the sirens. */
    public static int plantAlarmSeconds = 120;
    public static int crankSirenMinSpeed = 32;
    public static double crankSirenStress = 2.0;
    public static int seismicMemorySeconds = 300;
    public static double seismicFalloff = 256.0;
    public static double seismicFullScale = 100.0;
    public static double blastDoorBlocksPerRevolution = 2.0;
    public static int blastDoorRedstoneTicksPerBlock = 10;
    public static double blastDoorStress = 4.0;
    public static double blastWaveSpeed = 8.0;
    public static int blastWaveMaxRadius = 256;
    public static double blastWaveYieldPerPower = 250.0;
    public static double blastWaveResistanceScale = 6.0;
    public static double blastWaveDamageThreshold = 5.0;
    public static double blastWaveDamagePerKPa = 0.5;
    public static double blastWaveKnockbackPerKPa = 0.02;
    public static boolean blastWaveBreakFragileBlocks = true;
    public static double blastWaveFragileOverpressure = 5.0;
    public static int blastWaveFragileBlocksPerTick = 1500;
    public static int shelterMaxVolume = 4096;
    public static int shelterCellsPerTick = 2048;
    public static int shelterRescanTicks = 100;
    public static int shelterMinSpeed = 16;
    public static double shelterStress = 4.0;
    public static double shelterMinWallResistance = 1.5;
    public static double shelterMaxWallTransmission = 0.9;
    public static double shelterFilterFactor = 0.05;
    public static double shelterBlastFactor = 0.1;

    private static ModConfigSpec.IntValue sirenThreatRadiusValue;
    private static ModConfigSpec.IntValue sirenAudibleRangeValue;
    private static ModConfigSpec.IntValue plantAlarmSecondsValue;
    private static ModConfigSpec.IntValue crankSirenMinSpeedValue;
    private static ModConfigSpec.DoubleValue crankSirenStressValue;
    private static ModConfigSpec.IntValue seismicMemorySecondsValue;
    private static ModConfigSpec.DoubleValue seismicFalloffValue;
    private static ModConfigSpec.DoubleValue seismicFullScaleValue;
    private static ModConfigSpec.DoubleValue blastDoorBlocksPerRevolutionValue;
    private static ModConfigSpec.IntValue blastDoorRedstoneTicksPerBlockValue;
    private static ModConfigSpec.DoubleValue blastDoorStressValue;
    private static ModConfigSpec.DoubleValue blastWaveSpeedValue;
    private static ModConfigSpec.IntValue blastWaveMaxRadiusValue;
    private static ModConfigSpec.DoubleValue blastWaveYieldPerPowerValue;
    private static ModConfigSpec.DoubleValue blastWaveResistanceScaleValue;
    private static ModConfigSpec.DoubleValue blastWaveDamageThresholdValue;
    private static ModConfigSpec.DoubleValue blastWaveDamagePerKPaValue;
    private static ModConfigSpec.DoubleValue blastWaveKnockbackPerKPaValue;
    private static ModConfigSpec.BooleanValue blastWaveBreakFragileBlocksValue;
    private static ModConfigSpec.DoubleValue blastWaveFragileOverpressureValue;
    private static ModConfigSpec.IntValue blastWaveFragileBlocksPerTickValue;
    private static ModConfigSpec.IntValue shelterMaxVolumeValue;
    private static ModConfigSpec.IntValue shelterCellsPerTickValue;
    private static ModConfigSpec.IntValue shelterRescanTicksValue;
    private static ModConfigSpec.IntValue shelterMinSpeedValue;
    private static ModConfigSpec.DoubleValue shelterStressValue;
    private static ModConfigSpec.DoubleValue shelterMinWallResistanceValue;
    private static ModConfigSpec.DoubleValue shelterMaxWallTransmissionValue;
    private static ModConfigSpec.DoubleValue shelterFilterFactorValue;
    private static ModConfigSpec.DoubleValue shelterBlastFactorValue;

    private CivilDefenseConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        sirenThreatRadiusValue = builder
                .comment("Air raid sirens sound on their own when a missile, bomb or launch countdown is aimed this many blocks",
                        "from them, plus the radius the threat itself reaches (its fallout radius).")
                .defineInRange("civilDefense.siren.threatRadius", 96, 0, 4096);
        sirenAudibleRangeValue = builder
                .comment("How far away a sounding siren can be heard, in blocks.")
                .defineInRange("civilDefense.siren.audibleRange", 128, 16, 512);
        plantAlarmSecondsValue = builder
                .comment("Seconds the automatic sirens within threatRadius of a plant accident sound for: a meltdown, a breached",
                        "reactor vessel, a steam explosion, a burst containment or a hydrogen burn. Each new accident starts again.",
                        "0 keeps the sirens out of plant accidents.")
                .defineInRange("civilDefense.siren.plantAlarmSeconds", 120, 0, 3600);
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

        blastWaveSpeedValue = builder
                .comment("How fast the blast wave of a nuclear detonation spreads, in blocks per tick.")
                .defineInRange("civilDefense.blastWave.speed", 8.0, 0.5, 64.0);
        blastWaveMaxRadiusValue = builder
                .comment("Farthest a blast wave reaches, in blocks; it never goes beyond four times the crater radius either.",
                        "0 turns the wave off.")
                .defineInRange("civilDefense.blastWave.maxRadius", 256, 0, 2048);
        blastWaveYieldPerPowerValue = builder
                .comment("Kilograms of TNT each point of detonation power is worth to the overpressure model (a full",
                        "nuclear warhead has a power of 160). Overpressure at a distance r depends on r / yield^(1/3).")
                .defineInRange("civilDefense.blastWave.yieldPerPower", 250.0, 0.001, 1.0e6);
        blastWaveResistanceScaleValue = builder
                .comment("How well blocks hold the wave back: each block crossed lets exp(-explosionResistance / this)",
                        "through (stone, resistance 6, lets 37% through by default; obsidian nothing).")
                .defineInRange("civilDefense.blastWave.resistanceScale", 6.0, 0.01, 1.0e4);
        blastWaveDamageThresholdValue = builder
                .comment("Overpressure below which the wave does no damage, in kPa.")
                .defineInRange("civilDefense.blastWave.damageThreshold", 5.0, 0.0, 1.0e4);
        blastWaveDamagePerKPaValue = builder
                .comment("Damage (half hearts) per kPa of overpressure above the threshold.")
                .defineInRange("civilDefense.blastWave.damagePerKPa", 0.5, 0.0, 100.0);
        blastWaveKnockbackPerKPaValue = builder
                .comment("Outward push per kPa of overpressure, in blocks per tick (at most 4).")
                .defineInRange("civilDefense.blastWave.knockbackPerKPa", 0.02, 0.0, 10.0);
        blastWaveBreakFragileBlocksValue = builder
                .comment("Whether the wave shatters glass, panes and leaves (the create_nuclearindustry:blast_fragile tag)",
                        "near the surface as it passes.")
                .define("civilDefense.blastWave.breakFragileBlocks", true);
        blastWaveFragileOverpressureValue = builder
                .comment("Overpressure that shatters a fragile block, in kPa.")
                .defineInRange("civilDefense.blastWave.fragileOverpressure", 5.0, 0.0, 1.0e4);
        blastWaveFragileBlocksPerTickValue = builder
                .comment("Most fragile blocks one wave breaks per tick.")
                .defineInRange("civilDefense.blastWave.fragileBlocksPerTick", 1500, 0, 1500);

        shelterMaxVolumeValue = builder
                .comment("Largest room, in blocks of air, a shelter filtration unit can certify.")
                .defineInRange("civilDefense.shelter.maxVolume", 4096, 8, 65536);
        shelterCellsPerTickValue = builder
                .comment("Blocks of air a filtration unit checks per tick while it surveys its room.")
                .defineInRange("civilDefense.shelter.cellsPerTick", 2048, 16, 65536);
        shelterRescanTicksValue = builder
                .comment("Ticks between surveys of a shelter's room, to notice new holes and walls.")
                .defineInRange("civilDefense.shelter.rescanTicks", 100, 1, 12000);
        shelterMinSpeedValue = builder
                .comment("Slowest a filtration unit can turn and still filter, in RPM.")
                .defineInRange("civilDefense.shelter.minSpeed", 16, 1, 256);
        shelterStressValue = builder
                .comment("Stress impact of a shelter filtration unit, in SU per RPM.")
                .defineInRange("civilDefense.shelter.stress", 4.0, 0.0, 1.0e6);
        shelterMinWallResistanceValue = builder
                .comment("Weakest explosion resistance a block of a shelter's walls, floor or ceiling may have (stone 6,",
                        "planks 3, concrete 1.8, glass 0.3).")
                .defineInRange("civilDefense.shelter.minWallResistance", 1.5, 0.0, 3600000.0);
        shelterMaxWallTransmissionValue = builder
                .comment("Most radiation a block of a shelter's walls may let through (its radiation_transmission; plain",
                        "solid blocks use radiation.solidTransmission, 0.9 by default).")
                .defineInRange("civilDefense.shelter.maxWallTransmission", 0.9, 0.0, 1.0);
        shelterFilterFactorValue = builder
                .comment("Fraction of the fallout and ground contamination dose that reaches people inside a certified,",
                        "running shelter.")
                .defineInRange("civilDefense.shelter.filterFactor", 0.05, 0.0, 1.0);
        shelterBlastFactorValue = builder
                .comment("Fraction of a blast wave's overpressure that reaches people inside a certified shelter, on top",
                        "of what its walls hold back.")
                .defineInRange("civilDefense.shelter.blastFactor", 0.1, 0.0, 1.0);
    }

    public static void bake() {
        sirenThreatRadius = sirenThreatRadiusValue.get();
        sirenAudibleRange = sirenAudibleRangeValue.get();
        plantAlarmSeconds = plantAlarmSecondsValue.get();
        crankSirenMinSpeed = crankSirenMinSpeedValue.get();
        crankSirenStress = crankSirenStressValue.get();
        seismicMemorySeconds = seismicMemorySecondsValue.get();
        seismicFalloff = seismicFalloffValue.get();
        seismicFullScale = seismicFullScaleValue.get();
        blastDoorBlocksPerRevolution = blastDoorBlocksPerRevolutionValue.get();
        blastDoorRedstoneTicksPerBlock = blastDoorRedstoneTicksPerBlockValue.get();
        blastDoorStress = blastDoorStressValue.get();
        blastWaveSpeed = blastWaveSpeedValue.get();
        blastWaveMaxRadius = blastWaveMaxRadiusValue.get();
        blastWaveYieldPerPower = blastWaveYieldPerPowerValue.get();
        blastWaveResistanceScale = blastWaveResistanceScaleValue.get();
        blastWaveDamageThreshold = blastWaveDamageThresholdValue.get();
        blastWaveDamagePerKPa = blastWaveDamagePerKPaValue.get();
        blastWaveKnockbackPerKPa = blastWaveKnockbackPerKPaValue.get();
        blastWaveBreakFragileBlocks = blastWaveBreakFragileBlocksValue.get();
        blastWaveFragileOverpressure = blastWaveFragileOverpressureValue.get();
        blastWaveFragileBlocksPerTick = blastWaveFragileBlocksPerTickValue.get();
        shelterMaxVolume = shelterMaxVolumeValue.get();
        shelterCellsPerTick = shelterCellsPerTickValue.get();
        shelterRescanTicks = shelterRescanTicksValue.get();
        shelterMinSpeed = shelterMinSpeedValue.get();
        shelterStress = shelterStressValue.get();
        shelterMinWallResistance = shelterMinWallResistanceValue.get();
        shelterMaxWallTransmission = shelterMaxWallTransmissionValue.get();
        shelterFilterFactor = shelterFilterFactorValue.get();
        shelterBlastFactor = shelterBlastFactorValue.get();
    }
}
