package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rod worth curve, reactivity terms, the thermal nodes and core damage. */
class ReactivityPartsTest {
    private static final ReactivityModel.Params R = ReactivityModel.Params.DEFAULT;
    private static final CoreThermalModel.Params T = CoreThermalModel.Params.DEFAULT;
    private static final CoreDamageModel.Params D = CoreDamageModel.Params.DEFAULT;

    // ------------------------------------------------------------------ rods

    @Test
    void theRodCurveRunsFromNothingToAllAndIsSteepestInTheMiddle() {
        assertEquals(0, RodWorth.curve(0, 0.5), 1e-12);
        assertEquals(1, RodWorth.curve(1, 0.5), 1e-12);
        double previous = 0;
        double steepest = 0;
        double steepestAt = 0;
        for (int i = 1; i <= 100; i++) {
            double x = i / 100.0;
            double y = RodWorth.curve(x, 0.5);
            assertTrue(y >= previous, "monotonic at " + x);
            if (y - previous > steepest) {
                steepest = y - previous;
                steepestAt = x;
            }
            previous = y;
        }
        assertEquals(0.5, steepestAt, 0.05, "the middle block is worth the most");
        assertEquals(6000, RodWorth.bank(new double[]{1, 0}, 12000, 0.5), 1e-9, "two channels share the worth");
    }

    // ------------------------------------------------------------------ reactivity

    private static ReactivityModel.Inputs cold(double fuel, double moderation, double reflection, double rods) {
        return new ReactivityModel.Inputs(fuel, 0, 0, 0, moderation, reflection, ReactivityModel.Coolant.WATER, 0, rods, 20, 20, 0, 1, 0,
                 0.6, 0.002);
    }

    @Test
    void fullRodsShutDownEvenTheMostReactiveCore() {
        // Highly enriched fuel with full graphite and a reflector, cold and clean: the rods still win.
        double rods = ReactivityModel.rodBank(R, 1.0);
        ReactivityModel.Breakdown b = ReactivityModel.compute(R, cold(1.5, 1, 1, rods));
        assertTrue(b.total() < 0, "subcritical with every rod in: " + b.total());
        assertTrue(b.withoutRods() > 0, "and critical without them");
    }

    @Test
    void graphiteAndAReflectorAddReactivity() {
        double bare = ReactivityModel.compute(R, cold(1, 0, 0, 0)).total();
        assertEquals(1500, ReactivityModel.compute(R, cold(1, 1, 0, 0)).total() - bare, 1e-9);
        assertEquals(1000, ReactivityModel.compute(R, cold(1, 0, 1, 0)).total() - bare, 1e-9);
    }

    @Test
    void heatAndVoidsTakeReactivityAwayInWaterButVoidsAddItWithGraphite() {
        ReactivityModel.Inputs hot = new ReactivityModel.Inputs(1, 0, 0, 0, 0, 0, ReactivityModel.Coolant.WATER, 0, 0, 820, 285, 0.3, 1, 0,
                 0.6, 0.002);
        ReactivityModel.Breakdown b = ReactivityModel.compute(R, hot);
        assertEquals(-1600, b.doppler(), 1e-9, "-2 pcm/K on 800 K");
        assertEquals(-1060, b.moderator(), 1e-9);
        assertEquals(-450, b.voids(), 1e-9, "-15 pcm per percent of 30%");
        ReactivityModel.Inputs graphite = new ReactivityModel.Inputs(1, 0, 0, 0, 1, 0, ReactivityModel.Coolant.WATER, 0, 0, 20, 20, 0.3, 1, 0,
                 0.6, 0.002);
        assertTrue(ReactivityModel.compute(R, graphite).voids() > 0, "graphite cores have a positive void coefficient");
    }

    @Test
    void burnupAndEmptyChannelsCostReactivityButThoriumBreeds() {
        double fresh = ReactivityModel.compute(R, cold(1, 0, 0, 0)).fuel();
        double burnt = ReactivityModel.compute(R, new ReactivityModel.Inputs(1, 1, 0, 0, 0, 0, ReactivityModel.Coolant.WATER, 0, 0,
                20, 20, 0, 1, 0,  0.6, 0.002)).fuel();
        double breeder = ReactivityModel.compute(R, new ReactivityModel.Inputs(1, 1, 1, 0, 0, 0, ReactivityModel.Coolant.WATER, 0, 0,
                20, 20, 0, 1, 0,  0.6, 0.002)).fuel();
        double empty = ReactivityModel.compute(R, new ReactivityModel.Inputs(1, 0, 0, 0.5, 0, 0, ReactivityModel.Coolant.WATER, 0, 0,
                20, 20, 0, 1, 0,  0.6, 0.002)).fuel();
        assertEquals(fresh / 2, burnt, 1e-9);
        assertEquals(fresh, breeder, 1e-9);
        assertTrue(empty <= 0, "a half empty core cannot go critical: " + empty);
    }

