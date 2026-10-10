package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.PressurizerModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The automatic pressure controller: how its output splits, and the loop closed on a pressurizer model. */
class PressureControlTest {
    /** The default pressure tuning of {@code controlRoom.pid.pressure.*}. */
    private static final PidController.Tuning TUNING = new PidController.Tuning(3.0, 0.3, 0.0, 0.004, 0.0);
    /** The game's pressurizer at its default config (see PrimaryLoop.pressurizerParams). */
    private static final PressurizerModel.Params PARAMS = new PressurizerModel.Params(155, 2, 5, 0.25, 0.05, 0.002,
            20, 60, 170, 160, 0.3, 0.1);
    private static final double SCALE = 250.0;
    private static final int INTERVAL = 20;

    @Test
    void theOutputHeatsBelowTheMiddleAndSpraysAboveIt() {
        assertEquals(new PressureDemand(1.0, 0.0), PressureDemand.of(0.0));
        assertEquals(new PressureDemand(0.0, 0.0), PressureDemand.of(PressureDemand.NEUTRAL));
        assertEquals(new PressureDemand(0.0, 1.0), PressureDemand.of(1.0));
        PressureDemand quarter = PressureDemand.of(0.25);
        assertEquals(0.5, quarter.heaters(), 1e-9);
        assertEquals(0.0, quarter.spray(), 1e-9);
        assertEquals(0.5, PressureDemand.of(0.75).spray(), 1e-9);
        assertEquals(-0.5, PressureDemand.of(0.75).signed(), 1e-9);
        assertEquals(new PressureDemand(1.0, 0.0), PressureDemand.of(-3.0), "out of range clamps");
    }

    @Test
    void aControllerReplacesTheAutomaticHeatersAndSpray() {
        PressurizerModel model = PressurizerModel.atSetpoint(140, 0.3);
        // Far below the setpoint the automatic heaters would come on; the controller asks for nothing.
        model.tick(new PressurizerModel.Inputs(0.3, 2, 0, true, 290, 290, false, 0.5, 0.0,
                new PressurizerModel.Demand(true, 0.0, 0.0)), PARAMS);
        assertEquals(0, model.heatersRunning());
        model.tick(new PressurizerModel.Inputs(0.3, 2, 0, true, 290, 290, false, 0.5, 0.0,
                new PressurizerModel.Demand(true, 0.25, 0.0)), PARAMS);
        assertEquals(0.5, model.heaterPower(), 1e-9, "a quarter of two banks' power");
        assertEquals(1, model.heatersRunning(), "shows as a bank running");
    }

    @Test
    void aForcedSprayNeedsTheColdLegToFlow() {
        PressurizerModel dry = PressurizerModel.atSetpoint(155, 0.3);
        PressurizerModel flowing = PressurizerModel.atSetpoint(155, 0.3);
        PressurizerModel.Demand spray = new PressurizerModel.Demand(false, 0.0, 1.0);
        for (int t = 0; t < 40; t++) {
            dry.tick(new PressurizerModel.Inputs(0.3, 2, 0, false, 325, 290, false, 0.5, 0.0, spray), PARAMS);
            flowing.tick(new PressurizerModel.Inputs(0.3, 2, 0, true, 325, 290, false, 0.5, 0.0, spray), PARAMS);
        }
        assertTrue(dry.pressure() > 150, "no spray without cold leg flow, got " + dry.pressure());
        assertTrue(flowing.spraying() && flowing.pressure() < 120, "the open spray beats the heaters, got " + flowing.pressure());
    }

    @Test
    void theLoopSettlesBelowTheSetpointTheHeatersHold() {
        assertEquals(140.0, settle(140.0, 290.0), 2.0);
    }

    @Test
    void theLoopSettlesAboveIt() {
        assertEquals(165.0, settle(165.0, 325.0), 2.0);
    }

    /** Runs the controller on a pressurizer starting at 155 bar for a minute and returns the pressure. */
    private static double settle(double setpoint, double hotLeg) {
        PressurizerModel model = PressurizerModel.atSetpoint(155, 0.3);
        PidController pid = new PidController();
        double output = PressureDemand.NEUTRAL;
        for (int tick = 0; tick < 1_200; tick++) {
            if (tick % INTERVAL == 0) {
                output = pid.step(setpoint / SCALE, model.pressure() / SCALE, INTERVAL / 20.0, PressureDemand.NEUTRAL, 0, TUNING);
            }
            PressureDemand demand = PressureDemand.of(output);
            model.tick(new PressurizerModel.Inputs(0.3, 2, 0, true, hotLeg, 290, false, 0.5, 0.0,
                    new PressurizerModel.Demand(true, demand.heaters(), demand.spray())), PARAMS);
        }
        return model.pressure();
    }
}
