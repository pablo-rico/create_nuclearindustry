package org.papiricoh.create_nuclearindustry.reactor.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecayHeatModelTest {

    @Test
    void settlesAtTheFractionOfFissionHeat() {
        DecayHeatModel model = new DecayHeatModel();
        for (int i = 0; i < 50_000; i++) {
            model.tick(40.0, 0.07, 2400);
        }
        assertEquals(2.8, model.heat(), 1e-6);
    }

    @Test
    void lingersAfterFissionStops() {
        DecayHeatModel model = new DecayHeatModel();
        model.set(2.8);
        for (int i = 0; i < 2400; i++) {
            model.tick(0.0, 0.07, 2400);
        }
        assertTrue(model.heat() > 0.9 && model.heat() < 1.2, "about a third left after one lifetime, got " + model.heat());
        for (int i = 0; i < 24_000; i++) {
            model.tick(0.0, 0.07, 2400);
        }
        assertTrue(model.heat() < 0.01, "gone after ten lifetimes, got " + model.heat());
    }

    @Test
    void zeroFractionMakesNoHeat() {
        DecayHeatModel model = new DecayHeatModel();
        for (int i = 0; i < 1000; i++) {
            model.tick(100.0, 0.0, 2400);
        }
        assertEquals(0.0, model.heat());
    }
}
