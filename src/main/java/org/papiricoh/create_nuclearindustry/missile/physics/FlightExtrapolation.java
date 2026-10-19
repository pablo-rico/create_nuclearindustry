package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * Carries a flight on between the samples the server sends, so a rocket moving several blocks a tick never stutters: from the
 * last sample it flies on with the acceleration seen between the last two (or gravity alone once coasting), and what is drawn
 * eases onto that rather than jumping.
 */
public final class FlightExtrapolation {

    /** Where the server said the flight was at game tick {@code tick}. */
    public record Sample(long tick, V3 pos, V3 vel) {
    }

    private FlightExtrapolation() {
    }

    /** The acceleration between two samples, or zero if they share a tick. */
    public static V3 accel(Sample older, Sample newer) {
        long dt = newer.tick() - older.tick();
        return dt <= 0 ? V3.ZERO : newer.vel().sub(older.vel()).scale(1.0 / dt);
    }

    /** Free fall at {@code gravity} blocks per tick squared. */
    public static V3 gravity(double gravity) {
        return new V3(0, -gravity, 0);
    }

    /** Where {@code s} is {@code dtTicks} after it, at constant acceleration. */
    public static V3 position(Sample s, V3 accel, double dtTicks) {
        return s.pos().add(s.vel().scale(dtTicks)).add(accel.scale(0.5 * dtTicks * dtTicks));
    }

    public static V3 velocity(Sample s, V3 accel, double dtTicks) {
        return s.vel().add(accel.scale(dtTicks));
    }

    /**
     * {@code shown} moved {@code rate} (0 to 1) of the way to {@code target}: never past it, and all the way once they are
     * {@code snap} blocks apart, where easing would only trail behind.
     */
    public static V3 converge(V3 shown, V3 target, double rate, double snap) {
        V3 error = target.sub(shown);
        if (error.length() >= snap) {
            return target;
        }
        return shown.add(error.scale(Math.max(0, Math.min(1, rate))));
    }
}
