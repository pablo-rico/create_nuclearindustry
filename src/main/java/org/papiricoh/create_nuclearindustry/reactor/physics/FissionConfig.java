package org.papiricoh.create_nuclearindustry.reactor.physics;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.CoreDamageModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.CoreThermalModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.FissionCore;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.PointKinetics;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ReactivityModel;

/**
 * Common config for the fission core's physics, under {@code fission.kinetics.*},
 * {@code fission.reactivity.*}, {@code fission.thermal.*} and {@code fission.damage.*}. Values are
 * defined on the shared builder by {@link Config} and copied into public statics on load; the
 * simulator builds its parameter records from them.
 */
public final class FissionConfig {
    private static ModConfigSpec.BooleanValue SIX_GROUPS;
    private static ModConfigSpec.DoubleValue DELAYED_TIME_SCALE;
    private static ModConfigSpec.DoubleValue GENERATION_TIME;
    private static ModConfigSpec.DoubleValue SOURCE;
    private static ModConfigSpec.DoubleValue MAX_WITHDRAWAL_RATE;
    private static ModConfigSpec.DoubleValue MAX_POWER;
    private static ModConfigSpec.DoubleValue FUEL_EXCESS;
    private static ModConfigSpec.DoubleValue BURNUP_DEFECT;
    private static ModConfigSpec.DoubleValue EMPTY_CHANNEL_WORTH;
    private static ModConfigSpec.DoubleValue ROD_BANK_WORTH;
    private static ModConfigSpec.DoubleValue ROD_WORTH_SHAPE;
    private static ModConfigSpec.DoubleValue GRAPHITE_WORTH;
    private static ModConfigSpec.DoubleValue REFLECTOR_WORTH;
    private static ModConfigSpec.DoubleValue HEAVY_WATER_WORTH;
    private static ModConfigSpec.DoubleValue XENON_WORTH;
    private static ModConfigSpec.DoubleValue DOPPLER;
    private static ModConfigSpec.DoubleValue MODERATOR;
    private static ModConfigSpec.DoubleValue VOID;
    private static ModConfigSpec.DoubleValue GRAPHITE_MODERATOR;
    private static ModConfigSpec.DoubleValue GRAPHITE_VOID;
    private static ModConfigSpec.DoubleValue HEAVY_WATER_VOID;
    private static ModConfigSpec.DoubleValue DAMAGE_WORTH;
    private static ModConfigSpec.DoubleValue DRY_CORE_WORTH;
    private static ModConfigSpec.DoubleValue RATED_POWER_PER_ROD;
    private static ModConfigSpec.DoubleValue FUEL_RISE;
    private static ModConfigSpec.DoubleValue FUEL_HEAT_CAPACITY;
    private static ModConfigSpec.DoubleValue COOLANT_HEAT_CAPACITY;
    private static ModConfigSpec.DoubleValue STRUCTURE_HEAT_CAPACITY;
    private static ModConfigSpec.DoubleValue BOILING_TEMPERATURE;
    private static ModConfigSpec.DoubleValue STEAM_ENERGY;
    private static ModConfigSpec.DoubleValue HEAVY_STEAM_ENERGY;
    private static ModConfigSpec.DoubleValue RATED_VOID;
    private static ModConfigSpec.DoubleValue CHF_MARGIN;
    private static ModConfigSpec.DoubleValue MAX_FUEL_TEMPERATURE;
    private static ModConfigSpec.DoubleValue MAX_SUPERHEAT;
    private static ModConfigSpec.DoubleValue CLADDING_FAILURE;
    private static ModConfigSpec.DoubleValue FUEL_MELTING;
    private static ModConfigSpec.DoubleValue CLADDING_RATE;
    private static ModConfigSpec.DoubleValue MELTING_RATE;
    private static ModConfigSpec.DoubleValue MELTING_RATE_MAX;
    private static ModConfigSpec.DoubleValue MELTDOWN_DAMAGE;
    private static ModConfigSpec.DoubleValue FUEL_BURN;

