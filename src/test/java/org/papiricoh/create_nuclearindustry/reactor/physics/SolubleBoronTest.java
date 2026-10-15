package org.papiricoh.create_nuclearindustry.reactor.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolubleBoronTest {
    private static final double SOLUTION = 7000;
    private static final double MAX = 10000;

    @Test
    void boricAcidInAFullTankGivesItsShare() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(1150, SOLUTION);
        // 1150 mB of acid in a full 16000 mB tank: about 500 ppm.
        assertEquals(503, boron.ppm(16000, MAX), 1);
    }

    @Test
    void boilingConcentratesItAndWaterDilutesIt() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(1000, SOLUTION);
        double full = boron.ppm(16000, MAX);
        double boiledDown = boron.ppm(8000, MAX);
        assertEquals(2 * full, boiledDown, 1.0e-9, "steam leaves the boron behind");
        assertEquals(full, boron.ppm(16000, MAX), 1.0e-9, "and water fed back dilutes it to where it was");
    }

    @Test
    void pastItsSolubilityItPrecipitatesAndComesBack() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(4000, SOLUTION);
        assertEquals(MAX, boron.ppm(1000, MAX), 1.0e-9, "only so much dissolves");
        assertEquals(4000 * SOLUTION / 16000, boron.ppm(16000, MAX), 1.0e-9, "the rest dissolves again with more water");
        assertEquals(0, boron.ppm(0, MAX), 0, "no water, nothing dissolved");
    }

    @Test
    void onlyBleedingTakesItAway() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(1600, SOLUTION);
        double before = boron.ppm(16000, MAX);
        double removed = boron.removeWith(4000, 16000, MAX);
        assertEquals(4000 * before, removed, 1.0e-6, "a quarter of the water takes a quarter of the boron");
        assertEquals(before, boron.ppm(12000, MAX), 1.0e-9, "bleeding leaves the concentration as it was");
        assertEquals(before * 0.75, boron.ppm(16000, MAX), 1.0e-9, "and refilling with water dilutes it");
    }

    @Test
    void bleedingASaturatedVesselLeavesThePrecipitate() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(4000, SOLUTION);
        double mass = boron.mass();
        boron.removeWith(500, 1000, MAX);
        assertEquals(mass - 500 * MAX, boron.mass(), 1.0e-6, "only what is dissolved leaves");
    }

    @Test
    void bleedingItAllAndClearingEmptyIt() {
        SolubleBoron boron = new SolubleBoron();
        boron.add(1000, SOLUTION);
        boron.removeWith(16000, 16000, MAX);
        assertEquals(0, boron.mass(), 0);
        boron.add(1000, SOLUTION);
        boron.clear();
        assertEquals(0, boron.ppm(16000, MAX), 0);
    }

    @Test
    void savedMassRestores() {
        SolubleBoron boron = new SolubleBoron();
        boron.restore(1.0e6);
        assertEquals(1.0e6, boron.mass(), 0);
        boron.restore(Double.NaN);
        assertEquals(0, boron.mass(), 0);
        boron.restore(-5);
        assertTrue(boron.mass() == 0);
        boron.add(-100, SOLUTION);
        boron.add(100, -SOLUTION);
        assertEquals(0, boron.mass(), 0, "nothing negative goes in");
    }
}
