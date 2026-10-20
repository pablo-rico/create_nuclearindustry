package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The satellites' moments over time: the fairing, the satellite let go, a kill's fragments and a re-entry. */
class SatelliteFxCurvesTest {

    @Test
    void fairingStaysShutThenPeelsAndTumblesWithoutAJump() {
        assertEquals(0, SatelliteFxCurves.peelDegrees(0));
        assertEquals(0, SatelliteFxCurves.peelDegrees(SatelliteFxCurves.FAIRING_DELAY));
        double end = SatelliteFxCurves.FAIRING_DELAY + SatelliteFxCurves.PEEL_TICKS;
        assertEquals(SatelliteFxCurves.peelDegrees(end - 1.0e-6), SatelliteFxCurves.peelDegrees(end + 1.0e-6), 1.0e-3);
        // Turning as fast at the end of the peel as when tumbling free.
        double before = SatelliteFxCurves.peelDegrees(end) - SatelliteFxCurves.peelDegrees(end - 0.01);
        double after = SatelliteFxCurves.peelDegrees(end + 0.01) - SatelliteFxCurves.peelDegrees(end);
        assertEquals(before, after, 1.0e-3);
        double previous = -1;
        for (int t = 0; t < 200; t++) {
            double a = SatelliteFxCurves.peelDegrees(t);
            assertTrue(a >= previous, "only ever opens further");
            previous = a;
        }
        assertTrue(SatelliteFxCurves.peelDegrees(end + 10) > 90, "well open soon after");
    }

    @Test
    void halvesPartAndFallOnlyOnceOffTheirHinges() {
        assertEquals(0, SatelliteFxCurves.fairingDrift(SatelliteFxCurves.FAIRING_DELAY));
        assertEquals(0, SatelliteFxCurves.fairingFall(SatelliteFxCurves.FAIRING_DELAY + SatelliteFxCurves.PEEL_TICKS));
        double end = SatelliteFxCurves.FAIRING_DELAY + SatelliteFxCurves.PEEL_TICKS;
        assertEquals(SatelliteFxCurves.fairingDrift(end - 1.0e-6), SatelliteFxCurves.fairingDrift(end + 1.0e-6), 1.0e-6);
        assertTrue(SatelliteFxCurves.fairingDrift(100) > SatelliteFxCurves.fairingDrift(50));
        assertTrue(SatelliteFxCurves.fairingFall(SatelliteFxCurves.FAIRING_LIFE) > 50, "falling well away by the time it is let go of");
    }

    @Test
    void satelliteLeavesAfterTheFairingOpensAndFadesAtTheEnd() {
        assertEquals(0, SatelliteFxCurves.releaseDrift(SatelliteFxCurves.RELEASE_DELAY));
        assertTrue(SatelliteFxCurves.RELEASE_DELAY > SatelliteFxCurves.FAIRING_DELAY, "not before the bolts fire");
        assertTrue(SatelliteFxCurves.releaseDrift(SatelliteFxCurves.RELEASE_DELAY + 20) > 0);
        assertEquals(1, SatelliteFxCurves.fade(10, SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_FADE));
        assertEquals(0.5, SatelliteFxCurves.fade(SatelliteFxCurves.SATELLITE_LIFE - SatelliteFxCurves.SATELLITE_FADE / 2.0,
                SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_FADE), 1.0e-9);
        assertEquals(0, SatelliteFxCurves.fade(SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_FADE));
        assertEquals(0, SatelliteFxCurves.fade(-1, 10, 5));
    }

    @Test
    void fragmentsSlowInTheThinAirAndSink() {
        V3 from = new V3(0, 1663, 0), v = new V3(1, 0, 0);
        V3 early = SatelliteFxCurves.shard(from, v, 10), late = SatelliteFxCurves.shard(from, v, 300);
        assertEquals(from, SatelliteFxCurves.shard(from, v, 0));
        assertTrue(early.x() > 9 && early.x() < 10, "nearly its own speed at first: " + early.x());
        assertTrue(late.x() < 1 / SatelliteFxCurves.SHARD_DRAG, "never further than the drag lets it: " + late.x());
        assertTrue(late.y() < early.y() && late.y() > 1000, "sinks slowly: " + late.y());
    }

    @Test
    void glitterStaysDimMostlyWithShortGlints() {
        int bright = 0;
        for (int t = 0; t < 2000; t++) {
            double g = SatelliteFxCurves.glitter(t, 12345);
            assertTrue(g >= 0.25 && g <= 1.0, "in range: " + g);
            if (g > 0.8) bright++;
        }
        assertTrue(bright > 0 && bright < 400, "glints now and then: " + bright);
    }

    @Test
    void reentryPlungesSteeplyAndBurnsOutBeforeTheGround() {
        V3 v = SatelliteFxCurves.reentryStart(new V3(12, 0, 0));
        assertEquals(SatelliteFxCurves.REENTRY_SPEED, v.x(), 1.0e-9);
        assertTrue(v.y() < 0, "heading down");
        assertEquals(V3.ZERO.x(), SatelliteFxCurves.reentryStart(V3.ZERO).x());
        for (double startY : new double[]{863, 400, 163}) {
            double ground = 64;
            V3 pos = new V3(0, startY, 0), vel = SatelliteFxCurves.reentryStart(new V3(0, 0, 12));
            double lowestLit = Double.MAX_VALUE;
            for (int t = 0; t < SatelliteFxCurves.REENTRY_LIFE; t++) {
                vel = SatelliteFxCurves.reentryStep(vel);
                pos = pos.add(vel);
                if (SatelliteFxCurves.reentryGlow(t, pos.y(), ground) > 0) lowestLit = Math.min(lowestLit, pos.y());
            }
            assertTrue(lowestLit >= ground + SatelliteFxCurves.REENTRY_FLOOR, "out before the ground from " + startY + ": " + lowestLit);
        }
        assertEquals(0, SatelliteFxCurves.reentryGlow(SatelliteFxCurves.REENTRY_LIFE, 800, 64));
        assertTrue(SatelliteFxCurves.reentryGlow(SatelliteFxCurves.REENTRY_BREAKUP, 800, 64)
                > SatelliteFxCurves.reentryGlow(SatelliteFxCurves.REENTRY_BREAKUP - 30, 800, 64), "flares as it breaks up");
    }
}
