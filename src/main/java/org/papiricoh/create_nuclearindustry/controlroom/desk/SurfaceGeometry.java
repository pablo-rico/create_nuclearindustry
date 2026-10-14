package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * The plain geometry of the surfaces that hold control modules, in native model space: the operator
 * stands on the north side (z = 0) with +x on their left, and the blockstate turns the model
 * clockwise seen from above. Slots go left to right as the operator sees them, the near (or lower)
 * row 0 and 1, then the far (or upper) row 2 and 3. No game classes, so it is tested on its own.
 */
public final class SurfaceGeometry {
    /** Slot centres on a desk's slope, px: the slope rises 22.5 degrees from (z 1, y 9). */
    public static final float[][] DESK_SLOTS = {
            {12, 10.5307f, 4.6955f},
            {4, 10.5307f, 4.6955f},
            {12, 13.2095f, 11.1627f},
            {4, 13.2095f, 11.1627f},
    };
    /** A table's top is flush with the top of a desk's back rail. */
    public static final float TABLE_TOP = 15.5f;
    public static final float[][] TABLE_SLOTS = {
            {12, TABLE_TOP, 4},
            {4, TABLE_TOP, 4},
            {12, TABLE_TOP, 12},
            {4, TABLE_TOP, 12},
    };
    /** A wall panel is a plate against the block behind it (south), its face at this z. */
    public static final float WALL_FACE = 13;
    public static final float[][] WALL_SLOTS = {
            {12, 4, WALL_FACE},
            {4, 4, WALL_FACE},
            {12, 12, WALL_FACE},
            {4, 12, WALL_FACE},
    };
    private static final double EPSILON = 1.0e-3;
    /** From one row of slots to the next along the surface, px: 7 up a desk's slope, 8 on a table or wall. */
    public static final float DESK_ROW_PITCH = (float) Math.hypot(DESK_SLOTS[2][1] - DESK_SLOTS[0][1], DESK_SLOTS[2][2] - DESK_SLOTS[0][2]);
    public static final float FLAT_ROW_PITCH = 8;

    private SurfaceGeometry() {}

    /** Block-local (x, z) into native space by undoing a clockwise turn of {@code yaw} degrees. */
    public static double[] toNative(double x, double z, int yaw) {
        for (int i = 0; i < Math.floorMod(yaw, 360) / 90; i++) {
            double turned = z;
            z = 1 - x;
            x = turned;
        }
        return new double[]{x, z};
    }

    /** Native (x, z) into block-local space by a clockwise turn of {@code yaw} degrees, as a blockstate turns a model. */
    public static double[] fromNative(double x, double z, int yaw) {
        for (int i = 0; i < Math.floorMod(yaw, 360) / 90; i++) {
            double turned = x;
            x = 1 - z;
            z = turned;
        }
        return new double[]{x, z};
    }

    /** The slot under a native point of a flat top or a straight desk's slope, by quarters. */
    public static int topSlot(double x, double z) {
        int column = x >= 0.5 ? 0 : 1;
        int row = z < 0.5 ? 0 : 1;
        return row * 2 + column;
    }

    /** The slot under a native point of a table's top, or -1 below it (the sides). */
    public static int tableSlot(double x, double y, double z) {
        return y < TABLE_TOP / 16 - EPSILON ? -1 : topSlot(x, z);
    }

    /** The slot under a native point of a wall panel's face, or -1 on its edges. */
    public static int wallSlot(double x, double y, double z) {
        if (z > WALL_FACE / 16 + EPSILON) {
            return -1;
        }
        int column = x >= 0.5 ? 0 : 1;
        int row = y < 0.5 ? 0 : 1;
        return row * 2 + column;
    }
}
