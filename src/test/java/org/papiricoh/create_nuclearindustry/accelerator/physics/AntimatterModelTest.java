package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Antiproton yield above the threshold and the collector's share. */
class AntimatterModelTest {
    private static final AntimatterModel.Params P = AntimatterModel.Params.DEFAULT;

    @Test
    void nothingBelowTheThreshold() {
        assertEquals(0, AntimatterModel.yield(P, 10), 1e-9);
        assertEquals(0, AntimatterModel.yield(P, 25), 1e-9);
    }

    @Test
    void theYieldRisesWithEnergyUpToItsCap() {
        assertEquals(1.0, AntimatterModel.yield(P, 50), 1e-9);
        assertEquals(0.2, AntimatterModel.yield(P, 30), 1e-9);
        assertEquals(2.0, AntimatterModel.yield(P, 100), 1e-9, "capped");
    }

    @Test
    void theCollectorKeepsItsShare() {
        assertEquals(1.6, AntimatterModel.collected(P, 1.0, 100), 1e-9);
        assertEquals(0, AntimatterModel.collected(P, 1.0, 20), 1e-9);
    }

    @Test
    void aBigRingMakesAboutElevenNanogramsAMinute() {
        // A 6.8 MW, 100 GeV beam finishes a 60000 kJ target recipe every 60000 / 6800 s.
        double cycleSeconds = 60000.0 / 6800.0;
        double perMinute = AntimatterModel.collected(P, 1.0, 100) * 60 / cycleSeconds;
        assertEquals(10.9, perMinute, 0.1);
    }
}
