package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Energy gain, magnetic rigidity, injection, focusing, vacuum and beam power. */
class BeamModelTest {
    private static final BeamModel.Params P = BeamModel.Params.DEFAULT;

    @Test
    void ringEnergyFollowsTheMagneticRigidity() {
        // E = 0.3 B R f
        assertEquals(2.16, BeamModel.ringMaxEnergy(P, 1.8, 8, 0.5), 1e-9, "small normal-conducting ring");
        assertEquals(9.6, BeamModel.ringMaxEnergy(P, 8.0, 8, 0.5), 1e-9, "superconducting dipoles in the same ring");
        assertEquals(46.08, BeamModel.ringMaxEnergy(P, 8.0, 32, 0.6), 1e-9);
        assertEquals(99.84, BeamModel.ringMaxEnergy(P, 8.0, 64, 0.65), 1e-9, "a big ring reaches about 100 GeV");
        assertTrue(BeamModel.ringMaxEnergy(P, 8.0, 64, 0.65) > BeamModel.ringMaxEnergy(P, 8.0, 32, 0.65), "bigger rings go higher");
        assertEquals(0, BeamModel.ringMaxEnergy(P, 8.0, 64, 0), 1e-9, "no dipoles, no bending");
    }

    @Test
    void mixedDipolesAverageTheirFields() {
        assertEquals(1.8, BeamModel.dipoleField(P, 10, 0), 1e-9);
        assertEquals(8.0, BeamModel.dipoleField(P, 0, 10), 1e-9);
        assertEquals(4.9, BeamModel.dipoleField(P, 5, 5), 1e-9);
        assertEquals(0, BeamModel.dipoleField(P, 0, 0), 1e-9);
    }

    @Test
    void theLinacAddsEachCavitysGain() {
        assertEquals(0.05, BeamModel.linacEnergy(P, 0, 0, 1), 1e-9, "the source's own energy");
        assertEquals(20.05, BeamModel.linacEnergy(P, 8, 0, 1), 1e-9);
        assertEquals(10.05, BeamModel.linacEnergy(P, 8, 0, 0.5), 1e-9, "half the radio-frequency power, half the gain");
        assertEquals(0.05 + 2.5 * 4 + 12 * 80, BeamModel.linacEnergy(P, 4, 80, 1), 1e-9);
    }

    @Test
    void aRingNeedsALinacOfAHundredthOfItsEnergy() {
        assertEquals(21.6, BeamModel.injectionEnergy(P, 2.16), 1e-9);
        assertFalse(BeamModel.injectionOk(P, BeamModel.linacEnergy(P, 8, 0, 1), 2.16), "8 copper cavities are not enough");
        assertTrue(BeamModel.injectionOk(P, BeamModel.linacEnergy(P, 9, 0, 1), 2.16), "9 are");
        assertTrue(BeamModel.injectionOk(P, BeamModel.linacEnergy(P, 0, 84, 1), 99.84), "a ~100-block superconducting linac fills a 100 GeV ring");
        assertFalse(BeamModel.injectionOk(P, 1000, 0), "no ring, nothing to inject into");
    }

    @Test
    void missingQuadrupolesCostTheSquareOfTheirCoverage() {
        assertEquals(1, BeamModel.focus(12, 3, 4), 1e-9, "one quadrupole per four elements");
        assertEquals(1, BeamModel.focus(12, 5, 4), 1e-9, "more never helps beyond full coverage");
        assertEquals(0.25, BeamModel.focus(16, 2, 4), 1e-9, "half the quadrupoles, a quarter of the focus");
        assertEquals(0, BeamModel.focus(16, 0, 4), 1e-9);
        assertEquals(1, BeamModel.focus(0, 0, 4), 1e-9, "nothing to focus");
    }

    @Test
    void theVacuumLetsTheBeamThroughBetweenItsLimits() {
        assertEquals(1, BeamModel.vacuumFactor(P, -7), 1e-9);
        assertEquals(1, BeamModel.vacuumFactor(P, -6), 1e-9);
        assertEquals(0.5, BeamModel.vacuumFactor(P, -4.5), 1e-9);
        assertEquals(0, BeamModel.vacuumFactor(P, -3), 1e-9);
        assertEquals(0, BeamModel.vacuumFactor(P, 5), 1e-9, "atmosphere stops everything");
    }

    @Test
    void stagesPassTheirTransmissionAndLoseTheRest() {
        double t = BeamModel.transmission(P, 1, 1);
        assertEquals(0.95, t, 1e-9);
        BeamModel.Stage stage = BeamModel.stage(200, t);
        assertEquals(190, stage.current(), 1e-9);
        assertEquals(10, stage.lost(), 1e-9);
        assertEquals(0, BeamModel.stage(200, BeamModel.transmission(P, 1, 0)).current(), 1e-9);
    }

    @Test
    void beamPowerIsEnergyTimesCurrent() {
        assertEquals(4.5, BeamModel.beamPower(25, 180), 1e-9, "a medical cyclotron's few kW");
        assertEquals(6800, BeamModel.beamPower(100_000, 68), 1e-9, "6.8 MW from a big ring");
        assertEquals(0, BeamModel.beamPower(-1, 10), 1e-9);
    }

    @Test
    void radioFrequencyDriveIsTheShareOfThePowerNeeded() {
        assertEquals(2.0, BeamModel.rfDemand(P, 8, 0), 1e-9);
        assertEquals(1, BeamModel.rfDrive(4, 2), 1e-9);
        assertEquals(0.5, BeamModel.rfDrive(1, 2), 1e-9);
        assertEquals(1, BeamModel.rfDrive(0, 0), 1e-9, "no cavities need nothing");
    }

    @Test
    void theRingRampsTowardsItsSetpoint() {
        assertEquals(125, BeamModel.ramp(P, 100, 1000, 1, 1), 1e-9);
        assertEquals(1000, BeamModel.ramp(P, 990, 1000, 4, 1), 1e-9, "never overshoots");
        assertEquals(500, BeamModel.ramp(P, 600, 500, 10, 1), 1e-9, "ramps down too");
        assertEquals(100, BeamModel.ramp(P, 100, 1000, 4, 0), 1e-9, "no drive, no ramp");
    }

    @Test
    void theSourceNeedsDriveAndGas() {
        assertEquals(200, BeamModel.sourceCurrent(P, 1, 1), 1e-9);
        assertEquals(100, BeamModel.sourceCurrent(P, 0.5, 1), 1e-9);
        assertEquals(0, BeamModel.sourceCurrent(P, 1, 0), 1e-9);
    }
}
