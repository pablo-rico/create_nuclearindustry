package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

/**
 * Saturation pressure of water from 100 °C to the critical point, from the IAPWS steam tables.
 * The pressurized loop needs it both ways: the pressurizer's steam bubble sets the pressure from its
 * temperature, and the core boils once the coolant passes the saturation temperature at that pressure.
 * Between table points the logarithm of the pressure is interpolated linearly, which follows the
 * curve to well under 1%. Outside the range values are clamped to its ends.
 */
public final class SaturationTable {
    public static final double MIN_TEMPERATURE = 100.0;
    public static final double MAX_TEMPERATURE = 374.0;

    private static final double[] CELSIUS = {
            100, 110, 120, 130, 140, 150, 160, 170, 180, 190,
            200, 210, 220, 230, 240, 250, 260, 270, 280, 290,
            300, 310, 320, 330, 340, 350, 360, 370, 374};
    private static final double[] BAR = {
            1.0142, 1.4338, 1.9867, 2.7028, 3.6154, 4.7616, 6.1823, 7.9219, 10.028, 12.552,
            15.549, 19.077, 23.196, 27.971, 33.469, 39.762, 46.923, 55.030, 64.166, 74.418,
            85.879, 98.651, 112.84, 128.58, 146.01, 165.29, 186.66, 210.43, 220.64};
    private static final double[] LOG_BAR = new double[BAR.length];

    static {
        for (int i = 0; i < BAR.length; i++) {
            LOG_BAR[i] = Math.log(BAR[i]);
        }
    }

    private SaturationTable() {}

    public static double minPressure() {
        return BAR[0];
    }

    public static double maxPressure() {
        return BAR[BAR.length - 1];
    }

    /** Pressure, in bar, at which water boils at {@code celsius}. */
    public static double pressure(double celsius) {
        if (!(celsius > MIN_TEMPERATURE)) {
            return BAR[0];
        }
        if (celsius >= MAX_TEMPERATURE) {
            return BAR[BAR.length - 1];
        }
        int i = segment(CELSIUS, celsius);
        double t = (celsius - CELSIUS[i]) / (CELSIUS[i + 1] - CELSIUS[i]);
        return Math.exp(LOG_BAR[i] + t * (LOG_BAR[i + 1] - LOG_BAR[i]));
    }

    /** Temperature, in °C, at which water boils under {@code bar}. */
    public static double temperature(double bar) {
        if (!(bar > BAR[0])) {
            return MIN_TEMPERATURE;
        }
        if (bar >= BAR[BAR.length - 1]) {
            return MAX_TEMPERATURE;
        }
        double log = Math.log(bar);
        int i = segment(LOG_BAR, log);
        double t = (log - LOG_BAR[i]) / (LOG_BAR[i + 1] - LOG_BAR[i]);
        return CELSIUS[i] + t * (CELSIUS[i + 1] - CELSIUS[i]);
    }

    /** Index of the table interval holding {@code value}; the table is increasing. */
    private static int segment(double[] table, double value) {
        int low = 0;
        int high = table.length - 2;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (table[mid] <= value) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }
}
