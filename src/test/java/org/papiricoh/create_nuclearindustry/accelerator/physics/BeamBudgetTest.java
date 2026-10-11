package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The beam through a linac, a ring and its extraction line. */
class BeamBudgetTest {
    private static final BeamModel.Params P = BeamModel.Params.DEFAULT;

    /** Ten copper cavities, fully focused, good vacuum, everything powered. */
    private static BeamBudget.Inputs linac() {
        return BeamBudget.Inputs.linac(10, 0, 14, 4, 1, 1, 1, 1, -7, 1);
    }

    @Test
    void aGoodLinacDeliversItsFullEnergyAndMostOfItsCurrent() {
        BeamBudget.Result r = BeamBudget.compute(P, linac(), true);
        assertEquals(25.05, r.energy(), 1e-9);
        assertEquals(200, r.sourceCurrent(), 1e-9);
        assertEquals(190, r.current(), 1e-9);
        assertEquals(BeamModel.beamPower(25.05, 190), r.power(), 1e-9);
        assertEquals(0.05, r.lossFraction(), 1e-9);
        assertEquals(BeamModel.beamPower(25.05 / 2, 10), r.linacLoss(), 1e-9, "lost on average at half energy");
    }

    @Test
    void theEnergySetpointHoldsTheLinacBack() {
        BeamBudget.Inputs half = BeamBudget.Inputs.linac(10, 0, 14, 4, 1, 0.5, 1, 1, -7, 1);
        assertEquals(12.55, BeamBudget.compute(P, half, true).energy(), 1e-9);
    }

    @Test
    void missingQuadrupolesLoseMostOfTheBeam() {
        BeamBudget.Inputs poor = BeamBudget.Inputs.linac(10, 0, 16, 2, 1, 1, 1, 1, -7, 1);
        BeamBudget.Result r = BeamBudget.compute(P, poor, true);
        assertTrue(r.lossFraction() > 0.75, "half the quadrupoles keep a quarter of the focus: " + r.lossFraction());
    }

    @Test
    void noGasNoVacuumNoBeam() {
        assertEquals(0, BeamBudget.compute(P, BeamBudget.Inputs.linac(10, 0, 14, 4, 1, 1, 1, 0, -7, 1), true).current(), 1e-9);
        assertEquals(0, BeamBudget.compute(P, BeamBudget.Inputs.linac(10, 0, 14, 4, 1, 1, 1, 1, 5, 1), true).current(), 1e-9);
    }

    @Test
    void withTheSourceOffOnlyTheEnergyIsShown() {
        BeamBudget.Result r = BeamBudget.compute(P, linac(), false);
        assertEquals(0, r.current(), 1e-9);
        assertEquals(25.05, r.energy(), 1e-9);
    }

    @Test
    void aRingDeliversItsRampedEnergyForItsDutyShare() {
        BeamBudget.Inputs ring = new BeamBudget.Inputs(10, 0, 14, 4, 1, 1, 1, 1, -7, 1, 8, 50, 7, 2000, 8, 2);
        BeamBudget.Result r = BeamBudget.compute(P, ring, true);
        assertEquals(2000, r.energy(), 1e-9);
        assertEquals(200 * 0.95 * 0.95 * 0.95 * P.ringDuty(), r.current(), 1e-9);
        assertTrue(r.ringLoss() > 0 && r.extractionLoss() > 0);
        assertEquals(1 - 0.95 * 0.95 * 0.95, r.lossFraction(), 1e-9, "the duty is not counted as a loss");
    }
}
