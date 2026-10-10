package org.papiricoh.create_nuclearindustry.fusion.cryo;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel.Inputs;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel.Mode;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryoplantModelTest {
    /** The shipped defaults: 2 mB/t of liquid helium and 5 of liquid air per layer, 1% loop loss, +50% with nitrogen. */
    private static final CryoplantModel.Params PARAMS = new CryoplantModel.Params(2.0, 5.0, 0.01, 0.5, 1.0);
    private static final double MIN_SPEED = 32;
    private static final double RATED_SPEED = 128;
    private static final double STRESS_PER_LAYER = 10_240;

    private static Inputs helium(int layers, double speed, double warm, double space, double nitrogen) {
        return new Inputs(Mode.HELIUM, layers, speed, warm, space, nitrogen, 0);
    }

    @Test
    void speedFactorStartsAtTheMinimumAndCapsAtRated() {
        assertEquals(0.0, CryoplantModel.speedFactor(0, MIN_SPEED, RATED_SPEED));
        assertEquals(0.0, CryoplantModel.speedFactor(31.9, MIN_SPEED, RATED_SPEED), "below the minimum it does nothing");
        assertEquals(0.25, CryoplantModel.speedFactor(32, MIN_SPEED, RATED_SPEED), 1e-9);
        assertEquals(0.5, CryoplantModel.speedFactor(-64, MIN_SPEED, RATED_SPEED), 1e-9, "either direction turns it");
        assertEquals(1.0, CryoplantModel.speedFactor(128, MIN_SPEED, RATED_SPEED), 1e-9);
        assertEquals(1.0, CryoplantModel.speedFactor(256, MIN_SPEED, RATED_SPEED), 1e-9, "faster than rated gives no more");
    }

    @Test
    void outputGrowsWithLayersAndSpeed() {
        Result three = CryoplantModel.tick(helium(3, 1.0, 1000, 1000, 0), PARAMS);
        Result five = CryoplantModel.tick(helium(5, 1.0, 1000, 1000, 0), PARAMS);
        Result half = CryoplantModel.tick(helium(3, 0.5, 1000, 1000, 0), PARAMS);
        assertEquals(6.0, three.liquidHeliumMade(), 1e-9, "a 3x3x3 plant at rated speed makes 6 mB/t");
        assertEquals(10.0, five.liquidHeliumMade(), 1e-9, "a 3x3x5 plant makes 10 mB/t");
        assertEquals(3.0, half.liquidHeliumMade(), 1e-9, "half speed makes half");
        assertEquals(Result.IDLE, CryoplantModel.tick(helium(3, 0, 1000, 1000, 0), PARAMS), "stopped compressors make nothing");
    }

    @Test
    void theLoopLosesAPercentOfWhatPassesThrough() {
        Result result = CryoplantModel.tick(helium(3, 1.0, 1000, 1000, 0), PARAMS);
        assertEquals(6.0 / 0.99, result.heliumTaken(), 1e-9);
        assertEquals(result.heliumTaken() - result.liquidHeliumMade(), result.heliumLost(), 1e-12);
        assertEquals(0.01, result.heliumLost() / result.heliumTaken(), 1e-9);
    }

    @Test
    void neverMakesMoreThanItsInputOrRoomAllow() {
        Result starved = CryoplantModel.tick(helium(5, 1.0, 2.0, 1000, 0), PARAMS);
        assertEquals(2.0, starved.heliumTaken(), 1e-9, "it takes all the warm helium there is");
        assertEquals(1.98, starved.liquidHeliumMade(), 1e-9);
        Result full = CryoplantModel.tick(helium(5, 1.0, 1000, 1.5, 0), PARAMS);
        assertEquals(1.5, full.liquidHeliumMade(), 1e-9, "a nearly full tank takes only what fits");
        assertEquals(0.0, CryoplantModel.tick(helium(5, 1.0, 0, 1000, 0), PARAMS).liquidHeliumMade(), "no warm helium, no liquid");
    }

    @Test
    void nitrogenPrecoolingAddsHalfAndBoilsOffOnePerMb() {
        Result result = CryoplantModel.tick(helium(3, 1.0, 1000, 1000, 1000), PARAMS);
        assertEquals(9.0, result.liquidHeliumMade(), 1e-9, "+50% while pre-cooled");
        assertEquals(9.0, result.nitrogenUsed(), 1e-9, "1 mB of nitrogen per mB of liquid helium");
        assertEquals(1.0, result.precooling(), 1e-9);
    }

    @Test
    void runningOutOfNitrogenFadesTheBonusAndNeverOverdraws() {
        Result result = CryoplantModel.tick(helium(3, 1.0, 1000, 1000, 4.5), PARAMS);
        assertTrue(result.liquidHeliumMade() > 6.0 && result.liquidHeliumMade() < 9.0,
                "part of the bonus with half the nitrogen needed, got " + result.liquidHeliumMade());
        assertTrue(result.nitrogenUsed() <= 4.5 + 1e-12, "never boils off more nitrogen than there is");
        assertEquals(0.5, result.precooling(), 1e-9);
    }

    @Test
    void airModeMakesLiquidAirAndIgnoresHelium() {
        Result result = CryoplantModel.tick(new Inputs(Mode.AIR, 3, 1.0, 1000, 1000, 1000, 1000), PARAMS);
        assertEquals(15.0, result.liquidAirMade(), 1e-9, "5 mB/t per layer");
        assertEquals(0.0, result.heliumTaken());
        assertEquals(0.0, result.nitrogenUsed(), "pre-cooling is for helium only");
        Result full = CryoplantModel.tick(new Inputs(Mode.AIR, 3, 1.0, 0, 0, 0, 4), PARAMS);
        assertEquals(4.0, full.liquidAirMade(), 1e-9);
    }

    @Test
    void aSmallPlantKeepsAnR4TokamakColdForAQuarterOfItsTurbine() {
        // The coils of an R=4 tokamak boil about 4 mB/t of liquid helium at full field.
        double demand = 16 * 0.25;
        Result plant = CryoplantModel.tick(helium(3, CryoplantModel.speedFactor(128, MIN_SPEED, RATED_SPEED), 1000, 1000, 0), PARAMS);
        assertTrue(plant.liquidHeliumMade() >= demand, "3x3x3 at 128 RPM covers the R=4 coils");
        // Its compressors load the network with 3 layers' worth of stress; an R=4 tokamak's
        // plasma turbine gives about 129k SU per RPM.
        double share = 3 * STRESS_PER_LAYER / 129_000.0;
        assertTrue(share > 0.2 && share < 0.3, "the plant costs 20-30% of the turbine, got " + share);
    }
}
