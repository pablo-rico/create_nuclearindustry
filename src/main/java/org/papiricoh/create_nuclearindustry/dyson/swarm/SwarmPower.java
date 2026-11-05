package org.papiricoh.create_nuclearindustry.dyson.swarm;

import java.util.Arrays;

/**
 * How much power the swarm beams down and how the receivers share it. The sails catch the sun whatever the hour, but a receiver
 * only takes their light while the sun is over its horizon and the sky is clear enough: their yield follows the sine of the
 * sun's elevation, dimmed by rain and thunder, and stops below a cut-off a few degrees up. The receivers then split what there is
 * evenly, none taking more than it asks for, so building more of them never makes more power. Free of game classes, for JUnit.
 */
public final class SwarmPower {
    private SwarmPower() {
    }

    /**
     * The share of the sails' light that reaches the ground, 0 to 1: {@code sinElevation} (the sun's), nothing under
     * {@code minSinElevation}, times {@code rainFactor} in full rain and {@code thunderFactor} in a full thunderstorm ({@code rain}
     * and {@code thunder} as the level gives them, 0 to 1).
     */
    public static double sunFactor(double sinElevation, double rain, double thunder, double minSinElevation,
                                   double rainFactor, double thunderFactor) {
        if (sinElevation < minSinElevation || sinElevation <= 0) return 0;
        double weather = lerp(clamp01(rain), 1, clamp01(rainFactor));
        weather = lerp(clamp01(thunder), weather, Math.min(weather, clamp01(thunderFactor)));
        return Math.min(1, sinElevation) * weather;
    }

    /** FE per tick the whole swarm sends down: {@code sails} at {@code fePerSail} each in full sun, scaled by {@code sunFactor}. */
    public static double pool(long sails, double fePerSail, double sunFactor) {
        return Math.max(0, sails) * Math.max(0, fePerSail) * clamp01(sunFactor);
    }

    /**
     * Splits {@code pool} FE per tick among receivers asking for {@code demands} (FE per tick each): evenly, none given more than
     * it asks, what one leaves going to the others ("water-filling"). Whole FE, never more than the pool in all. Those that cannot
     * have all they ask get the same, the whole FE under an even split: the remainder (less than one FE per tick each) is not
     * handed to one of them, so two alike receivers never differ.
     */
    public static int[] allocate(double pool, int[] demands) {
        int n = demands.length;
        int[] shares = new int[n];
        if (n == 0 || pool <= 0) return shares;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(demands[a], demands[b]));
        double left = Math.floor(pool);
        for (int k = 0; k < n; k++) {
            int i = order[k];
            int demand = Math.max(0, demands[i]);
            int fair = (int) Math.min(Integer.MAX_VALUE, Math.floor(left / (n - k)));
            if (demand <= fair) {
                shares[i] = demand;
                left -= demand;
                continue;
            }
            // Sorted by demand: every one still to serve asks more than this level, so they all get it.
            for (int rest = k; rest < n; rest++) shares[order[rest]] = fair;
            break;
        }
        return shares;
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
