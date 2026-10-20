package org.papiricoh.create_nuclearindustry.missile.fx;

/**
 * How a rocket sounds from where the player stands: how late a sound arrives (at the speed of sound the explosions use), how
 * the near roar gives way to the distant rumble with distance, and when a warhead coming down cracks a sonic boom. Free of
 * game classes, for JUnit.
 * <p>
 * Rockets here fly at a few blocks a tick, well under the speed of sound in blocks (17.15 a tick), so a true boom never
 * happens. The game threshold instead: a warhead re-entering or in its last seconds that passes within
 * {@link #BOOM_DISTANCE} blocks while faster than {@link #BOOM_SPEED} blocks a tick booms once, at its closest.
 */
public final class RocketAcoustics {
    /** Blocks a sound travels in a tick (343 m/s), as for detonations. */
    public static final double SOUND_SPEED = 17.15;
    /** A boom needs the warhead to pass this close, blocks. */
    public static final double BOOM_DISTANCE = 64;
    /** And this fast, blocks per tick. */
    public static final double BOOM_SPEED = 4;
    /** The near roar is at full volume within this many blocks... */
    static final double NEAR_FULL = 40;
    /** ...and gone past this many. */
    static final double NEAR_END = 220;
    /** The distant rumble is heard to about this far. */
    public static final double FAR_REACH = 2400;

    private RocketAcoustics() {
    }

    /** Ticks a sound takes to cover {@code distance} blocks. */
    public static int delayTicks(double distance) {
        return (int) Math.round(Math.max(0, distance) / SOUND_SPEED);
    }

    /** The near roar's volume, 0 to 1, at {@code distance} blocks. */
    public static double nearVolume(double distance) {
        if (distance <= NEAR_FULL) return 1;
        if (distance >= NEAR_END) return 0;
        double t = 1 - (distance - NEAR_FULL) / (NEAR_END - NEAR_FULL);
        return t * t;
    }

    /** The distant rumble's volume, 0 to 1: it fades in as the near roar fades out, then falls off over kilometres. */
    public static double farVolume(double distance) {
        double in = Math.max(0, Math.min(1, (distance - 24) / 120.0));
        double out = 1 / (1 + Math.pow(Math.max(0, distance - 140) / 520.0, 2));
        return distance >= FAR_REACH ? 0 : in * out * (1 - Math.max(0, (distance - FAR_REACH * 0.75) / (FAR_REACH * 0.25)));
    }

    /** A one-off's volume, 0 to 1 (ignition, separation, a falling stage landing), falling off over {@code reach} blocks. */
    public static double oneShotVolume(double distance, double reach) {
        if (distance >= reach) return 0;
        double t = distance / reach;
        return Math.max(0, (1 - t) * (1 - t) / (1 + 6 * t));
    }

    /**
     * Follows a warhead's distance to the listener tick by tick and says when it booms: once, at its closest pass, if that
     * pass is within {@link #BOOM_DISTANCE} while it moves faster than {@link #BOOM_SPEED}.
     */
    public static final class Passage {
        private double closest = Double.POSITIVE_INFINITY;
        private double fastest;
        private boolean boomed;

        /** This tick's distance and speed, and whether it is re-entering or in its last seconds; true the tick it booms. */
        public boolean update(double distance, double speed, boolean descending) {
            if (boomed || !descending) return false;
            if (distance < closest) {
                closest = distance;
                fastest = Math.max(fastest, speed);
                return false;
            }
            // Moving away again: that was the closest pass.
            if (closest <= BOOM_DISTANCE && fastest > BOOM_SPEED) {
                boomed = true;
                return true;
            }
            return false;
        }

        /** It came down (an impact or the end of tracking): a boom still owed is due now. */
        public boolean finish() {
            if (!boomed && closest <= BOOM_DISTANCE && fastest > BOOM_SPEED) {
                boomed = true;
                return true;
            }
            return false;
        }

        public boolean boomed() {
            return boomed;
        }
    }
}
