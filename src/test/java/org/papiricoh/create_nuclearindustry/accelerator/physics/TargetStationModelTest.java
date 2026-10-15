package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Antiproton output of the target station from the beam's energy and power. */
class TargetStationModelTest {
    private static final TargetStationModel.Params P = TargetStationModel.Params.DEFAULT;
    private static final AntimatterModel.Params AM = AntimatterModel.Params.DEFAULT;

    @Test
    void nothingBelowTheThreshold() {
        assertFalse(TargetStationModel.produces(AM, 20));
        assertFalse(TargetStationModel.produces(AM, 25));
        assertTrue(TargetStationModel.produces(AM, 26));
        assertEquals(0, TargetStationModel.perTick(P, AM, 20, 10000), 1e-12);
    }

    @Test
    void aBigRingMakesAboutElevenNanogramsAMinute() {
        // Matches AntimatterModelTest: 6.8 MW at 100 GeV.
        assertEquals(10.9, TargetStationModel.perMinute(P, AM, 100, 6800), 0.1);
        assertEquals(10.9 / 1200, TargetStationModel.perTick(P, AM, 100, 6800), 1e-4);
    }

    @Test
    void outputScalesWithPowerAndRisesWithEnergy() {
        double base = TargetStationModel.perTick(P, AM, 50, 1000);
        assertEquals(2 * base, TargetStationModel.perTick(P, AM, 50, 2000), 1e-12);
        assertTrue(TargetStationModel.perTick(P, AM, 40, 1000) < base);
        assertEquals(0, TargetStationModel.perTick(P, AM, 100, -5), 1e-12);
        assertEquals(0, TargetStationModel.perTick(new TargetStationModel.Params(0), AM, 100, 1000), 1e-12);
    }

    @Test
    void depositAndLoss() {
        assertEquals(50, TargetStationModel.depositedKj(1000), 1e-9);
        assertEquals(1000, TargetStationModel.lossKw(1000), 1e-9);
        assertEquals(0, TargetStationModel.lossKw(-1), 1e-9);
    }

    @Test
    void theYieldSaturatesAtThreeTimesTheThreshold() {
        assertEquals(75, TargetStationModel.saturationGev(AM), 1e-9);
        assertEquals(TargetStationModel.perMinutePerMw(P, AM, 75), TargetStationModel.perMinutePerMw(P, AM, 200), 1e-12);
        assertTrue(TargetStationModel.perMinutePerMw(P, AM, 50) < TargetStationModel.perMinutePerMw(P, AM, 75));
        assertEquals(TargetStationModel.perMinute(P, AM, 100, 1000), TargetStationModel.perMinutePerMw(P, AM, 100), 1e-12);
    }

    @Test
    void theJeiCurveRunsFromThresholdPastSaturation() {
        double[][] curve = TargetStationModel.yieldCurve(P, AM, 6);
        assertEquals(6, curve.length);
        assertEquals(25, curve[0][0], 1e-9);
        assertEquals(0, curve[0][1], 1e-12, "nothing at the threshold itself");
        assertEquals(112.5, curve[5][0], 1e-9);
        for (int i = 1; i < curve.length; i++) {
            assertTrue(curve[i][1] >= curve[i - 1][1], "never falls with energy");
        }
        assertEquals(2, TargetStationModel.yieldCurve(P, AM, 0).length);
    }
}
