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
                        "A reactor boiling its water directly takes 0.225 per mB, so a lower value needs more flow.")
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
    }

    /** Hot leg temperature at full flow, °C. */
    public static double hotLegTemperature() {
        return coldLegTemperature + coreTemperatureRise;
    }
}
