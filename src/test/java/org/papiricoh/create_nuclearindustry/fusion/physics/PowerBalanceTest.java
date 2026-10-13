package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Whether a tokamak plant makes or loses Create stress, at the mod's balance numbers. */
class PowerBalanceTest {
    private static final PowerBalance.Params P = PowerBalance.Params.DEFAULT;
    /** An R=4 plant: one pump, one magnet drive, coils using 4 mB/t of liquid helium at full field. */
    private static final double R4_HELIUM = 4.0;

    private static PowerBalance.Plant r4(double fusionPower, double heating) {
        return new PowerBalance.Plant(fusionPower, heating, 0, R4_HELIUM, 1, 1);
    }

    @Test
    void heatingAlonePaysForItselfNearQThree() {
        double breakEven = PowerBalance.heatingBreakEvenGain(P);
        assertEquals(3.1, breakEven, 0.1);
        assertTrue(PowerBalance.turbineCapacity(P, 3.5 * 40) > PowerBalance.heatingCost(P, 40, 0));
        assertTrue(PowerBalance.turbineCapacity(P, 2.5 * 40) < PowerBalance.heatingCost(P, 40, 0));
    }

    @Test
    void aSubIgnitedR4GainsAtTheTargetQAndLosesAtQOne() {
        assertTrue(PowerBalance.net(P, r4(6 * 40, 40)) > 0, "Q=6 with two beams should be net positive");
        assertTrue(PowerBalance.net(P, r4(8 * 40, 40)) > PowerBalance.net(P, r4(6 * 40, 40)));
        assertTrue(PowerBalance.net(P, r4(40, 40)) < 0, "Q=1 must lose energy");
        assertTrue(PowerBalance.net(P, r4(3 * 40, 40)) < 0, "an R=4 plant needs Q above about 4 once cryogenics count");
    }

    @Test
    void anIgnitedPlasmaNeedsNoHeatingAndNetsFarMore() {
        double ignited = PowerBalance.net(P, r4(280, 0));
        double driven = PowerBalance.net(P, r4(6 * 40, 40));
        assertTrue(ignited > 2 * driven, "ignited " + ignited + " vs driven " + driven);
    }

    @Test
    void cryogenicsCostAQuarterOfAnR4AndLessForBiggerTokamaks() {
        double share4 = R4_HELIUM * P.cryoStressPerHelium() / PowerBalance.turbineCapacity(P, 175);
        assertTrue(share4 > 0.15 && share4 < 0.3, "R=4 cryogenics take " + share4 + " of the turbine");
        // An R=8 tokamak has twice the sectors and about twice the fusion power at the same density...
        double share8 = 2 * R4_HELIUM * P.cryoStressPerHelium() / PowerBalance.turbineCapacity(P, 2 * 175 * 1.1);
        assertTrue(share8 < share4, "...and its better confinement makes the cryogenics a smaller share");
    }

    @Test
    void theModelledR4WithTwoBeamsIsNetPositive() {
        PlasmaModel.Params params = PlasmaModel.Params.DEFAULT;
        int volume = TokamakGeometry.of(4).plasmaCount();
        double fuel = PlasmaModel.fuelRateFor(params, volume, 0.7);
        PlasmaModel model = new PlasmaModel();
        model.breakdown(10);
        for (int tick = 0; tick < 3600; tick++) {
            model.tick(params, new PlasmaModel.Inputs(volume, 4, 1.0, 1.0, 0.4, fuel / 2, fuel / 2, true, 40, 0.8, 0,
                    1e9, 1e9, false));
        }
        double net = PowerBalance.net(P, r4(model.getFusionPower(), 40));
        assertTrue(model.gain() > 3, "Q " + model.gain());
        assertTrue(net > 0, "a default R=4 with two beams should make a small net gain, got " + net + " SU/RPM at "
                + model.getFusionPower() + " MW");
    }
}