    public static boolean sixGroups = true;
    public static double delayedTimeScale = 1.0;
    public static double promptGenerationTime = 1.0e-3;
    public static double sourceStrength = 1.0e-3;
    public static double maxWithdrawalRate = 100;
    public static double maxRelativePower = 10;
    public static double fuelExcess = 12000;
    public static double burnupDefect = 0.6;
    public static double emptyChannelWorth = 24000;
    public static double rodBankWorth = 22000;
    public static double rodWorthShape = 0.5;
    public static double graphiteWorth = 1500;
    public static double reflectorWorth = 1000;
    public static double heavyWaterWorth = 500;
    public static double xenonWorthPcm = 2800;
    public static double dopplerCoefficient = -2.0;
    public static double moderatorCoefficient = -4.0;
    public static double voidCoefficient = -15.0;
    public static double graphiteModeratorCoefficient = -1.0;
    public static double graphiteVoidCoefficient = 3.0;
    public static double heavyWaterVoidCoefficient = -5.0;
    public static double damageWorth = 2000;
    public static double dryCoreWorth = 20000;
    public static double ratedPowerPerRod = 50;
    public static double fuelRiseAtRated = 600;
    public static double fuelHeatCapacity = 0.5;
    public static double coolantHeatCapacity = 1.5;
    public static double structureHeatCapacity = 0.2;
    public static double boilingTemperature = 285;
    public static double steamEnergy = 4.0;
    public static double heavyWaterSteamEnergy = 6.2;
    public static double ratedVoid = 0.30;
    public static double chfMargin = 1.25;
    public static double maxFuelTemperature = 4500;
    public static double maxSuperheat = 50;
    public static double claddingFailureTemperature = 1500;
    public static double fuelMeltingTemperature = 2800;
    public static double claddingDamageRate = 0.004;
    public static double meltingDamageRate = 0.01;
    public static double meltingDamageRateMax = 0.1;
    public static double meltdownDamage = 0.75;
    public static double fuelBurnPerRod = 7.5e-4;

    private FissionConfig() {}

