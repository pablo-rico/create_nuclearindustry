package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The automatic fusion power controller: how its output splits, and the loop closed on a simple plant. */
class FusionPowerControlTest {
    /** The default tuning of {@code controlRoom.pid.fusion.*}. */
    private static final PidController.Tuning TUNING = new PidController.Tuning(2.0, 0.3, 0.0, 0.005, 0.1);
    private static final double SCALE = 1000.0;

    @Test
    void theOutputFuelsFirstAndHeatsOnlyOnceTheFuelIsSaturated() {
        assertEquals(new FusionPowerDemand(0.0, 0.4), FusionPowerDemand.of(0.0, 0.4), "no output: no fuel, the operator's heating");
        assertEquals(new FusionPowerDemand(0.5, 0.4), FusionPowerDemand.of(0.25, 0.4));
        assertEquals(new FusionPowerDemand(1.0, 0.4), FusionPowerDemand.of(FusionPowerDemand.FUEL_SHARE, 0.4));
        FusionPowerDemand boost = FusionPowerDemand.of(0.75, 0.4);
        assertEquals(1.0, boost.fuel(), 1e-9);
        assertEquals(0.7, boost.heating(), 1e-9, "halfway from the floor to full");
        assertTrue(boost.boosting(0.4));
        assertEquals(new FusionPowerDemand(1.0, 1.0), FusionPowerDemand.of(1.0, 0.0));
        assertEquals(new FusionPowerDemand(0.0, 0.0), FusionPowerDemand.of(-2.0, -1.0), "out of range clamps");
    }

    @Test
    void outputForGivesBackTheOutputOfAnyDemand() {
        for (double floor : new double[]{0.0, 0.3, 1.0}) {
            for (double output = 0.0; output <= 1.0; output += 0.05) {
                FusionPowerDemand demand = FusionPowerDemand.of(output, floor);
                double back = FusionPowerDemand.outputFor(demand.fuel(), demand.heating(), floor);
                assertEquals(FusionPowerDemand.of(back, floor).fuel(), demand.fuel(), 1e-9);
                assertEquals(FusionPowerDemand.of(back, floor).heating(), demand.heating(), 1e-9);
            }
        }
    }

    @Test
    void aReachableSetpointIsHeldWithFuelAlone() {
        Plant plant = run(200, 0.5, 600);
        assertEquals(200, plant.power, 6, "within the 5 MW deadband");
        assertTrue(plant.fuel < 1.0, "fuel alone does it, got " + plant.fuel);
        assertEquals(0.5, plant.heating, 1e-9, "the heating stays where the operator left it");
    }

    @Test
    void theHeatingRisesOnlyWhenFuelIsNotEnough() {
        Plant plant = run(350, 0.5, 600);
        assertEquals(350, plant.power, 6);
        assertEquals(1.0, plant.fuel, 1e-9, "fuel saturated first");
        assertTrue(plant.heating > 0.5 && plant.heating < 1.0, "then the heating above the floor, got " + plant.heating);
    }

    @Test
    void anUnreachableSetpointSaturatesBothWithoutWindingUp() {
        Plant plant = run(600, 0.5, 300);
        assertEquals(1.0, plant.fuel, 1e-9);
        assertEquals(1.0, plant.heating, 1e-9);
        assertEquals(400, plant.power, 1, "the most the plant can give");
        // Lowering the setpoint pulls back at once: the integral tracked the saturated output.
        plant.setpoint = 200;
        plant.run(120);
        assertEquals(0.5, plant.heating, 1e-9, "back to the operator's heating");
        assertEquals(200, plant.power, 6);
    }

    @Test
    void theHeatingNeverDropsBelowTheOperatorsLevel() {
        Plant plant = run(20, 0.6, 300);
        assertTrue(plant.minHeating >= 0.6 - 1e-9, "never under the floor, got " + plant.minHeating);
        assertFalse(plant.fuel > 0.2, "little fuel for a low setpoint, got " + plant.fuel);
    }

    @Test
    void theFirstStepTakesOverWithoutABump() {
        PidController pid = new PidController();
        double current = FusionPowerDemand.outputFor(0.3, 0.5, 0.5);
        FusionPowerDemand first = FusionPowerDemand.step(pid, 0.5, 0.1, 1.0, current, 0.5, TUNING);
        assertEquals(0.3, first.fuel(), 1e-9, "the fuelling the operator set");
        assertEquals(0.5, first.heating(), 1e-9);
    }

    private static Plant run(double setpoint, double floor, int seconds) {
        Plant plant = new Plant(setpoint, floor);
        plant.run(seconds);
        return plant;
    }

    /**
     * A tokamak reduced to a lag: the steady fusion power is 400 MW at full fuel and heating, half of it
     * from fuel with no heating, and the plasma follows with a 3 s time constant.
     */
    private static final class Plant {
        final PidController pid = new PidController();
        final double floor;
        double setpoint;
        double power;
        double fuel;
        double heating;
        double minHeating = 1.0;

        Plant(double setpoint, double floor) {
            this.setpoint = setpoint;
            this.floor = floor;
            this.heating = floor;
        }

        void run(int seconds) {
            for (int s = 0; s < seconds; s++) {
                double current = FusionPowerDemand.outputFor(fuel, heating, floor);
                FusionPowerDemand demand = FusionPowerDemand.step(pid, setpoint / SCALE, power / SCALE, 1.0, current, floor, TUNING);
                fuel = demand.fuel();
                heating = demand.heating();
                minHeating = Math.min(minHeating, heating);
                for (int tick = 0; tick < 20; tick++) {
                    double steady = 400.0 * fuel * (0.5 + 0.5 * heating);
                    power += (steady - power) * (0.05 / 3.0);
                }
            }
        }
    }
}
