package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How hard a blast wave throws a ship. */
class ShipImpulseModelTest {
    @Test
    void moreOverpressureAndMoreFaceThrowHarder() {
        double base = ShipImpulseModel.impulse(10, 4, 1000, 1);
        assertTrue(ShipImpulseModel.impulse(20, 4, 1000, 1) > base);
        assertTrue(ShipImpulseModel.impulse(10, 8, 1000, 1) > base);
        assertEquals(2 * base, ShipImpulseModel.impulse(10, 4, 1000, 2), 1.0e-9, "the config scale multiplies it");
    }

    @Test
    void noShipLeavesFasterThanTheCap() {
        double mass = 8;
        double impulse = ShipImpulseModel.impulse(1.0e6, 100, mass, 1);
        assertEquals(ShipImpulseModel.MAX_SPEED * mass, impulse, 1.0e-9);
    }

    @Test
    void nothingWithoutPressureFaceOrMass() {
        assertEquals(0, ShipImpulseModel.impulse(0, 4, 10, 1));
        assertEquals(0, ShipImpulseModel.impulse(10, 0, 10, 1));
        assertEquals(0, ShipImpulseModel.impulse(10, 4, 0, 1));
        assertEquals(0, ShipImpulseModel.impulse(10, 4, 10, 0));
    }

    @Test
    void aBoxShowsTheFaceTheWaveMeets() {
        // A long hull, 10 along x, 2 high, 3 wide.
        assertEquals(2 * 3, ShipImpulseModel.exposedArea(10, 2, 3, 1, 0, 0), 1.0e-9, "end on");
        assertEquals(10 * 2, ShipImpulseModel.exposedArea(10, 2, 3, 0, 0, 1), 1.0e-9, "broadside");
        assertEquals(10 * 3, ShipImpulseModel.exposedArea(10, 2, 3, 0, -1, 0), 1.0e-9, "from above");
        assertEquals((2 * 3 + 10 * 2) / Math.sqrt(2), ShipImpulseModel.exposedArea(10, 2, 3, 1, 0, 1), 1.0e-9,
                "at an angle, both faces in proportion");
    }
}
