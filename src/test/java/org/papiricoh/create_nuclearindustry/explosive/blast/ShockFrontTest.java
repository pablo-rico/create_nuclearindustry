package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When the shock, the sound and the ground's tremor reach a listener. */
class ShockFrontTest {

    @Test
    void insideTheWaveTheBoomRidesTheShock() {
        assertEquals(10, ShockFront.arrivalTicks(80, 8, 256), 1e-9);
        assertEquals(0, ShockFront.arrivalTicks(0, 8, 256), 1e-9);
        assertEquals(0, ShockFront.arrivalTicks(-5, 8, 256), 1e-9, "never before it went off");
    }

    @Test
    void pastTheWaveOnlySoundGoesOn() {
        double atEdge = ShockFront.arrivalTicks(256, 8, 256);
        double beyond = ShockFront.arrivalTicks(256 + 171.5, 8, 256);
        assertEquals(atEdge + 10, beyond, 1e-9, "171.5 more blocks at the speed of sound take 10 ticks");
        assertTrue(ShockFront.arrivalTicks(1000, 8, 256) > ShockFront.soundArrivalTicks(1000),
                "a slow shock front arrives later than sound would have alone");
    }

    @Test
    void theGroundTremblesFirst() {
        for (double d : new double[]{50, 300, 2000}) {
            assertTrue(ShockFront.seismicArrivalTicks(d) < ShockFront.soundArrivalTicks(d));
            assertTrue(ShockFront.seismicArrivalTicks(d) < ShockFront.arrivalTicks(d, 8, 768));
        }
    }

    @Test
    void arrivalGrowsWithDistance() {
        double last = -1;
        for (int d = 0; d <= 4000; d += 50) {
            double t = ShockFront.arrivalTicks(d, 8, 600);
            assertTrue(t > last, "later the further off");
            last = t;
        }
    }
}
