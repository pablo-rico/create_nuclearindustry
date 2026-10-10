package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

/**
 * How far a blast door's panel has slid and what that leaves open, without game classes so it can be
 * unit tested. The panel slides sideways into a pocket in the wall; its offset goes from 0 (shut) to
 * the door's width (fully retracted). Columns are counted from the pocket side, so the column farthest
 * from the pocket is the first one uncovered.
 */
public final class BlastDoorTravel {

    /** What one cell of the doorway is at a given offset. */
    public enum Cell {
        /** The door is shut: the leaf shows and stops everything. */
        CLOSED,
        /** The panel is moving over this cell: drawn by the controller, still solid. */
        MOVING,
        /** The panel has left this cell: no collision, lets radiation and blasts through. */
        OPEN
    }

    private BlastDoorTravel() {}

    /** Blocks per tick when driven at {@code rpm}: {@code blocksPerRevolution} for every turn of the shaft. */
    public static double rotationRate(double rpm, double blocksPerRevolution) {
        return Math.abs(rpm) / 60.0 / 20.0 * Math.max(0, blocksPerRevolution);
    }

    /** Blocks per tick when moved by redstone, which takes {@code ticksPerBlock} ticks per block. */
    public static double redstoneRate(int ticksPerBlock) {
        return 1.0 / Math.max(1, ticksPerBlock);
    }

    /** The offset after one tick moving at {@code rate} towards open ({@code opening}) or shut. */
    public static double step(double offset, int width, double rate, boolean opening) {
        double next = offset + (opening ? rate : -rate);
        return Math.max(0, Math.min(width, next));
    }

    /**
     * The state of the column {@code fromPocket} columns away from the pocket side of a door
     * {@code width} wide whose panel has slid {@code offset} blocks.
     */
    public static Cell cell(int fromPocket, int width, double offset) {
        if (offset <= 0) {
            return Cell.CLOSED;
        }
        // Measured from the far edge, the panel covers [offset, offset + width) and this column
        // spans [width - fromPocket - 1, width - fromPocket): it is clear once offset reaches its end.
        return offset >= width - fromPocket - 1.0e-9 ? Cell.OPEN : Cell.MOVING;
    }

    /** Comparator signal of the controller: 0 shut, 15 fully open. */
    public static int comparatorSignal(double offset, int width) {
        if (width <= 0 || offset <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(15, (int) Math.round(15.0 * offset / width)));
    }
}
