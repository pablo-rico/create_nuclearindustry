package org.papiricoh.create_nuclearindustry.wastetransport.breach;

/**
 * The numbers behind a waste container's shielding and breaches, without game classes so they
 * can be unit tested. Activity is the cargo's bare dose rate in rad/s, as if it lay in the open.
 */
public final class BreachModel {
    /** Smallest spill radius, so even a trickle marks the chunk it fell in. */
    public static final int MIN_SPILL_RADIUS = 2;

    private BreachModel() {}

    /**
     * Dose rate a container gives off: the activity through its shielding (sealed) or through the
     * hole (breached), never above {@code max}.
     */
    public static double emission(double activity, boolean breached, double shieldedTransmission, double breachedTransmission,
                                  double max) {
        if (activity <= 0) {
            return 0;
        }
        double transmission = breached ? breachedTransmission : shieldedTransmission;
        return Math.min(max, activity * Math.max(0, transmission));
    }

    /** Radius of the contamination a spill leaves: grows with the square root of the activity, capped. */
    public static int spillRadius(double activity, int maxRadius) {
        if (activity <= 0 || maxRadius <= 0) {
            return 0;
        }
        int radius = MIN_SPILL_RADIUS + (int) Math.floor(Math.sqrt(activity));
        return Math.max(1, Math.min(maxRadius, radius));
    }

    /** Ground contamination, in rad/s, at the centre of a spill: proportional to the activity. */
    public static double spillIntensity(double activity, double intensityPerActivity) {
        return activity <= 0 ? 0 : activity * Math.max(0, intensityPerActivity);
    }

    /**
     * Ground contamination a breached container adds under itself every second: {@code leakFactor}
     * times its spill intensity, spread over a minute.
     */
    public static double leakPerSecond(double activity, double intensityPerActivity, double leakFactor) {
        return spillIntensity(activity, intensityPerActivity) * Math.max(0, leakFactor) / 60.0;
    }

    /** Millibuckets of a cistern's {@code amount} lost when it is breached. */
    public static int fluidSpill(int amount, double fraction) {
        if (amount <= 0 || fraction <= 0) {
            return 0;
        }
        return (int) Math.min(amount, Math.round(amount * Math.min(1, fraction)));
    }

    /** Millibuckets a breached cistern holding {@code amount} loses each second: {@code leakFactor} thousandths, at least one. */
    public static int fluidLeakPerSecond(int amount, double leakFactor) {
        if (amount <= 0 || leakFactor <= 0) {
            return 0;
        }
        return (int) Math.min(amount, Math.max(1, Math.ceil(amount * leakFactor / 1000.0)));
    }

    /** Breach damage left after a repair of {@code amount} (1 is a fresh breach, 0 sealed). */
    public static float repair(float damage, double amount) {
        return (float) Math.max(0, damage - Math.max(0, amount));
    }
}
