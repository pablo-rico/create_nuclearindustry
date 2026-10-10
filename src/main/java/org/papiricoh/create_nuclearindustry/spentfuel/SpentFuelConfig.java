package org.papiricoh.create_nuclearindustry.spentfuel;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for spent fuel cooling: pool racks and dry casks, under {@code spentFuel.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class SpentFuelConfig {
    private static ModConfigSpec.DoubleValue TIME_SCALE;
    private static ModConfigSpec.DoubleValue HOT_DOSE;
    private static ModConfigSpec.DoubleValue WARM_DOSE;
    private static ModConfigSpec.DoubleValue COOLED_DOSE;
    private static ModConfigSpec.DoubleValue AGED_DOSE;
    private static ModConfigSpec.DoubleValue HOT_BURN;
    private static ModConfigSpec.DoubleValue ASSEMBLY_HEAT;
    private static ModConfigSpec.DoubleValue POOL_COOLING;
    private static ModConfigSpec.IntValue POOL_SCAN_LIMIT;
    private static ModConfigSpec.DoubleValue BOIL_ENERGY;
    private static ModConfigSpec.DoubleValue WATER_TRANSMISSION;
    private static ModConfigSpec.DoubleValue COOLER_CAPACITY;
    private static ModConfigSpec.DoubleValue COOLER_RATED_SPEED;
    private static ModConfigSpec.DoubleValue COOLER_STRESS;
    private static ModConfigSpec.DoubleValue CASK_TRANSMISSION;

    public static double timeScale = 1.0;
    public static double hotDoseScale = 2.0;
    public static double warmDoseScale = 1.0;
    public static double cooledDoseScale = 0.65;
    public static double agedDoseScale = 0.4;
    public static double hotBurnHeat = 1.5;
    public static double assemblyDecayHeat = 20.0;
    public static double poolCoolingPerBlock = 0.4;
    public static int poolScanLimit = 4096;
    public static double boilEnergyPerBlock = 400.0;
    public static double waterTransmission = 0.15;
    public static double coolerCapacity = 60.0;
    public static double coolerRatedSpeed = 64.0;
    public static double coolerStress = 4.0;
    public static double caskTransmission = 0.02;

    private SpentFuelConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        TIME_SCALE = builder
                .comment("How fast spent fuel cools: in-game days since discharge are multiplied by this before reading the decay curve.",
                        "With 1, fuel is Hot for about 2 days, Warm until about a week, Cooled until about 3 weeks, then Aged.")
                .defineInRange("spentFuel.timeScale", 1.0, 0.01, 1000.0);
        HOT_DOSE = builder
                .comment("Dose multiplier of Hot spent fuel, on top of its radioactivity data map entry (which is that of Warm fuel).")
                .defineInRange("spentFuel.hotDoseScale", 2.0, 0.0, 100.0);
        WARM_DOSE = builder
                .comment("Dose multiplier of Warm spent fuel, and of spent fuel from before cooling existed.")
                .defineInRange("spentFuel.warmDoseScale", 1.0, 0.0, 100.0);
        COOLED_DOSE = builder
                .comment("Dose multiplier of Cooled spent fuel.")
                .defineInRange("spentFuel.cooledDoseScale", 0.65, 0.0, 100.0);
        AGED_DOSE = builder
                .comment("Dose multiplier of Aged spent fuel.")
                .defineInRange("spentFuel.agedDoseScale", 0.4, 0.0, 100.0);
        HOT_BURN = builder
                .comment("Decay heat of each carried Hot assembly, in the units of the radioactivity data map's heat_per_second.",
                        "Like plutonium, it burns players without at least half radiation shielding. 0 turns it off.")
                .defineInRange("spentFuel.hotBurnHeat", 1.5, 0.0, 100.0);
        ASSEMBLY_HEAT = builder
                .comment("Decay heat of one assembly in a pool rack right at discharge, in kW. It falls as the fuel cools.")
                .defineInRange("spentFuel.assemblyDecayHeat", 20.0, 0.0, 10000.0);
        POOL_COOLING = builder
                .comment("Heat each water source block of a pool carries away on its own, in kW.")
                .defineInRange("spentFuel.poolCoolingPerBlock", 0.4, 0.0, 10000.0);
        POOL_SCAN_LIMIT = builder
                .comment("Most water blocks a rack counts as its pool. A bigger body of water counts as this many and never boils.")
                .defineInRange("spentFuel.poolScanLimit", 4096, 64, 65536);
        BOIL_ENERGY = builder
                .comment("Surplus heat that boils off one water block, in kJ. The pool loses its whole top layer at once.")
                .defineInRange("spentFuel.boilEnergyPerBlock", 400.0, 1.0, 1.0e7);
        WATER_TRANSMISSION = builder
                .comment("Fraction of a rack's radiation each block of water directly above it lets through.")
                .defineInRange("spentFuel.waterTransmission", 0.15, 0.0, 1.0);
        COOLER_CAPACITY = builder
                .comment("Heat a Pool Cooler in the pool water carries away at its rated speed, in kW.")
                .defineInRange("spentFuel.coolerCapacity", 60.0, 0.0, 1.0e6);
        COOLER_RATED_SPEED = builder
                .comment("Speed at which a Pool Cooler reaches its full capacity, in RPM.")
                .defineInRange("spentFuel.coolerRatedSpeed", 64.0, 1.0, 256.0);
        COOLER_STRESS = builder
                .comment("Stress impact of a Pool Cooler, in SU per RPM.")
                .defineInRange("spentFuel.coolerStress", 4.0, 0.0, 1.0e6);
        CASK_TRANSMISSION = builder
                .comment("Fraction of its contents' radiation a Dry Cask lets out.")
                .defineInRange("spentFuel.caskTransmission", 0.02, 0.0, 1.0);
    }

    public static void bake() {
        timeScale = TIME_SCALE.get();
        hotDoseScale = HOT_DOSE.get();
        warmDoseScale = WARM_DOSE.get();
        cooledDoseScale = COOLED_DOSE.get();
        agedDoseScale = AGED_DOSE.get();
        hotBurnHeat = HOT_BURN.get();
        assemblyDecayHeat = ASSEMBLY_HEAT.get();
        poolCoolingPerBlock = POOL_COOLING.get();
        poolScanLimit = POOL_SCAN_LIMIT.get();
        boilEnergyPerBlock = BOIL_ENERGY.get();
        waterTransmission = WATER_TRANSMISSION.get();
        coolerCapacity = COOLER_CAPACITY.get();
        coolerRatedSpeed = COOLER_RATED_SPEED.get();
        coolerStress = COOLER_STRESS.get();
        caskTransmission = CASK_TRANSMISSION.get();
    }
}
