package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where satellites show in the sky and how brightly. */
class SkyViewTest {
    private static final double R = 16000;
    private static final V3 EYE = new V3(0, 70, 0);

    @Test
    void overheadIsStraightUp() {
        V3 d = SkyView.direction(R, EYE, new V3(0, 1663, 0));
        assertNotNull(d);
        assertEquals(1, d.y(), 1.0e-9);
        assertEquals(1, d.length(), 1.0e-9);
    }

    @Test
    void pastTheRadarsHorizonItIsHidden() {
        double horizon = RadarModel.horizon(R, EYE.y(), 1663);
        assertNotNull(SkyView.direction(R, EYE, new V3(horizon - 10, 1663, 0)));
        assertNull(SkyView.direction(R, EYE, new V3(horizon + 10, 1663, 0)));
        assertTrue(SkyView.aboveHorizon(R, EYE, new V3(0, 1663, horizon - 10)));
        assertTrue(!SkyView.aboveHorizon(R, EYE, new V3(0, 1663, horizon + 10)));
        // A geostationary one is seen much further off; a flat world (radius 0) has no horizon.
        assertNotNull(SkyView.direction(R, EYE, new V3(horizon + 10, 12063, 0)));
        assertNotNull(SkyView.direction(0, EYE, new V3(1.0e7, 1663, 0)));
    }

    @Test
    void curvatureLowersItTowardsTheHorizon() {
        V3 sat = new V3(4000, 1663, 0);
        V3 curved = SkyView.direction(R, EYE, sat), flat = SkyView.direction(0, EYE, sat);
        assertNotNull(curved);
        assertNotNull(flat);
        assertTrue(curved.y() < flat.y(), "lower on a round world");
        assertEquals(1, curved.length(), 1.0e-9);
    }

    @Test
    void onlyInADarkClearSky() {
        assertEquals(0, SkyView.skyDarkness(0, 0));
        assertEquals(1, SkyView.skyDarkness(0.5, 0));
        assertEquals(0, SkyView.skyDarkness(0.5, 1));
        assertEquals(0.5, SkyView.skyDarkness(0.25, 0), 1.0e-9);
    }

    @Test
    void fadesIntoTheHazeAtTheHorizon() {
        assertEquals(0, SkyView.haze(new V3(1, 0, 0)));
        assertEquals(0, SkyView.haze(new V3(1, -0.1, 0).normalize()));
        assertEquals(1, SkyView.haze(V3.UP));
        assertTrue(SkyView.haze(new V3(1, 0.05, 0).normalize()) < 1);
    }

    @Test
    void lowOnesFlareNowAndThenParkedOnesNever() {
        int flaring = 0;
        double brightest = 1;
        for (int t = 0; t < (int) SkyView.FLARE_PERIOD; t++) {
            assertEquals(1, SkyView.flare(t, 99, false, 2.5));
            double f = SkyView.flare(t, 99, true, 2.5);
            assertTrue(f >= 1 && f <= 3.5);
            brightest = Math.max(brightest, f);
            if (f > 1.5) flaring++;
        }
        assertTrue(brightest > 3.3, "flares nearly to full once a period: " + brightest);
        assertTrue(flaring > 10 && flaring < 120, "for a few seconds: " + flaring);
    }
}
