package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

/**
 * What the plasma torus looks like this frame: colour from {@link PlasmaColor}, opacity from the
 * density, a flicker that grows with the instability and a magenta flash at breakdown. The
 * renderer draws a bright core and a faint halo with these alphas. Pure, so JUnit can check it.
 *
 * @param red        0..1
 * @param green      0..1
 * @param blue       0..1
 * @param coreAlpha  opacity of the inner, brighter shell
 * @param haloAlpha  opacity of the outer glow
 */
public record PlasmaLook(float red, float green, float blue, float coreAlpha, float haloAlpha) {
    /** Density, as a fraction of the Greenwald limit, at which the plasma looks fully dense. */
    public static final double FULL_DENSITY = 0.8;
    /** Even a thin plasma stays faintly visible. */
    public static final double MIN_DENSITY_LOOK = 0.15;
    /** Share of the opacity an instability of 100 can take away at the bottom of a flicker. */
    public static final double MAX_FLICKER = 0.6;

    /**
     * @param time ticks, with the partial tick, for the flicker and the breakdown pulse
     * @return null when there is no plasma to draw
     */
    @Nullable
    public static PlasmaLook of(TokamakPhase phase, double temperature, double density, double instability, double time) {
        if (phase == null || !phase.hasPlasma()) {
            return null;
        }
        float[] color = PlasmaColor.of(temperature);
        double core;
        double halo;
        if (phase == TokamakPhase.BREAKDOWN) {
            color = PlasmaColor.lerp(color, PlasmaColor.BREAKDOWN, 0.85f);
            // The solenoid pulse strikes the gas in a few bright flashes.
            double pulse = 0.55 + 0.45 * Math.abs(Math.sin(time * 0.9));
            core = 0.75 * pulse;
            halo = 0.4 * pulse;
        } else {
            double dense = Math.max(MIN_DENSITY_LOOK, Math.min(1, density / FULL_DENSITY));
            double hot = 0.55 + 0.45 * Math.max(0, Math.min(1, temperature / 3000.0));
            core = (0.25 + 0.45 * dense) * hot;
            halo = (0.08 + 0.2 * dense) * hot;
        }
        double factor = flicker(instability, time);
        return new PlasmaLook(color[0], color[1], color[2], (float) clamp(core * factor), (float) clamp(halo * factor));
    }

    /**
     * Brightness multiplier, 0..1: a slight shimmer always, and deep, irregular dips as the plasma
     * becomes unstable. Deterministic in {@code time}.
     */
    public static double flicker(double instability, double time) {
        double amount = Math.max(0, Math.min(100, instability)) / 100.0 * MAX_FLICKER;
        // Two incommensurate waves make an irregular flutter in 0..1.
        double noise = 0.5 + 0.5 * Math.sin(time * 2.3) * Math.sin(time * 0.71 + 1.3);
        double shimmer = 0.03 * Math.sin(time * 0.37);
        return clamp(1.0 - 0.03 + shimmer - amount * noise);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
