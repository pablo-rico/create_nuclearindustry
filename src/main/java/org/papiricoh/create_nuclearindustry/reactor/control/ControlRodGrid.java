package org.papiricoh.create_nuclearindustry.reactor.control;

import java.util.HashMap;
import java.util.Map;

/**
 * The cells of a reactor's control channels during one rod scan: which are filled by a rod, each
 * counted once. Columns are indexed by their (x, z) when the grid is built, so marking a cell and
 * splitting the total per channel cost one lookup per filled cell, not one pass over every channel.
 * Pure Java so it can be tested without a level.
 */
public final class ControlRodGrid {
    private final Map<Long, Integer> columnByXz;
    /** Column of each channel, in the order the channels were given. */
    private final int[] channelColumn;
    private final int firstY;
    private final int height;
    private final boolean[] filled;
    private final int[] filledPerColumn;
    private int filledCells;

    /**
     * @param channelX x of each channel, in the structure's channel order
     * @param channelZ z of each channel, same order
     * @param firstY   lowest y of a channel cell
     * @param height   cells per channel
     */
    public ControlRodGrid(int[] channelX, int[] channelZ, int firstY, int height) {
        this.firstY = firstY;
        this.height = Math.max(0, height);
        this.channelColumn = new int[channelX.length];
        this.columnByXz = new HashMap<>(channelX.length * 2);
        for (int i = 0; i < channelX.length; i++) {
            Integer column = columnByXz.get(key(channelX[i], channelZ[i]));
            if (column == null) {
                column = columnByXz.size();
                columnByXz.put(key(channelX[i], channelZ[i]), column);
            }
            channelColumn[i] = column;
        }
        this.filled = new boolean[columnByXz.size() * this.height];
        this.filledPerColumn = new int[columnByXz.size()];
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** The column at (x, z), or -1 when it is not a channel. */
    public int column(int x, int z) {
        Integer column = columnByXz.get(key(x, z));
        return column == null ? -1 : column;
    }

    /** Lowest y of a channel cell. */
    public int firstY() {
        return firstY;
    }

    /** Highest y of a channel cell. */
    public int lastY() {
        return firstY + height - 1;
    }

    /**
     * Fills the cell at height y of a column.
     *
     * @return true when it was empty (and the column and y are inside the grid)
     */
    public boolean fill(int column, int y) {
        int row = y - firstY;
        if (column < 0 || column >= filledPerColumn.length || row < 0 || row >= height) {
            return false;
        }
        int index = column * height + row;
        if (filled[index]) {
            return false;
        }
        filled[index] = true;
        filledPerColumn[column]++;
        filledCells++;
        return true;
    }

    /** Distinct cells filled so far. */
    public int filledCells() {
        return filledCells;
    }

    /** How far each channel is filled, 0..1, in the order the channels were given. */
    public double[] channelInsertions() {
        double[] insertions = new double[channelColumn.length];
        for (int i = 0; i < insertions.length; i++) {
            insertions[i] = height <= 0 ? 0 : Math.min(1.0, filledPerColumn[channelColumn[i]] / (double) height);
        }
        return insertions;
    }

    /** The lowest block coordinate whose cell [c, c + 1) can overlap the span [min, max] by more than nothing. */
    public static int firstCell(double min) {
        return (int) Math.floor(min);
    }

    /** The highest block coordinate whose cell [c, c + 1) can overlap the span [min, max] by more than nothing. */
    public static int lastCell(double max) {
        return (int) Math.ceil(max) - 1;
    }
}
