package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the effects may spend, the detail by distance and the far curves. */
class FxBudgetTest {

    @Test
    void detailFallsAwayWithDistance() {
        assertEquals(FxBudget.Lod.NEAR, FxBudget.lod(10));
        assertEquals(FxBudget.Lod.NEAR, FxBudget.lod(FxBudget.NEAR));
        assertEquals(FxBudget.Lod.MID, FxBudget.lod(500));
        assertEquals(FxBudget.Lod.FAR, FxBudget.lod(1500));
        assertFalse(FxBudget.Lod.FAR.particles());
    }

    @Test
    void particlesNeverPassTheCapAndAreShared() {
        assertEquals(10, FxBudget.quota(10, 0, 1));
        assertEquals(0, FxBudget.quota(10, FxBudget.MAX_PARTICLES, 1));
        assertEquals(0, FxBudget.quota(10, FxBudget.MAX_PARTICLES + 5, 1));
        assertEquals(10, FxBudget.quota(50, FxBudget.MAX_PARTICLES - 10, 1));
        // Sixteen rockets asking for a lot share what is left.
        int alive = 0;
        for (int i = 0; i < 16; i++) alive += FxBudget.quota(500, alive, 16);
        assertTrue(alive <= FxBudget.MAX_PARTICLES, "alive " + alive);
    }

    @Test
    void onlyTheNearestSixteenTrail() {
        double[] d = new double[40];
        for (int i = 0; i < d.length; i++) d[i] = 1000 - i * 10;
        boolean[] keep = FxBudget.trailed(d);
        int kept = 0;
        for (int i = 0; i < d.length; i++) {
            if (keep[i]) {
                kept++;
                assertTrue(i >= d.length - FxBudget.MAX_TRAILS, "kept a far one: " + i);
            }
        }
        assertEquals(FxBudget.MAX_TRAILS, kept);
        boolean[] few = FxBudget.trailed(new double[]{5, 3000});
        assertTrue(few[0] && few[1]);
    }

    @Test
    void farThingsArePulledInsideTheFarPlane() {
        assertEquals(1, FxBudget.pullIn(100, 700), 1.0e-12);
        assertEquals(0.35, FxBudget.pullIn(2000, 700), 1.0e-12);
        assertEquals(700, 2000 * FxBudget.pullIn(2000, 700), 1.0e-9);
    }

    @Test
    void theDustRingRunsFromSixToTwentyFourBlocks() {
        assertEquals(FxCurves.DUST_START, FxCurves.dustRadius(0), 1.0e-12);
        assertEquals(FxCurves.DUST_END, FxCurves.dustRadius(FxCurves.DUST_SECONDS), 1.0e-12);
        assertEquals(FxCurves.DUST_END, FxCurves.dustRadius(100), 1.0e-12);
        assertTrue(FxCurves.dustRadius(0.5) - FxCurves.DUST_START > FxCurves.DUST_END - FxCurves.dustRadius(2.5), "fast, then easing out");
        assertEquals(0, FxCurves.dustAlpha(FxCurves.DUST_SECONDS * 2), 1.0e-12);
    }

    @Test
    void plasmaNeedsSpeedAndAir() {
        assertEquals(0, FxCurves.airDepth(500, 400), 1.0e-12);
        assertEquals(1, FxCurves.airDepth(63, 400), 1.0e-12);
        assertEquals(0, FxCurves.reentryHeat(8, 0), 1.0e-12);
        assertEquals(0, FxCurves.reentryHeat(1, 1), 1.0e-12);
        assertTrue(FxCurves.reentryHeat(8, FxCurves.airDepth(300, 400)) > 0.9, "full a quarter of the way into the air");
        assertTrue(FxCurves.reentryHeat(4, 0.5) < FxCurves.reentryHeat(7, 0.5));
    }

    @Test
    void theGroundShakesNearAndLow() {
        assertTrue(FxCurves.shake(10, 5) > 0.7);
        assertEquals(0, FxCurves.shake(FxCurves.SHAKE_REACH, 5), 1.0e-12);
        assertEquals(0, FxCurves.shake(10, FxCurves.SHAKE_HEIGHT), 1.0e-12);
    }

    @Test
    void aFlashDiesAway() {
        assertEquals(1, FxCurves.flash(0, 1), 1.0e-12);
        assertTrue(FxCurves.flash(0.2, 1) > FxCurves.flash(0.6, 1));
        assertEquals(0, FxCurves.flash(1.1, 1), 1.0e-12);
    }
}
