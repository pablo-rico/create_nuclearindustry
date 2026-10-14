package org.papiricoh.create_nuclearindustry.controlroom.client;

/**
 * Where a graph's legend entries go above it. Two entries keep the layout screens always had: the
 * first on the left, the second against the right edge. A third shares one row when each entry gets
 * at least {@link #MIN_SLOT} pixels, otherwise it goes on a second row on its own.
 */
public final class GraphLegend {
    public static final int LINE = 10;
    /** The narrowest a legend entry may be before the third one moves to a row of its own. */
    public static final float MIN_SLOT = 24;

    /** One entry: its text starts at {@code x} (or ends there when {@code right}), on row {@code row}, in at most {@code room} pixels. */
    public record Slot(float x, int row, float room, boolean right) {}

    /** The slots for {@code entries} (2 or 3) and how many rows they take. */
    public record Layout(Slot[] slots, int rows) {
        /** How far below the legend's top the graph starts. */
        public float height() {
            return rows * LINE + 1;
        }
    }

    private GraphLegend() {}

    public static Layout layout(float x, float width, int entries) {
        float half = width / 2 - 2;
        Slot first = new Slot(x, 0, half, false);
        Slot second = new Slot(x + width, 0, half, true);
        if (entries < 3) {
            return new Layout(new Slot[]{first, second}, 1);
        }
        float third = (width - 4) / 3;
        if (third >= MIN_SLOT) {
            Slot[] slots = new Slot[3];
            for (int i = 0; i < 3; i++) {
                slots[i] = new Slot(x + i * (third + 2), 0, third, false);
            }
            return new Layout(slots, 1);
        }
        return new Layout(new Slot[]{first, second, new Slot(x, 1, width, false)}, 2);
    }
}
