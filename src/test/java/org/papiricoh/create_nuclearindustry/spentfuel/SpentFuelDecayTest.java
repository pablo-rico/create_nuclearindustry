package org.papiricoh.create_nuclearindustry.spentfuel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpentFuelDecayTest {

    @Test
    void startsAtTheFullDischargeHeat() {
        assertEquals(1.0, SpentFuelDecay.heatFraction(0.0), 1e-12);
        assertEquals(1.0, SpentFuelDecay.heatFraction(-3.0), 1e-12, "before discharge counts as discharge");
    }

    @Test
    void onlyEverFalls() {
        double previous = SpentFuelDecay.heatFraction(0.0);
        for (double days = 0.25; days < 200; days += 0.25) {
            double now = SpentFuelDecay.heatFraction(days);
            assertTrue(now < previous, "heat should fall every step, at day " + days);
            previous = now;
        }
    }

    @Test
    void fallsFastThenSlowly() {
        double firstDay = 1.0 - SpentFuelDecay.heatFraction(1.0);
        double tenthDay = SpentFuelDecay.heatFraction(9.0) - SpentFuelDecay.heatFraction(10.0);
        assertTrue(firstDay > 10 * tenthDay, "the short-lived products go first: " + firstDay + " vs " + tenthDay);
        assertTrue(SpentFuelDecay.heatFraction(60.0) > 0.01, "the long-lived group lingers for months");
    }

    @Test
    void gradesFollowTheSchedule() {
        assertEquals(CoolingGrade.HOT, SpentFuelDecay.gradeAfter(0.0));
        assertEquals(CoolingGrade.HOT, SpentFuelDecay.gradeAfter(1.5));
        assertEquals(CoolingGrade.WARM, SpentFuelDecay.gradeAfter(3.0));
        assertEquals(CoolingGrade.WARM, SpentFuelDecay.gradeAfter(6.0));
        assertEquals(CoolingGrade.COOLED, SpentFuelDecay.gradeAfter(10.0));
        assertEquals(CoolingGrade.AGED, SpentFuelDecay.gradeAfter(30.0));
    }

    @Test
    void daysUntilIsTheInverseOfTheCurve() {
        for (CoolingGrade grade : CoolingGrade.values()) {
            double days = SpentFuelDecay.daysUntil(grade);
            assertEquals(grade, SpentFuelDecay.gradeAfter(days + 1e-6), "just after the threshold of " + grade);
            if (days > 0) {
                assertTrue(SpentFuelDecay.gradeAfter(days - 1e-3).isHotterThan(grade), "just before the threshold of " + grade);
            }
        }
        double warm = SpentFuelDecay.daysUntil(CoolingGrade.WARM);
        double cooled = SpentFuelDecay.daysUntil(CoolingGrade.COOLED);
        double aged = SpentFuelDecay.daysUntil(CoolingGrade.AGED);
        assertTrue(warm > 1.5 && warm < 3, "Hot for about two days, got " + warm);
        assertTrue(cooled > 5 && cooled < 9, "Warm until about a week, got " + cooled);
        assertTrue(aged > 15 && aged < 25, "Cooled until about three weeks, got " + aged);
    }

    @Test
    void gradesOrderFromHotToAged() {
        assertTrue(CoolingGrade.HOT.isHotterThan(CoolingGrade.WARM));
        assertTrue(CoolingGrade.COOLED.isHotterThan(CoolingGrade.AGED));
        assertEquals(CoolingGrade.COOLED, CoolingGrade.cooler(CoolingGrade.WARM, CoolingGrade.COOLED));
        assertEquals(CoolingGrade.AGED, CoolingGrade.cooler(CoolingGrade.AGED, CoolingGrade.HOT));
        for (CoolingGrade grade : CoolingGrade.values()) {
            assertEquals(grade, CoolingGrade.fromId(grade.id()));
        }
    }
}
