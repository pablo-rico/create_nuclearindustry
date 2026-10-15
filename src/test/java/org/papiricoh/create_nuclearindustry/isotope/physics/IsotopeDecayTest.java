package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Decay of isotope items, their lazy clock and the default half-lives. */
class IsotopeDecayTest {
    private static final long DAY = IsotopeDecay.TICKS_PER_DAY;

    @Test
    void halvesEveryHalfLife() {
        assertEquals(1.0, IsotopeDecay.remaining(60, 0), 1e-12);
        assertEquals(0.5, IsotopeDecay.remaining(60, 60), 1e-12);
        assertEquals(0.25, IsotopeDecay.remaining(60, 120), 1e-12);
        assertEquals(0.8 * 0.125, IsotopeDecay.activity(0.8, 2.75, 3 * 2.75), 1e-12);
    }

    @Test
    void matchesTheRtgPelletLaw() {
        // RtgFuel.decay: charge * 2^(-ticks / halfLifeTicks).
        long halfLifeTicks = 240_000L;
        long ticks = 100_000L;
        double rtg = 0.9 * Math.pow(2.0, -ticks / (double) halfLifeTicks);
        double ours = IsotopeDecay.activityAt(0.9, halfLifeTicks / (double) DAY, 0, ticks, 1.0);
        assertEquals(rtg, ours, 1e-12);
    }

    @Test
    void stableAndNegativeTimesDoNotDecay() {
        assertEquals(1.0, IsotopeDecay.remaining(0, 1000), 1e-12, "a half-life of 0 means stable");
        assertEquals(1.0, IsotopeDecay.remaining(-5, 1000), 1e-12);
        assertEquals(1.0, IsotopeDecay.remaining(60, -3), 1e-12, "before it was made");
        assertEquals(0.0, IsotopeDecay.activity(-1, 60, 0), 1e-12, "no negative activity");
        assertEquals(0.0, IsotopeDecay.decayConstant(0), 0.0);
    }

    @Test
    void timeScaleSpeedsTheClock() {
        assertEquals(1.0, IsotopeDecay.scaledDays(0, DAY, 1.0), 1e-12);
        assertEquals(2.0, IsotopeDecay.scaledDays(0, DAY, 2.0), 1e-12);
        assertEquals(0.0, IsotopeDecay.scaledDays(DAY, 0, 1.0), 0.0, "no time runs backwards");
        assertEquals(0.0, IsotopeDecay.scaledDays(0, DAY, -1.0), 0.0);
        double slow = IsotopeDecay.activityAt(1, 0.25, 0, DAY / 4, 1.0);
        double fast = IsotopeDecay.activityAt(1, 0.25, 0, DAY / 8, 2.0);
        assertEquals(slow, fast, 1e-12);
    }

    @Test
    void daysUntilInvertsTheDecay() {
        assertEquals(120, IsotopeDecay.daysUntil(1, 60, 0.25), 1e-9);
        assertEquals(0, IsotopeDecay.daysUntil(0.2, 60, 0.25), 0.0, "already below");
        assertEquals(Double.POSITIVE_INFINITY, IsotopeDecay.daysUntil(1, 0, 0.25));
        assertEquals(Double.POSITIVE_INFINITY, IsotopeDecay.daysUntil(1, 60, 0));
        for (double t : new double[] {0.3, 2.0, 17.5}) {
            double a = IsotopeDecay.activity(1, 2.75, t);
            assertEquals(t, IsotopeDecay.daysUntil(1, 2.75, a), 1e-9);
        }
    }

    @Test
    void ticksUntilRoundsUpAndHonoursTheScale() {
        assertEquals(6000, IsotopeDecay.ticksUntil(1, 0.25, 0.5, 1.0));
        assertEquals(3000, IsotopeDecay.ticksUntil(1, 0.25, 0.5, 2.0));
        assertEquals(0, IsotopeDecay.ticksUntil(0.4, 0.25, 0.5, 1.0));
        assertEquals(Long.MAX_VALUE, IsotopeDecay.ticksUntil(1, 0, 0.5, 1.0));
        assertEquals(Long.MAX_VALUE, IsotopeDecay.ticksUntil(1, 0.25, 0.5, 0.0), "a stopped clock never gets there");
        long t = IsotopeDecay.ticksUntil(1, 0.25, 0.3, 1.0);
        assertTrue(IsotopeDecay.activityAt(1, 0.25, 0, t, 1.0) <= 0.3 + 1e-12);
        assertTrue(IsotopeDecay.activityAt(1, 0.25, 0, t - 1, 1.0) > 0.3);
    }

