package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxScaleTest {
    @Test void normalMissilesKeepFullScale() {
        assertEquals(1.0f, FxScale.of(0, new RocketShape(2, 0.15f, java.util.List.of(), 2)));
    }

    @Test void interceptorScaleUsesRadiusAndClamp() {
        RocketShape small = new RocketShape(2, 0.15f, java.util.List.of(), 2);
        assertEquals(0.3f, FxScale.of(FlightFxState.INTERCEPTOR, small), 1e-6f);
        assertEquals(0.2f, FxScale.of(FlightFxState.INTERCEPTOR, new RocketShape(2, 0.01f, java.util.List.of(), 2)));
        assertEquals(1.0f, FxScale.of(FlightFxState.INTERCEPTOR, new RocketShape(2, 1.0f, java.util.List.of(), 2)));
    }
}
