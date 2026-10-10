package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeismicModelTest {

    @Test
    void groundMotionFallsWithDistance() {
        assertEquals(100.0, SeismicModel.groundMotion(100, 0, 256), 1e-9);
        assertEquals(50.0, SeismicModel.groundMotion(100, 256, 256), 1e-9, "half at the falloff distance");
        assertEquals(20.0, SeismicModel.groundMotion(100, 512, 256), 1e-9, "a fifth at twice the falloff");
        assertTrue(SeismicModel.groundMotion(100, 4000, 256) > 0, "felt anywhere in the dimension");
        assertEquals(0.0, SeismicModel.groundMotion(0, 10, 256), "no blast, no motion");
    }

    @Test
    void comparatorScalesUpToFullScale() {
        assertEquals(0, SeismicModel.comparatorSignal(0, 100));
        assertEquals(1, SeismicModel.comparatorSignal(0.01, 100), "anything felt gives a signal");
        assertEquals(8, SeismicModel.comparatorSignal(50, 100));
        assertEquals(15, SeismicModel.comparatorSignal(100, 100));
        assertEquals(15, SeismicModel.comparatorSignal(1000, 100), "capped");
    }

    @Test
    void bearingIsClockwiseFromNorth() {
        assertEquals(0.0, SeismicModel.bearing(0, -10), 1e-9);
        assertEquals(90.0, SeismicModel.bearing(10, 0), 1e-9);
        assertEquals(180.0, SeismicModel.bearing(0, 10), 1e-9);
        assertEquals(270.0, SeismicModel.bearing(-10, 0), 1e-9);
        assertEquals(45.0, SeismicModel.bearing(10, -10), 1e-9);
    }

    @Test
    void compassPointRoundsToTheNearestOfEight() {
        assertEquals("n", SeismicModel.COMPASS[SeismicModel.compassPoint(0)]);
        assertEquals("n", SeismicModel.COMPASS[SeismicModel.compassPoint(350)]);
        assertEquals("ne", SeismicModel.COMPASS[SeismicModel.compassPoint(40)]);
        assertEquals("w", SeismicModel.COMPASS[SeismicModel.compassPoint(271)]);
        assertEquals("nw", SeismicModel.COMPASS[SeismicModel.compassPoint(-45)]);
    }
}
