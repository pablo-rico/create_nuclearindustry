package org.papiricoh.create_nuclearindustry.reactor.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoriumBreedingTest {

    @Test
    void freshThoriumIsWeakerThanUranium() {
        double fresh = ThoriumBreeding.reactivityMultiplier(0.0);
        assertEquals(ThoriumBreeding.SEED_WORTH, fresh, 1e-12, "a fresh assembly only has its seed");
        assertTrue(fresh < 1.0, "fresh thorium is less reactive than low-enriched uranium, got " + fresh);
    }

    @Test
    void reactivityRisesAsUranium233Breeds() {
        double previous = ThoriumBreeding.reactivityMultiplier(0.0);
        double peak = ThoriumBreeding.peakBurnFraction();
        for (double burn = 0.05; burn <= peak; burn += 0.05) {
            double now = ThoriumBreeding.reactivityMultiplier(burn);
            assertTrue(now > previous, "reactivity should keep rising until the peak, fell at " + burn);
            previous = now;
        }
        assertTrue(ThoriumBreeding.reactivityMultiplier(peak) > 1.0, "a bred assembly beats low-enriched uranium");
    }

    @Test
    void thePeakIsWhereTheSeedRunsOutOfWorth() {
        double peak = ThoriumBreeding.peakBurnFraction();
        assertTrue(peak > 0.3 && peak < 0.8, "peak should come midway through the burn, got " + peak);
        double at = ThoriumBreeding.reactivityMultiplier(peak);
        assertTrue(at >= ThoriumBreeding.reactivityMultiplier(peak - 0.02), "nothing just before the peak is higher");
        assertTrue(at >= ThoriumBreeding.reactivityMultiplier(peak + 0.02), "nothing just after the peak is higher");
    }

    @Test
    void spentThoriumStillHoldsItsBredUranium() {
        double spent = ThoriumBreeding.reactivityMultiplier(1.0);
        assertEquals(0.0, ThoriumBreeding.seedFraction(1.0), 1e-12, "the seed is gone at the end");
        assertTrue(ThoriumBreeding.bredFraction(1.0) > 0.95, "uranium-233 is near equilibrium at the end");
        assertTrue(spent > ThoriumBreeding.reactivityMultiplier(0.0), "spent thorium is more reactive than fresh");
        assertTrue(spent < ThoriumBreeding.reactivityMultiplier(ThoriumBreeding.peakBurnFraction()), "but past its peak");
    }

    @Test
    void outOfRangeBurnIsClamped() {
        assertEquals(ThoriumBreeding.reactivityMultiplier(0.0), ThoriumBreeding.reactivityMultiplier(-1.0), 1e-12);
        assertEquals(ThoriumBreeding.reactivityMultiplier(1.0), ThoriumBreeding.reactivityMultiplier(3.0), 1e-12);
        assertEquals(ThoriumBreeding.reactivityMultiplier(0.0), ThoriumBreeding.reactivityMultiplier(Double.NaN), 1e-12);
    }

    @Test
    void thoriumLastsLongerThanItsRating() {
        assertTrue(ThoriumBreeding.LIFE_FACTOR > 1.0, "breeding stretches the assembly's life");
    }
}
