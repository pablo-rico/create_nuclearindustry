package org.papiricoh.create_nuclearindustry.dyson.swarm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the swarm beams down, and how receivers share it. */
class SwarmPowerTest {
    private static final double CUT = Math.sin(Math.toRadians(5));

    @Test
    void fullSunClearSkyGivesEverything() {
        assertEquals(1, SwarmPower.sunFactor(1, 0, 0, CUT, 0.4, 0.1), 1.0e-12);
        assertEquals(512 * 64, SwarmPower.pool(512, 64, 1), 1.0e-9);
    }

    @Test
    void nothingAtNightOrBelowTheCutOff() {
        assertEquals(0, SwarmPower.sunFactor(-0.5, 0, 0, CUT, 0.4, 0.1));
        assertEquals(0, SwarmPower.sunFactor(CUT * 0.9, 0, 0, CUT, 0.4, 0.1));
        assertEquals(0, SwarmPower.sunFactor(0.5, 0, 0, 0, 0.4, 0.1) * 0 + SwarmPower.sunFactor(0, 0, 0, 0, 0.4, 0.1));
    }

    @Test
    void itFollowsTheSunsHeight() {
        double low = SwarmPower.sunFactor(0.3, 0, 0, CUT, 0.4, 0.1), high = SwarmPower.sunFactor(0.9, 0, 0, CUT, 0.4, 0.1);
        assertTrue(low < high);
        assertEquals(0.3, low, 1.0e-12);
    }

    @Test
    void rainAndThunderDimIt() {
        assertEquals(0.4, SwarmPower.sunFactor(1, 1, 0, CUT, 0.4, 0.1), 1.0e-12);
        assertEquals(0.1, SwarmPower.sunFactor(1, 1, 1, CUT, 0.4, 0.1), 1.0e-12);
        assertEquals(0.7, SwarmPower.sunFactor(1, 0.5, 0, CUT, 0.4, 0.1), 1.0e-12);
        // Thunder never brightens a rainy sky, even set brighter than rain.
        assertEquals(0.4, SwarmPower.sunFactor(1, 1, 1, CUT, 0.4, 0.9), 1.0e-12);
    }

    @Test
    void receiversSplitEvenlyAndNeverMakeMore() {
        int[] shares = SwarmPower.allocate(1000, new int[] {32768, 32768, 32768, 32768});
        assertArrayEquals(new int[] {250, 250, 250, 250}, shares);
        int[] more = SwarmPower.allocate(1000, new int[] {32768, 32768, 32768, 32768, 32768, 32768, 32768, 32768});
        assertTrue(Arrays.stream(more).sum() <= 1000);
    }

    @Test
    void whatOneLeavesGoesToTheOthers() {
        int[] shares = SwarmPower.allocate(1000, new int[] {100, 32768, 32768});
        assertArrayEquals(new int[] {100, 450, 450}, shares);
    }

    @Test
    void noneTakesMoreThanItAsks() {
        int[] shares = SwarmPower.allocate(1.0e9, new int[] {32768, 10, 0});
        assertArrayEquals(new int[] {32768, 10, 0}, shares);
    }

    @Test
    void emptyCases() {
        assertEquals(0, SwarmPower.allocate(1000, new int[0]).length);
        assertArrayEquals(new int[] {0, 0}, SwarmPower.allocate(0, new int[] {5, 5}));
        assertEquals(0, SwarmPower.pool(-3, 64, 1));
    }
}
