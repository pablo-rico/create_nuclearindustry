package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * Free flight under the game's gravity, on a flat world without air: what a missile does once its
 * engines stop. A trajectory is fixed by where and how fast its engines left it, so everything after
 * burnout is worked out exactly rather than stepped, and a missile far from every player flies on
 * without its chunks.
 */
public final class BallisticModel {

    /**
     * How the flight is scaled to the game.
     *
     * @param gravity       blocks per tick squared pulling everything down
     * @param riseHeight    blocks the missile climbs straight up, clear of its silo, before it turns
     *                      towards the target
     * @param minRange      horizontal distance, blocks, the shortest shot is flown as: a nearer target
     *                      is lobbed high instead of skimmed
     * @param atmosphereTop the height where the air ends: above it a missile is in midcourse
     * @param terminalTicks ticks before impact counted as the terminal phase
     */
    public record Params(double gravity, double riseHeight, double minRange, double atmosphereTop, int terminalTicks) {
        public static final Params DEFAULT = new Params(0.005, 24, 64, 400, 100);
    }

    private BallisticModel() {}

    /**
     * Ticks a free flight takes over {@code horizontal} blocks at the speed that needs least energy: a
     * 45 degree shot, whose time grows with the square root of the range.
     */
    public static double timeOfFlight(double horizontal, Params params) {
        return Math.sqrt(2.0 * Math.max(horizontal, params.minRange()) / params.gravity());
    }

    /**
     * The velocity that, from {@code position}, falls freely on {@code aim} after the least-energy time
     * of flight for the distance between them.
     */
    public static V3 requiredVelocity(V3 position, V3 aim, Params params) {
        V3 toAim = aim.sub(position);
        double tf = timeOfFlight(toAim.horizontalLength(), params);
        return toAim.scale(1.0 / tf).add(V3.UP.scale(0.5 * params.gravity() * tf));
    }

    /** Where a free flight from {@code start} at {@code velocity} is {@code ticks} later. */
    public static V3 position(V3 start, V3 velocity, double ticks, double gravity) {
        return start.add(velocity.scale(ticks)).sub(V3.UP.scale(0.5 * gravity * ticks * ticks));
    }

    public static V3 velocity(V3 velocity, double ticks, double gravity) {
        return velocity.sub(V3.UP.scale(gravity * ticks));
    }

    /**
     * Ticks until a free flight from {@code start} at {@code velocity} comes down through height
     * {@code floor}; 0 if it is already below and falling.
     */
    public static double ticksToFloor(V3 start, V3 velocity, double floor, double gravity) {
        double drop = start.y() - floor;
        double disc = velocity.y() * velocity.y() + 2.0 * gravity * drop;
        if (disc < 0) {
            return 0;
        }
        return Math.max(0, (velocity.y() + Math.sqrt(disc)) / gravity);
    }

    /** Highest point a free flight from {@code start} at {@code velocity} reaches. */
    public static double apogee(V3 start, V3 velocity, double gravity) {
        return velocity.y() <= 0 ? start.y() : start.y() + velocity.y() * velocity.y() / (2.0 * gravity);
    }
}
