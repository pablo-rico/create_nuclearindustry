package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The vial's battery, its fill stages and its transfers with a trap. */
class VialBatteryTest {
    private static final VialBattery.Params P = VialBattery.Params.DEFAULT;

    @Test
    void defaultsMatchTheContract() {
        assertEquals(4.0, P.capacity(), 1e-9);
        assertEquals(600, P.batteryTicks(), "30 s");
    }

    @Test
    void aLoadedVialOutsideATrapRunsDownInThirtySeconds() {
        int charge = P.batteryTicks();
        int ticks = 0;
        while (!VialBattery.annihilates(charge, 4) && ticks < 10000) {
            charge = VialBattery.tick(P, charge, 4, false);
            ticks++;
        }
        assertEquals(600, ticks);
        assertEquals(0, charge);
    }

    @Test
    void anEmptyVialKeepsItsChargeAndNeverAnnihilates() {
        assertEquals(300, VialBattery.tick(P, 300, 0, false));
        assertFalse(VialBattery.annihilates(0, 0));
        assertFalse(VialBattery.annihilates(0, 1e-12));
    }

    @Test
    void aPoweredTrapRechargesIt() {
        assertEquals(10, VialBattery.tick(P, 0, 4, true));
        assertEquals(600, VialBattery.tick(P, 595, 4, true));
        assertEquals(10, VialBattery.tick(P, 0, 0, true), "empty vials charge too");
        int charge = 0;
        int ticks = 0;
        while (charge < P.batteryTicks()) {
            charge = VialBattery.tick(P, charge, 4, true);
            ticks++;
        }
        assertEquals(60, ticks, "full in 3 s");
    }

    @Test
    void chargeIsClampedAndNeverNegative() {
        assertEquals(0, VialBattery.tick(P, -20, 4, false));
        assertEquals(599, VialBattery.tick(P, 5000, 4, false));
        assertTrue(VialBattery.annihilates(-1, 4));
    }

    @Test
    void theGlowFlickersOnALowBattery() {
        assertTrue(VialBattery.lowBattery(P, 100, 4));
        assertFalse(VialBattery.lowBattery(P, 100, 0), "empty vials do not warn");
        assertFalse(VialBattery.lowBattery(P, 150, 4), "exactly a quarter");
        assertEquals(0.5, VialBattery.chargeShare(P, 300), 1e-9);
        assertEquals(15.0, VialBattery.secondsLeft(300), 1e-9);
        assertEquals(0, VialBattery.secondsLeft(-3), 1e-9);
    }

    @Test
    void fillStagesForTheItemModel() {
        assertEquals(0, VialBattery.fillStage(P, 0));
        assertEquals(1, VialBattery.fillStage(P, 0.1));
        assertEquals(1, VialBattery.fillStage(P, 2));
        assertEquals(2, VialBattery.fillStage(P, 2.5));
        assertEquals(3, VialBattery.fillStage(P, 4));
        assertEquals(3, VialBattery.fillStage(P, 5));
        assertEquals(0f, VialBattery.fillProperty(P, 0), 1e-6);
        assertEquals(1f / 3, VialBattery.fillProperty(P, 1), 1e-6);
        assertEquals(2f / 3, VialBattery.fillProperty(P, 3), 1e-6);
        assertEquals(1f, VialBattery.fillProperty(P, 4), 1e-6);
    }

    @Test
    void fillingAndDrainingAtTheSlotRate() {
        assertEquals(0.1, VialBattery.fillStep(P, 0, 32), 1e-9);
        assertEquals(0.05, VialBattery.fillStep(P, 3.95, 32), 1e-9, "only the room left");
        assertEquals(0.02, VialBattery.fillStep(P, 0, 0.02), 1e-9, "only what the trap has");
        assertEquals(0, VialBattery.fillStep(P, 4, 32), 1e-9);
        assertEquals(0.1, VialBattery.drainStep(P, 4, 32), 1e-9);
        assertEquals(0, VialBattery.drainStep(P, 4, 0), 1e-9, "trap full");
        assertEquals(0, VialBattery.drainStep(P, 0, 32), 1e-9);
        double vial = 0;
        int ticks = 0;
        while (vial < P.capacity() - 1e-9) {
            vial += VialBattery.fillStep(P, vial, 32);
            ticks++;
        }
        assertEquals(40, ticks, "a vial fills in 2 s");
    }
}
