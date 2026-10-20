package org.papiricoh.create_nuclearindustry.missile.defense;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for missile defence, under {@code missile.defense.*}: interceptor batteries, interceptor launch controllers and
 * the chance they kill. Defined on the shared builder by {@code MissileConfig} and copied into public statics on load.
 */
public final class DefenseConfig {
    public static boolean enabled = true;
    public static int autoIntervalTicks = 10;
    public static int trackMinSeconds = 2;
    public static boolean engageOwnMissiles = false;
    public static double fuseRadius = 6.0;
    public static double pkScale = 1.0;
    public static double maxPk = 0.95;
    public static double killExplosionPower = 1.5;
    public static boolean debris = true;
    public static int solverStepTicks = 4;
    public static int orderExpireTicks = 100;

    public static double batteryRange = 600;
    public static double batteryMinAltitude = 48;
    public static double batteryCeiling = 480;
    public static double batteryDeltaV = 9.0;
    public static double batteryAcceleration = 0.5;
    public static double batteryEjectSpeed = 0.6;
    public static int batteryReloadSeconds = 3;
    public static double batterySlewDegreesPerRpm = 0.05;
    public static double batteryStressImpact = 16;
    public static double batteryBasePk = 0.75;
    public static double batteryTerminalPkFactor = 0.7;

    public static double siloBasePk = 0.85;
    public static double siloKillVehicleMass = 0.4;
    public static int siloLaunchDelayTicks = 40;
    public static double siloMaxRange = 8000;

    private static ModConfigSpec.BooleanValue enabledValue, engageOwnMissilesValue, debrisValue;
    private static ModConfigSpec.IntValue autoIntervalTicksValue, trackMinSecondsValue, solverStepTicksValue, orderExpireTicksValue,
            batteryReloadSecondsValue, siloLaunchDelayTicksValue;
    private static ModConfigSpec.DoubleValue fuseRadiusValue, pkScaleValue, maxPkValue, killExplosionPowerValue, batteryRangeValue,
            batteryMinAltitudeValue, batteryCeilingValue, batteryDeltaVValue, batteryAccelerationValue, batteryEjectSpeedValue,
            batterySlewDegreesPerRpmValue, batteryStressImpactValue, batteryBasePkValue, batteryTerminalPkFactorValue, siloBasePkValue,
            siloKillVehicleMassValue, siloMaxRangeValue;

    private DefenseConfig() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        enabledValue = builder.comment("Whether interceptors can be launched at all.").define("missile.defense.enabled", true);
        autoIntervalTicksValue = builder.comment("Ticks between an automatic battery's or command table's looks for threats to engage.")
                .defineInRange("missile.defense.autoIntervalTicks", 10, 2, 100);
        trackMinSecondsValue = builder.comment("Seconds a missile must have been tracked before automatic defences engage it.")
                .defineInRange("missile.defense.trackMinSeconds", 2, 0, 30);
        engageOwnMissilesValue = builder.comment("Whether automatic defences also engage missiles launched by their own owner.")
                .define("missile.defense.engageOwnMissiles", false);
        fuseRadiusValue = builder.comment("Blocks an interceptor must pass within its target to have a chance to kill it.")
                .defineInRange("missile.defense.fuseRadius", 6.0, 0.5, 32.0);
        pkScaleValue = builder.comment("Multiplies every interceptor's chance to kill.").defineInRange("missile.defense.pkScale", 1.0, 0.0, 10.0);
        maxPkValue = builder.comment("Highest chance to kill any interceptor has.").defineInRange("missile.defense.maxPk", 0.95, 0.0, 1.0);
        killExplosionPowerValue = builder.comment("Explosion power where a missile is destroyed, if that is loaded; its warhead never goes off. 0 for none.")
                .defineInRange("missile.defense.killExplosionPower", 1.5, 0.0, 8.0);
        debrisValue = builder.comment("Whether a destroyed missile's wreckage is shown falling to the ground.").define("missile.defense.debris", true);
        solverStepTicksValue = builder.comment("Ticks between the meeting points tried when an interceptor's course is worked out; lower is finer and slower.")
                .defineInRange("missile.defense.solverStepTicks", 4, 1, 20);
        orderExpireTicksValue = builder.comment("Ticks an engagement order waits for its launcher before it is dropped.")
                .defineInRange("missile.defense.orderExpireTicks", 100, 20, 1200);

