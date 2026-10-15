package org.papiricoh.create_nuclearindustry.maintenance;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.maintenance.physics.ComponentHealth;
import org.papiricoh.create_nuclearindustry.maintenance.physics.WearPart;

import java.util.EnumMap;
import java.util.Map;

/**
 * Common config for plant operation and maintenance, under {@code maintenance.*}. Every feature has
 * its own switch under the master {@code maintenance.enabled}: wear, the failures it leads to, the
 * refuelling outage, natural earthquakes ({@code maintenance.earthquakes.enabled}) and the incident
 * log ({@code maintenance.incidentLog.enabled}). Defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load; the
 * {@code *On()} methods combine a feature's switch with the master one.
 */
public final class MaintenanceConfig {
    public static boolean enabled = true;
    public static boolean wear = true;
    public static boolean failures = true;
    public static boolean requireOutage = true;
    public static boolean earthquakes = true;
    public static boolean incidentLog = true;
    /** Multiplies every part's wear: 2 wears parts out twice as fast. */
    public static double wearScale = 1.0;
    /** Health one use of a Maintenance Kit gives back. */
    public static double kitRepair = 0.5;
    /** Natural earthquakes a game day, in each dimension that has them. */
    public static double earthquakesPerDay = 0.05;
    public static double earthquakeMinMagnitude = 3.0;
    public static double earthquakeMaxMagnitude = 6.5;
    /** Ground motion at which a reactor's seismic trip scrams it. */
    public static double seismicTrip = 4.0;
    /** Blocks around a player an earthquake may start in. */
    public static int earthquakeRange = 256;
    /** Entries the incident log keeps. */
    public static int incidentLogSize = 100;

    private static final Map<WearPart, ComponentHealth.Params> PARAMS = new EnumMap<>(WearPart.class);

    private static ModConfigSpec.BooleanValue ENABLED;
    private static ModConfigSpec.BooleanValue WEAR;
    private static ModConfigSpec.BooleanValue FAILURES;
    private static ModConfigSpec.BooleanValue REQUIRE_OUTAGE;
    private static ModConfigSpec.BooleanValue EARTHQUAKES;
    private static ModConfigSpec.BooleanValue INCIDENT_LOG;
    private static ModConfigSpec.DoubleValue WEAR_SCALE;
    private static ModConfigSpec.DoubleValue KIT_REPAIR;
    private static ModConfigSpec.DoubleValue QUAKES_PER_DAY;
    private static ModConfigSpec.DoubleValue QUAKE_MIN;
    private static ModConfigSpec.DoubleValue QUAKE_MAX;
    private static ModConfigSpec.DoubleValue SEISMIC_TRIP;
    private static ModConfigSpec.IntValue QUAKE_RANGE;
    private static ModConfigSpec.IntValue LOG_SIZE;

    static {
        for (WearPart part : WearPart.values()) {
            PARAMS.put(part, part.params(1.0));
        }
    }

    private MaintenanceConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        ENABLED = builder.comment("Master switch for plant operation and maintenance: wear, failures, the refuelling outage, earthquakes",
                        "and the incident log. Off, plants never wear and refuel at power as before.")
                .define("maintenance.enabled", true);
        WEAR = builder.comment("Control rod drives, the relief valve, steam generator tubes and turbine blades wear with use.")
                .define("maintenance.wear", true);
        FAILURES = builder.comment("Worn parts fail: a stuck control rod, a steam generator tube rupture, a turbine trip. Off, they only wear.")
                .define("maintenance.failures", true);
        REQUIRE_OUTAGE = builder.comment("A fission reactor takes fresh fuel only in a refuelling outage: formed, shut down cold with every",
                        "rod in. Off, it refuels at power as before 2.0.")
                .define("maintenance.requireRefuellingOutage", true);
        EARTHQUAKES = builder.comment("Natural earthquakes: seismic detectors record them, they scram reactors close by and wear their parts.")
                .define("maintenance.earthquakes.enabled", true);
        INCIDENT_LOG = builder.comment("Keep a log of plant incidents rated on the INES scale (/nuclear incident list, the Incident Log display source).")
                .define("maintenance.incidentLog.enabled", true);
        WEAR_SCALE = builder.comment("Multiplies every part's wear: 2 wears parts out twice as fast, 0.5 half as fast.")
                .defineInRange("maintenance.wearScale", 1.0, 0.0, 1000.0);
        KIT_REPAIR = builder.comment("Health (0-1) one use of a Maintenance Kit gives a part back.")
                .defineInRange("maintenance.kitRepair", 0.5, 0.01, 1.0);
        QUAKES_PER_DAY = builder.comment("Natural earthquakes a game day in each dimension with players.")
                .defineInRange("maintenance.earthquakes.perDay", 0.05, 0.0, 1000.0);
        QUAKE_MIN = builder.comment("Smallest magnitude of a natural earthquake.")
                .defineInRange("maintenance.earthquakes.minMagnitude", 3.0, 1.0, 9.0);
        QUAKE_MAX = builder.comment("Largest magnitude of a natural earthquake; each magnitude is ten times rarer.")
                .defineInRange("maintenance.earthquakes.maxMagnitude", 6.5, 1.0, 9.0);
        SEISMIC_TRIP = builder.comment("Ground motion at which a reactor's seismic trip scrams it (a magnitude 3 gives 1 at its epicentre, each magnitude ten times more).")
                .defineInRange("maintenance.earthquakes.seismicTrip", 4.0, 0.0, 1.0e6);
        QUAKE_RANGE = builder.comment("Blocks from a player within which an earthquake may start.")
                .defineInRange("maintenance.earthquakes.range", 256, 16, 4096);
        LOG_SIZE = builder.comment("Incidents the log keeps; the oldest go first.")
                .defineInRange("maintenance.incidentLog.size", 100, 1, 10000);
    }

    public static void bake() {
        enabled = ENABLED.get();
        wear = WEAR.get();
        failures = FAILURES.get();
        requireOutage = REQUIRE_OUTAGE.get();
        earthquakes = EARTHQUAKES.get();
        incidentLog = INCIDENT_LOG.get();
        wearScale = WEAR_SCALE.get();
        kitRepair = KIT_REPAIR.get();
        earthquakesPerDay = QUAKES_PER_DAY.get();
        earthquakeMinMagnitude = QUAKE_MIN.get();
        earthquakeMaxMagnitude = Math.max(QUAKE_MIN.get(), QUAKE_MAX.get());
        seismicTrip = SEISMIC_TRIP.get();
        earthquakeRange = QUAKE_RANGE.get();
        incidentLogSize = LOG_SIZE.get();
        for (WearPart part : WearPart.values()) {
            PARAMS.put(part, part.params(wearScale));
        }
    }

    public static boolean wearOn() {
        return enabled && wear;
    }

    public static boolean failuresOn() {
        return enabled && wear && failures;
    }

    public static boolean outageRequired() {
        return enabled && requireOutage;
    }

    public static boolean earthquakesOn() {
        return enabled && earthquakes;
    }

    public static boolean incidentLogOn() {
        return enabled && incidentLog;
    }

    public static ComponentHealth.Params params(WearPart part) {
        return PARAMS.get(part);
    }
}
