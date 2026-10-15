package org.papiricoh.create_nuclearindustry.maintenance.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rating plant events on the INES scale. */
class InesScaleTest {

    @Test
    void releasesRateFiveToSeven() {
        assertEquals(InesScale.NONE, InesScale.release(0.001));
        assertEquals(5, InesScale.release(0.02));
        assertEquals(6, InesScale.release(0.2));
        assertEquals(7, InesScale.release(0.8));
    }

    @Test
    void damagedAndMeltedCores() {
        assertEquals(InesScale.NONE, InesScale.core(0.01, false, 0));
        assertEquals(4, InesScale.core(0.1, false, 0));
        assertEquals(5, InesScale.core(1.0, true, 0), "a contained meltdown is a 5");
        assertEquals(7, InesScale.core(1.0, true, 0.6), "an open one with most of the core out is a 7");
        assertEquals(6, InesScale.core(0.1, false, 0.2), "a release rates above the damage that caused it");
    }

    @Test
    void accidentsStartAtFour() {
        assertFalse(InesScale.accident(3));
        assertTrue(InesScale.accident(4));
    }
}
