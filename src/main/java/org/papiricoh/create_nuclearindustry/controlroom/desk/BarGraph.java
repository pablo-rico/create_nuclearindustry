package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * A bar graph module's column of LED segments: how many are lit for a reading, and the colour of each.
 * Segments above the alarm level the player set are red, the band just under it amber, the rest green,
 * so the column turns colour as the reading climbs towards trouble. No game classes.
 */
public final class BarGraph {
    public static final int SEGMENTS = 20;
    /** Where the red zone starts unless the player sets it, in % of full scale. */
    public static final int DEFAULT_ALARM = 90;
    /** The amber band runs this many % of full scale under the alarm level. */
    public static final int AMBER_BAND = 15;

    public enum Zone {GREEN, AMBER, RED}

    private BarGraph() {}

    /** Segments lit for a reading at {@code fraction} of full scale, counted from the bottom. */
    public static int lit(float fraction) {
        if (Float.isNaN(fraction)) {
            return 0;
        }
        return Math.max(0, Math.min(SEGMENTS, Math.round(fraction * SEGMENTS)));
    }

    /** The zone of segment {@code index} (0 at the bottom) for an alarm level of {@code alarm} %. */
    public static Zone zone(int index, int alarm) {
        float middle = (index + 0.5f) * 100.0f / SEGMENTS;
        if (middle >= alarm) {
            return Zone.RED;
        }
        return middle >= alarm - AMBER_BAND ? Zone.AMBER : Zone.GREEN;
    }
}
