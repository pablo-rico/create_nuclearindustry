package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cells of a synchrotron ring of radius {@code R}: one block high, the columns whose rounded
 * distance to the centre is exactly {@code R} ({@link TokamakGeometry#rho}). Rounded distances of
 * face-adjacent columns differ by at most one, so the band always separates the inside of the ring
 * from the outside. Cells are listed in angular order and grouped into sectors of about eight cells
 * each, which the magnets' cooling and the focusing count per sector. Cached per radius; pure.
 */
public final class RingGeometry {
    public static final int MIN_RADIUS = 6;
    /** Ring cells per sector, roughly. */
    public static final double CELLS_PER_SECTOR = 8.0;

    private static final Map<Integer, RingGeometry> CACHE = new ConcurrentHashMap<>();

    private final int radius;
    private final int sectors;
    /** Packed {@code dx, dz} pairs in angular order. */
    private final int[] cells;
    private final int[] cellSector;

    private RingGeometry(int radius) {
        this.radius = radius;
        this.sectors = Math.max(4, (int) Math.round(2 * Math.PI * radius / CELLS_PER_SECTOR));
        int[] buffer = new int[(radius * 2 + 3) * (radius * 2 + 3) * 2];
        int count = 0;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                if (TokamakGeometry.rho(dx, dz) == radius) {
                    buffer[count * 2] = dx;
                    buffer[count * 2 + 1] = dz;
                    count++;
                }
            }
        }
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        int[] raw = buffer;
        Arrays.sort(order, (a, b) -> Double.compare(angle(raw[a * 2], raw[a * 2 + 1]), angle(raw[b * 2], raw[b * 2 + 1])));
        this.cells = new int[count * 2];
        this.cellSector = new int[count];
        for (int i = 0; i < count; i++) {
            cells[i * 2] = raw[order[i] * 2];
            cells[i * 2 + 1] = raw[order[i] * 2 + 1];
            cellSector[i] = sectorOf(cells[i * 2], cells[i * 2 + 1]);
        }
    }

    public static RingGeometry of(int radius) {
        if (radius < MIN_RADIUS) {
            throw new IllegalArgumentException("Ring radius must be at least " + MIN_RADIUS + ", got " + radius);
        }
        return CACHE.computeIfAbsent(radius, RingGeometry::new);
    }

    /** Whether the column {@code (dx, dz)} from the centre is a ring cell. */
    public boolean isRing(int dx, int dz) {
        return TokamakGeometry.rho(dx, dz) == radius;
    }

    /** Sector of a column, {@code 0 .. sectorCount()-1}. */
    public int sectorOf(int dx, int dz) {
        int sector = (int) Math.floor(angle(dx, dz) / (2 * Math.PI) * sectors);
        return Math.min(sectors - 1, Math.max(0, sector));
    }

    /** Angle of a column from the centre, 0..2 pi. */
    private static double angle(int dx, int dz) {
        return Math.atan2(dz, dx) + Math.PI;
    }

    public int radius() {
        return radius;
    }

    public int sectorCount() {
        return sectors;
    }

    public int cellCount() {
        return cellSector.length;
    }

    public int dx(int cell) {
        return cells[cell * 2];
    }

    public int dz(int cell) {
        return cells[cell * 2 + 1];
    }

    public int sector(int cell) {
        return cellSector[cell];
    }
}
