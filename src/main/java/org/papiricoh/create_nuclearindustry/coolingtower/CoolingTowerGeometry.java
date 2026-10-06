package org.papiricoh.create_nuclearindustry.coolingtower;

import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Integer geometry of a hyperboloid cooling tower with base radius {@code Rb} and height
 * {@code H} (layers 0..H-1, layer 0 being the floor).
 *
 * <p>The wall radius follows {@code R(y) = round(sqrt(a^2 + (Rb^2 - a^2) (y - yw)^2 / yw^2))},
 * with the waist {@code a ~ 0.6 Rb} at {@code yw ~ 0.75 H}, in exact integer arithmetic so
 * tools/structgen/ponder_cooling_tower.py builds the same shape. Horizontal distance is
 * {@link TokamakGeometry#rho}. Layer 0 is a full floor out to {@code Rb}; above it the wall is
 * one block thick, plus a ledge where the radius steps so the interior is sealed; the
 * interior is air and the top stays open.
 */
public final class CoolingTowerGeometry {
    public static final int MIN_BASE_RADIUS = 4;
    public static final int MAX_BASE_RADIUS = 16;
    public static final int ABSOLUTE_MAX_HEIGHT = 64;

    public enum Cell { OUTSIDE, FLOOR, WALL, INTERIOR }

    private static final ConcurrentHashMap<Long, CoolingTowerGeometry> CACHE = new ConcurrentHashMap<>();

    private final int baseRadius;
    private final int height;
    private final int waist;
    private final int waistY;
    private final int[] radius;
    private final int[] outer;

    private CoolingTowerGeometry(int baseRadius, int height) {
        this.baseRadius = baseRadius;
        this.height = height;
        this.waist = waistRadius(baseRadius);
        this.waistY = waistHeight(height);
        this.radius = new int[height];
        for (int y = 0; y < height; y++) {
            radius[y] = profile(y);
        }
        this.outer = new int[height];
        for (int y = 1; y < height; y++) {
            int w = radius[y];
            w = Math.max(w, radius[y - 1] - 1);
            if (y + 1 < height) {
                w = Math.max(w, radius[y + 1] - 1);
            }
            outer[y] = w;
        }
        outer[0] = baseRadius;
    }

    public static CoolingTowerGeometry of(int baseRadius, int height) {
        if (!isValid(baseRadius, height)) {
            throw new IllegalArgumentException("No cooling tower of base radius " + baseRadius + " and height " + height);
        }
        return CACHE.computeIfAbsent(((long) baseRadius << 32) | height, key -> new CoolingTowerGeometry(baseRadius, height));
    }

    public static boolean isValid(int baseRadius, int height) {
        return baseRadius >= MIN_BASE_RADIUS && baseRadius <= MAX_BASE_RADIUS
                && height >= minHeight(baseRadius) && height <= maxHeight(baseRadius);
    }

    public static int minHeight(int baseRadius) {
        return Math.max(10, 2 * baseRadius + 4);
    }

    public static int maxHeight(int baseRadius) {
        return Math.min(ABSOLUTE_MAX_HEIGHT, 5 * baseRadius);
    }

    public static int waistRadius(int baseRadius) {
        return Math.max(2, (3 * baseRadius + 2) / 5);
    }

    public static int waistHeight(int height) {
        return (3 * (height - 1) + 2) / 4;
    }

    /** Rounded wall radius of layer {@code y}: (2r-1)^2 yw^2 <= 4 (a^2 yw^2 + (Rb^2-a^2)(y-yw)^2) < (2r+1)^2 yw^2. */
    private int profile(int y) {
        long yw2 = (long) waistY * waistY;
        long dy = y - waistY;
        long n = 4L * ((long) waist * waist * yw2 + ((long) baseRadius * baseRadius - (long) waist * waist) * dy * dy);
        int r = (int) Math.round(Math.sqrt(n / (4.0 * yw2)));
        while (r > 0 && (long) (2 * r - 1) * (2 * r - 1) * yw2 > n) {
            r--;
        }
        while ((long) (2 * r + 1) * (2 * r + 1) * yw2 <= n) {
            r++;
        }
        return r;
    }

    public int baseRadius() {
        return baseRadius;
    }

    public int height() {
        return height;
    }

    /** Inner wall radius of layer {@code y}; the interior is {@code rho < radius(y)}. */
    public int radius(int y) {
        return radius[y];
    }

    /** Outermost wall radius of layer {@code y} (the floor's for layer 0). */
    public int outerRadius(int y) {
        return outer[y];
    }

    public Cell classify(int dx, int y, int dz) {
        if (y < 0 || y >= height) {
            return Cell.OUTSIDE;
        }
        int rho = TokamakGeometry.rho(dx, dz);
        if (y == 0) {
            return rho <= baseRadius ? Cell.FLOOR : Cell.OUTSIDE;
        }
        if (rho < radius[y]) {
            return Cell.INTERIOR;
        }
        return rho <= outer[y] ? Cell.WALL : Cell.OUTSIDE;
    }

    /** Condensing capacity without fans: one mB/t per block of base radius per layer. */
    public int passiveCapacity(double perBlock) {
        return (int) Math.round(baseRadius * height * perBlock);
    }
}
