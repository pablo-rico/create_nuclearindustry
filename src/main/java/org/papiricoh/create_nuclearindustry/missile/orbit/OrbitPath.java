package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.papiricoh.create_nuclearindustry.missile.physics.TargetPath;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * A satellite as an interceptor sees it: its orbit from game tick {@code now} on, for {@code horizon} ticks (an orbit never comes
 * down, so how far ahead to look is the shooter's choice), flown straight on without wrapping round the world border: an
 * interceptor meets it where it is about to be, not where it reappears.
 */
public record OrbitPath(Orbit orbit, long now, double horizon) implements TargetPath {
    @Override
    public V3 at(double ahead) {
        return orbit.unwrapped(now + ahead, now);
    }

    @Override
    public V3 velocityAt(double ahead) {
        return orbit.velocity(now + ahead);
    }

    @Override
    public double remaining() {
        return horizon;
    }
}
