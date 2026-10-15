package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Americium ingrowth in aged plutonium, the technetium medical kit and the cobalt radiography camera. */
class IsotopeUsesTest {
    private static final AmericiumIngrowth.Params AM = AmericiumIngrowth.Params.DEFAULT;
    private static final MedicalKitModel.Params KIT = MedicalKitModel.Params.DEFAULT;
    private static final RadiographyModel.Params CAM = RadiographyModel.Params.DEFAULT;

    // ------------------------------------------------------------------ americium

    @Test
    void freshPlutoniumHasNoAmericium() {
        assertEquals(0, AmericiumIngrowth.chance(AM, 0), 1e-12);
        assertEquals(0, AmericiumIngrowth.yield(AM, 0, 64, 0.999), "no americium yet, whatever the roll");
    }

    @Test
    void olderPlutoniumYieldsMore() {
        double previous = -1;
        for (double days = 0; days <= 100; days += 2.5) {
            double chance = AmericiumIngrowth.chance(AM, days);
            assertTrue(chance > previous || days == 0, "at day " + days);
            assertTrue(chance <= AM.maxYield());
            previous = chance;
        }
        assertEquals(AM.maxYield() / 2, AmericiumIngrowth.chance(AM, AM.ingrowthHalfLifeDays()), 1e-12, "half grown in after one Pu-241 half-life");
    }

    @Test
    void yieldAveragesTheChanceAndIsNeverOffByMoreThanOne() {
        double days = 20;
        int count = 16;
        double expected = count * AmericiumIngrowth.chance(AM, days);
        double total = 0;
        int samples = 1000;
        for (int i = 0; i < samples; i++) {
            double roll = (i + 0.5) / samples;
            int got = AmericiumIngrowth.yield(AM, days, count, roll);
            assertTrue(Math.abs(got - expected) < 1.0 + 1e-9, "roll " + roll + " gave " + got);
            total += got;
        }
        assertEquals(expected, total / samples, 0.01);
        assertEquals(0, AmericiumIngrowth.yield(AM, days, 0, 0.5));
        assertTrue(AmericiumIngrowth.yield(AM, 1e6, 10, 1.5) <= 10, "a roll of 1 or more is clamped");
    }

    @Test
    void daysForInvertsTheChance() {
        assertEquals(0, AmericiumIngrowth.daysFor(AM, 0), 0.0);
        assertEquals(AM.ingrowthHalfLifeDays(), AmericiumIngrowth.daysFor(AM, AM.maxYield() / 2), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, AmericiumIngrowth.daysFor(AM, AM.maxYield()));
        double days = AmericiumIngrowth.daysFor(AM, 0.3);
        assertEquals(0.3, AmericiumIngrowth.chance(AM, days), 1e-9);
        assertEquals(0, AmericiumIngrowth.chance(new AmericiumIngrowth.Params(0, 0.5), 100), 0.0, "no Pu-241, no americium");
    }

    // ------------------------------------------------------------------ medical kit

    @Test
    void aFreshKitBoostsTreatmentByHalf() {
        assertTrue(MedicalKitModel.usable(KIT, 1.0));
        assertEquals(1.5, MedicalKitModel.boost(KIT, 1.0), 1e-12);
        assertEquals(1.5, MedicalKitModel.boost(KIT, 3.0), 1e-12, "capped at a fresh kit");
        assertEquals(2400, MedicalKitModel.boostTicks(KIT, 1.0));
        assertEquals(1200, MedicalKitModel.boostTicks(KIT, 0.5));
    }

    @Test
    void aDecayedKitDoesNothing() {
        assertFalse(MedicalKitModel.usable(KIT, 0.05));
        assertEquals(1.0, MedicalKitModel.boost(KIT, 0.05), 0.0);
        assertEquals(0, MedicalKitModel.boostTicks(KIT, 0.05));
        // Tc-99m: a kit is past use a little after three half-lives.
        double tc = Isotope.TECHNETIUM_99M.halfLifeDays();
        assertTrue(MedicalKitModel.usable(KIT, IsotopeDecay.activity(1, tc, 3 * tc)));
        assertFalse(MedicalKitModel.usable(KIT, IsotopeDecay.activity(1, tc, 3.5 * tc)));
    }

