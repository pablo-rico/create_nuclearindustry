package org.papiricoh.create_nuclearindustry.missile.rocket;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The contraption's anchor puts the flight's point where it flies, and the Euler angles Create gets rebuild the same turn. */
class RocketPoseTest {

    private static void assertClose(V3 expected, V3 actual, String what) {
        assertEquals(expected.x(), actual.x(), 1e-6, what + " x");
        assertEquals(expected.y(), actual.y(), 1e-6, what + " y");
        assertEquals(expected.z(), actual.z(), 1e-6, what + " z");
    }

    private static V3 rx(double deg, V3 v) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new V3(v.x(), c * v.y() - s * v.z(), s * v.y() + c * v.z());
    }

    private static V3 ry(double deg, V3 v) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new V3(c * v.x() + s * v.z(), v.y(), -s * v.x() + c * v.z());
    }

    private static V3 rz(double deg, V3 v) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new V3(c * v.x() - s * v.y(), s * v.x() + c * v.y(), v.z());
    }

    @Test
    void standingInTheSiloTheAnchorIsTheFloorBlock() {
        V3 pivot = new V3(100.5, 65, -20.5);
        V3 anchor = RocketPose.anchor(pivot, RocketAttitude.Quat.IDENTITY);
        assertClose(new V3(100, 64, -21), anchor, "the floor block's corner");
    }

    @Test
    void thePivotLandsOnTheFlightWhateverTheAttitude() {
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            V3 velocity = new V3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            RocketAttitude.Quat q = RocketAttitude.orientation(velocity, random.nextDouble() * 6.28);
            V3 pivot = new V3(random.nextDouble() * 1000, random.nextDouble() * 300, random.nextDouble() * 1000);
            V3 anchor = RocketPose.anchor(pivot, q);
            assertClose(pivot, RocketPose.toWorld(anchor, q, RocketPose.PIVOT), "pivot");
            // A block five up the axis is five blocks along the rocket from the pivot.
            V3 nose = RocketPose.toWorld(anchor, q, RocketPose.PIVOT.add(new V3(0, 5, 0)));
            assertClose(pivot.add(q.rotate(new V3(0, 5, 0))), nose, "nose");
        }
    }

    @Test
    void eulerAnglesRebuildTheRotation() {
        Random random = new Random(7);
        V3[] probes = {new V3(1, 0, 0), new V3(0, 1, 0), new V3(0, 0, 1), new V3(0.3, -0.7, 2)};
        for (int i = 0; i < 300; i++) {
            V3 velocity = i == 0 ? new V3(0, 1, 0) : i == 1 ? new V3(0, -1, 0)
                    : new V3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            RocketAttitude.Quat q = RocketAttitude.orientation(velocity, random.nextDouble() * 6.28);
            double[] e = RocketPose.euler(q);
            for (V3 probe : probes) {
                assertClose(q.rotate(probe), rz(e[2], ry(e[1], rx(e[0], probe))), "probe " + probe + " at " + velocity);
            }
        }
    }

    @Test
    void theInverseUndoesTheTurn() {
        RocketAttitude.Quat q = RocketAttitude.orientation(new V3(0.4, 0.2, -0.9), 1);
        V3 v = new V3(1, 2, 3);
        assertClose(v, RocketPose.inverse(q).rotate(q.rotate(v)), "round trip");
    }
}
