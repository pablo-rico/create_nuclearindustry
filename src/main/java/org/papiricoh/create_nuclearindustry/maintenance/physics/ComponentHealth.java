package org.papiricoh.create_nuclearindustry.maintenance.physics;

/**
 * The health of one wearing part of a plant, from 1 (new) down to 0, like
 * {@code VesselIntegrity} keeps a vessel's: it wears with use in proportion to how hard it works,
 * loses a step at every transient (a SCRAM, a valve lift, an earthquake), and once below
 * {@code safeAbove} it may fail at each check, the more likely the more worn. A failed part stays
 * failed until it is repaired. The random roll is passed in, so tests choose it. No game classes.
 */
public final class ComponentHealth {

    /**
     * @param lifeTicks     ticks working at stress 1 that wear a new part out
     * @param transientWear health one transient of severity 1 takes
     * @param safeAbove     health above which the part never fails
     * @param failChance    chance of failing at one check with no health left
     */
    public record Params(double lifeTicks, double transientWear, double safeAbove, double failChance) {}

    /** What the goggles and the radiograph call a part's state. */
    public enum Condition {
        GOOD, WORN, POOR, FAILED;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final double WORN_BELOW = 0.7;
    public static final double POOR_BELOW = 0.4;

    private double health = 1.0;
    private boolean failed;

    public double health() {
        return health;
    }

    public boolean failed() {
        return failed;
    }

    /** Wears the part for {@code ticks} ticks of work at {@code stress} (1 = rated duty, 0 = idle). */
    public void wear(Params p, double stress, int ticks) {
        if (!(stress > 0) || ticks <= 0 || !(p.lifeTicks() > 0)) {
            return;
        }
        health = clamp(health - stress * ticks / p.lifeTicks());
    }

    /** One transient of {@code severity} (1 = a full SCRAM or lift). */
    public void shock(Params p, double severity) {
        if (severity > 0) {
            health = clamp(health - p.transientWear() * severity);
        }
    }

    /** Chance one check fails a part this healthy: 0 above {@code safeAbove}, rising as the square of the wear below it. */
    public static double failureChance(Params p, double health) {
        if (health >= p.safeAbove() || !(p.safeAbove() > 0)) {
            return 0.0;
        }
        double x = (p.safeAbove() - Math.max(0, health)) / p.safeAbove();
        return Math.max(0, Math.min(1, p.failChance() * x * x));
    }

    /** One failure check with a uniform {@code roll} in [0, 1). Returns true only on the check that fails it. */
    public boolean check(Params p, double roll) {
        if (failed || roll >= failureChance(p, health)) {
            return false;
        }
        failed = true;
        return true;
    }

    /** Fails the part outright (a drill, a test, an earthquake that breaks it). */
    public void fail() {
        failed = true;
    }

    /** Maintenance: gives back {@code amount} of health and puts a failed part right. */
    public void repair(double amount) {
        health = clamp(health + Math.max(0, amount));
        failed = false;
    }

    /** A new part. */
    public void restore() {
        health = 1.0;
        failed = false;
    }

    /** Sets the state outright, as saved. */
    public void set(double health, boolean failed) {
        this.health = clamp(health);
        this.failed = failed;
    }

    public Condition condition() {
        return condition(health, failed);
    }

    public static Condition condition(double health, boolean failed) {
        if (failed) {
            return Condition.FAILED;
        }
        return health >= WORN_BELOW ? Condition.GOOD : health >= POOR_BELOW ? Condition.WORN : Condition.POOR;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
