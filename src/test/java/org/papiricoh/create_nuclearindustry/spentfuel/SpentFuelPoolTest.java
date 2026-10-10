package org.papiricoh.create_nuclearindustry.spentfuel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpentFuelPoolTest {

    @Test
    void twoBlocksOfWaterCutMostOfTheRadiation() {
        assertEquals(1.0, SpentFuelPool.waterShielding(0, 0.15), 1e-12, "uncovered lets everything out");
        assertTrue(SpentFuelPool.waterShielding(2, 0.15) < 0.03, "two blocks stop over 97%");
        assertTrue(SpentFuelPool.waterShielding(4, 0.15) < SpentFuelPool.waterShielding(3, 0.15), "deeper is safer");
    }

    @Test
    void coolerNeedsSpeedAndSaturates() {
        assertEquals(0.0, SpentFuelPool.coolerCapacity(4, 8, 64, 60), 1e-12, "below the minimum it does nothing");
        assertEquals(30.0, SpentFuelPool.coolerCapacity(-32, 8, 64, 60), 1e-12, "either direction, scaling with speed");
        assertEquals(60.0, SpentFuelPool.coolerCapacity(256, 8, 64, 60), 1e-12, "full capacity at and above rated speed");
    }

    @Test
    void surplusBuildsUpAndDrainsBack() {
        double stored = SpentFuelPool.accumulateBoil(0, 50, 30, 10);
        assertEquals(200.0, stored, 1e-9, "20 kW too much for 10 s");
        stored = SpentFuelPool.accumulateBoil(stored, 10, 30, 5);
        assertEquals(100.0, stored, 1e-9, "spare cooling drains it");
        assertEquals(0.0, SpentFuelPool.accumulateBoil(stored, 0, 30, 60), 1e-12, "never below zero");
    }

    @Test
    void aLayerBoilsOnlyWithEnoughSurplus() {
        assertFalse(SpentFuelPool.boilsLayer(9_999, 25, 400));
        assertTrue(SpentFuelPool.boilsLayer(10_000, 25, 400));
        assertFalse(SpentFuelPool.boilsLayer(1e9, 0, 400), "no surface, nothing to boil");
    }

    @Test
    void passiveCoolingGrowsWithTheVolume() {
        assertEquals(40.0, SpentFuelPool.passiveCooling(100, 0.4), 1e-9);
        assertEquals(0.0, SpentFuelPool.passiveCooling(-5, 0.4), 1e-12);
    }
}
