package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for satellites, under {@code missile.orbit.*}: what a rocket needs to reach orbit, the two orbits, how long a low
 * one lasts and what satellites do for their network. Defined on the shared builder by {@code MissileConfig} and copied into
 * public statics on load.
 */
public final class OrbitConfig {
    public static boolean enabled = true;
    public static double satelliteMass = 1.0;
    public static double leoDeltaV = 9.5;
    public static double geoDeltaV = 11.5;
    public static double insertionAltitude = 1200;
    public static double leoAltitude = 1600;
    public static double geoAltitude = 12000;
    public static double leoSpeed = 12;
    public static double maxOrbitSize = 65536;
    public static double leoDecayPerDay = 100;
    public static double leoMinAltitude = 800;
    public static int maxSatellitesPerCode = 8;
    public static double gpsCepFactor = 0.4;
    public static double satelliteBasePk = 0.7;
    public static double asatCeiling = 4000;
    public static int syncSeconds = 10;

    private static ModConfigSpec.BooleanValue enabledValue;
    private static ModConfigSpec.IntValue maxSatellitesPerCodeValue, syncSecondsValue;
    private static ModConfigSpec.DoubleValue satelliteMassValue, leoDeltaVValue, geoDeltaVValue, insertionAltitudeValue, leoAltitudeValue,
            geoAltitudeValue, leoSpeedValue, maxOrbitSizeValue, leoDecayPerDayValue, leoMinAltitudeValue, gpsCepFactorValue,
            satelliteBasePkValue, asatCeilingValue;

    private OrbitConfig() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        enabledValue = builder.comment("Whether satellites can be launched at all.").define("missile.orbit.enabled", true);
        satelliteMassValue = builder.comment("Mass of a satellite and its fairing, in the rockets' units (a warhead is about 1).")
                .defineInRange("missile.orbit.satelliteMass", 1.0, 0.1, 10.0);
        leoDeltaVValue = builder.comment("Speed a rocket must be able to give its satellite to reach low orbit, blocks per tick (three full solid stages are about 10, two full liquid ones about 11).")
                .defineInRange("missile.orbit.leoDeltaV", 9.5, 1.0, 100.0);
        geoDeltaVValue = builder.comment("Speed a rocket must be able to give its satellite to reach geostationary orbit, blocks per tick (three full liquid stages are about 13, two full liquid ones about 11).")
                .defineInRange("missile.orbit.geoDeltaV", 11.5, 1.0, 100.0);
        insertionAltitudeValue = builder.comment("Blocks above sea level a launch climbs to before the satellite counts as in orbit and the rocket is gone.")
                .defineInRange("missile.orbit.insertionAltitude", 1200.0, 300.0, 20000.0);
        leoAltitudeValue = builder.comment("Blocks above sea level of a new low orbit: the higher, the wider it sees.")
                .defineInRange("missile.orbit.leoAltitude", 1600.0, 300.0, 100000.0);
        geoAltitudeValue = builder.comment("Blocks above sea level of a geostationary orbit.")
                .defineInRange("missile.orbit.geoAltitude", 12000.0, 300.0, 1000000.0);
        leoSpeedValue = builder.comment("Ground speed of a low satellite, blocks per tick: it crosses the world and comes back in over the far side of the world border.")
                .defineInRange("missile.orbit.leoSpeed", 12.0, 0.5, 1000.0);
        maxOrbitSizeValue = builder.comment("Widest square, centred on the world border's centre, low orbits wrap round when the border is larger (as the default one is): satellites still come back over in useful time.")
                .defineInRange("missile.orbit.maxOrbitSize", 65536.0, 1024.0, 6.0E7);
        leoDecayPerDayValue = builder.comment("Blocks of altitude a low satellite loses each Minecraft day (24000 ticks); 0 to keep it up for good.")
                .defineInRange("missile.orbit.leoDecayPerDay", 100.0, 0.0, 100000.0);
        leoMinAltitudeValue = builder.comment("Altitude, blocks above sea level, below which a decaying low satellite burns up.")
                .defineInRange("missile.orbit.leoMinAltitude", 800.0, 100.0, 100000.0);
        maxSatellitesPerCodeValue = builder.comment("Most satellites one network code can have in orbit at once.")
                .defineInRange("missile.orbit.maxSatellitesPerCode", 8, 1, 64);
        gpsCepFactorValue = builder.comment("Multiplies a missile's spread when its launch site and target are both under one of its network's communications satellites.")
                .defineInRange("missile.orbit.gpsCepFactor", 0.4, 0.0, 1.0);
        satelliteBasePkValue = builder.comment("Chance an anti-satellite interceptor that reaches its satellite destroys it, before radar and seeker.")
                .defineInRange("missile.orbit.satelliteBasePk", 0.7, 0.0, 1.0);
        asatCeilingValue = builder.comment("Highest above sea level an interceptor can meet a satellite, blocks: geostationary ones are out of reach above it.")
                .defineInRange("missile.orbit.asatCeiling", 4000.0, 300.0, 1000000.0);
        syncSecondsValue = builder.comment("Seconds between full refreshes of the satellites players' clients show; changes are sent at once anyway.")
                .defineInRange("missile.orbit.syncSeconds", 10, 1, 300);
    }

    public static void bake() {
        enabled = enabledValue.get();
        satelliteMass = satelliteMassValue.get();
        leoDeltaV = leoDeltaVValue.get();
        geoDeltaV = geoDeltaVValue.get();
        insertionAltitude = insertionAltitudeValue.get();
        leoAltitude = leoAltitudeValue.get();
        geoAltitude = geoAltitudeValue.get();
        leoSpeed = leoSpeedValue.get();
        maxOrbitSize = maxOrbitSizeValue.get();
        leoDecayPerDay = leoDecayPerDayValue.get();
        leoMinAltitude = leoMinAltitudeValue.get();
        maxSatellitesPerCode = maxSatellitesPerCodeValue.get();
        gpsCepFactor = gpsCepFactorValue.get();
        satelliteBasePk = satelliteBasePkValue.get();
        asatCeiling = asatCeilingValue.get();
        syncSeconds = syncSecondsValue.get();
    }

    /** The square low orbits over {@code border} wrap round: the border, or at most {@link #maxOrbitSize} of it round its centre. */
    public static Orbit.Bounds bounds(net.minecraft.world.level.border.WorldBorder border) {
        double half = Math.min(border.getSize(), maxOrbitSize) / 2;
        return new Orbit.Bounds(Math.max(border.getMinX(), border.getCenterX() - half), Math.max(border.getMinZ(), border.getCenterZ() - half),
                Math.min(border.getMaxX(), border.getCenterX() + half), Math.min(border.getMaxZ(), border.getCenterZ() + half));
    }

    /** A new low orbit through {@code (x, z)} at {@code heading} (radians, 0 is +Z), over it at game tick {@code epoch}. */
    public static Orbit leo(double x, double z, double heading, long epoch, Orbit.Bounds bounds) {
        return Orbit.leo(x, z, heading, leoAltitude, epoch, leoSpeed, bounds);
    }

    /** A new geostationary orbit over {@code (x, z)}, kept inside {@code bounds}. */
    public static Orbit geo(double x, double z, long epoch, Orbit.Bounds bounds) {
        return Orbit.geo(x, z, geoAltitude, epoch, bounds);
    }
}
