package org.papiricoh.create_nuclearindustry.propulsion.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.propulsion.physics.ThermalRocketModel.Params;
import org.papiricoh.create_nuclearindustry.propulsion.physics.ThermalRocketModel.Propellant;
import org.papiricoh.create_nuclearindustry.propulsion.physics.ThermalRocketModel.Step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The nuclear thermal rocket: flow, coolant, propellant and the nozzle's heat. */
class ThermalRocketModelTest {
    private static final Params P = Params.DEFAULT;

    private static Step full(Propellant propellant, int segments) {
        return ThermalRocketModel.tick(1, segments, propellant, 100_000, 100_000, 100_000, ThermalRocketModel.AMBIENT, 0, P);
    }

    @Test
    void eachSegmentPassesItsShareOfTheFlow() {
        assertEquals(4 * P.flowPerSegment(), full(Propellant.HYDROGEN, 4).propellantUsed());
        assertEquals(2 * P.flowPerSegment(), ThermalRocketModel.tick(0.5, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0, P).propellantUsed());
        assertEquals(0, ThermalRocketModel.tick(0, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0, P).propellantUsed(), "idle at zero throttle");
    }

    @Test
    void lighterPropellantThrustsHarder() {
        double hydrogen = full(Propellant.HYDROGEN, 4).thrust();
        double deuterium = full(Propellant.DEUTERIUM, 4).thrust();
        double water = full(Propellant.WATER, 4).thrust();
        assertTrue(hydrogen > deuterium && deuterium > water);
        assertEquals(3.0, hydrogen / water, 0.01, "√(18/2): hydrogen gives three times water's thrust");
    }

    @Test
    void itHandsTheCoolantBackColdAndNeedsIt() {
        Step step = full(Propellant.HYDROGEN, 4);
        assertEquals(step.propellantUsed() * P.primaryPerMb(), step.primaryCooled(), 1.0);
        Step noCoolant = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 0, 1000, 20, 0, P);
        assertEquals(0, noCoolant.thrust(), "no heat, no thrust");
        Step noRoom = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 3, 20, 0, P);
        assertEquals(3, noRoom.propellantUsed(), "nowhere to put the cooled coolant limits the flow");
    }

    @Test
    void waterAtFullThrottleOverheatsTheNozzleHydrogenDoesNot() {
        double nozzle = ThermalRocketModel.AMBIENT;
        double wear = 0;
        for (int tick = 0; tick < 2000; tick++) {
            Step step = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 1000, nozzle, wear, P);
            nozzle = step.nozzle();
            wear = step.wear();
        }
        assertTrue(nozzle < P.nozzleLimit() && wear == 0, "hydrogen keeps the nozzle below its limit: " + nozzle);
        nozzle = ThermalRocketModel.AMBIENT;
        for (int tick = 0; tick < 2000; tick++) {
            Step step = ThermalRocketModel.tick(1, 4, Propellant.WATER, 1000, 1000, 1000, nozzle, wear, P);
            nozzle = step.nozzle();
            wear = step.wear();
        }
        assertTrue(nozzle > P.nozzleLimit() && wear > 0, "water does not: " + nozzle);
    }

    @Test
    void aWornOutNozzleFailsAndStopsTheEngine() {
        Step step = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 2000, 0.9999, P);
        assertTrue(step.failed());
        Step after = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 2000, 1.0, P);
        assertEquals(0, after.thrust(), "a failed nozzle gives nothing");
    }

    @Test
    void wearLetsMoreActivityOut() {
        double fresh = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0, P).contamination();
        double worn = ThermalRocketModel.tick(1, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0.5, P).contamination();
        assertTrue(fresh > 0 && worn > fresh);
        assertFalse(ThermalRocketModel.tick(0, 4, Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0.5, P).contamination() > 0,
                "nothing leaks while idle");
    }
}
