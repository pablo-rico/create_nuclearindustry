package org.papiricoh.create_nuclearindustry.fusion.heating;

import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;

/**
 * How brightly a tokamak viewport glows, 0..15, for what the plasma is doing. The plasma is the
 * only light in the hall: dark while there is none, a dim pink flash at breakdown, then brighter as
 * the plasma heats up. Pure, so JUnit can check it.
 */
public final class ViewportGlow {
    public static final int MAX = 15;
    /** The faint glow of the freshly ionised gas. */
    public static final int BREAKDOWN = 4;
    /** Glow of a plasma that is lit but still cold. */
    public static final int COLD_PLASMA = 6;
    /** Plasma temperature, in the model's degrees, at which the windows glow at full strength. */
    public static final double FULL_GLOW_TEMPERATURE = 4000.0;

    private ViewportGlow() {}

    public static int level(TokamakReadings readings) {
        if (readings == null || !readings.formed() || readings.suspended()) {
            return 0;
        }
        TokamakPhase phase = readings.phase();
        if (phase == null || !phase.hasPlasma()) {
            return 0;
        }
        if (phase == TokamakPhase.BREAKDOWN) {
            return BREAKDOWN;
        }
        double heat = Math.max(0, Math.min(1, readings.temperature() / FULL_GLOW_TEMPERATURE));
        return (int) Math.round(COLD_PLASMA + (MAX - COLD_PLASMA) * heat);
    }
}
