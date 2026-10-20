package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class InterceptSolverTest {
    private final BallisticModel.Params params = BallisticModel.Params.DEFAULT;
    private final TargetArc arc = new TargetArc(new V3(100, 150, 0), new V3(-0.2, 0.1, 0), 0.005, 0, 200);
    private InterceptorEnvelope envelope(boolean exo, double range, double min, double max, int ticks) {
        return new InterceptorEnvelope(InterceptorDesigns.battery(9, 0.5), 0, 0.6, range, min, max, exo, 0.75, 20, ticks);
    }
    @Test void earliestIsVerifiedByActualSimulationIncludingDelay() {
        var env = envelope(false, 600, 48, 400, 180);
        var result = InterceptSolver.solve(arc, V3.ZERO, env, params, 7).orElseThrow();
        assertEquals(result, InterceptSolver.solve(arc, V3.ZERO, env, params, 7, 1).orElseThrow());
        assertEquals(arc.at(result.arrivalTicks() + 7), result.point());
        var flight = MissileFlight.timed(env.design(), V3.ZERO, result.point(), result.ejectVelocity(), result.arrivalTicks(), env.rise());
        assertTrue(flight.predict(params).onTarget());
        while (!flight.impacted()) flight.step(params);
        assertTrue(flight.position().distance(result.point()) < 1);
        assertEquals(flight.velocity(), result.arrivalVelocity());
        for (int n = 1; n < result.arrivalTicks(); n++) {
            var shorter = envelope(false, 600, 48, 400, n);
            assertTrue(InterceptSolver.solve(arc, V3.ZERO, shorter, params, 7, 1).isEmpty());
        }
    }
    @Test void respectsBandsRangeAltitudeAndTime() {
        assertTrue(InterceptSolver.solve(arc, V3.ZERO, envelope(true, 600, 0, 1000, 180), params, 0).isEmpty());
        assertTrue(InterceptSolver.solve(arc, V3.ZERO, envelope(false, 1, 0, 400, 180), params, 0).isEmpty());
        assertTrue(InterceptSolver.solve(arc, V3.ZERO, envelope(false, 600, 300, 400, 180), params, 0).isEmpty());
        assertTrue(InterceptSolver.solve(arc, V3.ZERO, envelope(false, 600, 0, 400, 1), params, 0).isEmpty());
        assertTrue(InterceptSolver.solve(arc, V3.ZERO, envelope(false, 600, 0, 400, 180), params, 199).isEmpty());
    }
}
