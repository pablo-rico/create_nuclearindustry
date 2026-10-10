package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimaryCoolantModelTest {
    private static final PrimaryCoolantModel.Params PARAMS = new PrimaryCoolantModel.Params(290, 0.12, 35, 0.6, 7, 8, 0);
    private static final double SATURATION = 344.8;
    private static final int CORE = 12_000;

    private static PrimaryCoolantModel.Conditions flowing(double heat, double pumps) {
        return new PrimaryCoolantModel.Conditions(heat, 10_000, 10_000, pumps, 16_000, CORE, SATURATION);
    }

    @Test
    void enoughFlowCarriesAllTheHeat() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        int moved = 0;
        for (int t = 0; t < 200; t++) {
            moved += model.step(flowing(9.0, 128), PARAMS);
        }
        // 9 heat units a tick at 0.12 per mB is 75 mB/t of cold coolant made hot.
        assertEquals(75 * 200, moved, 2);
        assertTrue(model.storedHeat() < 0.12, "nothing builds up in the vessel");
        assertEquals(290.0, model.coldLeg(), 0.01);
        // 75 of 128 mB/t carry the heat: the core outlet runs 35 * 75/128 above the cold leg.
        assertEquals(290 + 35 * 75.0 / 128, model.hotLeg(), 0.5);
        assertEquals(0.0, model.voidFraction(), 0.0);
    }

    @Test
    void lessFlowMeansAHotterHotLeg() {
        PrimaryCoolantModel weak = new PrimaryCoolantModel();
        PrimaryCoolantModel strong = new PrimaryCoolantModel();
        for (int t = 0; t < 100; t++) {
            weak.step(flowing(9.0, 80), PARAMS);
            strong.step(flowing(9.0, 256), PARAMS);
        }
        assertTrue(weak.hotLeg() > strong.hotLeg() + 10, "weak " + weak.hotLeg() + " vs strong " + strong.hotLeg());
    }

    @Test
    void lossOfFlowHeatsTheCoreToBoiling() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        PrimaryCoolantModel.Params noCirculation = new PrimaryCoolantModel.Params(290, 0.12, 35, 0.6, 7, 0, 0);
        double previous = 0;
        boolean rose = false;
        for (int t = 0; t < 400; t++) {
            model.step(new PrimaryCoolantModel.Conditions(9.0, 10_000, 10_000, 0, 16_000, CORE, SATURATION), noCirculation);
            if (t == 20) {
                previous = model.coldLeg();
            }
            if (t == 60) {
                rose = model.coldLeg() > previous + 5;
            }
        }
        assertTrue(rose, "stagnant water heats up");
        assertEquals(SATURATION, model.hotLeg(), 1.0e-6);
        assertTrue(model.voidFraction() > 0.2, "the core boils, void " + model.voidFraction());
    }

    @Test
    void energyIsConserved() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        double heat = 0;
        int moved = 0;
        for (int t = 0; t < 300; t++) {
            double q = 3 + (t % 7);
            heat += q;
            // the hot pool fills up after a while, so some heat stays behind
            int space = Math.max(0, 8_000 - moved);
            moved += model.step(new PrimaryCoolantModel.Conditions(q, 10_000, space, 40, 16_000, CORE, SATURATION), PARAMS);
        }
        assertEquals(heat, moved * 0.12 + model.storedHeat(), 1.0e-6);
    }

    @Test
    void stoppedCirculationWithoutRoomStopsConversion() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        int moved = model.step(new PrimaryCoolantModel.Conditions(9.0, 10_000, 0, 128, 16_000, CORE, SATURATION), PARAMS);
        assertEquals(0, moved, "no room for hot coolant");
        assertEquals(9.0, model.storedHeat(), 1.0e-9);
    }

    @Test
    void coverageFollowsTheInventory() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        model.update(6_000, CORE, SATURATION, PARAMS);
        assertEquals(0.5, model.coverage(), 1.0e-9);
        model.update(20_000, CORE, SATURATION, PARAMS);
        assertEquals(1.0, model.coverage(), 1.0e-9);
    }

    @Test
    void depressurizingFlashesHotWaterIntoSteam() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        model.addHeat(400);
        model.update(16_000, CORE, SATURATION, PARAMS);
        assertEquals(0.0, model.voidFraction(), 0.0);
        // Lower pressure, lower boiling point: the same heat is now partly steam.
        model.update(16_000, CORE, 300, PARAMS);
        assertTrue(model.voidFraction() > 0.0);
        assertEquals(300.0, model.coldLeg(), 1.0e-9);
    }

    @Test
    void ventingTakesOnlyTheSteam() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        model.addHeat(5_000);
        model.update(16_000, CORE, SATURATION, PARAMS);
        double sensible = 3_000 * (0.12 / 35) * (SATURATION - 290);
        double vented = model.ventSteam(1.0e9, PARAMS);
        assertEquals((5_000 - sensible) / 0.6, vented, 1.0e-6);
        assertEquals(sensible, model.storedHeat(), 1.0e-6);
        assertEquals(0.0, model.ventSteam(1.0e9, PARAMS), 1.0e-9);
    }

    @Test
    void naturalCirculationStartsALoopWithNoFlowYet() {
        PrimaryCoolantModel model = new PrimaryCoolantModel();
        int moved = 0;
        for (int t = 0; t < 10; t++) {
            moved += model.step(flowing(9.0, 0), PARAMS);
        }
        assertEquals(80, moved, "8 mB/t of natural circulation before the pumps see any coolant");
    }
}
