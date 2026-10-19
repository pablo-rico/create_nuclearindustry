package org.papiricoh.create_nuclearindustry.missile.fx;

/**
 * The shape of a rocket's exhaust plume at one moment, from its nozzle, its propellant and the air around it. Near the ground
 * the air squeezes the jet into a long narrow flame with a train of shock diamonds behind the nozzle; as the air thins the
 * jet is no longer held in and balloons out, dimmer and wider, until it is a broad glowing fan above the atmosphere. A solid
 * motor burns smoky orange; a liquid one a cleaner blue-white with brighter diamonds. Free of game classes, for JUnit.
 */
public final class PlumeModel {
    /** Blocks above the launch site within which shock diamonds show. */
    public static final double DIAMOND_HEIGHT = 120;
    /** Furthest the plume opens out, as a multiple of its width at the ground. */
    public static final double MAX_EXPANSION = 5;
    /** Sea level: the air is at full density at and below it. */
    static final double SEA_LEVEL = 63;

    /**
     * One plume, all lengths in blocks from the nozzle exit along the axis.
     *
     * @param length          how far the visible flame reaches
     * @param exitRadius      its radius at the nozzle
     * @param endRadius       its radius at its far end
     * @param coreLength      the white-hot core's length
     * @param coreRadius      the core's radius
     * @param diamonds        how many shock diamonds to draw
     * @param diamondSpacing  blocks between them
     * @param diamondStrength how bright they are, 0 to 1
     * @param smoke           how much smoke it leaves, 0 to 1
     * @param brightness      overall brightness, 0 to 1
     */
    public record Plume(double length, double exitRadius, double endRadius, double coreLength, double coreRadius, int diamonds,
                        double diamondSpacing, double diamondStrength, double smoke, double brightness) {
    }

    private PlumeModel() {
    }

    /**
     * The air's density at height {@code y} relative to sea level: 1 at and below it, about a twentieth at
     * {@code atmosphereTop} and next to nothing above.
     */
    public static double airDensity(double y, double atmosphereTop) {
        double h = Math.max(0, y - SEA_LEVEL);
        // Three scale heights up to the top of the air: e^-3 is about 5 %.
        double scale = Math.max(16, (atmosphereTop - SEA_LEVEL) / 3.0);
        return Math.exp(-h / scale);
    }

    /** How far an under-expanded jet opens out in air of {@code density}: 1 at the ground, up to {@link #MAX_EXPANSION}. */
    public static double expansion(double density) {
        double floor = 1.0 / (MAX_EXPANSION * MAX_EXPANSION);
        return Math.min(MAX_EXPANSION, 1.0 / Math.sqrt(Math.max(floor, density)));
    }

    /** 0 to 1 how strongly the shock diamonds show {@code above} blocks over the launch site. */
    public static double diamondStrength(double above) {
        if (above >= DIAMOND_HEIGHT) return 0;
        double t = 1 - Math.max(0, above) / DIAMOND_HEIGHT;
        return t * t * (3 - 2 * t);
    }

    /**
     * The plume of one nozzle of exit radius {@code nozzleRadius}, burning at {@code throttle} (0 to 1) at height {@code y},
     * {@code above} blocks over where it was launched.
     */
    public static Plume of(boolean liquid, double nozzleRadius, double above, double y, double atmosphereTop, double throttle) {
        double r = Math.max(0.1, nozzleRadius);
        double density = airDensity(y, atmosphereTop);
        double e = expansion(density);
        double k = Math.max(0, Math.min(1, throttle));
        double length = r * (liquid ? 26 : 34) * (1 + 0.45 * (e - 1)) * (0.4 + 0.6 * k);
        double endRadius = r * (liquid ? 1.9 : 2.8) * Math.pow(e, 0.85);
        double coreLength = r * (liquid ? 8 : 6) * (0.5 + 0.5 * k);
        double coreRadius = r * (liquid ? 0.75 : 0.85);
        double strength = diamondStrength(above) * (liquid ? 1.0 : 0.6) * k;
        int diamonds = strength <= 0.01 ? 0 : liquid ? 5 : 4;
        double spacing = r * 2.6 * (1 + 0.3 * (e - 1));
        double smoke = (liquid ? 0.25 : 1.0) * (0.35 + 0.65 * Math.sqrt(density));
        // Thin air dims the flame a little: less to burn against and less to light up.
        double brightness = k * (0.55 + 0.45 * Math.sqrt(density));
        return new Plume(length, r, endRadius, coreLength, coreRadius, diamonds, spacing, strength, smoke, brightness);
    }

    /** A flicker of the flame, around 1, smooth in {@code time} (ticks) and different for each {@code seed}. */
    public static double flicker(double time, int seed) {
        double s = seed * 0.6180339887;
        return 1 + 0.07 * Math.sin(time * 2.31 + s * 7.1) + 0.05 * Math.sin(time * 5.17 + s * 3.3) + 0.03 * Math.sin(time * 11.9 + s);
    }
}
