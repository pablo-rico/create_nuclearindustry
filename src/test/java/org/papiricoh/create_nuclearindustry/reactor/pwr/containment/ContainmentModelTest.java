package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainmentModelTest {
    private static final ContainmentModel.Params PARAMS = new ContainmentModel.Params(0.05, 0.008, 40.0, 0.002, 0.0002);
    private static final double STEAM_K = 400.0;

    /** A 10x10x10 room: 1000 m³ and 600 m² of wall. */
    private static ContainmentModel room() {
        return ContainmentModel.atmospheric(1000, 600);
    }

    @Test
    void startsAtTheOutsidePressure() {
        ContainmentModel model = room();
        assertEquals(ContainmentModel.ATMOSPHERE, model.pressure(), 1e-6);
        assertEquals(0.0, model.hydrogenFraction(), 0.0);
        assertEquals(0.21, model.oxygenFraction(), 1e-9);
    }

    @Test
    void steamRaisesThePressureAndTheWallsCondenseIt() {
        ContainmentModel model = room();
        model.addSteam(60_000, STEAM_K);
        double peak = model.pressure();
        assertTrue(peak > 2.0, "a big steam release pressurizes the room, got " + peak);
        for (int t = 0; t < 6_000; t++) {
            model.tick(0, 0, PARAMS);
        }
        assertTrue(model.pressure() < 1.2, "condensation brings it back down, got " + model.pressure());
        assertTrue(model.steamFraction() < 0.05, "the steam is gone, " + model.steamFraction());
    }

    @Test
    void moreWallCondensesFaster() {
        ContainmentModel small = ContainmentModel.atmospheric(1000, 300);
        ContainmentModel big = ContainmentModel.atmospheric(1000, 900);
        small.addSteam(40_000, STEAM_K);
        big.addSteam(40_000, STEAM_K);
        for (int t = 0; t < 200; t++) {
            small.tick(0, 0, PARAMS);
            big.tick(0, 0, PARAMS);
        }
        assertTrue(big.pressure() < small.pressure(), "more wall area, lower pressure: " + big.pressure() + " vs " + small.pressure());
    }

    @Test
    void hydrogenFractionAndFlammability() {
        ContainmentModel model = room();
        double air = model.totalMoles();
        model.addHydrogen(air * 0.03 / 0.97, ContainmentModel.WALL_TEMPERATURE);
        assertEquals(0.03, model.hydrogenFraction(), 1e-6);
        assertFalse(model.flammable(), "3% is below the lower flammability limit");
        model.addHydrogen(air * 0.03, ContainmentModel.WALL_TEMPERATURE);
        assertTrue(model.flammable(), "about 6% burns");
    }

    @Test
    void steamInertsTheMixture() {
        ContainmentModel model = room();
        double air = model.totalMoles();
        model.addHydrogen(air * 0.1, ContainmentModel.WALL_TEMPERATURE);
        model.addSteam(air * 2.0, ContainmentModel.WALL_TEMPERATURE);
        assertTrue(model.steamFraction() > ContainmentModel.STEAM_INERTING);
        assertFalse(model.flammable(), "with that much steam it cannot burn");
    }

    @Test
    void ignitionChanceFollowsTheLimits() {
        assertEquals(0.0, ContainmentModel.ignitionChance(0.03, 0.2), 0.0);
        assertEquals(0.2, ContainmentModel.ignitionChance(0.05, 0.2), 0.0);
        assertEquals(1.0, ContainmentModel.ignitionChance(0.08, 0.2), 0.0);
        assertEquals(1.0, ContainmentModel.ignitionChance(0.15, 0.0), 0.0);
    }

    @Test
    void aBurnSpikesThePressureAndConsumesTheHydrogen() {
        ContainmentModel model = room();
        double air = model.totalMoles();
        model.addHydrogen(air * 0.1 / 0.9, ContainmentModel.WALL_TEMPERATURE);
        double before = model.pressure();
        double rise = model.burn();
        assertTrue(rise > 2.0, "a 10% burn at least triples the pressure, rose " + rise + " from " + before);
        assertEquals(0.0, model.hydrogenFraction(), 1e-9, "all of it burnt: there is plenty of oxygen");
        assertTrue(model.oxygenFraction() < 0.21, "the burn used oxygen");
    }

    @Test
    void recombinersRemoveHydrogenWithoutASpike() {
        ContainmentModel model = room();
        double air = model.totalMoles();
        model.addHydrogen(air * 0.06, ContainmentModel.WALL_TEMPERATURE);
        double peak = 0;
        for (int t = 0; t < 20_000; t++) {
            model.tick(4, 0, PARAMS);
            peak = Math.max(peak, model.pressure());
        }
        assertTrue(model.hydrogenFraction() < 0.01, "recombiners take it below 1%, left " + model.hydrogenFraction());
        assertTrue(peak < 1.4, "slowly, without a big pressure spike: peak " + peak);
    }

    @Test
    void recombinersStopAtTraces() {
        ContainmentModel model = room();
        model.addHydrogen(model.totalMoles() * 0.003, ContainmentModel.WALL_TEMPERATURE);
        double before = model.hydrogen();
        model.tick(4, 0, PARAMS);
        assertEquals(before, model.hydrogen(), 1e-9);
    }

    @Test
    void ventsLetGasAndActivityOutDownToTheOutsidePressure() {
        ContainmentModel model = room();
        model.addSteam(80_000, STEAM_K);
        model.addActivity(50);
        double vented = 0;
        for (int t = 0; t < 4_000; t++) {
            vented += model.tick(0, 2, PARAMS).ventedActivity();
        }
        assertTrue(vented > 10, "activity left with the gas, " + vented);
        assertTrue(model.pressure() > 0.95 && model.pressure() < 1.1, "it vents down to about the outside pressure, " + model.pressure());
    }

    @Test
    void aVentNeverPullsTheRoomBelowTheOutsideInOneTick() {
        ContainmentModel model = room();
        model.addSteam(80_000, ContainmentModel.WALL_TEMPERATURE);
        ContainmentModel.Params noCondensation = new ContainmentModel.Params(0, 0, 0, 0.5, 0);
        for (int t = 0; t < 50; t++) {
            model.tick(10, 10, noCondensation);
            assertTrue(model.pressure() >= ContainmentModel.ATMOSPHERE - 1e-9, "got " + model.pressure());
        }
    }

    @Test
    void airSeepsBackIntoAnUnderpressuredRoom() {
        ContainmentModel model = room();
        model.addSteam(80_000, 450);
        double lowest = Double.MAX_VALUE;
        for (int t = 0; t < 30_000; t++) {
            model.tick(0, 1, new ContainmentModel.Params(0.05, 0.008, 0, t < 300 ? 0.01 : 0, 0));
            lowest = Math.min(lowest, model.pressure());
        }
        assertTrue(lowest < ContainmentModel.ATMOSPHERE, "venting hot steam then cooling leaves it below the outside, " + lowest);
        assertEquals(ContainmentModel.ATMOSPHERE, model.pressure(), 0.02, "air seeps back in");
    }

    @Test
    void closedVentsKeepEverythingIn() {
        ContainmentModel model = room();
        model.addSteam(30_000, STEAM_K);
        model.addActivity(10);
        double vented = 0;
        for (int t = 0; t < 100; t++) {
            vented += model.tick(0, 0, PARAMS).ventedActivity();
        }
        assertEquals(0.0, vented, 0.0);
    }

    @Test
    void depressurizingReleasesTheActivity() {
        ContainmentModel model = room();
        model.addSteam(50_000, STEAM_K);
        model.addActivity(42);
        double released = model.depressurize();
        assertEquals(42, released, 1e-9);
        assertEquals(ContainmentModel.ATMOSPHERE, model.pressure(), 1e-6);
    }

    @Test
    void aBiggerRoomTakesTheSameReleaseAtLowerPressure() {
        ContainmentModel small = ContainmentModel.atmospheric(500, 400);
        ContainmentModel big = ContainmentModel.atmospheric(5_000, 1_800);
        small.addSteam(30_000, STEAM_K);
        big.addSteam(30_000, STEAM_K);
        assertTrue(big.pressure() < small.pressure());
    }

    @Test
    void leakingThinsTheHydrogenByItsShare() {
        ContainmentModel model = room();
        model.addHydrogen(model.totalMoles() * 0.05, ContainmentModel.WALL_TEMPERATURE);
        double before = model.hydrogen();
        double excess = model.pressure() - ContainmentModel.ATMOSPHERE;
        model.leak(0.01);
        assertEquals(before * 0.99, model.hydrogen(), before * 1e-9);
        for (int t = 0; t < 2_000; t++) {
            model.leak(0.0005);
        }
        // 0.9995^2000 is about 0.37
        double kept = 0.99 * Math.pow(0.9995, 2_000);
        assertEquals(kept * before, model.hydrogen(), before * 1e-6);
        assertEquals(ContainmentModel.ATMOSPHERE + kept * excess, model.pressure(), 1e-4,
                "fresh air takes its place, at the outside pressure");
        assertTrue(model.oxygenFraction() > 0.2, "and brings oxygen back, " + model.oxygenFraction());
    }

    @Test
    void leakingRelievesOverpressureAndTakesTheActivity() {
        ContainmentModel model = room();
        model.addSteam(40_000, STEAM_K);
        model.addActivity(10.0);
        assertTrue(model.pressure() > 1.8);
        double leaked = model.leak(0.1);
        assertEquals(1.0, leaked, 1e-9, "a tenth of the activity goes with a tenth of the gas");
        assertEquals(9.0, model.activity(), 1e-9);
        for (int t = 0; t < 200; t++) {
            model.leak(0.05);
        }
        assertEquals(ContainmentModel.ATMOSPHERE, model.pressure(), 0.02, "the room settles at the outside pressure");
    }

    @Test
    void noLeakChangesNothing() {
        ContainmentModel model = room();
        model.addHydrogen(1_000, ContainmentModel.WALL_TEMPERATURE);
        double moles = model.totalMoles(), h2 = model.hydrogen();
        assertEquals(0.0, model.leak(0.0), 0.0);
        assertEquals(0.0, model.leak(Double.NaN), 0.0);
        assertEquals(moles, model.totalMoles(), 0.0);
        assertEquals(h2, model.hydrogen(), 0.0);
        model.leak(5.0);
        assertEquals(0.0, model.hydrogen(), 0.0, "a share above 1 swaps all the gas");
        assertEquals(ContainmentModel.ATMOSPHERE, model.pressure(), 1e-6);
    }
}
