package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Mo-99 / Tc-99m generator: Bateman build-up, transient equilibrium and elution. */
class TechnetiumCowTest {
    private static final TechnetiumCow.Params P = TechnetiumCow.Params.DEFAULT;
    private static final TechnetiumCow.State FRESH = TechnetiumCow.State.FRESH;

    @Test
    void aFreshColumnHasNoTechnetium() {
        TechnetiumCow.State now = TechnetiumCow.advance(P, FRESH, 0);
        assertEquals(1.0, now.parent(), 1e-12);
        assertEquals(0.0, now.daughter(), 1e-12);
    }

    @Test
    void theParentDecaysWithItsOwnHalfLife() {
        TechnetiumCow.State later = TechnetiumCow.advance(P, FRESH, P.parentHalfLifeDays());
        assertEquals(0.5, later.parent(), 1e-12);
    }

    @Test
    void matchesAStepByStepIntegration() {
        // dNp/dt = -lp Np, dNd/dt = b lp Np - ld Nd, with activities A = lambda N.
        double lp = Math.log(2) / P.parentHalfLifeDays();
        double ld = Math.log(2) / P.daughterHalfLifeDays();
        double np = 1.0 / lp;
        double nd = 0.0;
        double dt = 1.0e-4;
        for (int i = 0; i < 20_000; i++) {
            double dnp = -lp * np;
            double dnd = P.branching() * lp * np - ld * nd;
            np += dnp * dt;
            nd += dnd * dt;
        }
        TechnetiumCow.State exact = TechnetiumCow.advance(P, FRESH, 2.0);
        assertEquals(lp * np, exact.parent(), 1e-3);
        assertEquals(ld * nd, exact.daughter(), 1e-3);
    }

    @Test
    void peaksAboutADayAfterAnElution() {
        double peak = TechnetiumCow.peakDays(P);
        assertEquals(0.95, peak, 0.02, "real cows peak about 23 h after milking");
        double atPeak = TechnetiumCow.advance(P, FRESH, peak).daughter();
        assertTrue(atPeak > TechnetiumCow.advance(P, FRESH, peak - 0.05).daughter());
        assertTrue(atPeak > TechnetiumCow.advance(P, FRESH, peak + 0.05).daughter());
        assertEquals(0.69, atPeak, 0.01);
    }

    @Test
    void reachesTransientEquilibrium() {
        double ratio = TechnetiumCow.equilibriumRatio(P);
        assertEquals(0.875 * 1.1, ratio, 1e-3);
        TechnetiumCow.State late = TechnetiumCow.advance(P, FRESH, 5.0);
        assertEquals(ratio, late.daughter() / late.parent(), 1e-3, "after many daughter half-lives");
    }

    @Test
    void advancingInStepsEqualsOneStep() {
        TechnetiumCow.State a = TechnetiumCow.advance(P, FRESH, 1.7);
        TechnetiumCow.State b = TechnetiumCow.advance(P, TechnetiumCow.advance(P, FRESH, 0.6), 1.1);
        assertEquals(a.parent(), b.parent(), 1e-12);
        assertEquals(a.daughter(), b.daughter(), 1e-12);
    }

    @Test
    void elutionTakesItsShareAndLeavesTheParent() {
        TechnetiumCow.State ready = TechnetiumCow.advance(P, FRESH, 1.0);
        TechnetiumCow.Elution milk = TechnetiumCow.elute(P, ready);
        assertEquals(ready.daughter() * 0.9, milk.eluted(), 1e-12);
        assertEquals(ready.daughter() * 0.1, milk.after().daughter(), 1e-12);
        assertEquals(ready.parent(), milk.after().parent(), 0.0);
        TechnetiumCow.Elution again = TechnetiumCow.elute(P, milk.after());
        assertEquals(0.1 * milk.eluted(), again.eluted(), 1e-12, "milking twice at once gives only what the first left");
    }

    @Test
    void dailyMilkingWearsTheCowOutInUnderTwoWeeks() {
        TechnetiumCow.State cow = FRESH;
        double previous = Double.MAX_VALUE;
        int days = 0;
        while (!TechnetiumCow.exhausted(P, cow)) {
            cow = TechnetiumCow.advance(P, cow, 1.0);
            TechnetiumCow.Elution milk = TechnetiumCow.elute(P, cow);
            assertTrue(milk.eluted() < previous || days == 0, "each day gives less, day " + days);
            previous = milk.eluted();
            cow = milk.after();
            days++;
        }
        assertTrue(days >= 10 && days <= 13, "days " + days);
    }

    @Test
    void edgeCases() {
        TechnetiumCow.Params same = new TechnetiumCow.Params(1, 1, 1, 1, 0.05);
        TechnetiumCow.State s = TechnetiumCow.advance(same, FRESH, 1.0);
        assertEquals(Math.log(2) * Math.exp(-Math.log(2)), s.daughter(), 1e-12, "equal half-lives: b A0 λ t e^(-λt)");
        assertEquals(1.0 / Math.log(2), TechnetiumCow.peakDays(same), 1e-12);
        TechnetiumCow.Params stableDaughter = new TechnetiumCow.Params(1, 0, 1, 1, 0.05);
        assertEquals(0.0, TechnetiumCow.advance(stableDaughter, FRESH, 1).daughter(), 0.0);
        TechnetiumCow.Params longDaughter = new TechnetiumCow.Params(1, 5, 1, 1, 0.05);
        assertEquals(Double.POSITIVE_INFINITY, TechnetiumCow.equilibriumRatio(longDaughter), "no transient equilibrium");
        TechnetiumCow.State back = TechnetiumCow.advance(P, FRESH, -2);
        assertEquals(1.0, back.parent(), 0.0, "no time runs backwards");
        TechnetiumCow.State neg = TechnetiumCow.advance(P, new TechnetiumCow.State(-1, -1), 1);
        assertEquals(0.0, neg.parent(), 0.0);
        assertEquals(0.0, neg.daughter(), 0.0);
    }

    @Test
    void kitsPerElution() {
        assertEquals(0, TechnetiumCow.kits(0.1, 0.15, 4));
        assertEquals(1, TechnetiumCow.kits(0.15, 0.15, 4));
        assertEquals(4, TechnetiumCow.kits(0.62, 0.15, 4), "capped");
        assertEquals(0, TechnetiumCow.kits(1, 0, 4));
        assertEquals(0, TechnetiumCow.kits(1, 0.15, 0));
        // A peak elution of a fresh cow fills four kits; a quarter day after milking, two.
        double peak = TechnetiumCow.elute(P, TechnetiumCow.advance(P, FRESH, TechnetiumCow.peakDays(P))).eluted();
        assertEquals(4, TechnetiumCow.kits(peak, 0.15, 4));
        double quick = TechnetiumCow.elute(P, TechnetiumCow.advance(P, FRESH, 0.25)).eluted();
        assertEquals(2, TechnetiumCow.kits(quick, 0.15, 4));
        assertFalse(TechnetiumCow.exhausted(P, FRESH));
    }
}
