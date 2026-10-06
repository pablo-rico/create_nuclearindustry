package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Integer geometry of a tokamak of major radius {@code R}, centred on the central solenoid.
 *
 * <p>Horizontal distance from the axis is rounded to whole blocks:
 * {@code rho = round(sqrt(dx^2 + dz^2))}. The torus has a 5x5 cross-section:
 * <ul>
 *   <li>plasma chamber: {@code |rho - R| <= 1} and {@code |dy| <= 1} (must be air)</li>
 *   <li>shell: the rest of {@code |rho - R| <= 2}, {@code |dy| <= 2} (vessel, coils, blankets, ports)</li>
 * </ul>
 * Rounded distance changes by at most one between face-adjacent columns, so the shell
 * always seals the chamber. Pure arithmetic, cached per radius.
 */
public final class TokamakGeometry {
    public static final int MIN_RADIUS = 4;
    public static final int HALF_HEIGHT = 2;
    public static final int PLASMA_HALF_WIDTH = 1;

    private static final ConcurrentHashMap<Integer, TokamakGeometry> CACHE = new ConcurrentHashMap<>();

    private final int radius;
    private final int extent;
    private final int sectors;
    /** Packed (dx, dy, dz) triples. */
    private final int[] shell;
    private final int[] plasma;
    private final int[] shellSector;

    public enum Cell { OUTSIDE, SHELL, PLASMA }

    private TokamakGeometry(int radius) {
        this.radius = radius;
        this.extent = radius + 2;
        this.sectors = 4 * radius;
        int side = 2 * extent + 1;
        int[] shellBuffer = new int[side * side * (2 * HALF_HEIGHT + 1) * 3];
        int[] plasmaBuffer = new int[shellBuffer.length];
        int[] sectorBuffer = new int[shellBuffer.length / 3];
        int shellCount = 0;
        int plasmaCount = 0;
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                for (int dy = -HALF_HEIGHT; dy <= HALF_HEIGHT; dy++) {
                    Cell cell = classify(dx, dy, dz);
                    if (cell == Cell.SHELL) {
                        sectorBuffer[shellCount] = sectorOf(dx, dz);
                        shellBuffer[shellCount * 3] = dx;
                        shellBuffer[shellCount * 3 + 1] = dy;
                        shellBuffer[shellCount * 3 + 2] = dz;
                        shellCount++;
                    } else if (cell == Cell.PLASMA) {
                        plasmaBuffer[plasmaCount * 3] = dx;
                        plasmaBuffer[plasmaCount * 3 + 1] = dy;
                        plasmaBuffer[plasmaCount * 3 + 2] = dz;
                        plasmaCount++;
                    }
                }
            }
        }
        this.shell = java.util.Arrays.copyOf(shellBuffer, shellCount * 3);
        this.plasma = java.util.Arrays.copyOf(plasmaBuffer, plasmaCount * 3);
        this.shellSector = java.util.Arrays.copyOf(sectorBuffer, shellCount);
    }

    public static TokamakGeometry of(int radius) {
        if (radius < MIN_RADIUS) {
            throw new IllegalArgumentException("Tokamak major radius must be at least " + MIN_RADIUS + ", got " + radius);
        }
        return CACHE.computeIfAbsent(radius, TokamakGeometry::new);
    }

    /** {@code round(sqrt(dx^2 + dz^2))}, computed exactly in integers (halves round up). */
    public static int rho(int dx, int dz) {
        long fourD2 = 4L * ((long) dx * dx + (long) dz * dz);
        int r = (int) Math.floor(Math.sqrt((double) dx * dx + (double) dz * dz) + 0.5);
        // Correct any floating point drift so that (2r-1)^2 <= 4d^2 < (2r+1)^2.
        while (r > 0 && (long) (2 * r - 1) * (2 * r - 1) > fourD2) {
            r--;
        }
        while ((long) (2 * r + 1) * (2 * r + 1) <= fourD2) {
            r++;
        }
        return r;
    }

    public Cell classify(int dx, int dy, int dz) {
        if (Math.abs(dy) > HALF_HEIGHT || Math.abs(dx) > extent || Math.abs(dz) > extent) {
            return Cell.OUTSIDE;
        }
        int offset = Math.abs(rho(dx, dz) - radius);
        if (offset > 2) {
            return Cell.OUTSIDE;
        }
        if (offset <= PLASMA_HALF_WIDTH && Math.abs(dy) <= PLASMA_HALF_WIDTH) {
            return Cell.PLASMA;
        }
        return Cell.SHELL;
    }

    /** Toroidal sector of a column, {@code 0 .. sectorCount()-1}. */
    public int sectorOf(int dx, int dz) {
        double angle = Math.atan2(dz, dx) + Math.PI;
        int sector = (int) Math.floor(angle / (2 * Math.PI) * sectors);
        return Math.min(sectors - 1, Math.max(0, sector));
    }

    public int majorRadius() {
        return radius;
    }

    /** Horizontal half-width of the bounding box. */
    public int extent() {
        return extent;
    }

    public int sectorCount() {
        return sectors;
    }

    public int shellCount() {
        return shell.length / 3;
    }

    public int plasmaCount() {
        return plasma.length / 3;
    }

    public int shellX(int i) { return shell[i * 3]; }
    public int shellY(int i) { return shell[i * 3 + 1]; }
    public int shellZ(int i) { return shell[i * 3 + 2]; }
    public int shellSector(int i) { return shellSector[i]; }

    public int plasmaX(int i) { return plasma[i * 3]; }
    public int plasmaY(int i) { return plasma[i * 3 + 1]; }
    public int plasmaZ(int i) { return plasma[i * 3 + 2]; }

    /**
     * Distance along a cardinal axis from the controller (outer equator of the shell) to
     * the axis. The controller sits at {@code rho = R + 2}.
     */
    public static int controllerToAxis(int radius) {
        return radius + 2;
    }
}