    @Test
    void boostedTreatmentsBeatPlainOnes() {
        double iodide = 0.5;
        assertEquals(0.5, MedicalKitModel.boostedUptake(iodide, 0, 1.0), 1e-12, "k = 1 is the plain treatment");
        assertEquals(0.25, MedicalKitModel.boostedUptake(iodide, 1, 1.0), 1e-12);
        assertTrue(MedicalKitModel.boostedUptake(iodide, 0, 1.5) < 0.5);
        assertEquals(Math.pow(0.5, 1.5), MedicalKitModel.boostedUptake(iodide, 0, 1.5), 1e-12);
        assertEquals(0.5, MedicalKitModel.boostedUptake(iodide, 0, 0.2), 1e-12, "a boost never weakens");
        assertEquals(15, MedicalKitModel.boostedPurge(15, 0, 1.0), 1e-12);
        assertEquals(45, MedicalKitModel.boostedPurge(15, 1, 1.5), 1e-12);
        assertEquals(0, MedicalKitModel.boostedPurge(-3, 0, 1.5), 0.0);
    }

    @Test
    void theBoostRoundTripsThroughAnEffectAmplifier() {
        assertEquals(-1, MedicalKitModel.effectAmplifier(1.0), "no boost, no effect");
        assertEquals(-1, MedicalKitModel.effectAmplifier(1.04));
        assertEquals(4, MedicalKitModel.effectAmplifier(1.5));
        assertEquals(1.5, MedicalKitModel.boostFromAmplifier(4), 1e-12);
        assertEquals(1.0, MedicalKitModel.boostFromAmplifier(-1), 0.0);
        for (double activity = 0.1; activity <= 1.0; activity += 0.05) {
            double k = MedicalKitModel.boost(KIT, activity);
            double back = MedicalKitModel.boostFromAmplifier(MedicalKitModel.effectAmplifier(k));
            assertEquals(k, back, MedicalKitModel.BOOST_PER_LEVEL / 2 + 1e-9, "activity " + activity);
        }
    }

    // ------------------------------------------------------------------ radiography

    @Test
    void aFreshSourceSeesEightBlocksInOneSecond() {
        assertEquals(20, RadiographyModel.exposureTicks(CAM, 1.0));
        assertEquals(8, RadiographyModel.penetration(CAM, 1.0));
        assertEquals(2.0, RadiographyModel.operatorDose(CAM, 1.0), 1e-12);
    }

    @Test
    void eachHalfLifeCostsAHalfValueLayer() {
        assertEquals(7, RadiographyModel.penetration(CAM, 0.5));
        assertEquals(6, RadiographyModel.penetration(CAM, 0.25));
        assertEquals(40, RadiographyModel.exposureTicks(CAM, 0.5));
        assertEquals(1.0, RadiographyModel.operatorDose(CAM, 0.5), 1e-12);
        assertEquals(8, RadiographyModel.penetration(CAM, 4.0), "a hotter source than nominal is capped");
    }

    @Test
    void aDeadSourceTakesNoPicture() {
        assertFalse(RadiographyModel.usable(CAM, 0.04));
        assertEquals(0, RadiographyModel.exposureTicks(CAM, 0.04));
        assertEquals(0, RadiographyModel.penetration(CAM, 0.04));
        assertEquals(0, RadiographyModel.operatorDose(CAM, 0.04), 0.0);
        assertTrue(RadiographyModel.usable(CAM, 0.05));
        assertTrue(RadiographyModel.penetration(CAM, 0.05) >= 1);
        assertTrue(RadiographyModel.exposureTicks(CAM, 0.05) <= CAM.maxExposureTicks());
    }

    @Test
    void cobaltLastsSeveralHalfLivesInTheCamera() {
        double co = Isotope.COBALT_60.halfLifeDays();
        double service = IsotopeDecay.daysUntil(1, co, CAM.minActivity());
        assertTrue(service > 4 * co && service < 5 * co, "log2(20) = 4.3 half-lives: " + service);
    }
}
