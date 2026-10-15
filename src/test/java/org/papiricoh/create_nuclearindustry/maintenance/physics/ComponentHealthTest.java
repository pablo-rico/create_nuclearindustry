package org.papiricoh.create_nuclearindustry.maintenance.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Wear, transients, failure checks and repair of a plant part, and the default parts. */
class ComponentHealthTest {
    private static final ComponentHealth.Params P = new ComponentHealth.Params(1000, 0.1, 0.5, 0.8);

    @Test
    void wearsInProportionToStressAndTime() {
        ComponentHealth part = new ComponentHealth();
        part.wear(P, 1.0, 250);
        assertEquals(0.75, part.health(), 1e-12);
        part.wear(P, 0.5, 500);
        assertEquals(0.5, part.health(), 1e-12);
        part.wear(P, 0.0, 10_000);
        assertEquals(0.5, part.health(), 1e-12, "an idle part does not wear");
        part.wear(P, 10.0, 10_000);
        assertEquals(0.0, part.health(), 1e-12, "health never goes below zero");
    }

    @Test
    void aTransientTakesAStep() {
        ComponentHealth part = new ComponentHealth();
        part.shock(P, 1.0);
        part.shock(P, 2.0);
        assertEquals(0.7, part.health(), 1e-12);
    }

    @Test
    void neverFailsAboveTheSafeLevelAndLikelierTheMoreWorn() {
        assertEquals(0.0, ComponentHealth.failureChance(P, 0.6));
        assertEquals(0.0, ComponentHealth.failureChance(P, 0.5));
        assertEquals(0.2, ComponentHealth.failureChance(P, 0.25), 1e-12);
        assertEquals(0.8, ComponentHealth.failureChance(P, 0.0), 1e-12);
        assertTrue(ComponentHealth.failureChance(P, 0.1) > ComponentHealth.failureChance(P, 0.3));
    }

    @Test
    void aCheckFailsOnlyOnceAndRepairPutsItRight() {
        ComponentHealth part = new ComponentHealth();
        part.set(0.0, false);
        assertFalse(part.check(P, 0.9), "a roll above the chance passes");
        assertTrue(part.check(P, 0.1), "a roll below it fails the part");
        assertFalse(part.check(P, 0.0), "a failed part does not fail again");
        assertTrue(part.failed());
        assertEquals(ComponentHealth.Condition.FAILED, part.condition());
        part.repair(0.5);
        assertFalse(part.failed());
        assertEquals(0.5, part.health(), 1e-12);
        assertEquals(ComponentHealth.Condition.WORN, part.condition());
        part.restore();
        assertEquals(ComponentHealth.Condition.GOOD, part.condition());
    }

    @Test
    void conditionsByHealth() {
        assertEquals(ComponentHealth.Condition.GOOD, ComponentHealth.condition(0.8, false));
        assertEquals(ComponentHealth.Condition.WORN, ComponentHealth.condition(0.5, false));
        assertEquals(ComponentHealth.Condition.POOR, ComponentHealth.condition(0.2, false));
    }

    @Test
    void defaultPartsScaleWithTheWearSetting() {
        ComponentHealth.Params normal = WearPart.ROD_DRIVES.params(1.0);
        ComponentHealth.Params fast = WearPart.ROD_DRIVES.params(2.0);
        assertEquals(20 * WearPart.TICKS_PER_DAY, normal.lifeTicks(), 1e-6);
        assertEquals(normal.lifeTicks() / 2, fast.lifeTicks(), 1e-6);
        assertEquals(normal.transientWear() * 2, fast.transientWear(), 1e-12);
        ComponentHealth.Params none = WearPart.ROD_DRIVES.params(0.0);
        ComponentHealth part = new ComponentHealth();
        part.wear(none, 1.0, 1_000_000);
        part.shock(none, 1.0);
        assertEquals(1.0, part.health(), 1e-12, "wear scale 0 turns wear off");
    }

    @Test
    void aWornValveSticksTenTimesMoreOften() {
        assertEquals(1.0, WearPart.valveStickScale(1.0), 1e-12);
        assertEquals(WearPart.WORN_VALVE_STICK, WearPart.valveStickScale(0.0), 1e-12);
        assertEquals(5.5, WearPart.valveStickScale(0.5), 1e-12);
    }
}
