package org.papiricoh.create_nuclearindustry.missile.fx;

import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;

/** Keeps interceptor effects in proportion to their smaller airframe. */
public final class FxScale {
    private FxScale() {}

    public static float of(int flags, RocketShape shape) {
        if ((flags & FlightFxState.INTERCEPTOR) == 0) return 1.0f;
        return Math.max(0.2f, Math.min(1.0f, (float) (shape.radius() / 0.5)));
    }
}