    public static void define(ModConfigSpec.Builder b) {
        SIX_GROUPS = b.comment("Model the delayed neutrons in six groups (true) or one (false).")
                .define("fission.kinetics.sixGroups", true);
        DELAYED_TIME_SCALE = b.comment("Speeds up the decay of the delayed neutron precursors. 1 is real U-235; higher makes",
                        "the reactor answer its rods faster.")
                .defineInRange("fission.kinetics.delayedTimeScale", 1.0, 0.1, 100.0);
        GENERATION_TIME = b.comment("Prompt neutron generation time, in seconds. Only matters near and above prompt criticality.")
                .defineInRange("fission.kinetics.promptGenerationTime", 1.0e-3, 1.0e-6, 1.0);
        SOURCE = b.comment("Neutron source keeping a shut-down core faintly alive, as a share of rated power per second.")
                .defineInRange("fission.kinetics.sourceStrength", 1.0e-3, 0.0, 1.0);
        MAX_WITHDRAWAL_RATE = b.comment("Reactivity a control rod withdrawal frees per second at most, in pcm: the speed of the rod",
                        "drives. Pistons jerking rods out never take the core prompt critical at once. Inserting is immediate.")
                .defineInRange("fission.kinetics.maxWithdrawalRate", 100.0, 1.0, 100000.0);
        MAX_POWER = b.comment("Highest power the chain reaction can reach, as a multiple of the rated power.")
                .defineInRange("fission.kinetics.maxRelativePower", 10.0, 1.0, 100.0);

        FUEL_EXCESS = b.comment("Excess reactivity of fresh 5% uranium with nothing absorbing, in pcm. Richer fuel and MOX scale it.")
                .defineInRange("fission.reactivity.fuelExcess", 12000.0, 0.0, 100000.0);
        BURNUP_DEFECT = b.comment("Share of the fuel's excess reactivity gone at the end of its life (thorium breeds and keeps it).")
                .defineInRange("fission.reactivity.burnupDefect", 0.6, 0.0, 1.0);
        EMPTY_CHANNEL_WORTH = b.comment("Reactivity lost when every fuel channel is empty, in pcm (a partly loaded core loses its share).")
                .defineInRange("fission.reactivity.emptyChannelWorth", 24000.0, 0.0, 100000.0);
        ROD_BANK_WORTH = b.comment("Worth of a full set of control rods, in pcm. Fewer rod channels than fuel scale it down",
                        "(fission.boronAbsorption x channels / fuel, at most 1).")
                .defineInRange("fission.reactivity.rodBankWorth", 22000.0, 0.0, 100000.0);
        ROD_WORTH_SHAPE = b.comment("How much rods are worth more in the middle of the core: 0 the same along their length, 1 a full S curve.")
                .defineInRange("fission.reactivity.rodWorthShape", 0.5, 0.0, 1.0);
        GRAPHITE_WORTH = b.comment("Reactivity added by graphite round every fuel column, in pcm.")
                .defineInRange("fission.reactivity.graphiteWorth", 1500.0, 0.0, 100000.0);
        REFLECTOR_WORTH = b.comment("Reactivity added by beryllium lining the whole inner ring, in pcm.")
                .defineInRange("fission.reactivity.reflectorWorth", 1000.0, 0.0, 100000.0);
        HEAVY_WATER_WORTH = b.comment("Reactivity added by heavy water as coolant, in pcm.")
                .defineInRange("fission.reactivity.heavyWaterWorth", 500.0, 0.0, 100000.0);
        XENON_WORTH = b.comment("Reactivity a fully poisoned core loses to xenon-135, in pcm. A core at full power settles near",
                        "half of it; the peak after a shutdown (the iodine pit) is higher.")
                .defineInRange("fission.reactivity.xenonWorthPcm", 2800.0, 0.0, 100000.0);
        DOPPLER = b.comment("Fuel temperature coefficient, pcm per kelvin. Negative: hot fuel holds its own power back.")
                .defineInRange("fission.reactivity.dopplerCoefficient", -2.0, -100.0, 100.0);
        MODERATOR = b.comment("Coolant temperature coefficient of a water moderated core, pcm per kelvin.")
                .defineInRange("fission.reactivity.moderatorCoefficient", -4.0, -100.0, 100.0);
        VOID = b.comment("Steam void coefficient of a water moderated core, pcm per percent of void.")
                .defineInRange("fission.reactivity.voidCoefficient", -15.0, -1000.0, 1000.0);
        GRAPHITE_MODERATOR = b.comment("Coolant temperature coefficient with graphite round every fuel column, pcm per kelvin.")
                .defineInRange("fission.reactivity.graphiteModeratorCoefficient", -1.0, -100.0, 100.0);
        GRAPHITE_VOID = b.comment("Void coefficient with graphite round every fuel column, pcm per percent. Positive: graphite keeps",
                        "moderating when the water boils away, so boiling adds reactivity.")
                .defineInRange("fission.reactivity.graphiteVoidCoefficient", 3.0, -1000.0, 1000.0);
        HEAVY_WATER_VOID = b.comment("Void coefficient of a heavy water cooled core, pcm per percent.")
                .defineInRange("fission.reactivity.heavyWaterVoidCoefficient", -5.0, -1000.0, 1000.0);
        DAMAGE_WORTH = b.comment("Reactivity a fully damaged core loses as its fuel slumps, in pcm.")
                .defineInRange("fission.reactivity.damageWorth", 2000.0, 0.0, 100000.0);
        DRY_CORE_WORTH = b.comment("Reactivity a water moderated core loses with no water at all, in pcm: without its moderator the",
                        "chain reaction stops. Graphite keeps moderating, so it counts for less in a graphite core.")
                .defineInRange("fission.reactivity.dryCoreWorth", 20000.0, 0.0, 100000.0);

        RATED_POWER_PER_ROD = b.comment("Thermal power of one fuel rod block at rated power, in MW. The reactor's rated power is this",
                        "times its fuel rods (MOX runs hotter, thorium cooler).")
                .defineInRange("fission.thermal.ratedPowerPerRod", 50.0, 0.1, 10000.0);
        FUEL_RISE = b.comment("Fuel temperature above its coolant at rated power, in kelvin.")
                .defineInRange("fission.thermal.fuelRiseAtRated", 600.0, 10.0, 5000.0);
        FUEL_HEAT_CAPACITY = b.comment("Heat capacity of one rod's fuel, MJ per kelvin. Higher makes the core heat and cool slower.")
                .defineInRange("fission.thermal.fuelHeatCapacity", 0.5, 0.01, 100.0);
        COOLANT_HEAT_CAPACITY = b.comment("Heat capacity of the water round one rod in a full core, MJ per kelvin.")
                .defineInRange("fission.thermal.coolantHeatCapacity", 1.5, 0.01, 100.0);
        STRUCTURE_HEAT_CAPACITY = b.comment("Heat capacity of the steel round one rod, MJ per kelvin.")
                .defineInRange("fission.thermal.structureHeatCapacity", 0.2, 0.01, 100.0);
        BOILING_TEMPERATURE = b.comment("Temperature the core's water boils at, Celsius.")
                .defineInRange("fission.thermal.boilingTemperature", 285.0, 100.0, 400.0);
        STEAM_ENERGY = b.comment("Heat one mB per tick of water carries off as steam, in MW. Steam output is the heat removed",
                        "divided by this, and the water fed in follows it. The heat exchangers keep their MW whatever it is;",
                        "the ports, pumps and turbines move mB, so a higher value needs fewer of them per MW.")
                .defineInRange("fission.thermal.steamEnergyPerMb", 4.0, 0.01, 1000.0);
        HEAVY_STEAM_ENERGY = b.comment("The same for heavy water, in MW per mB/t.")
                .defineInRange("fission.thermal.heavySteamEnergyPerMb", 6.2, 0.01, 1000.0);
        RATED_VOID = b.comment("Steam void of a core boiling off its rated power, 0-1.")
                .defineInRange("fission.thermal.ratedVoid", 0.30, 0.0, 1.0);
        CHF_MARGIN = b.comment("Critical heat flux over what the heat exchangers rate: above it the fuel blankets in steam and",
                        "passes on only a tenth of the excess.")
                .defineInRange("fission.thermal.chfMargin", 1.25, 1.0, 100.0);
        MAX_FUEL_TEMPERATURE = b.comment("The fuel never gets hotter than this, Celsius.")
                .defineInRange("fission.thermal.maxFuelTemperature", 4500.0, 1000.0, 10000.0);
        MAX_SUPERHEAT = b.comment("How far above boiling the core's water can be driven when its steam cannot leave, in kelvin.",
                        "Past it the water takes no more heat: the fuel heats up instead, so a steam-bound core still",
                        "answers its rods with its temperature.")
                .defineInRange("fission.thermal.maxSuperheat", 50.0, 5.0, 1000.0);

        CLADDING_FAILURE = b.comment("Fuel temperature where the cladding starts to fail and the core takes damage, Celsius.",
                        "The core's Warning state and its over-temperature alarm start here.")
                .defineInRange("fission.damage.claddingFailureTemperature", 1500.0, 500.0, 10000.0);
        FUEL_MELTING = b.comment("Fuel temperature where the fuel itself melts and the damage runs fast, Celsius.")
                .defineInRange("fission.damage.fuelMeltingTemperature", 2800.0, 500.0, 10000.0);
        CLADDING_RATE = b.comment("Core damage per second 800 K above cladding failure (it grows with the square of the overheat).")
                .defineInRange("fission.damage.claddingRate", 0.004, 0.0, 10.0);
        MELTING_RATE = b.comment("Core damage per second as the fuel starts to melt.")
                .defineInRange("fission.damage.meltingRate", 0.01, 0.0, 10.0);
        MELTING_RATE_MAX = b.comment("Core damage per second 1200 K above fuel melting.")
                .defineInRange("fission.damage.meltingRateMax", 0.1, 0.0, 10.0);
        MELTDOWN_DAMAGE = b.comment("Core damage at which the core melts down (0-1).")
                .defineInRange("fission.damage.meltdownDamage", 0.75, 0.01, 1.0);

        FUEL_BURN = b.comment("Fuel units each rod burns per tick at rated power. Zero disables fuel depletion.")
                .defineInRange("fission.fuelBurnPerRod", 7.5e-4, 0.0, 100.0);
    }

