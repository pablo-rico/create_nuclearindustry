package org.papiricoh.create_nuclearindustry.propulsion.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.propulsion.physics.AntimatterRocketModel.Params;
import org.papiricoh.create_nuclearindustry.propulsion.physics.AntimatterRocketModel.Step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The antimatter engine: feed, coils, energy and confinement. */
class AntimatterRocketModelTest {
    private static final Params P = Params.DEFAULT;

    @Test
    void moreCoilsCatchMoreOfIt() {
        assertEquals(0.5, AntimatterRocketModel.efficiency(2), 1.0e-9);
        assertTrue(AntimatterRocketModel.efficiency(6) > 0.85);
        assertEquals(0, AntimatterRocketModel.efficiency(0));
        double two = AntimatterRocketModel.tick(1, 2, 10, 1_000_000, 20, 0, P).thrust();
        double six = AntimatterRocketModel.tick(1, 6, 10, 1_000_000, 20, 0, P).thrust();
        assertTrue(six > two);
    }

    @Test
    void itFarOutpullsTheThermalEngine() {
        double antimatter = AntimatterRocketModel.tick(1, 6, 10, 1_000_000, 20, 0, P).thrust();
        double thermal = ThermalRocketModel.tick(1, 8, ThermalRocketModel.Propellant.HYDROGEN, 1000, 1000, 1000, 20, 0,
                ThermalRocketModel.Params.DEFAULT).thrust();
        assertTrue(antimatter > 3 * thermal, antimatter + " vs " + thermal);
    }

    @Test
    void itFeedsAtTheThrottleAndNeverMoreThanItHolds() {
        assertEquals(P.maxFeed() / 2, AntimatterRocketModel.tick(0.5, 4, 10, 1_000_000, 20, 0, P).fed(), 1.0e-12);
        assertEquals(0.001, AntimatterRocketModel.tick(1, 4, 0.001, 1_000_000, 20, 0, P).fed(), 1.0e-12);
        assertEquals(0, AntimatterRocketModel.tick(1, 4, 0, 1_000_000, 20, 0, P).thrust());
    }

    @Test
    void withoutEnergyTheFieldFails() {
        Step starved = AntimatterRocketModel.tick(1, 4, 10, 10, 20, 0, P);
        assertFalse(starved.confined());
        assertEquals(0, starved.fed(), "nothing is fed while the field is down");
        Step idle = AntimatterRocketModel.tick(0, 4, 10, AntimatterRocketModel.energyNeed(4, 0, P), 20, 0, P);
        assertTrue(idle.confined(), "idle, the coils' own share holds it");
    }

    @Test
    void tooFewCoilsOverheatAndWear() {
        double coil = 20;
        double wear = 0;
        double hottest = 0;
        boolean lost = false;
        for (int tick = 0; tick < 4000 && !lost; tick++) {
            Step step = AntimatterRocketModel.tick(1, 2, 10, 1_000_000, coil, wear, P);
            coil = step.coil();
            wear = step.wear();
            hottest = Math.max(hottest, coil);
            lost = !step.confined();
        }
        assertTrue(hottest > P.coilLimit() && wear >= 1.0, "two coils at full feed overheat and wear through: " + hottest);
        assertFalse(AntimatterRocketModel.tick(1, 2, 10, 1_000_000, coil, wear, P).confined(), "worn through, the field fails");
        coil = 20;
        wear = 0;
        for (int tick = 0; tick < 2000; tick++) {
            Step step = AntimatterRocketModel.tick(1, 6, 10, 1_000_000, coil, wear, P);
            coil = step.coil();
            wear = step.wear();
        }
        assertTrue(coil < P.coilLimit() && wear == 0, "six stay cool: " + coil);
    }

    @Test
    void whatTheCoilsMissLeavesAsGamma() {
        double two = AntimatterRocketModel.tick(1, 2, 10, 1_000_000, 20, 0, P).gamma();
        double six = AntimatterRocketModel.tick(1, 6, 10, 1_000_000, 20, 0, P).gamma();
        assertTrue(two > six && six > 0);
        assertEquals(0, AntimatterRocketModel.tick(0, 6, 10, 1_000_000, 20, 0, P).gamma());
    }
}
