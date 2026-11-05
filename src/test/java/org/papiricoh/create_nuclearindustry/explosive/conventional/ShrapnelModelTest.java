package org.papiricoh.create_nuclearindustry.explosive.conventional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How many casing fragments hit a body, and how hard. */
class ShrapnelModelTest {

    @Test
    void fewerHitTheFurtherOff() {
        double near = ShrapnelModel.expectedHits(4000, 5, 1, false);
        double far = ShrapnelModel.expectedHits(4000, 20, 1, false);
        assertEquals(16, near / far, 1e-9, "the spread grows with the square of the distance");
        assertTrue(ShrapnelModel.expectedHits(4000, 20, 1, true) < far, "an airburst spreads them over the whole sphere");
        assertEquals(0, ShrapnelModel.expectedHits(0, 5, 1, false));
    }

    @Test
    void aBodyIsNeverHitByMoreThanThereAre() {
        assertTrue(ShrapnelModel.expectedHits(100, 0, 50, false) <= 100);
    }

    @Test
    void eachFragmentFadesToNothingAtItsReach() {
        assertEquals(ShrapnelModel.HIT_DAMAGE, ShrapnelModel.hitDamage(0, 40), 1e-9);
        assertTrue(ShrapnelModel.hitDamage(20, 40) < ShrapnelModel.HIT_DAMAGE);
        assertEquals(0, ShrapnelModel.hitDamage(40, 40));
        assertEquals(0, ShrapnelModel.hitDamage(10, 0));
    }
}
