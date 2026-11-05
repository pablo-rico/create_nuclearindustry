package org.papiricoh.create_nuclearindustry.dyson.swarm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the swarm's sails show round the sun. */
class SwarmGeometryTest {
    private static final double[] SUN = {0.6, 0.8, 0};

    @Test
    void sailsAreUnitDirectionsNearTheirRadius() {
        OrbitShape orbit = new OrbitShape(true, 20, 0, 0);
        double[] out = new double[4];
        for (int i = 0; i < 200; i++) {
            SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], i, i * 37.0, out);
            assertEquals(1, Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]), 1.0e-9);
            double degrees = SwarmGeometry.degreesFromSun(out, SUN[0], SUN[1], SUN[2]);
            assertTrue(degrees > 17 && degrees < 23, "sail " + i + " at " + degrees + " degrees");
            assertTrue(Math.abs(out[3]) <= 0.06, "a face-on ring is level with the sun, give or take its spread");
        }
    }

    @Test
    void anInclinedRingGoesInFrontAndBehind() {
        OrbitShape orbit = new OrbitShape(true, 20, 80, 30);
        double[] out = new double[4];
        boolean front = false, behind = false;
        for (int i = 0; i < 100; i++) {
            SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], i, 0, out);
            front |= out[3] < -0.5;
            behind |= out[3] > 0.5;
        }
        assertTrue(front && behind);
    }

    @Test
    void aTippedRingStaysCentredOnTheSun() {
        // The outermost, most tipped orbit: with perspective its near half would swell and drag the ring off the sun.
        OrbitShape orbit = new OrbitShape(true, 34, 80, 200);
        double[] out = new double[4];
        double mx = 0, my = 0, mz = 0, near = 0, far = 0;
        int n = 2000;
        for (int i = 0; i < n; i++) {
            SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], i, 0, out);
            mx += out[0];
            my += out[1];
            mz += out[2];
            double degrees = SwarmGeometry.degreesFromSun(out, SUN[0], SUN[1], SUN[2]);
            if (out[3] < -0.5) near = Math.max(near, degrees);
            if (out[3] > 0.5) far = Math.max(far, degrees);
        }
        double length = Math.sqrt(mx * mx + my * my + mz * mz);
        double[] mean = {mx / length, my / length, mz / length, 0};
        assertTrue(SwarmGeometry.degreesFromSun(mean, SUN[0], SUN[1], SUN[2]) < 1, "the ring's centre is the sun");
        assertEquals(near, far, 1.5, "its near and far halves reach as far out");
    }

    @Test
    void addingSailsNeverMovesTheOthers() {
        OrbitShape orbit = new OrbitShape(true, 15, 30, 10);
        double[] a = new double[4], b = new double[4];
        SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], 7, 1234, a);
        SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], 7, 1234, b);
        for (int k = 0; k < 4; k++) assertEquals(a[k], b[k]);
    }

    @Test
    void sailsGoRoundAndFurtherOnesSlower() {
        OrbitShape orbit = new OrbitShape(true, 10, 0, 0);
        double[] a = new double[4], b = new double[4];
        SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], 3, 0, a);
        SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], 3, SwarmGeometry.period(10) / 2, b);
        assertNotEquals(a[0], b[0], 1.0e-3);
        SwarmGeometry.sail(orbit, SUN[0], SUN[1], SUN[2], 3, SwarmGeometry.period(10), b);
        for (int k = 0; k < 3; k++) assertEquals(a[k], b[k], 1.0e-9);
        assertTrue(SwarmGeometry.period(40) > SwarmGeometry.period(10));
    }

    @Test
    void theSunHidesOnlyWhatIsBehindItsDisc() {
        double[] behind = {SUN[0], SUN[1], SUN[2], 0.8};
        double[] front = {SUN[0], SUN[1], SUN[2], -0.8};
        assertTrue(SwarmGeometry.hiddenBySun(behind, SUN[0], SUN[1], SUN[2]));
        assertTrue(!SwarmGeometry.hiddenBySun(front, SUN[0], SUN[1], SUN[2]));
    }

    @Test
    void worksWithTheSunStraightOverhead() {
        double[] out = new double[4];
        SwarmGeometry.sail(new OrbitShape(true, 25, 45, 90), 0, 1, 0, 11, 99, out);
        assertEquals(1, Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]), 1.0e-9);
    }
}
