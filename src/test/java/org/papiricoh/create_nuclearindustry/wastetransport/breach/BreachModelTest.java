package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A waste container's shielding, its spill when breached, its leak and its repair. */
class BreachModelTest {
    private static final double SHIELDED = 0.002;
    private static final double BREACHED = 0.35;
    private static final double MAX = 2000;

    @Test
    void sealedContainersLetAlmostNothingOut() {
        assertEquals(0.48, BreachModel.emission(240, false, SHIELDED, BREACHED, MAX), 1e-9);
        assertEquals(84, BreachModel.emission(240, true, SHIELDED, BREACHED, MAX), 1e-9);
        assertTrue(BreachModel.emission(240, true, SHIELDED, BREACHED, MAX) >= 50 * BreachModel.emission(240, false, SHIELDED, BREACHED, MAX),
                "a breach should let out far more than the shielding");
    }

    @Test
    void emissionIsCappedAndNeverNegative() {
        assertEquals(MAX, BreachModel.emission(1e9, true, SHIELDED, BREACHED, MAX), 1e-9);
        assertEquals(0, BreachModel.emission(0, true, SHIELDED, BREACHED, MAX));
        assertEquals(0, BreachModel.emission(-5, true, SHIELDED, BREACHED, MAX));
        assertEquals(0, BreachModel.emission(100, true, SHIELDED, -1, MAX));
    }

    @Test
    void spillsGrowWithActivity() {
        assertEquals(0, BreachModel.spillRadius(0, 16));
        assertEquals(BreachModel.MIN_SPILL_RADIUS, BreachModel.spillRadius(0.5, 16));
        assertEquals(12, BreachModel.spillRadius(100, 16));
        assertEquals(16, BreachModel.spillRadius(1_000_000, 16), "the radius is capped");
        assertTrue(BreachModel.spillRadius(400, 64) > BreachModel.spillRadius(100, 64));

        assertEquals(1.0, BreachModel.spillIntensity(100, 0.01), 1e-9);
        assertEquals(2 * BreachModel.spillIntensity(37, 0.01), BreachModel.spillIntensity(74, 0.01), 1e-9,
                "the spill's intensity is proportional to the activity");
        assertEquals(0, BreachModel.spillIntensity(-1, 0.01));
    }

    @Test
    void aBreachKeepsLeakingAtItsFactorOverAMinute() {
        // leakFactor 2: twice the spill intensity is added over each minute.
        assertEquals(2.0 / 60.0, BreachModel.leakPerSecond(100, 0.01, 2.0), 1e-9);
        assertEquals(0, BreachModel.leakPerSecond(100, 0.01, 0));
    }

    @Test
    void cisternsLoseFluid() {
        assertEquals(4000, BreachModel.fluidSpill(8000, 0.5));
        assertEquals(0, BreachModel.fluidSpill(8000, 0));
        assertEquals(8000, BreachModel.fluidSpill(8000, 3), "never more than there is");
        assertEquals(16, BreachModel.fluidLeakPerSecond(8000, 2.0));
        assertEquals(1, BreachModel.fluidLeakPerSecond(10, 2.0), "at least a millibucket while anything is left");
        assertEquals(0, BreachModel.fluidLeakPerSecond(0, 2.0));
    }

    @Test
    void twoHalfRepairsSealABreach() {
        float damage = 1;
        damage = BreachModel.repair(damage, 0.5);
        assertTrue(damage > 0, "half mended is still breached");
        damage = BreachModel.repair(damage, 0.5);
        assertEquals(0, damage, 1e-6);
        assertEquals(0, BreachModel.repair(0, 0.5));
    }
}
