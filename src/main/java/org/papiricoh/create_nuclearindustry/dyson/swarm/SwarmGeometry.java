package org.papiricoh.create_nuclearindustry.dyson.swarm;

/**
 * Where each sail of an orbit shows in the sky. The sun is put at unit distance along its direction and the orbit is a ring round
 * it, {@code tan(radius)} across, face-on to the viewer before it is tipped by its inclination about an axis turned by its node; a
 * sail is a point on that ring, seen from the ground as from far away (without perspective), so every ring is centred on the sun
 * as the terminal draws it. Sails are spread by the golden angle, so adding one never moves the others,
 * and each sits a little in or out of the ring and above or below it, so an orbit reads as a swarm rather than a wire. They go round
 * once every {@link #period} ticks, slower further out (Kepler's third law, loosely).
 * <p>
 * Allocation-free: results go into a caller's {@code double[4]} (direction x, y, z, and depth: from -1 in front of the sun to 1
 * behind it). Free of game classes, for JUnit.
 */
public final class SwarmGeometry {
    /** Ticks a sail takes round the innermost orbit. */
    public static final double BASE_PERIOD = 2400;
    /** The vanilla sun's disc, degrees from its centre: sails behind the sun and inside this are hidden by it. */
    public static final double SUN_DISC_DEGREES = 7.5;
    private static final double GOLDEN = 0.6180339887498949;
    /** How far sails stray from their ring: in and out, and above and below it, as shares of its radius. */
    private static final double RADIAL_SPREAD = 0.07, NORMAL_SPREAD = 0.05;
    /** The {@link #hash} salts of a sail's stray in and out of its ring and out of its plane. */
    public static final int RADIAL_SALT = 0x9E37, NORMAL_SALT = 0x7F4A;

    private SwarmGeometry() {
    }

    /** Ticks for one turn of an orbit {@code radius} degrees out. */
    public static double period(double radius) {
        return BASE_PERIOD * Math.pow(Math.max(OrbitShape.MIN_RADIUS, radius) / OrbitShape.MIN_RADIUS, 1.5);
    }

    /**
     * The sky direction of sail {@code index} of {@code orbit} at {@code time} (ticks), with the sun towards ({@code sx, sy, sz}, a
     * unit vector), into {@code out}: x, y, z of a unit vector, then its depth.
     */
    public static void sail(OrbitShape orbit, double sx, double sy, double sz, int index, double time, double[] out) {
        // A frame round the sun: u across the sky north-south where it can be, w = s × u along the sun's daily path.
        double ux = 0, uy = 0, uz = 1;
        double dot = sz;
        ux -= dot * sx;
        uy -= dot * sy;
        uz -= dot * sz;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1.0e-6) {
            ux = 1;
            uy = 0;
            uz = 0;
            ul = 1;
        }
        ux /= ul;
        uy /= ul;
        uz /= ul;
        double wx = sy * uz - sz * uy, wy = sz * ux - sx * uz, wz = sx * uy - sy * ux;

        double node = Math.toRadians(orbit.node()), tilt = Math.toRadians(orbit.inclination());
        double cn = Math.cos(node), sn = Math.sin(node), ci = Math.cos(tilt), si = Math.sin(tilt);
        // The ring's axes: a along the line of nodes; b the other, tipped towards the sun's depth by the inclination.
        double ax = cn * ux + sn * wx, ay = cn * uy + sn * wy, az = cn * uz + sn * wz;
        double b0x = -sn * ux + cn * wx, b0y = -sn * uy + cn * wy, b0z = -sn * uz + cn * wz;
        double bx = ci * b0x + si * sx, by = ci * b0y + si * sy, bz = ci * b0z + si * sz;
        // Its normal, for the sails' spread out of the ring's plane.
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;

        double h1 = hash(index, RADIAL_SALT), h2 = hash(index, NORMAL_SALT);
        double rho = Math.tan(Math.toRadians(orbit.radius())) * (1 + RADIAL_SPREAD * (2 * h1 - 1));
        double phase = Math.PI * 2 * (frac(index * GOLDEN) + time / period(orbit.radius()));
        double c = Math.cos(phase), s = Math.sin(phase), lift = rho * NORMAL_SPREAD * (2 * h2 - 1);
        double ox = rho * (c * ax + s * bx) + lift * nx;
        double oy = rho * (c * ay + s * by) + lift * ny;
        double oz = rho * (c * az + s * bz) + lift * nz;
        // Seen from far off, as the swarm is: only the offset across the line of sight moves the sail on the sky, so a tipped
        // ring is an ellipse centred on the sun. Its offset along the line of sight (up to 2/3 of the sun's distance for the
        // outermost orbit) would otherwise swell the near half of the ring by perspective and pull the whole ring off the sun.
        double depth = ox * sx + oy * sy + oz * sz;
        double px = sx + ox - depth * sx, py = sy + oy - depth * sy, pz = sz + oz - depth * sz;
        double length = Math.sqrt(px * px + py * py + pz * pz);
        out[0] = px / length;
        out[1] = py / length;
        out[2] = pz / length;
        out[3] = rho > 0 ? Math.max(-1, Math.min(1, depth / rho)) : 0;
    }

    /** Degrees between the sky direction in {@code out} (from {@link #sail}) and the sun's. */
    public static double degreesFromSun(double[] out, double sx, double sy, double sz) {
        double dot = out[0] * sx + out[1] * sy + out[2] * sz;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }

    /** Whether the sail in {@code out} is behind the sun's disc. */
    public static boolean hiddenBySun(double[] out, double sx, double sy, double sz) {
        return out[3] > 0 && degreesFromSun(out, sx, sy, sz) < SUN_DISC_DEGREES;
    }

    /** A steady pseudo-random number in [0, 1) for sail {@code index}. */
    public static double hash(int index, int salt) {
        int h = index * 0x45D9F3B + salt;
        h = (h ^ (h >>> 16)) * 0x45D9F3B;
        h = (h ^ (h >>> 16)) * 0x45D9F3B;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }
}
