package org.papiricoh.create_nuclearindustry.reactor.pwr;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the pressurized water loop, steam generator, containment and the accident chain, under {@code pwr.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class PwrConfig {
    private static ModConfigSpec.DoubleValue COLD_LEG_TEMPERATURE;
    private static ModConfigSpec.DoubleValue CORE_TEMPERATURE_RISE;
    private static ModConfigSpec.DoubleValue ENTHALPY_PER_MB;
    private static ModConfigSpec.DoubleValue HEAT_TRANSFER;
    private static ModConfigSpec.DoubleValue NATURAL_CIRCULATION;
    private static ModConfigSpec.DoubleValue PRESSURE_SETPOINT;
    private static ModConfigSpec.DoubleValue HEATER_RATE;
    private static ModConfigSpec.DoubleValue RELIEF_OPEN;
    private static ModConfigSpec.DoubleValue RELIEF_CLOSE;
    private static ModConfigSpec.DoubleValue RELIEF_STICK_CHANCE;
    private static ModConfigSpec.DoubleValue VOID_WORTH;
    private static ModConfigSpec.DoubleValue TEMPERATURE_COEFFICIENT;
    private static ModConfigSpec.DoubleValue SG_UA_PER_TUBE;
    private static ModConfigSpec.DoubleValue SG_MIN_PRESSURE;
    private static ModConfigSpec.DoubleValue SG_MAX_PRESSURE;
    private static ModConfigSpec.IntValue CONTAINMENT_MAX_VOLUME;
    private static ModConfigSpec.IntValue CONTAINMENT_CELLS_PER_TICK;
    private static ModConfigSpec.DoubleValue CONTAINMENT_DOOR_INTERLOCK;
    private static ModConfigSpec.DoubleValue MOLES_PER_MB;
    private static ModConfigSpec.DoubleValue CONDENSATION;
    private static ModConfigSpec.DoubleValue RECOMBINER_RATE;
    private static ModConfigSpec.DoubleValue VENT_FLOW;
    private static ModConfigSpec.DoubleValue VENT_DECONTAMINATION;
    private static ModConfigSpec.DoubleValue IGNITION_CHANCE;
    private static ModConfigSpec.BooleanValue BLOCK_DESTRUCTION;
    private static ModConfigSpec.BooleanValue HYDROGEN;
    private static ModConfigSpec.BooleanValue CORIUM;
    private static ModConfigSpec.BooleanValue STEAM_EXPLOSIONS;
    private static ModConfigSpec.BooleanValue CONTAINMENT_RUPTURE;
    private static ModConfigSpec.BooleanValue LEGACY_MELTDOWN;
    private static ModConfigSpec.DoubleValue OXIDATION_TEMPERATURE;
    private static ModConfigSpec.DoubleValue OXIDATION_RATE;
    private static ModConfigSpec.DoubleValue ZIRCONIUM_PER_ROD;
    private static ModConfigSpec.DoubleValue OXIDATION_HEAT;
    private static ModConfigSpec.DoubleValue VESSEL_FAILURE_PRESSURE;
    private static ModConfigSpec.IntValue VESSEL_FAILURE_TICKS;
    private static ModConfigSpec.DoubleValue STEAM_EXPLOSION_TEMPERATURE;

    public static double coldLegTemperature = 290.0;
    public static double coreTemperatureRise = 35.0;
    public static double enthalpyPerMb = 0.12;
    public static double heatTransfer = 0.02;
    public static double naturalCirculation = 8.0;
    public static double pressureSetpoint = 155.0;
    public static double heaterRate = 0.25;
    public static double reliefOpenPressure = 170.0;
    public static double reliefClosePressure = 160.0;
    public static double reliefStickChance = 0.05;
    public static double voidWorth = 0.6;
    public static double temperatureCoefficient = 0.002;
    public static double steamGeneratorUaPerTube = 0.3;
    public static double steamGeneratorMinPressure = 50.0;
    public static double steamGeneratorMaxPressure = 120.0;
    public static int containmentMaxVolume = 32_768;
    public static int containmentCellsPerTick = 4_096;
    public static double containmentDoorInterlock = 0.2;
    public static double molesPerMb = 10.0;
    public static double condensation = 0.05;
    public static double recombinerRate = 40.0;
    public static double ventFlow = 0.002;
    public static double ventDecontamination = 100.0;
    public static double hydrogenIgnitionChance = 0.1;
    public static boolean blockDestruction = true;
    public static boolean hydrogen = true;
    public static boolean corium = true;
    public static boolean steamExplosions = true;
    public static boolean containmentRupture = true;
    public static boolean legacyMeltdown = false;
    public static double oxidationTemperature = 1200.0;
    public static double oxidationRate = 0.5;
    public static double zirconiumPerRod = 500.0;
    public static double oxidationHeat = 1.2;
    public static double vesselFailurePressure = 250.0;
    public static int vesselFailureTicks = 100;
    public static double steamExplosionTemperature = 2800.0;

    private PwrConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        COLD_LEG_TEMPERATURE = builder
                .comment("Temperature of the primary coolant a steam generator hands back, in °C.")
                .defineInRange("pwr.coldLegTemperature", 290.0, 150.0, 340.0);
        CORE_TEMPERATURE_RISE = builder
                .comment("How much the primary coolant warms crossing the core at full flow, in °C. The hot leg is the cold leg plus this.")
                .defineInRange("pwr.coreTemperatureRise", 35.0, 5.0, 80.0);
        ENTHALPY_PER_MB = builder
                .comment("Heat one mB of primary coolant carries from the core to the steam generator.",
                        "A steam generator boils its feedwater with 0.225 per mB, so a lower value needs more flow.")
                .defineInRange("pwr.enthalpyPerMb", 0.12, 0.01, 10.0);
        HEAT_TRANSFER = builder
                .comment("Core cooling per °C between the core and the cold leg, in °C per tick, while the water covers the core.")
                .defineInRange("pwr.heatTransfer", 0.02, 0.0, 1.0);
        NATURAL_CIRCULATION = builder
                .comment("Coolant that still crosses the core with every pump stopped, in mB per tick.")
                .defineInRange("pwr.naturalCirculation", 8.0, 0.0, 1000.0);
        PRESSURE_SETPOINT = builder
                .comment("Primary pressure the automatic pressurizer heaters and spray hold, in bar.")
                .defineInRange("pwr.pressureSetpoint", 155.0, 20.0, 210.0);
        HEATER_RATE = builder
                .comment("°C per tick each running heater port adds to the pressurizer's steam bubble.")
                .defineInRange("pwr.heaterRate", 0.25, 0.0, 10.0);
        RELIEF_OPEN = builder
                .comment("Pressure the relief valve opens at, in bar.")
                .defineInRange("pwr.reliefOpenPressure", 170.0, 30.0, 400.0);
        RELIEF_CLOSE = builder
                .comment("Pressure the relief valve closes at again, in bar. Must be below the opening pressure.")
                .defineInRange("pwr.reliefClosePressure", 160.0, 20.0, 400.0);
        RELIEF_STICK_CHANCE = builder
                .comment("Chance the relief valve sticks open each time it should close. A redstone signal on the valve shuts",
                        "its block valve and frees it.")
                .defineInRange("pwr.reliefStickChance", 0.05, 0.0, 1.0);
        VOID_WORTH = builder
                .comment("Share of the chain reaction lost when the core holds no water: steam or an uncovered core moderate less.")
                .defineInRange("pwr.voidWorth", 0.6, 0.0, 1.0);
        TEMPERATURE_COEFFICIENT = builder
                .comment("Share of the chain reaction lost per °C the coolant runs above its normal average.")
                .defineInRange("pwr.temperatureCoefficient", 0.002, 0.0, 0.1);
        SG_UA_PER_TUBE = builder
                .comment("Heat each Steam Generator Tube Bundle moves per °C between the primary and the boiling secondary, per tick.")
                .defineInRange("pwr.steamGenerator.uaPerTube", 0.3, 0.0, 100.0);
        SG_MIN_PRESSURE = builder
                .comment("Secondary pressure of a steam generator whose steam is taken away as it is made, in bar.")
                .defineInRange("pwr.steamGenerator.minPressure", 50.0, 1.5, 200.0);
        SG_MAX_PRESSURE = builder
                .comment("Secondary pressure of a steam generator with a full steam tank, in bar. Near the hot leg's boiling",
                        "pressure the generator stops taking heat.")
                .defineInRange("pwr.steamGenerator.maxPressure", 120.0, 2.0, 220.0);

        CONTAINMENT_MAX_VOLUME = builder
                .comment("Most blocks of air a containment may enclose; a bigger space counts as open to the outside.")
                .defineInRange("pwr.containment.maxVolume", 32_768, 64, 262_144);
        CONTAINMENT_CELLS_PER_TICK = builder
                .comment("Blocks a Containment Monitor checks per tick while it measures its building, to spread the work.")
                .defineInRange("pwr.containment.cellsPerTick", 4_096, 64, 65_536);
        CONTAINMENT_DOOR_INTERLOCK = builder
                .comment("Overpressure, in bar above the outside, at which a Containment Door in a sealed containment's wall stays shut.",
                        "0 turns the interlock off.")
                .defineInRange("pwr.containment.doorInterlock", 0.2, 0.0, 100.0);
        MOLES_PER_MB = builder
                .comment("Gas one mB of reactor coolant makes when it flashes to steam in a containment, in mol.",
                        "Real water makes 55; the default keeps game-sized buildings within their ratings.")
                .defineInRange("pwr.containment.molesPerMb", 10.0, 0.1, 100.0);
        CONDENSATION = builder
                .comment("Steam the walls condense, in mol per tick per block face of wall per bar of steam.")
                .defineInRange("pwr.containment.condensation", 0.05, 0.0, 10.0);
        RECOMBINER_RATE = builder
                .comment("Hydrogen one Passive Autocatalytic Recombiner removes per tick, in mol, times the hydrogen",
                        "fraction (at 4% it removes 4% of this). Recombiners start working above 0.5%.")
                .defineInRange("pwr.containment.recombinerRate", 40.0, 0.0, 10_000.0);
        VENT_FLOW = builder
                .comment("Share of the containment's gas one open Filtered Vent lets out per tick, per bar above the outside.")
                .defineInRange("pwr.containment.ventFlow", 0.002, 0.0, 0.5);
        VENT_DECONTAMINATION = builder
                .comment("Decontamination factor of the Filtered Vent: the activity it lets out is divided by this.")
                .defineInRange("pwr.containment.ventDecontamination", 100.0, 1.0, 1.0e6);
        IGNITION_CHANCE = builder
                .comment("Chance per second that a containment with 4-8% hydrogen ignites. From 8% it always does.")
                .defineInRange("pwr.containment.hydrogenIgnitionChance", 0.1, 0.0, 1.0);

        BLOCK_DESTRUCTION = builder
                .comment("Accidents may break blocks: explosions, corium melting through floors, a ruptured containment wall.",
                        "False keeps every effect but leaves the world's blocks alone; only the molten fuel itself changes.")
                .define("pwr.accidents.blockDestruction", true);
        HYDROGEN = builder
                .comment("Hot fuel cladding makes hydrogen, which can burn inside a containment.")
                .define("pwr.accidents.hydrogen", true);
        CORIUM = builder
                .comment("A meltdown leaves molten corium that melts its way down and freezes into radioactive slag.",
                        "False turns the fuel straight into slag where it stands.")
                .define("pwr.accidents.corium", true);
        STEAM_EXPLOSIONS = builder
                .comment("Water reaching a core hotter than steamExplosionTemperature, or corium touching water, explodes.")
                .define("pwr.accidents.steamExplosions", true);
        CONTAINMENT_RUPTURE = builder
                .comment("A containment whose pressure passes its weakest wall's rating bursts there and releases everything inside.")
                .define("pwr.accidents.containmentRupture", true);
        LEGACY_MELTDOWN = builder
                .comment("Brings back the old meltdown: a power 80 explosion and wide fallout, instead of the realistic chain.")
                .define("pwr.accidents.legacyMeltdown", false);
        OXIDATION_TEMPERATURE = builder
                .comment("Core temperature where the zirconium cladding starts reacting with steam, in °C.")
                .defineInRange("pwr.accidents.oxidationTemperature", 1200.0, 300.0, 4000.0);
        OXIDATION_RATE = builder
                .comment("Zirconium each fuel rod loses per tick 300 °C above that, in mol. It grows with the square of the excess.")
                .defineInRange("pwr.accidents.oxidationRate", 0.5, 0.0, 100.0);
        ZIRCONIUM_PER_ROD = builder
                .comment("Zirconium in the cladding of one fuel rod, in mol: each mol makes two mol of hydrogen.")
                .defineInRange("pwr.accidents.zirconiumPerRod", 500.0, 0.0, 100_000.0);
        OXIDATION_HEAT = builder
                .comment("Heat of each mol of zirconium oxidised per rod, in kelvin of that rod's fuel",
                        "(times fission.thermal.fuelHeatCapacity, in MJ). The whole cladding adds at most zirconiumPerRod times this.")
                .defineInRange("pwr.accidents.oxidationHeat", 1.2, 0.0, 100.0);
        VESSEL_FAILURE_PRESSURE = builder
                .comment("Primary pressure a pressurized reactor's vessel fails above, in bar, if held long enough.")
                .defineInRange("pwr.accidents.vesselFailurePressure", 250.0, 50.0, 400.0);
        VESSEL_FAILURE_TICKS = builder
                .comment("Ticks above that pressure before the vessel breaks.")
                .defineInRange("pwr.accidents.vesselFailureTicks", 100, 1, 72_000);
        STEAM_EXPLOSION_TEMPERATURE = builder
                .comment("Core temperature above which water pumped back into a dried-out core explodes, in °C.")
                .defineInRange("pwr.accidents.steamExplosionTemperature", 2800.0, 500.0, 4000.0);
    }

    public static void bake() {
        coldLegTemperature = COLD_LEG_TEMPERATURE.get();
        coreTemperatureRise = CORE_TEMPERATURE_RISE.get();
        enthalpyPerMb = ENTHALPY_PER_MB.get();
        heatTransfer = HEAT_TRANSFER.get();
        naturalCirculation = NATURAL_CIRCULATION.get();
        pressureSetpoint = PRESSURE_SETPOINT.get();
        heaterRate = HEATER_RATE.get();
        reliefOpenPressure = RELIEF_OPEN.get();
        reliefClosePressure = Math.min(RELIEF_CLOSE.get(), RELIEF_OPEN.get() - 1.0);
        reliefStickChance = RELIEF_STICK_CHANCE.get();
        voidWorth = VOID_WORTH.get();
        temperatureCoefficient = TEMPERATURE_COEFFICIENT.get();
        steamGeneratorUaPerTube = SG_UA_PER_TUBE.get();
        steamGeneratorMinPressure = SG_MIN_PRESSURE.get();
        steamGeneratorMaxPressure = Math.max(SG_MAX_PRESSURE.get(), SG_MIN_PRESSURE.get());
        containmentMaxVolume = CONTAINMENT_MAX_VOLUME.get();
        containmentCellsPerTick = CONTAINMENT_CELLS_PER_TICK.get();
        containmentDoorInterlock = CONTAINMENT_DOOR_INTERLOCK.get();
        molesPerMb = MOLES_PER_MB.get();
        condensation = CONDENSATION.get();
        recombinerRate = RECOMBINER_RATE.get();
        ventFlow = VENT_FLOW.get();
        ventDecontamination = VENT_DECONTAMINATION.get();
        hydrogenIgnitionChance = IGNITION_CHANCE.get();
        blockDestruction = BLOCK_DESTRUCTION.get();
        hydrogen = HYDROGEN.get();
        corium = CORIUM.get();
        steamExplosions = STEAM_EXPLOSIONS.get();
        containmentRupture = CONTAINMENT_RUPTURE.get();
        legacyMeltdown = LEGACY_MELTDOWN.get();
        oxidationTemperature = OXIDATION_TEMPERATURE.get();
        oxidationRate = OXIDATION_RATE.get();
        zirconiumPerRod = ZIRCONIUM_PER_ROD.get();
        oxidationHeat = OXIDATION_HEAT.get();
        vesselFailurePressure = VESSEL_FAILURE_PRESSURE.get();
        vesselFailureTicks = VESSEL_FAILURE_TICKS.get();
        steamExplosionTemperature = STEAM_EXPLOSION_TEMPERATURE.get();
    }

    /** Hot leg temperature at full flow, °C. */
    public static double hotLegTemperature() {
        return coldLegTemperature + coreTemperatureRise;
    }
}
