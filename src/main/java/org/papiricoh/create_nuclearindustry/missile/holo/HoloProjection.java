package org.papiricoh.create_nuclearindustry.missile.holo;

/**
 * How the command table lays the world over its glass, the same on the client that draws it and the server that answers clicks:
 * {@code view} blocks across centred {@code (ox, oz)} blocks from the table's centre, heights squeezed by {@code heightScale}.
 * Table coordinates are blocks from the centre piece's north-west bottom corner; world offsets are blocks from the centre of the
 * centre piece. Pure: no game classes, so it can be tested on its own.
 */
public record HoloProjection(int view, double ox, double oz, float heightScale) {
    /** The glass inside the rim. */
    public static final float MIN = -1 + 2 / 16f, MAX = 2 - 2 / 16f, SIZE = MAX - MIN, MID = 0.5f;
    /** The glass's top, where the map lies. */
    public static final float Y = 13.6f / 16f;
    /** The table's footprint, all nine pieces. */
    public static final float OUTER_MIN = -1, OUTER_MAX = 2;
    /** Sea level: heights are shown over it. */
    public static final double SEA_LEVEL = 63;

    /** Where a click on the table lands. */
    public enum Region { GLASS, FRAME, NONE }

    /** A projection for {@code view} centred at {@code (ox, oz)} with arcs up to {@code top} blocks above sea level. */
    public static HoloProjection of(int view, double ox, double oz, double top) {
        return new HoloProjection(view, ox, oz, HoloView.heightScale(view, SIZE, top));
    }

    public float x(double dx) {
        return (float) (MID + (dx - ox) / view * SIZE);
    }

    public float z(double dz) {
        return (float) (MID + (dz - oz) / view * SIZE);
    }

    public float y(double worldY) {
        return Y + 0.01f + (float) Math.max(0, worldY - SEA_LEVEL) * heightScale;
    }

    /** Blocks of glass per block of the world across. */
    public float scale() {
        return SIZE / view;
    }

    /** The world offset from the table's centre under glass point {@code lx}. */
    public double worldDx(double lx) {
        return ox + (lx - MID) / SIZE * view;
    }

    public double worldDz(double lz) {
        return oz + (lz - MID) / SIZE * view;
    }

    public static boolean onGlass(double lx, double lz) {
        return lx >= MIN && lx <= MAX && lz >= MIN && lz <= MAX;
    }

    /** Whether a world offset shows on the glass. */
    public boolean shows(double dx, double dz) {
        return onGlass(x(dx), z(dz));
    }

    /** What a point on the table's surface is: the glass (on top, inside the rim), the frame (the rim and the sides), or neither. */
    public static Region region(double lx, double ly, double lz) {
        if (lx < OUTER_MIN - 1e-3 || lx > OUTER_MAX + 1e-3 || lz < OUTER_MIN - 1e-3 || lz > OUTER_MAX + 1e-3 || ly < -1e-3 || ly > 1.01) return Region.NONE;
        if (onGlass(lx, lz) && ly >= Y - 0.06 && ly <= Y + 0.06) return Region.GLASS;
        return Region.FRAME;
    }

    /** Where the free-flying point {@code (x, y, z)} moving at {@code v} is {@code t} ticks on under gravity {@code g}. */
    public static double[] fly(double x, double y, double z, double vx, double vy, double vz, double g, double t) {
        return new double[]{x + vx * t, y + vy * t - 0.5 * g * t * t, z + vz * t};
    }

    /**
     * Of {@code points} (table coordinates, {@code x, y, z} triples), the index of the one nearest the ray from {@code eye} along
     * {@code dir}, ahead of the eye and within {@code radius} of it; -1 if none.
     */
    public static int pick(double[] eye, double[] dir, double[] points, double radius) {
        double length = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        if (length < 1e-9) return -1;
        double dx = dir[0] / length, dy = dir[1] / length, dz = dir[2] / length;
        int best = -1;
        double bestDistance = radius * radius;
        for (int i = 0; i + 2 < points.length; i += 3) {
            double px = points[i] - eye[0], py = points[i + 1] - eye[1], pz = points[i + 2] - eye[2];
            double along = px * dx + py * dy + pz * dz;
            if (along <= 0) continue;
            double cx = px - dx * along, cy = py - dy * along, cz = pz - dz * along;
            double d2 = cx * cx + cy * cy + cz * cz;
            if (d2 < bestDistance) {
                bestDistance = d2;
                best = i / 3;
            }
        }
        return best;
    }

    /** Where the ray from {@code eye} along {@code dir} meets the glass's plane, or null if it does not go down to it. */
    public static double[] onGlassPlane(double[] eye, double[] dir) {
        if (dir[1] >= -1e-9) return null;
        double t = (Y - eye[1]) / dir[1];
        if (t <= 0) return null;
        return new double[]{eye[0] + dir[0] * t, Y, eye[2] + dir[2] * t};
    }
}