    @Test
    void thePressurizedLoopKeepsItsCoefficients() {
        ReactivityModel.Inputs pwr = new ReactivityModel.Inputs(1, 0, 0, 0, 0, 0, ReactivityModel.Coolant.PRESSURIZED, 0, 0, 20, 30, 0.1,
                1, 0, 0.6, 0.002);
        ReactivityModel.Breakdown b = ReactivityModel.compute(R, pwr);
        assertEquals(-40, b.moderator(), 1e-9);
        assertEquals(-150, b.voids(), 1e-9);
    }

    // ------------------------------------------------------------------ thermal

    @Test
    void ratedPowerRunsTheFuelItsRatedRiseAboveBoilingWater() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(285, 285);
        CoreThermalModel.Boiling water = new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6);
        CoreThermalModel.Result r = CoreThermalModel.Result.NONE;
        for (int i = 0; i < 20 * 120; i++) {
            r = t.step(T, water, 10, T.ratedPowerPerRod(), 0, 0.05);
        }
        assertEquals(285, t.coolantTemperature(), 0.5, "the water stays at saturation");
        assertEquals(285 + T.fuelRiseAtRated(), t.fuelTemperature(), 25);
        assertEquals(10 * T.ratedPowerPerRod(), r.boiled(), 15, "what goes in boils off");
        assertEquals(T.ratedVoid(), r.voidFraction(), 0.02);
    }

    @Test
    void heatThatCannotLeaveWarmsTheWaterGradually() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(600, 285);
        CoreThermalModel.Boiling blocked = new CoreThermalModel.Boiling(1, 0, 1.0e6);
        for (int i = 0; i < 20; i++) {
            t.step(T, blocked, 1, T.ratedPowerPerRod(), 0, 0.05);
        }
        assertTrue(t.coolantTemperature() > 285 && t.coolantTemperature() < 310, "a second later: " + t.coolantTemperature());
    }

    @Test
    void aDryCorePassesLittleHeat() {
        CoreThermalModel wet = new CoreThermalModel();
        CoreThermalModel dry = new CoreThermalModel();
        wet.restore(800, 285);
        dry.restore(800, 285);
        for (int i = 0; i < 200; i++) {
            wet.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6), 1, 10, 0, 0.05);
            dry.step(T, new CoreThermalModel.Dry(), 1, 10, 0, 0.05);
        }
        assertTrue(dry.fuelTemperature() > wet.fuelTemperature() + 100, "uncovered fuel heats up: " + dry.fuelTemperature());
    }

    @Test
    void theCriticalHeatFluxCapsTheTransfer() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(2000, 285);
        CoreThermalModel.Result r = t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 10), 1, 0, 0, 0.05);
        double unlimited = T.conductance() * (2000 - 285);
        assertTrue(r.boiled() < 0.2 * unlimited, "film boiling: " + r.boiled() + " of " + unlimited);
    }

    // ------------------------------------------------------------------ damage

    @Test
    void damageStartsWithTheCladdingAndRunsAwayAsTheFuelMelts() {
        assertEquals(0, CoreDamageModel.rate(D, 1900), 1e-12);
        assertEquals(D.claddingRate(), CoreDamageModel.rate(D, 2800), 1e-9);
        assertTrue(CoreDamageModel.rate(D, 3200) > 5 * CoreDamageModel.rate(D, 2700), "molten fuel slumps fast");
        CoreDamageModel damage = new CoreDamageModel();
        int seconds = 0;
        while (!damage.meltedDown(D) && seconds < 600) {
            damage.step(D, 4000, 1);
            seconds++;
        }
        assertTrue(seconds < 15, "a core held at 4000 C melts down within seconds: " + seconds);
        damage.refuel(0.5);
        assertTrue(damage.damage() > 0.37 && damage.damage() < 0.45, "fresh fuel replaces the damaged: " + damage.damage());
    }
}
