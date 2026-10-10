package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressurizerModelTest {
    private static final PressurizerModel.Params PARAMS = new PressurizerModel.Params(155, 2, 5, 0.1, 0.05, 0.002,
            20, 60, 170, 160, 0.3, 0.1);

    private static PressurizerModel.Inputs steady(double level) {
        return new PressurizerModel.Inputs(level, 0, 0, true, 325, 290, false, 0.5, 0.0);
    }

    private static PressurizerModel.Inputs with(double level, int auto, int manual, boolean spray, boolean isolated,
                                                double roll, double stick) {
        return new PressurizerModel.Inputs(level, auto, manual, spray, 325, 290, isolated, roll, stick);
    }

    @Test
    void startsAtTheSetpoint() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.5);
        assertEquals(155.0, model.pressure(), 0.01);
        assertEquals(0.5, model.level(), 0.0);
    }

    @Test
    void withoutHeatersThePressureSags() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.5);
        for (int t = 0; t < 400; t++) {
            model.tick(steady(0.5), PARAMS);
        }
        assertTrue(model.pressure() < 140, "heat loss to the hot leg lowers it, got " + model.pressure());
    }

    @Test
    void automaticHeatersHoldTheSetpoint() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.5);
        for (int t = 0; t < 2_000; t++) {
            model.tick(with(0.5, 2, 0, true, false, 0.5, 0), PARAMS);
        }
        assertEquals(155, model.pressure(), 2.5);
    }

    @Test
    void manualHeatersRaiseThePressure() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.5);
        for (int t = 0; t < 100; t++) {
            model.tick(with(0.5, 0, 2, false, false, 0.5, 0), PARAMS);
        }
        assertTrue(model.pressure() > 160, "got " + model.pressure());
        assertEquals(2, model.heatersRunning());
    }

    @Test
    void heatersTripWhenTheLevelIsTooLow() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.05);
        model.tick(with(0.05, 0, 2, false, false, 0.5, 0), PARAMS);
        assertEquals(0, model.heatersRunning());
    }

    @Test
    void sprayNeedsColdLegFlow() {
        PressurizerModel sprayed = PressurizerModel.atSetpoint(165, 0.5);
        PressurizerModel dry = PressurizerModel.atSetpoint(165, 0.5);
        for (int t = 0; t < 20; t++) {
            sprayed.tick(with(0.5, 0, 0, true, false, 0.5, 0), PARAMS);
            dry.tick(with(0.5, 0, 0, false, false, 0.5, 0), PARAMS);
        }
        // Spray brings it back down to the top of its band and stops there.
        assertTrue(sprayed.pressure() < 161, "sprayed " + sprayed.pressure());
        assertTrue(dry.pressure() > sprayed.pressure() + 3, sprayed.pressure() + " vs " + dry.pressure());
    }

    @Test
    void insurgeRaisesAndOutsurgeLowersThePressure() {
        PressurizerModel in = PressurizerModel.atSetpoint(155, 0.5);
        in.tick(with(0.7, 0, 0, false, false, 0.5, 0), PARAMS);
        PressurizerModel out = PressurizerModel.atSetpoint(155, 0.5);
        out.tick(with(0.3, 0, 0, false, false, 0.5, 0), PARAMS);
        assertTrue(in.pressure() > 160, "insurge " + in.pressure());
        assertTrue(out.pressure() < 150, "outsurge " + out.pressure());
    }

    @Test
    void aFullPressurizerHasNoCushion() {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 1.0);
        double before = model.pressure();
        model.tick(new PressurizerModel.Inputs(1.2, 0, 0, false, 345, 290, true, 0.5, 0), PARAMS);
        assertTrue(model.solid());
        assertEquals(before + 0.2 * 60, model.pressure(), 1.0);
    }

    @Test
    void reliefValveOpensAtItsPressureAndClosesLower() {
        PressurizerModel model = PressurizerModel.atSetpoint(165, 0.5);
        boolean opened = false;
        for (int t = 0; t < 200 && !opened; t++) {
            opened = model.tick(with(0.5, 0, 4, false, false, 0.5, 0), PARAMS);
        }
        assertTrue(opened, "manual heaters drive it to the relief pressure");
        assertTrue(model.pressure() >= 165);
        boolean closed = false;
        for (int t = 0; t < 400 && !closed; t++) {
            model.tick(with(0.5, 0, 0, false, false, 0.5, 0), PARAMS);
            closed = !model.reliefOpen();
        }
        assertTrue(closed);
        assertTrue(model.pressure() <= 160.5, "closes at 160 bar, got " + model.pressure());
    }

    @Test
    void aStuckValveStaysOpenUntilIsolated() {
        PressurizerModel model = PressurizerModel.atSetpoint(171, 0.5);
        model.tick(with(0.5, 0, 0, false, false, 0.5, 1.0), PARAMS);
        assertTrue(model.reliefOpen());
        for (int t = 0; t < 600; t++) {
            model.tick(with(0.5, 0, 0, false, false, 0.5, 1.0), PARAMS);
        }
        assertTrue(model.reliefOpen() && model.reliefStuck(), "stuck open far below the closing pressure");
        assertTrue(model.pressure() < 120);
        boolean venting = model.tick(with(0.5, 0, 0, false, true, 0.5, 1.0), PARAMS);
        assertFalse(venting);
        assertFalse(model.reliefOpen());
        assertFalse(model.reliefStuck());
    }

    @Test
    void levelDemandAddsInventoryExpansionAndSteam() {
        assertEquals(0.5, PressurizerModel.levelDemand(16_000, 12_000, 8_000, 307.5, 307.5, 0.005, 0), 1.0e-9);
        assertEquals(0.55, PressurizerModel.levelDemand(16_000, 12_000, 8_000, 317.5, 307.5, 0.005, 0), 1.0e-9);
        assertEquals(1.0, PressurizerModel.levelDemand(16_000, 12_000, 8_000, 307.5, 307.5, 0.005, 4_000), 1.0e-9);
        assertTrue(PressurizerModel.levelDemand(8_000, 12_000, 8_000, 307.5, 307.5, 0.005, 0) < 0, "a leak empties it");
    }
}
