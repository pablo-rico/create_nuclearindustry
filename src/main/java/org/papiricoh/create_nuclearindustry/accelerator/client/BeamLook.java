package org.papiricoh.create_nuclearindustry.accelerator.client;

/**
 * How the beam looks: its colour climbs from blue at low energy through violet to white above
 * 10 GeV, it brightens with the logarithm of its current, and it flickers when much of it is lost.
 * Pure arithmetic, so JUnit can check it.
 *
 * @param red   0..1
 * @param green 0..1
 * @param blue  0..1
 * @param alpha core opacity, 0..1
 */
public record BeamLook(float red, float green, float blue, float alpha) {
    private static final float[] LOW = {0.25f, 0.55f, 1.0f};
    private static final float[] MID = {0.70f, 0.40f, 1.0f};
    private static final float[] HIGH = {1.0f, 0.97f, 1.0f};

    /**
     * @param energyMev    beam energy
     * @param current      beam current, uA
     * @param lossFraction share of the beam lost
     * @param time         game time in ticks, for the flicker
     */
    public static BeamLook of(double energyMev, double current, double lossFraction, double time) {
        // 0 at 10 MeV, 1 at 1 GeV, 2 at 100 GeV.
        double x = Math.max(0, Math.min(2, (Math.log10(Math.max(1, energyMev)) - 1) / 2));
        float[] from = x < 1 ? LOW : MID;
        float[] to = x < 1 ? MID : HIGH;
        float t = (float) (x < 1 ? x : x - 1);
        float alpha = (float) Math.max(0, Math.min(0.9, 0.25 + 0.2 * Math.log10(1 + Math.max(0, current))));
        if (current <= 0) {
            alpha = 0;
        } else if (lossFraction > 0.25) {
            alpha *= (float) (0.75 + 0.25 * Math.sin(time * 1.7));
        }
        return new BeamLook(from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t,
                from[2] + (to[2] - from[2]) * t, alpha);
    }
}
