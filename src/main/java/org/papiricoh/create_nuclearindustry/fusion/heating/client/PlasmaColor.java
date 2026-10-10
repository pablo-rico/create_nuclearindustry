package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

/**
 * The colour of the plasma for its temperature, in the model's degrees: a dull red glow while cold,
 * orange as it heats, white around burning temperature and violet near the danger temperature,
 * where an operator should be worried. Pure, so JUnit can check the ramp.
 */
public final class PlasmaColor {
    /** Plasma temperature, in the model's degrees, past which the plasma overheats (the core's danger mark). */
    public static final double DANGER_TEMPERATURE = PlasmaModel.DANGER_TEMP;
    /** The model's ceiling. */
    public static final double MAX_TEMPERATURE = PlasmaModel.MAX_TEMP;
    /** The pink-magenta flash of the gas breaking down into plasma. */
    public static final float[] BREAKDOWN = {1.0f, 0.3f, 0.85f};

    /** Temperature stops and their colours (r, g, b in 0..1), in increasing temperature. */
    private static final double[] STOPS = {20, 800, 1800, 3000, 4200, DANGER_TEMPERATURE, MAX_TEMPERATURE};
    private static final float[][] COLORS = {
            {0.45f, 0.08f, 0.04f},  // dull red
            {0.85f, 0.22f, 0.06f},  // red-orange
            {1.00f, 0.52f, 0.14f},  // orange
            {1.00f, 0.86f, 0.62f},  // yellow-white
            {0.96f, 0.95f, 1.00f},  // white
            {0.78f, 0.52f, 1.00f},  // violet: danger
            {0.62f, 0.26f, 1.00f},  // deep violet
    };

    private PlasmaColor() {}

    /** {r, g, b} in 0..1 for this temperature; clamped to the ends of the ramp. */
    public static float[] of(double temperature) {
        if (!(temperature > STOPS[0])) {
            return COLORS[0].clone();
        }
        for (int i = 1; i < STOPS.length; i++) {
            if (temperature <= STOPS[i]) {
                float t = (float) ((temperature - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]));
                return lerp(COLORS[i - 1], COLORS[i], t);
            }
        }
        return COLORS[COLORS.length - 1].clone();
    }

    static float[] lerp(float[] a, float[] b, float t) {
        float s = Math.max(0, Math.min(1, t));
        return new float[]{a[0] + (b[0] - a[0]) * s, a[1] + (b[1] - a[1]) * s, a[2] + (b[2] - a[2]) * s};
    }
}
