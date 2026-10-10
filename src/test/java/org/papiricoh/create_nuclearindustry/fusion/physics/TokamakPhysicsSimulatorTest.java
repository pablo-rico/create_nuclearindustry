package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Long-run behaviour of the plasma model, with unlimited fuel unless stated. */
class TokamakPhysicsSimulatorTest {
    private static final int TEN_MINUTES = 12_000;
    private static final double BURN_RATE = 0.0011;

    private static TokamakPhysicsSimulator.Inputs inputs(int radius, double coolant, double tritium, double blankets) {
        return new TokamakPhysicsSimulator.Inputs(1.0, 1.0, true, TokamakGeometry.of(radius).plasmaCount(),
                blankets, true, 1e9, tritium, coolant, 1e9, BURN_RATE);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16})
    void cooledTokamakSettlesBelowDangerAndScalesWithSize(int radius) {
        TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
        TokamakPhysicsSimulator.Outputs out = null;
        for (int tick = 0; tick < TEN_MINUTES; tick++) {
            out = physics.tick(inputs(radius, 1e9, 1e9, 0.4));
            assertFalse(physics.isDisrupting(), "a cooled tokamak must not disrupt (tick " + tick + ")");
        }
        assertEquals(TokamakPhysicsSimulator.Mode.DT, physics.getMode());
        assertTrue(physics.getPlasmaTemperature() < TokamakPhysicsSimulator.DANGER_TEMP,
                "settled at " + physics.getPlasmaTemperature());
        double steamPerCell = out.steamProduced() / TokamakGeometry.of(radius).plasmaCount();
        assertEquals(1.41, steamPerCell, 0.15, "steam output should scale with plasma volume");
        assertTrue(out.tritiumBred() > out.tritiumUsed(), "40% blanket coverage should breed more tritium than it burns");
    }

    @Test
    void dryTokamakDisrupts() {
        TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
        boolean disrupted = false;
        for (int tick = 0; tick < TEN_MINUTES && !disrupted; tick++) {
            physics.tick(inputs(4, 0, 1e9, 0.4));
            disrupted = physics.isDisrupting();
        }
        assertTrue(disrupted, "without coolant the plasma must overheat and disrupt");
    }

    @Test
    void lostMagnetsQuenchWithoutDisrupting() {
        TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
        for (int tick = 0; tick < 2_000; tick++) {
            physics.tick(inputs(4, 1e9, 1e9, 0.4));
        }
        assertTrue(physics.isBurning());
        for (int tick = 0; tick < 400; tick++) {
            physics.tick(new TokamakPhysicsSimulator.Inputs(1.0, 0.0, true, TokamakGeometry.of(4).plasmaCount(),
                    0.4, true, 1e9, 1e9, 1e9, 1e9, BURN_RATE));
            assertFalse(physics.isDisrupting(), "losing the magnet drive should cool the plasma, not explode it");
        }
        assertFalse(physics.isBurning());
    }

    @Test
    void deuteriumOnlyRunsDdAndBreedsTritium() {
        TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
        double bred = 0;
        for (int tick = 0; tick < TEN_MINUTES; tick++) {
            bred += physics.tick(inputs(4, 1e9, 0, 0.4)).tritiumBred();
        }
        assertEquals(TokamakPhysicsSimulator.Mode.DD, physics.getMode());
        assertTrue(bred > 100, "ten minutes of D-D should breed over 100 mB of tritium, got " + bred);
    }

    @Test
    void poorConfinementNeverBurns() {
        TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
        for (int tick = 0; tick < TEN_MINUTES; tick++) {
            physics.tick(new TokamakPhysicsSimulator.Inputs(0.5, 1.0, true, TokamakGeometry.of(4).plasmaCount(),
                    0.4, true, 1e9, 1e9, 1e9, 1e9, BURN_RATE));
        }
        assertFalse(physics.isBurning(), "half coil coverage cannot reach the burn confinement");
    }
}
