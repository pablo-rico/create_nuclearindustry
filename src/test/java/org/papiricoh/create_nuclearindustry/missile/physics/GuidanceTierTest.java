package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each guidance tier misses by less than the one below it, and every tier misses by more the further it flies. */
class GuidanceTierTest {

    @Test
    void betterGuidanceIsTighterAtEveryRange() {
        for (double range : new double[]{0, 100, 1000, 5000, 20000}) {
            assertTrue(GuidanceTier.STELLAR.cep(range) < GuidanceTier.INERTIAL.cep(range), "stellar beats inertial at " + range);
            assertTrue(GuidanceTier.INERTIAL.cep(range) < GuidanceTier.BASIC.cep(range), "inertial beats dead reckoning at " + range);
        }
    }

    @Test
    void errorsGrowWithRange() {
        for (GuidanceTier tier : GuidanceTier.values()) {
            assertTrue(tier.cep(3000) > tier.cep(300), tier + " drifts further over a longer flight");
            assertTrue(tier.cep(0) > 0, tier + " is never perfect");
            assertEquals(tier.cep(0), tier.cep(-50), 1e-9, "a negative range counts as none");
        }
    }

    @Test
    void typicalShotsLandWhereThePlayerWouldExpect() {
        // At 3 km an unguided rocket lands within about a hundred blocks, a star tracker within about ten.
        assertTrue(GuidanceTier.BASIC.cep(3000) > 60 && GuidanceTier.BASIC.cep(3000) < 150);
        assertTrue(GuidanceTier.INERTIAL.cep(3000) > 15 && GuidanceTier.INERTIAL.cep(3000) < 40);
        assertTrue(GuidanceTier.STELLAR.cep(3000) < 10);
    }

    @Test
    void unknownOrdinalsFallBackToBasic() {
        assertEquals(GuidanceTier.BASIC, GuidanceTier.byOrdinal(-1));
        assertEquals(GuidanceTier.BASIC, GuidanceTier.byOrdinal(99));
        assertEquals(GuidanceTier.STELLAR, GuidanceTier.byOrdinal(GuidanceTier.STELLAR.ordinal()));
    }
}
