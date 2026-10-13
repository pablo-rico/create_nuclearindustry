package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteamGeneratorModelTest {
    private static final SteamGeneratorModel.Params PARAMS = new SteamGeneratorModel.Params(0.3, 325, 0.12, 0.225, 50, 120, 500);

    @Test
    void heatCrossesTheTubesAtUaTimesTheGap() {
        SteamGeneratorModel model = new SteamGeneratorModel();
        SteamGeneratorModel.Result result = model.tick(8_000, 8_000, 16_000, 16_000, 0, 2, PARAMS);
        double gap = 325 - SaturationTable.temperature(50);
        assertEquals(0.3 * 2 * gap, result.heat(), 1.0e-9);
        assertEquals(50, result.pressure(), 1.0e-9);
    }

    @Test
    void cooledPrimaryAndSteamCarryTheSameHeat() {
        SteamGeneratorModel model = new SteamGeneratorModel();
        long cooled = 0;
        long steam = 0;
        double heat = 0;
        for (int t = 0; t < 500; t++) {
            SteamGeneratorModel.Result result = model.tick(8_000, 8_000, 16_000, 16_000, 0, 4, PARAMS);
            cooled += result.primaryCooled();
            steam += result.steamMade();
            heat += result.heat();
        }
        assertEquals(heat, cooled * 0.12, 0.12 + 1.0e-6);
        assertEquals(heat, steam * 0.225, 0.225 + 1.0e-6);
        assertTrue(steam < cooled, "a mB of primary carries less heat than a mB of steam takes");
    }

    @Test
    void moreTubesMoveMoreHeat() {
        double few = new SteamGeneratorModel().tick(8_000, 8_000, 16_000, 16_000, 0, 2, PARAMS).heat();
        double many = new SteamGeneratorModel().tick(8_000, 8_000, 16_000, 16_000, 0, 6, PARAMS).heat();
        assertEquals(3.0, many / few, 1.0e-9);
    }

    @Test
    void steamLeftInsideRaisesThePressureAndClosesTheGap() {
        SteamGeneratorModel.Result empty = new SteamGeneratorModel().tick(8_000, 8_000, 16_000, 16_000, 0.0, 4, PARAMS);
        SteamGeneratorModel.Result full = new SteamGeneratorModel().tick(8_000, 8_000, 16_000, 16_000, 1.0, 4, PARAMS);
        assertEquals(120, full.pressure(), 1.0e-9);
        assertTrue(full.heat() < empty.heat() * 0.05, "at 120 bar the secondary boils at the primary's temperature");
    }

    @Test
    void drysOutWithoutFeedwater() {
        SteamGeneratorModel model = new SteamGeneratorModel();
        SteamGeneratorModel.Result dry = model.tick(8_000, 8_000, 0, 16_000, 0, 4, PARAMS);
        assertEquals(0, dry.steamMade());
        assertEquals(0, dry.primaryCooled());
        assertEquals(0.0, dry.wetFraction(), 0.0);
        // Half the water the tubes need: half the heat.
        SteamGeneratorModel.Result half = new SteamGeneratorModel().tick(8_000, 8_000, 1_000, 16_000, 0, 4, PARAMS);
        SteamGeneratorModel.Result wet = new SteamGeneratorModel().tick(8_000, 8_000, 16_000, 16_000, 0, 4, PARAMS);
        assertEquals(0.5, half.wetFraction(), 1.0e-9);
        assertEquals(wet.heat() / 2, half.heat(), 1.0e-9);
    }

    @Test
    void neverMovesMoreThanItHas() {
        SteamGeneratorModel model = new SteamGeneratorModel();
        SteamGeneratorModel.Result result = model.tick(3, 8_000, 16_000, 16_000, 0, 6, PARAMS);
        assertTrue(result.primaryCooled() <= 3);
        assertEquals(3 * 0.12, result.heat(), 1.0e-9);
        SteamGeneratorModel.Result noRoom = new SteamGeneratorModel().tick(8_000, 0, 16_000, 16_000, 0, 6, PARAMS);
        assertEquals(0.0, noRoom.heat(), 0.0);
    }
}
