package org.papiricoh.create_nuclearindustry.missile.holo;

import org.jetbrains.annotations.Nullable;

/**
 * The command table's 3×3 grid, pure: a formed piece is one of nine parts, row-major from the north-west corner (0) to the
 * south-east (8), 4 the centre. Ported from SupernaturalCraft's war table.
 */
public final class HoloGrid {
    public static final int CENTER = 4;
    public static final int PARTS = 9;

    /** Whether the cell at (x, z), relative to the piece just placed, holds a loose piece. */
    @FunctionalInterface
    public interface Cells {
        boolean loose(int x, int z);
    }

    private HoloGrid() {
    }

    /** Offset of {@code part} from the centre, east. */
    public static int dx(int part) {
        return part % 3 - 1;
    }

    /** Offset of {@code part} from the centre, south. */
    public static int dz(int part) {
        return part / 3 - 1;
    }

    /** The part at offset (dx, dz) from the centre, or −1 off the grid. */
    public static int of(int dx, int dz) {
        if (dx < -1 || dx > 1 || dz < -1 || dz > 1) return -1;
        return (dz + 1) * 3 + dx + 1;
    }

    /**
     * The 3×3 window a piece just placed at (0, 0) completes: of the nine windows holding it, those whose nine cells are all loose;
     * of those, the one whose centre lies furthest north, then west, whatever the order the pieces went down in.
     *
     * @return the window's centre relative to the piece, {@code {cx, cz}}, or null
     */
    public static int @Nullable [] window(Cells cells) {
        for (int cz = -1; cz <= 1; cz++) {
            for (int cx = -1; cx <= 1; cx++) {
                if (full(cells, cx, cz)) return new int[]{cx, cz};
            }
        }
        return null;
    }

    private static boolean full(Cells cells, int cx, int cz) {
        for (int p = 0; p < PARTS; p++) if (!cells.loose(cx + dx(p), cz + dz(p))) return false;
        return true;
    }
}
