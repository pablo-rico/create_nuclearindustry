package org.papiricoh.create_nuclearindustry.reactor.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecayHeatModelTest {
    private static final double LIFETIME = 300;

    @Test
    void settlesAtTheFractionOfFissionHeat() {
        DecayHeatModel model = new DecayHeatModel();
        for (int i = 0; i < 500_000; i++) {
            model.tick(40.0, 0.07, LIFETIME);
        }
        assertEquals(2.8, model.heat(), 1e-3);
    }

    @Test
    void fallsFastAtFirstThenLingers() {
        DecayHeatModel model = new DecayHeatModel();
        model.set(2.8);
        for (int i = 0; i < LIFETIME / 10; i++) {
            model.tick(0.0, 0.07, LIFETIME);
        }
        double early = model.heat();
        assertTrue(early > 0.8 && early < 1.4, "the short-lived products go first: " + early);
        for (int i = 0; i < LIFETIME * 0.9; i++) {
            model.tick(0.0, 0.07, LIFETIME);
        }
        assertTrue(model.heat() > 0.65 && model.heat() < 0.95, "under a third after one lifetime: " + model.heat());
        for (int i = 0; i < LIFETIME * 9; i++) {
            model.tick(0.0, 0.07, LIFETIME);
        }
        assertTrue(model.heat() > 0.4 && model.heat() < 0.6, "and the long-lived products linger after ten: " + model.heat());
    }

    @Test
    void aBriefRunLeavesLessOfTheLongTail() {
        DecayHeatModel brief = new DecayHeatModel();
        DecayHeatModel steady = new DecayHeatModel();
        steady.set(2.8);
        for (int i = 0; i < LIFETIME; i++) {
            brief.tick(40.0, 0.07, LIFETIME);
        }
        for (int i = 0; i < LIFETIME * 10; i++) {
            brief.tick(0.0, 0.07, LIFETIME);
            steady.tick(0.0, 0.07, LIFETIME);
        }
        assertTrue(brief.heat() < steady.heat() / 3, brief.heat() + " vs " + steady.heat());
    }

    @Test
    void savedGroupsComeBack() {
        DecayHeatModel model = new DecayHeatModel();
        for (int i = 0; i < 1000; i++) {
            model.tick(40.0, 0.07, LIFETIME);
        }
        DecayHeatModel loaded = new DecayHeatModel();
        loaded.restore(model.groups(), 0);
        assertEquals(model.heat(), loaded.heat(), 1e-12);
        loaded.restore(null, 1.0);
        assertEquals(1.0, loaded.heat(), 1e-12);
    }

    @Test
    void zeroFractionMakesNoHeat() {
        DecayHeatModel model = new DecayHeatModel();
        for (int i = 0; i < 1000; i++) {
            model.tick(100.0, 0.0, LIFETIME);
        }
        assertEquals(0.0, model.heat());
    }
}
