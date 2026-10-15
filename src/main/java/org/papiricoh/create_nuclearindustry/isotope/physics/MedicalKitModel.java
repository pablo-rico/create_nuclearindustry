package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * The medical isotope kit: a technetium-99m tracer. Injected, it maps where the body holds its
 * contamination, so the doctor gets a full diagnosis (dose held, lifetime dose, internal
 * contamination) and the treatments that follow work better: potassium iodide blocks more uptake and
 * Prussian blue flushes more. Tc-99m lives six hours, so a kit is only good for a short while after
 * the elution that filled it; a weaker kit gives a shorter, smaller boost. Pure arithmetic.
 *
 * <p>The boost {@code k} applies on top of the configured treatments:
 * <ul>
 *   <li>iodide: the dose share taken up is {@code iodideAbsorption^((amplifier + 1) * k)};</li>
 *   <li>Prussian blue: the dose flushed per second is {@code purgePerSecond * (amplifier + 1) * k}.</li>
 * </ul>
 */
public final class MedicalKitModel {

    /**
     * @param minActivity lowest kit activity that still images, relative
     * @param maxBoost    extra treatment effect at full activity ({@code k = 1 + maxBoost * activity})
     * @param boostTicks  how long the boost lasts at full activity, ticks
     */
    public record Params(double minActivity, double maxBoost, int boostTicks) {
        /** Half again as effective for two minutes; a kit is no use once under a tenth (3.3 half-lives, ~20 min). */
        public static final Params DEFAULT = new Params(0.1, 0.5, 2400);
    }

    private MedicalKitModel() {}

    /** Whether a kit of this activity can still be used. */
    public static boolean usable(Params p, double activity) {
        return activity >= p.minActivity();
    }

    /** Treatment multiplier {@code k}, 1 with an unusable kit. */
    public static double boost(Params p, double activity) {
        if (!usable(p, activity)) {
            return 1.0;
        }
        return 1.0 + Math.max(0.0, p.maxBoost()) * Math.min(1.0, activity);
    }

    /** Ticks the boost lasts, in proportion to the kit's activity (capped at full); 0 if unusable. */
    public static int boostTicks(Params p, double activity) {
        if (!usable(p, activity)) {
            return 0;
        }
        return (int) Math.max(1, Math.round(Math.max(0, p.boostTicks()) * Math.min(1.0, activity)));
    }

    /**
     * Share of the dose rate taken up under potassium iodide of {@code amplifier}, boosted by {@code k}.
     * {@code k = 1} gives the plain {@code base^(amplifier + 1)}.
     */
    public static double boostedUptake(double baseAbsorption, int amplifier, double k) {
        double base = Math.max(0.0, Math.min(1.0, baseAbsorption));
        return Math.pow(base, (Math.max(0, amplifier) + 1) * Math.max(1.0, k));
    }

    /** Dose flushed per second by Prussian blue of {@code amplifier}, boosted by {@code k}. */
    public static double boostedPurge(double purgePerSecond, int amplifier, double k) {
        return Math.max(0.0, purgePerSecond) * (Math.max(0, amplifier) + 1) * Math.max(1.0, k);
    }

    /** Boost carried per amplifier level of a status effect: 0.1, so the amplifier stores {@code k} to a tenth. */
    public static final double BOOST_PER_LEVEL = 0.1;

    /**
     * Amplifier of a status effect that carries the boost {@code k}: {@code round((k - 1) / 0.1) - 1},
     * or -1 (give no effect) for no boost. {@link #boostFromAmplifier} reads it back.
     */
    public static int effectAmplifier(double k) {
        long steps = Math.round((k - 1.0) / BOOST_PER_LEVEL);
        return steps <= 0 ? -1 : (int) Math.min(255, steps - 1);
    }

    /** The boost {@code k} a status effect of this amplifier carries; 1 for none (amplifier below 0). */
    public static double boostFromAmplifier(int amplifier) {
        return amplifier < 0 ? 1.0 : 1.0 + (amplifier + 1) * BOOST_PER_LEVEL;
    }
}
