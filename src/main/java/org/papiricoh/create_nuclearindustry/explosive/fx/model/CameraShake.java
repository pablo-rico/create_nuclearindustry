package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;

/**
 * How a detonation shakes the view, in three parts that arrive one after another as they do in films of real tests: first the
 * ground's tremor ({@link #SEISMIC_SECONDS}, carried by rock faster than the air), then the shock front's hard jolt
 * ({@link #SHOCK_SECONDS}), then the long, low rumble that follows it ({@link #RUMBLE_SECONDS}). Each part's strength comes from
 * the distance and the yield; the shock's from the overpressure where the player stands, so it matches the boom and the damage.
 * The motion itself is a sum of sines, the same for every frame at the same moment. No game classes.
 */
public final class CameraShake {
    public static final double SEISMIC_SECONDS = 3.5;
    public static final double SHOCK_SECONDS = 1.8;
    public static final double RUMBLE_SECONDS = 14;
    /** The most the view ever turns, degrees, whatever is going off. */
    public static final double MAX_DEGREES = 6;

    private CameraShake() {}

    /** How hard each part shakes, 0 for none: around 1 for the shock is a hard jolt. */
    public record Strength(double seismic, double shock, double rumble) {
        public static final Strength NONE = new Strength(0, 0, 0);

        public boolean none() {
            return seismic <= 0 && shock <= 0 && rumble <= 0;
        }
    }

    /**
     * A nuclear detonation of {@code yieldKg} (TNT, kg) {@code distance} blocks away whose shock dies out at {@code waveRadius}:
     * past it the jolt is only the air's thump, fading with distance.
     */
    public static Strength nuclear(double distance, double yieldKg, double waveRadius) {
        if (yieldKg <= 0) return Strength.NONE;
        double reach = 25 * Math.cbrt(yieldKg);
        double shock = distance <= waveRadius * 1.1
                ? clamp(BlastWaveModel.overpressure(Math.max(1, distance), yieldKg) / 15, 0.08, 1.5)
                : clamp(0.15 * waveRadius / Math.max(1, distance), 0, 0.15);
        double seismic = 0.6 * Math.exp(-distance / reach);
        double rumble = 0.35 * Math.exp(-distance / (1.5 * reach));
        return new Strength(seismic, shock, rumble);
    }

    /** A conventional blast: a jolt where its wave still bites, a light tremor close by and next to no rumble. */
    public static Strength conventional(double distance, double yieldKg, double waveRadius) {
        if (yieldKg <= 0) return Strength.NONE;
        double reach = Math.max(8, 2.5 * waveRadius);
        if (distance > reach * 2) return Strength.NONE;
        double kPa = BlastWaveModel.overpressure(Math.max(1, distance), yieldKg);
        double shock = clamp(kPa / 20, 0, 1.0) + clamp(0.08 * (1 - distance / (reach * 2)), 0, 0.08);
        double seismic = 0.25 * Math.exp(-distance / reach) * clamp(Math.cbrt(yieldKg) / 20, 0.1, 1);
        double rumble = 0.08 * Math.exp(-distance / reach) * clamp(Math.cbrt(yieldKg) / 20, 0, 1);
        return new Strength(seismic, shock, rumble);
    }

    /**
     * The view's turn, degrees (yaw, pitch, roll into {@code out}), {@code sinceSeismic} seconds after the tremor reached the
     * player and {@code sinceShock} after the shock did (negative before), for a detonation's own {@code phase}.
     */
    public static void sample(Strength s, double sinceSeismic, double sinceShock, double phase, double[] out) {
        double yaw = 0, pitch = 0, roll = 0;
        if (s.seismic() > 0 && sinceSeismic >= 0 && sinceSeismic <= SEISMIC_SECONDS) {
            double x = sinceSeismic / SEISMIC_SECONDS;
            double env = Math.min(1, sinceSeismic / 0.25) * (1 - x) * (1 - x);
            double a = 0.9 * s.seismic() * env;
            double t = sinceSeismic;
            // The ground heaves: mostly up and down, tipping the view.
            pitch += a * (Math.sin(2 * Math.PI * 7 * t + phase) + 0.5 * Math.sin(2 * Math.PI * 11 * t + 1.7 * phase));
            roll += a * 0.6 * Math.sin(2 * Math.PI * 5 * t + 2.3 + phase);
            yaw += a * 0.3 * Math.sin(2 * Math.PI * 9 * t + 0.4 + phase);
        }
        if (s.shock() > 0 && sinceShock >= 0 && sinceShock <= SHOCK_SECONDS) {
            double x = sinceShock / SHOCK_SECONDS;
            double env = Math.exp(-sinceShock * 2.6) * (1 - x);
            double a = 3.0 * s.shock() * env;
            double t = sinceShock;
            yaw += a * Math.sin(2 * Math.PI * 7.5 * t + phase);
            pitch += a * Math.sin(2 * Math.PI * 9.7 * t + 1.3 + phase);
            roll += a * 0.6 * Math.sin(2 * Math.PI * 5.9 * t + 2.1 + phase);
        }
        if (s.rumble() > 0 && sinceShock >= 0.3 && sinceShock <= RUMBLE_SECONDS) {
            double x = (sinceShock - 0.3) / (RUMBLE_SECONDS - 0.3);
            double env = Math.sqrt(Math.sin(Math.PI * Math.min(1, x * 4 + 0.0001) * 0.5)) * (1 - x);
            double a = 0.6 * s.rumble() * env;
            double t = sinceShock;
            yaw += a * Math.sin(2 * Math.PI * 2.3 * t + 0.7 + phase);
            pitch += a * Math.sin(2 * Math.PI * 3.1 * t + 2.9 + phase);
            roll += a * 0.5 * Math.sin(2 * Math.PI * 1.7 * t + 1.1 + phase);
        }
        out[0] = clamp(yaw, -MAX_DEGREES, MAX_DEGREES);
        out[1] = clamp(pitch, -MAX_DEGREES, MAX_DEGREES);
        out[2] = clamp(roll, -MAX_DEGREES, MAX_DEGREES);
    }

    /** Whether any part still shakes at these moments. */
    public static boolean active(Strength s, double sinceSeismic, double sinceShock) {
        return (s.seismic() > 0 && sinceSeismic >= 0 && sinceSeismic <= SEISMIC_SECONDS)
                || (s.shock() > 0 && sinceShock >= 0 && sinceShock <= SHOCK_SECONDS)
                || (s.rumble() > 0 && sinceShock >= 0 && sinceShock <= RUMBLE_SECONDS);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
