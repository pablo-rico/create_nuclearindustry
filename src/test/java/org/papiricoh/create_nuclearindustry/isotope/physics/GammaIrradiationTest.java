package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Food sterilisation under cobalt-60 sources, weakening as they decay. */
class GammaIrradiationTest {
    private static final GammaIrradiation.Params P = GammaIrradiation.Params.DEFAULT;

    @Test
    void aFreshSourceSterilisesInFiveSeconds() {
        assertEquals(100, GammaIrradiation.processingTicks(P, 1.0));
        assertEquals(50, GammaIrradiation.processingTicks(P, 2.0), "two sources, twice the rate");
        assertEquals(200, GammaIrradiation.processingTicks(P, 0.5));
        assertTrue(GammaIrradiation.sterile(P, GammaIrradiation.absorbed(P, 1.0, 100)));
        assertFalse(GammaIrradiation.sterile(P, GammaIrradiation.absorbed(P, 1.0, 99)));
    }

    @Test
    void aWeakSourceStopsSterilising() {
        double min = GammaIrradiation.minActivity(P);
        assertEquals(1.0 / 12, min, 1e-12);
        assertTrue(GammaIrradiation.canSterilise(P, min));
        assertEquals(1200, GammaIrradiation.processingTicks(P, min));
        assertFalse(GammaIrradiation.canSterilise(P, min * 0.99));
        assertEquals(GammaIrradiation.TOO_WEAK, GammaIrradiation.processingTicks(P, 0));
        assertEquals(GammaIrradiation.TOO_WEAK, GammaIrradiation.processingTicks(P, -1));
    }

    @Test
    void theSourceWeakensWithCobaltDecay() {
        double halfLife = Isotope.COBALT_60.halfLifeDays();
        int fresh = GammaIrradiation.processingTicks(P, IsotopeDecay.activity(1, halfLife, 0));
        int oneHalfLife = GammaIrradiation.processingTicks(P, IsotopeDecay.activity(1, halfLife, halfLife));
        assertEquals(2 * fresh, oneHalfLife);
        double service = GammaIrradiation.daysOfService(P, 1.0, halfLife);
        assertEquals(halfLife * Math.log(12) / Math.log(2), service, 1e-9, "about 3.6 half-lives");
        assertTrue(GammaIrradiation.canSterilise(P, IsotopeDecay.activity(1, halfLife, service - 0.1)));
        assertFalse(GammaIrradiation.canSterilise(P, IsotopeDecay.activity(1, halfLife, service + 0.1)));
    }

    @Test
    void sourcesAddUp() {
        assertEquals(1.5, GammaIrradiation.sourceActivity(1.0, 0.5), 1e-12);
        assertEquals(0.5, GammaIrradiation.sourceActivity(-1.0, 0.5), 1e-12);
        assertEquals(0, GammaIrradiation.sourceActivity(), 1e-12);
    }

    @Test
    void emissionAndComparatorFollowTheStrength() {
        assertEquals(4.0, GammaIrradiation.emission(P, 1.0), 1e-12);
        assertEquals(2.0, GammaIrradiation.emission(P, 0.5), 1e-12);
        assertEquals(0, GammaIrradiation.emission(P, -2), 1e-12);
        assertEquals(15, GammaIrradiation.comparator(P, 1.0));
        assertEquals(15, GammaIrradiation.comparator(P, 3.0));
        assertEquals(8, GammaIrradiation.comparator(P, 0.5));
        assertEquals(0, GammaIrradiation.comparator(P, 0.01), "too weak reads 0");
        assertTrue(GammaIrradiation.comparator(P, GammaIrradiation.minActivity(P)) >= 1);
    }
}
