package org.papiricoh.create_nuclearindustry.missile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The server's limits on launches: dimensions, range, spawn protection, cooldown and who is warned. */
class LaunchRulesTest {

    @Test
    void anEmptyListAllowsEveryDimension() {
        assertTrue(LaunchRules.dimensionAllowed("minecraft:the_nether", List.of()));
        assertTrue(LaunchRules.dimensionAllowed("minecraft:overworld", List.of("minecraft:overworld")));
        assertFalse(LaunchRules.dimensionAllowed("minecraft:the_nether", List.of("minecraft:overworld")));
    }

    @Test
    void rangeIsHorizontalAndZeroSetsNoLimit() {
        assertFalse(LaunchRules.outOfRange(0, 0, 300, 400, 500), "exactly at the limit is allowed");
        assertTrue(LaunchRules.outOfRange(0, 0, 300, 401, 500));
        assertFalse(LaunchRules.outOfRange(0, 0, 1.0e6, 0, 0), "0 is no limit");
    }

    @Test
    void theSpawnIsProtectedInsideItsRadius() {
        assertTrue(LaunchRules.nearSpawn(60, 0, 0, 0, 64));
        assertTrue(LaunchRules.nearSpawn(64, 0, 0, 0, 64), "the edge counts");
        assertFalse(LaunchRules.nearSpawn(46, 46, 0, 0, 64), "a circle, not a square");
        assertFalse(LaunchRules.nearSpawn(0, 0, 0, 0, 0), "0 protects nothing");
    }

    @Test
    void theCooldownRunsFromTheLastLaunch() {
        assertEquals(0, LaunchRules.cooldownTicksLeft(-1, 1000, 60), "never launched");
        assertEquals(0, LaunchRules.cooldownTicksLeft(900, 1000, 0), "no cooldown");
        assertEquals(1100, LaunchRules.cooldownTicksLeft(900, 1000, 60));
        assertEquals(0, LaunchRules.cooldownTicksLeft(900, 900 + 1200, 60), "over once the time is up");
    }

    @Test
    void playersAreWarnedTwiceAsFarAsTheSirens() {
        assertEquals(400, LaunchRules.warningRadius(200, 128));
        assertEquals(128, LaunchRules.warningRadius(10, 128), "never closer than the floor");
    }

    @Test
    void refusalsReadBackByOrdinal() {
        for (LaunchRules.Refusal refusal : LaunchRules.Refusal.values()) {
            assertEquals(refusal, LaunchRules.Refusal.byOrdinal(refusal.ordinal()));
        }
        assertEquals(LaunchRules.Refusal.NONE, LaunchRules.Refusal.byOrdinal(99), "an unknown one reads as none");
    }
}
