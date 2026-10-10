package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Coil cool-down, helium use, field ramp and quench of the superconducting coils. */
class MagnetModelTest {
    private static final MagnetModel.Params P = MagnetModel.Params.DEFAULT;
    /** An R=4 tokamak has 16 toroidal sectors. */
    private static final int SECTORS = 16;

    private static MagnetModel.Inputs cooling(double target, boolean energize, double helium) {
        return new MagnetModel.Inputs(SECTORS, target, true, energize, false, helium);
    }

    @Test
    void coolDownTakesAboutTwentySecondsWithEnoughHelium() {
        MagnetModel magnets = new MagnetModel();
        double helium = 0;
        int ticks = 0;
        while (!magnets.ready() && ticks < 2000) {
            helium += magnets.tick(P, cooling(0, false, 1e9));
            ticks++;
        }
        assertTrue(ticks >= 380 && ticks <= 420, "cool-down took " + ticks + " ticks");
        assertTrue(magnets.superconducting(P));
        assertTrue(helium > 2000, "cooling boils off extra helium: " + helium + " mB");
    }

    @Test
    void coldCoilsUseFourMillibucketsPerTickAtFullField() {
        assertEquals(4.0, MagnetModel.heliumUse(P, SECTORS, 1.0), 1e-9);
        assertEquals(1.2, MagnetModel.heliumUse(P, SECTORS, 0.0), 1e-9, "30% standby with no field");
        assertEquals(1.2 + 2.8 * 0.25, MagnetModel.heliumUse(P, SECTORS, 0.5), 1e-9, "the load grows with the square of the field");

        MagnetModel magnets = new MagnetModel();
        magnets.restore(1.0, MagnetModel.OPERATING_TEMPERATURE);
        assertEquals(4.0, magnets.tick(P, cooling(1.0, true, 1e9)), 1e-9);
    }

    @Test
    void fieldRampsAtTenPercentPerSecond() {
        MagnetModel magnets = new MagnetModel();
        magnets.restore(0, MagnetModel.OPERATING_TEMPERATURE);
        int ticks = 0;
        while (magnets.getField() < 1.0 && ticks < 1000) {
            magnets.tick(P, cooling(1.0, true, 1e9));
            ticks++;
        }
        assertEquals(200, ticks, 2, "0 to full field");
        magnets.tick(P, cooling(0.6, true, 1e9));
        assertTrue(magnets.getField() < 1.0, "the field follows a lower target");
        for (int i = 0; i < 300; i++) {
            magnets.tick(P, cooling(0.6, false, 1e9));
        }
        assertEquals(0, magnets.getField(), 1e-9, "de-energised coils ramp to zero");
    }

    @Test
    void aPlasmaHoldsTheFieldWhenTheDrivesSlow() {
        MagnetModel magnets = new MagnetModel();
        magnets.restore(1.0, MagnetModel.OPERATING_TEMPERATURE);
        for (int i = 0; i < 100; i++) {
            magnets.tick(P, new MagnetModel.Inputs(SECTORS, 0.2, true, true, true, 1e9));
        }
        assertEquals(1.0, magnets.getField(), 1e-9, "superconducting coils keep their current under a plasma");
    }

    @Test
    void coilsWithoutHeliumWarmAndQuenchAndTheFieldIsDumped() {
        MagnetModel magnets = new MagnetModel();
        magnets.restore(1.0, MagnetModel.OPERATING_TEMPERATURE);
        int ticks = 0;
        while (!magnets.quenching(P) && ticks < 1000) {
            magnets.tick(P, cooling(1.0, true, 0));
            ticks++;
        }
        assertTrue(magnets.quenching(P), "coils with no helium must quench");
        assertTrue(ticks < 60, "within a few seconds, took " + ticks);
        for (int i = 0; i < 60; i++) {
            magnets.tick(P, cooling(1.0, true, 0));
        }
        assertEquals(0, magnets.getField(), 1e-9, "a warm coil cannot hold field");
    }

    @Test
    void warmCoilsCannotBeEnergised() {
        MagnetModel magnets = new MagnetModel();
        for (int i = 0; i < 100; i++) {
            magnets.tick(P, new MagnetModel.Inputs(SECTORS, 1.0, false, true, false, 0));
        }
        assertEquals(0, magnets.getField(), 1e-9);
        assertFalse(magnets.superconducting(P));
        assertEquals(MagnetModel.AMBIENT_TEMPERATURE, magnets.getTemperature(), 1e-9);
    }

    @Test
    void uncooledCoilsWarmSlowly() {
        MagnetModel magnets = new MagnetModel();
        magnets.restore(0, MagnetModel.OPERATING_TEMPERATURE);
        for (int i = 0; i < 200; i++) {
            magnets.tick(P, new MagnetModel.Inputs(SECTORS, 0, false, false, false, 0));
        }
        assertTrue(magnets.superconducting(P), "a well insulated cryostat stays cold for a while: " + magnets.getTemperature());
    }
}
