package org.papiricoh.create_nuclearindustry.civildefense.siren;

/**
 * The pitch of a siren over time, applied to one steady recorded tone. Pure, so it can be unit
 * tested; the client sound only samples it. Pitches stay inside the 0.5 to 2.0 the sound engine
 * accepts.
 */
public final class SirenTone {
    public static final float LOW_PITCH = 0.55f;
    public static final float HIGH_PITCH = 1.3f;
    /** A steady alert (redstone): one long rise and fall every 8 seconds. */
    public static final int ALERT_PERIOD = 160;
    /** An attack warning (a threat in range): a quick wavering rise and fall every 3 seconds. */
    public static final int ATTACK_PERIOD = 60;

    private SirenTone() {}

    /**
     * Pitch {@code ticks} into a wail of {@code period} ticks: the rotor spins up over the first
     * 60%, with an ease-out, then winds down.
     */
    public static float wail(double ticks, int period) {
        double phase = ((ticks % period) + period) % period / period;
        double shape;
        if (phase < 0.6) {
            double rise = phase / 0.6;
            shape = 1.0 - (1.0 - rise) * (1.0 - rise);
        } else {
            double fall = (phase - 0.6) / 0.4;
            shape = 1.0 - fall * fall;
        }
        return (float) (LOW_PITCH + (HIGH_PITCH - LOW_PITCH) * shape);
    }

    /** Pitch of a hand-cranked siren: rises with the speed it is turned at, flat past 256 RPM. */
    public static float crank(double rpm) {
        double fraction = Math.min(1.0, Math.abs(rpm) / 256.0);
        return (float) (LOW_PITCH + (HIGH_PITCH - LOW_PITCH) * Math.sqrt(fraction));
    }
}
