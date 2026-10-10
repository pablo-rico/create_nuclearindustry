package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pump-down, pumping requirements and outgassing of the vacuum chamber. */
class VacuumModelTest {
    private static final VacuumModel.Params P = VacuumModel.Params.DEFAULT;

    @Test
    void enoughPumpsReachPlasmaVacuumInAboutFifteenSeconds() {
        VacuumModel vacuum = new VacuumModel();
        assertEquals(VacuumModel.ATMOSPHERE, vacuum.getPressure());
        int ticks = 0;
        while (!vacuum.atPlasmaVacuum(P) && ticks < 2000) {
            vacuum.tick(P, 1.0, false);
            ticks++;
        }
        assertEquals(300, ticks, 5, "pump-down took " + ticks);
        for (int i = 0; i < 200; i++) {
            vacuum.tick(P, 1.0, true);
        }
        assertTrue(vacuum.atPlasmaVacuum(P), "a lit plasma stays within the limit with full pumping: " + vacuum.getPressure());
    }

    @Test
    void tooFewPumpsNeverReachPlasmaVacuum() {
        assertTrue(VacuumModel.canReachPlasmaVacuum(P, 1.0));
        assertFalse(VacuumModel.canReachPlasmaVacuum(P, 0.5), "half the pumping is not enough");
        assertFalse(VacuumModel.canReachPlasmaVacuum(P, 0));
        VacuumModel vacuum = new VacuumModel();
        for (int i = 0; i < 2000; i++) {
            vacuum.tick(P, 0.5, false);
        }
        assertFalse(vacuum.atPlasmaVacuum(P), "settled at " + vacuum.getPressure());
        assertEquals(VacuumModel.basePressure(P, 0.5), vacuum.getPressure(), 1e-9);
    }

    @Test
    void pressureCreepsBackWhenThePumpsStop() {
        VacuumModel vacuum = new VacuumModel();
        vacuum.restore(-4.0);
        int ticks = 0;
        while (vacuum.atPlasmaVacuum(P) && ticks < 1000) {
            vacuum.tick(P, 0, true);
            ticks++;
        }
        assertTrue(ticks > 20 && ticks < 100, "lost the plasma vacuum after " + ticks + " ticks");
        vacuum.vent();
        assertEquals(VacuumModel.ATMOSPHERE, vacuum.getPressure());
    }

    @Test
    void biggerTokamaksNeedMorePumps() {
        assertEquals(1, VacuumModel.pumpsNeeded(4));
        assertEquals(2, VacuumModel.pumpsNeeded(8));
        assertEquals(3, VacuumModel.pumpsNeeded(9));
        assertEquals(4, VacuumModel.pumpsNeeded(16));
    }
}
