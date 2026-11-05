package org.papiricoh.create_nuclearindustry.explosive.conventional;

/**
 * Casing fragments flying out of a blast: they spread evenly over the half sphere above the ground (all of it for a burst in
 * the air), so a body is hit by as many as its cross-section takes of that spread, and each hurts less the further it has flown
 * as the air slows it. No game classes, so it is tested on its own.
 */
public final class ShrapnelModel {
    /** Damage, half hearts, of one fragment at the blast. */
    public static final double HIT_DAMAGE = 4.0;

    private ShrapnelModel() {}

    /** Fragments expected to hit a body of {@code crossSection} square blocks {@code distance} blocks from the blast. */
    public static double expectedHits(int fragments, double distance, double crossSection, boolean airburst) {
        if (fragments <= 0 || crossSection <= 0) {
            return 0;
        }
        double d = Math.max(0.75, distance);
        double spread = (airburst ? 4 : 2) * Math.PI * d * d;
        return fragments * Math.min(1, crossSection / spread);
    }

    /** Damage of one fragment {@code distance} blocks out, fading to nothing at {@code reach}. */
    public static double hitDamage(double distance, double reach) {
        if (reach <= 0 || distance >= reach) {
            return 0;
        }
        double left = 1 - Math.max(0, distance) / reach;
        return HIT_DAMAGE * left * left;
    }
}
