package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * The flash of light and heat a nuclear fireball gives off, which reaches further than the blast: its
 * strength falls with the square of the distance, 1 at the edge of its reach. Whatever sees the fireball
 * burns for a while and takes damage closer in; the ground scorches, leaves burn off, trees char and
 * fires start. No game classes.
 */
public final class ThermalPulse {
    private ThermalPulse() {}

    /** The flash's strength {@code distance} blocks from the fireball reaching {@code reach}: 1 at the edge. */
    public static double strength(double distance, double reach) {
        if (reach <= 0) {
            return 0;
        }
        double r = Math.max(1.0, distance);
        return (reach / r) * (reach / r);
    }

    /** Seconds a body in the open burns for. */
    public static int fireSeconds(double strength) {
        return strength < 1 ? 0 : (int) Math.min(20, Math.round(4 * strength));
    }

    /** Damage, half hearts, a body in the open takes. */
    public static float damage(double strength) {
        return (float) Math.min(40, Math.max(0, strength - 1) * 6);
    }

    /** Grass and the like burn bare above this. */
    public static boolean scorches(double strength) {
        return strength >= 2;
    }

    /** Leaves burn away above this. */
    public static boolean burnsLeaves(double strength) {
        return strength >= 2.5;
    }

    /** Exposed wood chars above this. */
    public static boolean chars(double strength) {
        return strength >= 3;
    }

    /** The chance something flammable in the open catches fire. */
    public static double fireChance(double strength) {
        return Math.max(0, Math.min(0.35, (strength - 1) * 0.08));
    }
}
