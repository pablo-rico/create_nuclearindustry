package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * What a core map module shows of a fission reactor: one cell per interior column, by what fills it,
 * with how far a control channel's rods are in or how burnt a fuel column is. Packed into bytes for the
 * desk's sync: the side of the square, then a kind and a value per cell, row by row from the north,
 * each row from the west. No game classes, so the packing is tested on its own.
 */
public final class CoreMap {
    /** A reactor's interior is at most 7 columns across. */
    public static final int MAX_SIDE = 7;
    /** A value's full scale: rods all the way in, or fuel all burnt. */
    public static final int FULL = 250;
    /** A cell with no value: a fuel column without assemblies. */
    public static final int NO_VALUE = 255;

    public enum Kind {EMPTY, FUEL, CONTROL, MODERATOR, REFLECTOR, EXCHANGER}

    private static final Kind[] KINDS = Kind.values();

    private CoreMap() {}

    /** A square of {@code side} columns, every cell empty. */
    public static byte[] blank(int side) {
        side = Math.max(0, Math.min(MAX_SIDE, side));
        byte[] data = new byte[1 + 2 * side * side];
        data[0] = (byte) side;
        return data;
    }

    /**
     * Sets a cell: {@code value} is a 0-1 fraction (rods in, fuel burnt), or NaN for none. Cells outside
     * the square are ignored.
     */
    public static void set(byte[] data, int column, int row, Kind kind, float value) {
        int side = side(data);
        if (column < 0 || row < 0 || column >= side || row >= side) {
            return;
        }
        int at = 1 + 2 * (row * side + column);
        data[at] = (byte) kind.ordinal();
        data[at + 1] = (byte) (Float.isNaN(value) ? NO_VALUE : Math.round(Math.max(0, Math.min(1, value)) * FULL));
    }

    /** How many columns the square has across, 0 for no map. */
    public static int side(byte[] data) {
        if (data.length == 0) {
            return 0;
        }
        int side = data[0] & 0xFF;
        return side <= MAX_SIDE && data.length >= 1 + 2 * side * side ? side : 0;
    }

    public static Kind kind(byte[] data, int column, int row) {
        int side = side(data);
        if (column < 0 || row < 0 || column >= side || row >= side) {
            return Kind.EMPTY;
        }
        int ordinal = data[1 + 2 * (row * side + column)] & 0xFF;
        return ordinal < KINDS.length ? KINDS[ordinal] : Kind.EMPTY;
    }

    /** A cell's value as a 0-1 fraction, or NaN when it has none. */
    public static float value(byte[] data, int column, int row) {
        int side = side(data);
        if (column < 0 || row < 0 || column >= side || row >= side) {
            return Float.NaN;
        }
        int stored = data[2 + 2 * (row * side + column)] & 0xFF;
        return stored == NO_VALUE ? Float.NaN : Math.min(FULL, stored) / (float) FULL;
    }

    /** How many cells of a kind the map has. */
    public static int count(byte[] data, Kind kind) {
        int side = side(data);
        int count = 0;
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                if (kind(data, column, row) == kind) {
                    count++;
                }
            }
        }
        return count;
    }
}