    public static void bake() {
        sixGroups = SIX_GROUPS.get();
        delayedTimeScale = DELAYED_TIME_SCALE.get();
        promptGenerationTime = GENERATION_TIME.get();
        sourceStrength = SOURCE.get();
        maxWithdrawalRate = MAX_WITHDRAWAL_RATE.get();
        maxRelativePower = MAX_POWER.get();
        fuelExcess = FUEL_EXCESS.get();
        burnupDefect = BURNUP_DEFECT.get();
        emptyChannelWorth = EMPTY_CHANNEL_WORTH.get();
        rodBankWorth = ROD_BANK_WORTH.get();
        rodWorthShape = ROD_WORTH_SHAPE.get();
        graphiteWorth = GRAPHITE_WORTH.get();
        reflectorWorth = REFLECTOR_WORTH.get();
        heavyWaterWorth = HEAVY_WATER_WORTH.get();
        xenonWorthPcm = XENON_WORTH.get();
        dopplerCoefficient = DOPPLER.get();
        moderatorCoefficient = MODERATOR.get();
        voidCoefficient = VOID.get();
        graphiteModeratorCoefficient = GRAPHITE_MODERATOR.get();
        graphiteVoidCoefficient = GRAPHITE_VOID.get();
        heavyWaterVoidCoefficient = HEAVY_WATER_VOID.get();
        damageWorth = DAMAGE_WORTH.get();
        dryCoreWorth = DRY_CORE_WORTH.get();
        ratedPowerPerRod = RATED_POWER_PER_ROD.get();
        fuelRiseAtRated = FUEL_RISE.get();
        fuelHeatCapacity = FUEL_HEAT_CAPACITY.get();
        coolantHeatCapacity = COOLANT_HEAT_CAPACITY.get();
        structureHeatCapacity = STRUCTURE_HEAT_CAPACITY.get();
        boilingTemperature = BOILING_TEMPERATURE.get();
        steamEnergy = STEAM_ENERGY.get();
        heavyWaterSteamEnergy = HEAVY_STEAM_ENERGY.get();
        ratedVoid = RATED_VOID.get();
        chfMargin = CHF_MARGIN.get();
        maxFuelTemperature = MAX_FUEL_TEMPERATURE.get();
        maxSuperheat = MAX_SUPERHEAT.get();
        claddingFailureTemperature = CLADDING_FAILURE.get();
        fuelMeltingTemperature = Math.max(FUEL_MELTING.get(), claddingFailureTemperature + 1);
        claddingDamageRate = CLADDING_RATE.get();
        meltingDamageRate = MELTING_RATE.get();
        meltingDamageRateMax = Math.max(MELTING_RATE_MAX.get(), meltingDamageRate);
        meltdownDamage = MELTDOWN_DAMAGE.get();
        fuelBurnPerRod = FUEL_BURN.get();
    }

