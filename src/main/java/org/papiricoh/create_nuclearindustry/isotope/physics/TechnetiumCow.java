package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * The technetium generator ("cow"): molybdenum-99 bound to an alumina column decays to
 * technetium-99m, which builds up until it is washed out (eluted) with saline. The activities follow
 * the two-member Bateman solution
 * <pre>
 *   A_p(t) = A_p0 e^(-λp t)
 *   A_d(t) = A_d0 e^(-λd t) + b A_p0 λd / (λd - λp) (e^(-λp t) - e^(-λd t))
 * </pre>
 * with {@code b} the share of Mo-99 decays that feed the metastable state (87.5 % in reality). With
 * the daughter shorter-lived than the parent the pair reaches transient equilibrium, the daughter at
 * {@code b λd / (λd - λp)} of the parent. Activities are relative to a fresh cow's Mo-99 (1).
 *
 * <p>The state is stored lazily, as {@link IsotopeDecay}: {@code (refTick, parent, daughter)} at the
 * last elution (or creation), and advanced to the present when someone looks. No game classes.
 */
public final class TechnetiumCow {

    /**
     * @param parentHalfLifeDays   Mo-99, game days
     * @param daughterHalfLifeDays Tc-99m, game days
     * @param branching            share of Mo-99 decays that make Tc-99m, 0..1
     * @param elutionEfficiency    share of the Tc-99m one elution washes out, 0..1
     * @param exhaustedBelow       parent activity below which the cow is spent
     */
    public record Params(double parentHalfLifeDays, double daughterHalfLifeDays, double branching,
                         double elutionEfficiency, double exhaustedBelow) {
        /** Real half-lives (one game day per real day), real branching, a 90 % elution. */
        public static final Params DEFAULT = new Params(Isotope.MOLYBDENUM_99.halfLifeDays(),
                Isotope.TECHNETIUM_99M.halfLifeDays(), 0.875, 0.9, 0.05);
    }

    /** Activities of the column: Mo-99 and the Tc-99m it holds, relative. */
    public record State(double parent, double daughter) {
        /** A freshly loaded column: nominal Mo-99, no technetium yet. */
        public static final State FRESH = new State(1.0, 0.0);
    }

    /**
     * One elution.
     *
     * @param eluted Tc-99m activity washed out, relative
     * @param after  the column afterwards (same moment)
     */
    public record Elution(double eluted, State after) {}

    private TechnetiumCow() {}

    /** The column {@code days} (scaled) later. */
    public static State advance(Params p, State state, double days) {
        double t = Math.max(0.0, days);
        double parent = Math.max(0.0, state.parent());
        double daughter = Math.max(0.0, state.daughter());
        double lp = IsotopeDecay.decayConstant(p.parentHalfLifeDays());
        double ld = IsotopeDecay.decayConstant(p.daughterHalfLifeDays());
        double b = clamp(p.branching(), 0, 1);
        double newParent = parent * Math.exp(-lp * t);
        if (ld <= 0) {
            // A stable daughter has no activity of its own.
            return new State(newParent, 0.0);
        }
        double grown;
        if (Math.abs(ld - lp) < 1.0e-12 * Math.max(ld, lp)) {
            grown = b * parent * ld * t * Math.exp(-ld * t);
        } else {
            grown = b * parent * ld / (ld - lp) * (Math.exp(-lp * t) - Math.exp(-ld * t));
        }
        return new State(newParent, Math.max(0.0, daughter * Math.exp(-ld * t) + grown));
    }

    /** Washes the column out now: takes {@code elutionEfficiency} of its Tc-99m. */
    public static Elution elute(Params p, State state) {
        double daughter = Math.max(0.0, state.daughter());
        double eluted = daughter * clamp(p.elutionEfficiency(), 0, 1);
        return new Elution(eluted, new State(Math.max(0.0, state.parent()), daughter - eluted));
    }

    /** Days after an elution (an empty column) until the Tc-99m peaks: {@code ln(λd/λp) / (λd - λp)}. */
    public static double peakDays(Params p) {
        double lp = IsotopeDecay.decayConstant(p.parentHalfLifeDays());
        double ld = IsotopeDecay.decayConstant(p.daughterHalfLifeDays());
        if (lp <= 0 || ld <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        if (Math.abs(ld - lp) < 1.0e-12 * Math.max(ld, lp)) {
            return 1.0 / ld;
        }
        return Math.log(ld / lp) / (ld - lp);
    }

    /** Tc-99m to Mo-99 in transient equilibrium, {@code b λd / (λd - λp)}; infinite if the daughter outlives the parent. */
    public static double equilibriumRatio(Params p) {
        double lp = IsotopeDecay.decayConstant(p.parentHalfLifeDays());
        double ld = IsotopeDecay.decayConstant(p.daughterHalfLifeDays());
        if (ld <= lp) {
            return Double.POSITIVE_INFINITY;
        }
        return clamp(p.branching(), 0, 1) * ld / (ld - lp);
    }

    /** Whether the column's Mo-99 is too weak to be worth milking. */
    public static boolean exhausted(Params p, State state) {
        return state.parent() < p.exhaustedBelow();
    }

    /**
     * Medical isotope kits an elution fills: one per {@code activityPerKit} of Tc-99m, at most
     * {@code maxKits}; each kit leaves at nominal activity (1).
     */
    public static int kits(double eluted, double activityPerKit, int maxKits) {
        if (!(eluted > 0) || !(activityPerKit > 0) || maxKits <= 0) {
            return 0;
        }
        return (int) Math.min(maxKits, Math.floor(eluted / activityPerKit + 1.0e-9));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
