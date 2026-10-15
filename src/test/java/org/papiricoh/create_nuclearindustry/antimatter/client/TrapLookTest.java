package org.papiricoh.create_nuclearindustry.antimatter.client;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The trap cloud's look: nothing when empty, bigger with the charge, red and shaking in a quench. */
class TrapLookTest {
    private static final int CYAN = 0x3FD8E8;

    @Test
    void anEmptyTrapDrawsNoCloud() {
        TrapLook look = TrapLook.of(TrapState.STABLE, 0, 1f, 0, CYAN, 10);
        assertEquals(0, look.radius());
        assertEquals(0, look.glow());
    }

    @Test
    void theCloudGrowsWithTheCubeRootOfTheCharge() {
        // Same clock, so the heartbeat is the same for both.
        float eighth = TrapLook.of(TrapState.STABLE, 0.125, 1f, 0, CYAN, 0).radius();
        float full = TrapLook.of(TrapState.STABLE, 1.0, 1f, 0, CYAN, 0).radius();
        assertEquals(0.5, eighth / full, 1e-4);
        assertTrue(TrapLook.of(TrapState.STABLE, 1.0e-4, 1f, 0, CYAN, 0).radius() > 0.1f, "a trace still shows");
    }

    @Test
    void aStableCloudHasTheTierColourAndDoesNotShake() {
        TrapLook look = TrapLook.of(TrapState.STABLE, 0.5, 1f, 0, CYAN, 3);
        assertEquals(0x3F / 255f, look.red(), 1e-4);
        assertEquals(0xE8 / 255f, look.blue(), 1e-4);
        assertEquals(0, look.shake());
    }

    @Test
    void aQuenchTurnsTheCloudRedAndMakesItShakeMoreAsItRunsOut() {
        TrapLook early = TrapLook.of(TrapState.QUENCHING, 0.5, 0.3f, 0.1f, CYAN, 3);
        TrapLook late = TrapLook.of(TrapState.QUENCHING, 0.5, 0.3f, 0.9f, CYAN, 3);
        assertTrue(early.red() > 0.6f && early.blue() < 0.6f, "red: " + early);
        assertTrue(late.red() > early.red() || late.red() >= 0.99f, "redder towards the end");
        assertTrue(late.shake() > early.shake(), "shakes more towards the end");
    }

    @Test
    void theNoiseIsRepeatableAndInRange() {
        for (long step = -50; step < 50; step++) {
            float n = TrapLook.noise(step);
            assertEquals(n, TrapLook.noise(step));
            assertTrue(n >= 0 && n < 1, "noise " + n);
        }
    }
}