        batteryRangeValue = builder.comment("Furthest from an interceptor battery, over the ground, it meets a missile.")
                .defineInRange("missile.defense.battery.range", 600.0, 64.0, 4096.0);
        batteryMinAltitudeValue = builder.comment("Lowest above a battery it meets a missile, blocks.")
                .defineInRange("missile.defense.battery.minAltitude", 48.0, 8.0, 512.0);
        batteryCeilingValue = builder.comment("Highest above a battery it meets a missile, blocks; never above the top of the air.")
                .defineInRange("missile.defense.battery.ceiling", 480.0, 32.0, 4096.0);
        batteryDeltaVValue = builder.comment("Speed a battery's interceptor can gain, blocks per tick.")
                .defineInRange("missile.defense.battery.deltaV", 9.0, 1.0, 40.0);
        batteryAccelerationValue = builder.comment("A battery interceptor's acceleration when its motor lights, blocks per tick squared.")
                .defineInRange("missile.defense.battery.acceleration", 0.5, 0.05, 4.0);
        batteryEjectSpeedValue = builder.comment("Speed a battery's canister throws its interceptor out at before the motor lights, blocks per tick.")
                .defineInRange("missile.defense.battery.ejectSpeed", 0.6, 0.0, 4.0);
        batteryReloadSecondsValue = builder.comment("Seconds a battery tube takes to be ready after an interceptor is loaded.")
                .defineInRange("missile.defense.battery.reloadSeconds", 3, 0, 120);
        batterySlewDegreesPerRpmValue = builder.comment("Degrees per tick a battery turns for each RPM it is driven at.")
                .defineInRange("missile.defense.battery.slewDegreesPerRpm", 0.05, 0.001, 1.0);
        batteryStressImpactValue = builder.comment("Stress a battery draws.").defineInRange("missile.defense.battery.stressImpact", 16.0, 0.0, 1024.0);
        batteryBasePkValue = builder.comment("A battery interceptor's best chance to kill.").defineInRange("missile.defense.battery.basePk", 0.75, 0.0, 1.0);
        batteryTerminalPkFactorValue = builder.comment("What is left of a battery's chance to kill against a missile in its terminal phase.")
                .defineInRange("missile.defense.battery.terminalPkFactor", 0.7, 0.0, 1.0);

        siloBasePkValue = builder.comment("A kill vehicle's best chance to kill.").defineInRange("missile.defense.silo.basePk", 0.85, 0.0, 1.0);
        siloKillVehicleMassValue = builder.comment("Mass of a rocket's kill vehicle, rocket units.")
                .defineInRange("missile.defense.silo.killVehicleMass", 0.4, 0.05, 4.0);
        siloLaunchDelayTicksValue = builder.comment("Ticks an interceptor launch controller counts down once ordered to engage.")
                .defineInRange("missile.defense.silo.launchDelayTicks", 40, 0, 600);
        siloMaxRangeValue = builder.comment("Furthest from its launch controller, over the ground, a kill vehicle meets a missile.")
                .defineInRange("missile.defense.silo.maxRange", 8000.0, 256.0, 100000.0);
    }

    public static void bake() {
        enabled = enabledValue.get();
        autoIntervalTicks = autoIntervalTicksValue.get();
        trackMinSeconds = trackMinSecondsValue.get();
        engageOwnMissiles = engageOwnMissilesValue.get();
        fuseRadius = fuseRadiusValue.get();
        pkScale = pkScaleValue.get();
        maxPk = maxPkValue.get();
        killExplosionPower = killExplosionPowerValue.get();
        debris = debrisValue.get();
        solverStepTicks = solverStepTicksValue.get();
        orderExpireTicks = orderExpireTicksValue.get();
        batteryRange = batteryRangeValue.get();
        batteryMinAltitude = batteryMinAltitudeValue.get();
        batteryCeiling = batteryCeilingValue.get();
        batteryDeltaV = batteryDeltaVValue.get();
        batteryAcceleration = batteryAccelerationValue.get();
        batteryEjectSpeed = batteryEjectSpeedValue.get();
        batteryReloadSeconds = batteryReloadSecondsValue.get();
        batterySlewDegreesPerRpm = batterySlewDegreesPerRpmValue.get();
        batteryStressImpact = batteryStressImpactValue.get();
        batteryBasePk = batteryBasePkValue.get();
        batteryTerminalPkFactor = batteryTerminalPkFactorValue.get();
        siloBasePk = siloBasePkValue.get();
        siloKillVehicleMass = siloKillVehicleMassValue.get();
        siloLaunchDelayTicks = siloLaunchDelayTicksValue.get();
        siloMaxRange = siloMaxRangeValue.get();
    }
}
