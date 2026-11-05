package org.papiricoh.create_nuclearindustry.dyson.swarm;

/**
 * Where the sun stands, from the game's celestial angle ({@code Level.getTimeOfDay}: 0 at noon, 0.25 at sunset, 0.5 at midnight,
 * 0.75 at sunrise). The same turn {@code LevelRenderer.renderSky} gives the sun quad, so the swarm, the ejectors' rails and the
 * receivers' dishes line up with the sun on screen: it rises in the east (+x), crosses the zenith and sets in the west, never
 * leaving the x–y plane, unless a shader pack tips its path. Free of game classes, for JUnit.
 */
public final class SunPath {
    private SunPath() {
    }

    /** The unit direction to the sun at celestial angle {@code celestial}, into {@code out} (x, y, z). */
    public static void direction(double celestial, double[] out) {
        direction(celestial, 0, out);
    }

    /**
     * The same with the sun's path tipped {@code pathRotation} degrees towards the south or north, as shader packs draw it
     * ({@code sunPathRotation}; Iris turns the sky by it about z between the game's two turns).
     */
    public static void direction(double celestial, double pathRotation, double[] out) {
        double a = celestial * Math.PI * 2, tilt = Math.toRadians(pathRotation);
        out[0] = -Math.sin(a);
        out[1] = Math.cos(a) * Math.cos(tilt);
        out[2] = -Math.cos(a) * Math.sin(tilt);
    }

    /** The sine of the sun's elevation over the horizon at celestial angle {@code celestial}: 1 at noon, below 0 at night. */
    public static double sinElevation(double celestial) {
        return Math.cos(celestial * Math.PI * 2);
    }

    /** The sun's elevation in degrees, -90 to 90. */
    public static double elevationDegrees(double celestial) {
        return Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sinElevation(celestial)))));
    }
}
