package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * Activation of a target in a reactor's irradiation channel (cobalt-59 to cobalt-60). The target
 * sees the core's neutron flux, so its fluence grows by the reactor's neutron level every tick
 * ({@code getNeutronLevel()}: 1000 at rated power, 2000 at most). The share of target atoms
 * activated saturates as the target burns in:
 * <pre>
 *   activated(Φ) = 1 - e^(-Φ / fluenceScale)
 * </pre>
 * The target is done once it reaches {@code targetActivity} of saturation, and leaves as an isotope of
 * nominal activity (1); a lower power simply takes longer. Pure arithmetic.
 */
public final class NeutronActivation {

    /**
     * @param fluenceScale   fluence for 63 % activation (one e-fold), neutron-level ticks
     * @param targetActivity share of saturation a finished target reaches, 0..1 (exclusive)
     */
    public record Params(double fluenceScale, double targetActivity) {
        /**
         * 80 % of saturation after a quarter game day (6000 ticks, five real minutes) at rated power:
         * {@code fluenceScale = 6e6 / ln 5}.
         */
        public static final Params DEFAULT = new Params(6.0e6 / Math.log(5.0), 0.8);
    }

    private NeutronActivation() {}

    /** Fluence after {@code ticks} more at {@code neutronLevel}; a shut-down reactor adds nothing. */
    public static double fluenceAfter(double fluence, double neutronLevel, long ticks) {
        double add = Math.max(0.0, neutronLevel) * Math.max(0L, ticks);
        return Math.max(0.0, fluence) + add;
    }

    /** Share of the target activated at this fluence, 0..1. */
    public static double activated(Params p, double fluence) {
        if (!(p.fluenceScale() > 0)) {
            return fluence > 0 ? 1.0 : 0.0;
        }
        return 1.0 - Math.exp(-Math.max(0.0, fluence) / p.fluenceScale());
    }

    /** Fluence a target needs: {@code -fluenceScale ln(1 - targetActivity)}. */
    public static double requiredFluence(Params p) {
        double target = Math.max(0.0, Math.min(0.999999, p.targetActivity()));
        return -Math.max(0.0, p.fluenceScale()) * Math.log(1.0 - target);
    }

    /** Whether the target is finished. */
    public static boolean done(Params p, double fluence) {
        return fluence >= requiredFluence(p) * (1 - 1.0e-9);
    }

    /** Progress for a bar and the goggles, 0..1: linear in fluence, so it moves steadily at constant power. */
    public static double progress(Params p, double fluence) {
        double need = requiredFluence(p);
        if (need <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, fluence / need));
    }

    /** Ticks left at this neutron level, rounded up; {@link Long#MAX_VALUE} with no flux. */
    public static long ticksLeft(Params p, double fluence, double neutronLevel) {
        double left = requiredFluence(p) - Math.max(0.0, fluence);
        if (left <= 0) {
            return 0L;
        }
        if (!(neutronLevel > 0)) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(left / neutronLevel - 1.0e-9);
    }

    /**
     * Activity of the isotope a target becomes, relative: 1 at exactly {@link #requiredFluence}, a
     * little more if it overshot within the tick, never above {@code 1 / targetActivity}.
     */
    public static double outputActivity(Params p, double fluence) {
        double target = p.targetActivity();
        if (!(target > 0)) {
            return 1.0;
        }
        return activated(p, fluence) / target;
    }

    /** Fluence left over after a finished target, carried to the next one (0 if not finished). */
    public static double carry(Params p, double fluence) {
        return done(p, fluence) ? Math.max(0.0, fluence - requiredFluence(p)) : Math.max(0.0, fluence);
    }
}
