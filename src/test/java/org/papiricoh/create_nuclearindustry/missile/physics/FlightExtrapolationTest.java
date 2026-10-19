package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flying a flight on between samples. */
class FlightExtrapolationTest {

    @Test
    void constantAccelerationIsExact() {
        V3 a = new V3(0.01, -0.005, 0);
        FlightExtrapolation.Sample s0 = new FlightExtrapolation.Sample(100, new V3(0, 70, 0), new V3(1, 2, 0));
        FlightExtrapolation.Sample s1 = new FlightExtrapolation.Sample(104, FlightExtrapolation.position(s0, a, 4),
                FlightExtrapolation.velocity(s0, a, 4));
        V3 seen = FlightExtrapolation.accel(s0, s1);
        assertEquals(a.x(), seen.x(), 1.0e-12);
        assertEquals(a.y(), seen.y(), 1.0e-12);
        V3 later = FlightExtrapolation.position(s1, seen, 6);
        V3 exact = FlightExtrapolation.position(s0, a, 10);
        assertEquals(0, later.distance(exact), 1.0e-9);
    }

    @Test
    void samplesOnTheSameTickGiveNoAcceleration() {
        FlightExtrapolation.Sample s = new FlightExtrapolation.Sample(5, V3.ZERO, new V3(1, 0, 0));
        assertEquals(V3.ZERO, FlightExtrapolation.accel(s, s));
    }

    @Test
    void convergingNeverOvershootsAndSnapsWhenFarOff() {
        V3 target = new V3(10, 0, 0);
        V3 shown = V3.ZERO;
        double before = shown.distance(target);
        for (int i = 0; i < 20; i++) {
            shown = FlightExtrapolation.converge(shown, target, 0.35, 24);
            double now = shown.distance(target);
            assertTrue(now <= before, "each step closes in");
            assertTrue(shown.x() <= target.x(), "and never passes the target");
            before = now;
        }
        assertEquals(target, FlightExtrapolation.converge(V3.ZERO, new V3(100, 0, 0).add(target), 0.35, 24).sub(new V3(100, 0, 0)));
    }
}
