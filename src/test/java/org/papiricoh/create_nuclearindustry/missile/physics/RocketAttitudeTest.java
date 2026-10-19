package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The rocket's axis follows its velocity, and its heading holds while it climbs straight up. */
class RocketAttitudeTest {
    private static final double EPS = 1.0e-9;

    private static void assertV3(V3 expected, V3 actual) {
        assertEquals(expected.x(), actual.x(), 1.0e-6, "x of " + actual);
        assertEquals(expected.y(), actual.y(), 1.0e-6, "y of " + actual);
        assertEquals(expected.z(), actual.z(), 1.0e-6, "z of " + actual);
    }

    @Test
    void theAxisLiesAlongTheVelocity() {
        for (V3 v : new V3[]{new V3(0, 3, 0), new V3(1, 1, 0), new V3(0, 0, -2), new V3(-1, -3, 2), new V3(0.3, -5, -0.4)}) {
            RocketAttitude.Quat q = RocketAttitude.orientation(v, 0.7);
            assertV3(v.normalize(), q.rotate(V3.UP));
        }
    }

    @Test
    void climbingStraightUpKeepsTheLaunchHeading() {
        double heading = Math.PI / 2; // towards +X
        RocketAttitude.Quat q = RocketAttitude.orientation(new V3(0, 4, 0), heading);
        assertV3(V3.UP, q.rotate(V3.UP));
        assertV3(new V3(1, 0, 0), q.rotate(new V3(0, 0, 1)));
    }

    @Test
    void inLevelFlightTheBellyFacesDown() {
        // Flying level towards +Z the rocket's local +Z (its heading side) points down.
        RocketAttitude.Quat q = RocketAttitude.orientation(new V3(0, 0, 3), 0);
        assertV3(new V3(0, 0, 1), q.rotate(V3.UP));
        assertV3(new V3(0, -1, 0), q.rotate(new V3(0, 0, 1)));
        assertV3(new V3(1, 0, 0), q.rotate(new V3(1, 0, 0)));
    }

    @Test
    void headingIsZeroTowardsPlusZ() {
        assertEquals(0, RocketAttitude.heading(0, 1), EPS);
        assertEquals(Math.PI / 2, RocketAttitude.heading(1, 0), EPS);
        assertEquals(0.4, RocketAttitude.heading(new V3(0, 1, 0), 0.4), EPS);
    }

    @Test
    void aNozzleOffsetTurnsWithTheRocket() {
        RocketAttitude.Quat q = RocketAttitude.orientation(new V3(1, 0, 0), 0);
        V3 world = RocketAttitude.toWorld(new V3(10, 64, 10), q, new V3(0, 3, 0));
        assertV3(new V3(13, 64, 10), world);
    }
}
