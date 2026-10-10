package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CladdingOxidationModelTest {
    private static final CladdingOxidationModel.Params PARAMS = new CladdingOxidationModel.Params(1200, 0.5, 500, 4.0);

    @Test
    void nothingBelowTheThreshold() {
        CladdingOxidationModel model = new CladdingOxidationModel();
        CladdingOxidationModel.Step step = model.step(1150, 6, 1.0, PARAMS);
        assertEquals(0.0, step.hydrogen());
        assertEquals(0.0, step.heat());
    }

    @Test
    void makesTwoHydrogenPerZirconiumAndHeat() {
        CladdingOxidationModel model = new CladdingOxidationModel();
        CladdingOxidationModel.Step step = model.step(1500, 6, 1.0, PARAMS);
        // 300 °C over: the reference rate, 0.5 mol per rod
        assertEquals(6.0, step.hydrogen(), 1e-9);
        assertEquals(2.0, step.heat(), 1e-9);
        assertEquals(3.0, model.oxidized(), 1e-9);
    }

    @Test
    void hotterIsFaster() {
        CladdingOxidationModel warm = new CladdingOxidationModel();
        CladdingOxidationModel hot = new CladdingOxidationModel();
        assertTrue(hot.step(1900, 6, 1.0, PARAMS).hydrogen() > warm.step(1400, 6, 1.0, PARAMS).hydrogen());
    }

    @Test
    void aDryCoreDoesNotOxidise() {
        CladdingOxidationModel model = new CladdingOxidationModel();
        assertEquals(0.0, model.step(2000, 6, 0.0, PARAMS).hydrogen());
    }

    @Test
    void boundedByTheZirconium() {
        CladdingOxidationModel model = new CladdingOxidationModel();
        double hydrogen = 0;
        for (int t = 0; t < 100_000; t++) {
            hydrogen += model.step(2500, 6, 1.0, PARAMS).hydrogen();
        }
        assertEquals(2 * 6 * 500, hydrogen, 1e-6, "never more than two hydrogen per zirconium atom");
        assertEquals(1.0, model.damage(6, 500), 1e-9);
        assertEquals(0.0, model.step(2500, 6, 1.0, PARAMS).heat(), "spent cladding gives no more heat");
    }

    @Test
    void freshFuelBringsFreshCladding() {
        CladdingOxidationModel model = new CladdingOxidationModel();
        for (int t = 0; t < 100_000; t++) {
            model.step(2500, 6, 1.0, PARAMS);
        }
        model.refuel(2, 500);
        assertEquals(4.0 / 6.0, model.damage(6, 500), 1e-9);
        assertTrue(model.step(1600, 6, 1.0, PARAMS).hydrogen() > 0);
    }
}