    /** Every parameter of the core's physics, from the current config. */
    public static FissionCore.Params core() {
        return new FissionCore.Params(
                new PointKinetics.Params(promptGenerationTime, sourceStrength, maxRelativePower, 4),
                new ReactivityModel.Params(fuelExcess, burnupDefect, emptyChannelWorth, rodBankWorth, rodWorthShape, graphiteWorth,
                        reflectorWorth, heavyWaterWorth, dopplerCoefficient, moderatorCoefficient, voidCoefficient,
                        graphiteModeratorCoefficient, graphiteVoidCoefficient, heavyWaterVoidCoefficient, damageWorth,
                        dryCoreWorth),
                thermal(),
                new CoreDamageModel.Params(claddingFailureTemperature, fuelMeltingTemperature, claddingDamageRate, meltingDamageRate,
                        meltingDamageRateMax, meltdownDamage),
                sixGroups, delayedTimeScale, maxWithdrawalRate, xenonWorthPcm, Config.xenonPoisoning, Config.iodineHalfLifeTicks,
                Config.xenonHalfLifeTicks, Config.decayHeatFraction, Config.decayHeatTimeTicks);
    }

    public static CoreThermalModel.Params thermal() {
        CoreThermalModel.Params d = CoreThermalModel.Params.DEFAULT;
        return new CoreThermalModel.Params(ratedPowerPerRod, fuelRiseAtRated, fuelHeatCapacity, coolantHeatCapacity,
                structureHeatCapacity, boilingTemperature, ratedVoid, d.passiveLoss(), d.radiation(), chfMargin, maxFuelTemperature,
                maxSuperheat);
    }

    /** Fuel temperature, Celsius, from which the core reads Warning: where its cladding starts to fail. */
    public static double warningTemperature() {
        return claddingFailureTemperature;
    }

    /** Fuel temperature, Celsius, from which the core reads Critical: halfway from cladding failure to melting. */
    public static double dangerTemperature() {
        return (claddingFailureTemperature + fuelMeltingTemperature) / 2;
    }

    /** Fuel temperature, Celsius, from which the core reads Melting: where its fuel melts. */
    public static double meltdownTemperature() {
        return fuelMeltingTemperature;
    }
}
