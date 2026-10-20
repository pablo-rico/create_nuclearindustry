package org.papiricoh.create_nuclearindustry.missile.holo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HoloProjectionTest {
    private static final HoloProjection P = new HoloProjection(4096, 100, -50, 0.001f);

    @Test
    void theGlassCentreIsTheViewCentreAndItsEdgesHalfAViewAway() {
        assertEquals(100, P.worldDx(HoloProjection.MID), 1e-6);
        assertEquals(-50, P.worldDz(HoloProjection.MID), 1e-6);
        assertEquals(100 - 2048, P.worldDx(HoloProjection.MIN), 1e-3);
        assertEquals(100 + 2048, P.worldDx(HoloProjection.MAX), 1e-3);
    }

    @Test
    void aPointOnTheGlassGoesToTheWorldAndBack() {
        for (double lx = HoloProjection.MIN; lx <= HoloProjection.MAX; lx += 0.17) {
            assertEquals(lx, P.x(P.worldDx(lx)), 1e-5);
            assertEquals(lx, P.z(P.worldDz(lx)), 1e-5);
        }
        assertEquals(HoloProjection.SIZE / 4096, P.scale(), 1e-9);
    }

    @Test
    void heightsStandOverTheGlassFromSeaLevel() {
        assertEquals(HoloProjection.Y + 0.01f, P.y(HoloProjection.SEA_LEVEL), 1e-6);
        assertEquals(HoloProjection.Y + 0.01f, P.y(0), 1e-6, "below the sea it lies on the glass");
        assertEquals(HoloProjection.Y + 0.01f + 1, P.y(HoloProjection.SEA_LEVEL + 1000), 1e-4);
    }

    @Test
    void clicksFallOnTheGlassTheFrameOrNothing() {
        assertEquals(HoloProjection.Region.GLASS, HoloProjection.region(0.5, HoloProjection.Y, 0.5));
        assertEquals(HoloProjection.Region.GLASS, HoloProjection.region(-0.8, HoloProjection.Y, 1.8));
        assertEquals(HoloProjection.Region.FRAME, HoloProjection.region(-0.95, 14.6 / 16, 0.5), "the rim");
        assertEquals(HoloProjection.Region.FRAME, HoloProjection.region(0.5, 0.4, -1), "a side");
        assertEquals(HoloProjection.Region.NONE, HoloProjection.region(3, HoloProjection.Y, 0.5));
    }

    @Test
    void theLookPicksTheMarkNearestItsLine() {
        double[] eye = {0.5, 2, 3};
        double[] marks = {0.5, 1, 0.5, 1.5, 1, 0.5, 0.5, 1, 6};
        double[] atFirst = {0, -1, -2.5};
        assertEquals(0, HoloProjection.pick(eye, atFirst, marks, 0.09));
        assertEquals(1, HoloProjection.pick(eye, new double[]{1, -1, -2.5}, marks, 0.09));
        assertEquals(-1, HoloProjection.pick(eye, new double[]{0, 1, 0}, marks, 0.09), "looking away picks nothing");
        assertEquals(-1, HoloProjection.pick(eye, new double[]{0, 1, 3}, marks, 0.09), "nor what is behind the eye");
        assertEquals(-1, HoloProjection.pick(eye, new double[]{0.3, -1, -2.5}, marks, 0.09), "nor a look that misses by more than the radius");
    }

    @Test
    void theLookMeetsTheGlassBelowTheEye() {
        double[] hit = HoloProjection.onGlassPlane(new double[]{0.5, 2, 0.5}, new double[]{0, -1, 0});
        assertArrayEquals(new double[]{0.5, HoloProjection.Y, 0.5}, hit, 1e-9);
        assertNull(HoloProjection.onGlassPlane(new double[]{0.5, 2, 0.5}, new double[]{0, 1, 0}));
    }

    @Test
    void aFlightFallsFreely() {
        double[] at = HoloProjection.fly(0, 100, 0, 1, 2, 0, 0.1, 10);
        assertArrayEquals(new double[]{10, 100 + 20 - 5, 0}, at, 1e-9);
    }
}