    @Test
    void halfLifeTicks() {
        assertEquals(6000, IsotopeDecay.halfLifeTicks(0.25, 1.0));
        assertEquals(60 * DAY / 4, IsotopeDecay.halfLifeTicks(60, 4.0));
        assertEquals(Long.MAX_VALUE, IsotopeDecay.halfLifeTicks(0, 1.0));
    }

    @Test
    void stampQuantumLetsCloseItemsStack() {
        long tc = IsotopeDecay.quantumTicks(0.25, 1.0);
        assertEquals(120, tc, "2 % of 6000 ticks");
        assertEquals(IsotopeDecay.MAX_QUANTUM_TICKS, IsotopeDecay.quantumTicks(60, 1.0), "a long half-life stamps by the day");
        assertEquals(IsotopeDecay.MAX_QUANTUM_TICKS, IsotopeDecay.quantumTicks(0, 1.0));
        assertEquals(IsotopeDecay.MIN_QUANTUM_TICKS, IsotopeDecay.quantumTicks(0.25, 1000.0));
        assertEquals(IsotopeDecay.stampTick(12_345, tc), IsotopeDecay.stampTick(12_300, tc));
        assertEquals(12_240, IsotopeDecay.stampTick(12_345, tc));
        assertEquals(-120, IsotopeDecay.stampTick(-1, tc), "rounds down, not towards zero");
        // Items in one quantum differ by under 1.4 % in activity.
        double a = IsotopeDecay.activityAt(1, 0.25, 0, tc, 1.0);
        assertTrue(1 - a < 0.014, "1 - " + a);
    }

    @Test
    void backDatingGivesTheWantedActivityNow() {
        long now = 1_000_000;
        long made = IsotopeDecay.tickForActivity(1, 0.5, 60, now, 1.0);
        assertEquals(now - 60 * DAY, made);
        assertEquals(0.5, IsotopeDecay.activityAt(1, 60, made, now, 1.0), 1e-6);
        assertEquals(now, IsotopeDecay.tickForActivity(1, 0.5, 0, now, 1.0), "a stable isotope needs no back-dating");
    }

    @Test
    void percentAndUsable() {
        assertEquals(100, IsotopeDecay.percent(1.0));
        assertEquals(50, IsotopeDecay.percent(0.4999));
        assertEquals(0, IsotopeDecay.percent(-1));
        assertEquals(0, IsotopeDecay.percent(Double.NaN));
        assertEquals(999, IsotopeDecay.percent(50));
        assertTrue(IsotopeDecay.usable(0.2, 0.1));
        assertFalse(IsotopeDecay.usable(0.1, 0.1));
    }

    @Test
    void defaultHalfLivesKeepTheRealOrderAndRatios() {
        assertEquals(0.25, Isotope.TECHNETIUM_99M.halfLifeDays(), 1e-12, "6 h, in real time");
        assertEquals(2.75, Isotope.MOLYBDENUM_99.halfLifeDays(), 1e-12, "66 h, in real time");
        double[] order = {Isotope.TECHNETIUM_99M.halfLifeDays(), Isotope.MOLYBDENUM_99.halfLifeDays(),
                Isotope.COBALT_60.halfLifeDays(), Isotope.STRONTIUM_90.halfLifeDays(), Isotope.AMERICIUM_241.halfLifeDays()};
        for (int i = 1; i < order.length; i++) {
            assertTrue(order[i] > order[i - 1], "index " + i);
        }
        // Real Sr-90 / Co-60 = 28.8 y / 5.27 y = 5.46; the compressed clock keeps it within 2 %.
        double ratio = Isotope.STRONTIUM_90.halfLifeDays() / Isotope.COBALT_60.halfLifeDays();
        assertEquals(28.8 / 5.27, ratio, 0.02 * ratio);
        // Americium barely moves in a long game: over 98 % after 100 game days.
        assertTrue(IsotopeDecay.remaining(Isotope.AMERICIUM_241.halfLifeDays(), 100) > 0.98);
        // A Tc-99m kit is past use within twenty minutes of play (under a game day).
        double tcDays = IsotopeDecay.daysUntil(1, Isotope.TECHNETIUM_99M.halfLifeDays(), Isotope.TECHNETIUM_99M.usableAbove());
        assertTrue(tcDays < 1, "" + tcDays);
    }

    @Test
    void idsRoundTrip() {
        for (Isotope isotope : Isotope.values()) {
            assertSame(isotope, Isotope.byId(isotope.id()));
            assertNotNull(isotope.id());
        }
        assertEquals("cobalt_60", Isotope.COBALT_60.id());
        assertNull(Isotope.byId("unobtainium"));
    }
}
