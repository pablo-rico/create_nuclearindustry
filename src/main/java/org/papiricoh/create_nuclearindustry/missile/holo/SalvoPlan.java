package org.papiricoh.create_nuclearindustry.missile.holo;

/** Launch offsets for simultaneous or ordered impacts. This never triggers a launch. */
public final class SalvoPlan {
    private SalvoPlan() {}

    public static int[] delays(double[] flightTicks, int spacing, int maxDelay) {
        if (spacing < 0 || maxDelay < 0) return null;
        double start = 0;
        for (int i = 0; i < flightTicks.length; i++) {
            if (!Double.isFinite(flightTicks[i]) || flightTicks[i] < 0) return null;
            start = Math.max(start, flightTicks[i] - (double) i * spacing);
        }
        int[] delays = new int[flightTicks.length];
        for (int i = 0; i < delays.length; i++) {
            long delay = Math.round(start + (double) i * spacing - flightTicks[i]);
            if (delay < 0 || delay > maxDelay) return null;
            delays[i] = (int) delay;
        }
        return delays;
    }
}
