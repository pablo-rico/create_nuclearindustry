package org.papiricoh.create_nuclearindustry.ruins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ground check of the abandoned power plants. */
class FlatnessTest {

    @Test
    void spreadIsHighestMinusLowest() {
        assertEquals(0, Flatness.spread());
        assertEquals(0, Flatness.spread(70));
        assertEquals(9, Flatness.spread(64, 70, 61, 66, 63));
    }

    @Test
    void levelGroundWithinTheLimitIsFlat() {
        assertTrue(Flatness.isFlat(4, 64, 64, 64, 64, 64));
        assertTrue(Flatness.isFlat(4, 64, 66, 68, 65, 67));
        assertTrue(Flatness.isFlat(0, 70, 70, 70));
    }

    @Test
    void aSlopeOverTheLimitIsNot() {
        assertFalse(Flatness.isFlat(4, 64, 66, 69, 65, 67));
        assertFalse(Flatness.isFlat(0, 70, 71));
    }

    @Test
    void aNegativeLimitAcceptsAnything() {
        assertTrue(Flatness.isFlat(-1, 0, 200, 40));
    }

    @Test
    void waterOverTheGroundIsNotDry() {
        assertTrue(Flatness.isDry(new int[]{64, 65}, new int[]{64, 65}));
        assertFalse(Flatness.isDry(new int[]{63, 63}, new int[]{63, 58}));
        assertThrows(IllegalArgumentException.class, () -> Flatness.isDry(new int[]{1}, new int[]{1, 2}));
    }
}
