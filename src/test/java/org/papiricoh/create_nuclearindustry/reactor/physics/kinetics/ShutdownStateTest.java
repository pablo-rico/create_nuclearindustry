package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownStateTest {
    private static final double REQUIRED = 1000;

    private static ShutdownState classify(boolean running, double net, double margin, double fuel, double coolant, double coverage) {
        return ShutdownState.classify(running, net, margin, REQUIRED, fuel, coolant, coverage);
    }

    @Test
    void aRunningOrCriticalCoreIsOperating() {
        assertEquals(ShutdownState.OPERATING, classify(true, -50, 5000, 20, 20, 1));
        assertEquals(ShutdownState.OPERATING, classify(false, 0, 5000, 20, 20, 1), "critical at source level: it will run");
        assertEquals(ShutdownState.OPERATING, classify(false, 300, 5000, 600, 285, 1));
    }

    @Test
    void offButWithoutColdMarginIsOnlySubcritical() {
        assertEquals(ShutdownState.SUBCRITICAL, classify(false, -2000, 500, 300, 285, 1),
                "it will restart as it cools or its xenon decays");
        assertEquals(ShutdownState.SUBCRITICAL, classify(false, -2000, -1500, 20, 20, 1));
        assertEquals(ShutdownState.SUBCRITICAL, classify(false, -2000, Double.NaN, 20, 20, 1), "no reading is no margin");
    }

    @Test
    void heldOffWithMarginItIsHotUntilEverythingIsColdAndCovered() {
        assertEquals(ShutdownState.HOT_SHUTDOWN, classify(false, -5000, 3000, 400, 285, 1));
        assertEquals(ShutdownState.HOT_SHUTDOWN, classify(false, -5000, 3000, 120, 60, 1), "the fuel is still hot");
        assertEquals(ShutdownState.HOT_SHUTDOWN, classify(false, -5000, 3000, 60, 100, 1), "the water is still at 100");
        assertEquals(ShutdownState.HOT_SHUTDOWN, classify(false, -5000, 3000, 40, 30, 0.9), "part of the core is out of the water");
        assertEquals(ShutdownState.COLD_SHUTDOWN, classify(false, -5000, 3000, 99, 99, 1));
        assertEquals(ShutdownState.COLD_SHUTDOWN, classify(false, -5000, REQUIRED, 30, 30, 1), "the required margin is enough");
    }

    @Test
    void onlyHotAndColdShutdownsAreSafe() {
        assertFalse(ShutdownState.OPERATING.safe());
        assertFalse(ShutdownState.SUBCRITICAL.safe());
        assertTrue(ShutdownState.HOT_SHUTDOWN.safe());
        assertTrue(ShutdownState.COLD_SHUTDOWN.safe());
    }

    @Test
    void ordinalsReadBackAndUnknownOnesAreOperating() {
        for (ShutdownState state : ShutdownState.values()) {
            assertEquals(state, ShutdownState.byOrdinal(state.ordinal()));
        }
        assertEquals(ShutdownState.OPERATING, ShutdownState.byOrdinal(-1));
        assertEquals(ShutdownState.OPERATING, ShutdownState.byOrdinal(99));
    }
}
