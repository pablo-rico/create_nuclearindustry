package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * The chance an interceptor that reaches its target destroys it: best for a head-on meeting tracked by several radars well
 * within its reach, worse in the terminal phase, never against a boosting missile. Pure: no game classes, no config.
 */
public final class KillProbability {
    private KillProbability() {
    }

    /**
     * What the chance depends on.
     *
     * @param basePk          the interceptor's best chance
     * @param phaseAtArrival  the target's phase at the meeting
     * @param closingCos      cosine of the angle between the interceptor's heading and the target's reversed: 1 head-on
     * @param rangeFraction   how far into its reach the meeting is, 0 to 1
     * @param radarsTracking  working radars that see the target
     * @param trackAgeSeconds how long it has been tracked
     * @param seekerFactor    the interceptor's guidance, 0 to 1
     * @param terminalFactor  what the terminal phase leaves of the chance
     * @param pkScale         the server's multiplier
     * @param maxPk           the server's ceiling
     */
    public record Inputs(double basePk, MissileFlight.Phase phaseAtArrival, double closingCos, double rangeFraction, int radarsTracking,
                         double trackAgeSeconds, double seekerFactor, double terminalFactor, double pkScale, double maxPk) {
    }

    /** The chance, 0 to 1. */
    public static double of(Inputs in) {
        return Math.max(0, Math.min(in.maxPk(), in.basePk() * in.pkScale()));
    }
}
