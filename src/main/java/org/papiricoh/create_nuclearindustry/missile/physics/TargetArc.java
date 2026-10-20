package org.papiricoh.create_nuclearindustry.missile.physics;

import org.jetbrains.annotations.Nullable;

/**
 * A burnt-out missile's free fall as an interceptor sees it: where it left its engines, how fast, and how far along it is.
 * Free fall is exact, so its place any number of ticks ahead is known, up to the tick it comes down ({@link #remaining}).
 *
 * @param since     ticks since burnout, now
 * @param remaining ticks from now until it comes down
 */
public record TargetArc(V3 burnoutPos, V3 burnoutVel, double gravity, double since, double remaining) implements TargetPath {

    /** The arc {@code flight} is on now, or null while it still burns or once it is down: a boosting missile cannot be met. */
    @Nullable
    public static TargetArc of(MissileFlight flight, double gravity) {
        if (!flight.burnedOut() || flight.impacted()) return null;
        double since = flight.tick() - flight.burnoutTick();
        return new TargetArc(flight.burnoutPosition(), flight.burnoutVelocity(), gravity, since, Math.max(0, flight.fallTicks() - since));
    }

    /** Where it is {@code ahead} ticks from now (the state after that many more steps of its flight). */
    @Override
    public V3 at(double ahead) {
        return BallisticModel.position(burnoutPos, burnoutVel, since + ahead, gravity);
    }

    @Override
    public V3 velocityAt(double ahead) {
        return BallisticModel.velocity(burnoutVel, since + ahead, gravity);
    }

    /** Whether it is still in the air {@code ahead} ticks from now. */
    @Override
    public boolean alive(double ahead) {
        return ahead < remaining;
    }
}
