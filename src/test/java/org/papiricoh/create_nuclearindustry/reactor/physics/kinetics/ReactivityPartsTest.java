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
    void thePartsGroupTheBreakdownAndAddUpToTheTotal() {
        ReactivityModel.Breakdown b = new ReactivityModel.Breakdown(12000, 1500, 1000, 500, -1400, -9000, -1700, -1060, -450, -200);
        ReactivityModel.Parts parts = b.parts();
        assertEquals(15000, parts.fuel(), 1e-9);
        assertEquals(-9000, parts.rods(), 1e-9);
        assertEquals(-2760, parts.temperature(), 1e-9);
        assertEquals(-450, parts.voids(), 1e-9);
        assertEquals(-1400, parts.xenon(), 1e-9);
        assertEquals(-200, parts.damage(), 1e-9);
        assertEquals(b.total(), parts.total(), 1e-9);
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
        assertEquals(fresh * (1 - R.burnupDefect()), burnt, 1e-9);
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
    void theWaterStopsAtItsSuperheatCeilingAndTheFuelKeepsTheRest() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(600, 285);
        CoreThermalModel.Boiling blocked = new CoreThermalModel.Boiling(1, 0, 1.0e6);
        double ceiling = T.boilingTemperature() + T.maxSuperheat();
        for (int i = 0; i < 20 * 10; i++) {
            t.step(T, blocked, 1, 10, 0, 0.05);
        }
        assertEquals(ceiling, t.coolantTemperature(), 0.01, "the water is driven no further than its ceiling");
        double fuel = t.fuelTemperature();
        CoreThermalModel.Result r = CoreThermalModel.Result.NONE;
        for (int i = 0; i < 20; i++) {
            r = t.step(T, blocked, 1, T.ratedPowerPerRod(), 0, 0.05);
        }
        double rise = t.fuelTemperature() - fuel;
        double allOfIt = T.ratedPowerPerRod() / T.fuelHeatCapacity();
        assertTrue(rise > 0.9 * allOfIt && rise <= allOfIt, "every bit of heat then stays in the fuel (less what it radiates): "
                + rise + " of " + allOfIt + " K in a second");
        assertEquals(1, r.voidFraction(), 0.05, "and the core is all steam");
    }

    @Test
    void voidsFollowTheirTargetWithALag() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(885, 285);
        CoreThermalModel.Boiling open = new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6);
        CoreThermalModel.Result r = CoreThermalModel.Result.NONE;
        for (int i = 0; i < 20 * 60; i++) {
            r = t.step(T, open, 1, T.ratedPowerPerRod(), 0, 0.05);
        }
        assertEquals(T.ratedVoid(), r.voidFraction(), 0.02, "boiling off rated power: the rated void");
        // The outlet shuts for one tick: the void barely moves, instead of jumping to the flashing water's.
        CoreThermalModel.Boiling shut = new CoreThermalModel.Boiling(1, 0, 1.0e6);
        CoreThermalModel.Result shutTick = t.step(T, shut, 1, T.ratedPowerPerRod(), 0, 0.05);
        assertTrue(Math.abs(shutTick.voidFraction() - r.voidFraction()) < 0.03, "one tick moves it little: " + shutTick.voidFraction());
        // Held shut, it gets there within a few time constants.
        CoreThermalModel.Result held = shutTick;
        for (int i = 0; i < 20 * 5; i++) {
            held = t.step(T, shut, 1, T.ratedPowerPerRod(), 0, 0.05);
        }
        assertTrue(held.voidFraction() > 0.5, "held shut, the flashing water fills the core with steam: " + held.voidFraction());
    }

    @Test
    void aRestoredCoreKeepsItsVoidThroughItsFirstStep() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(885, 285);
        CoreThermalModel.Boiling half = new CoreThermalModel.Boiling(0.5, 1.0e6, 1.0e6);
        CoreThermalModel.Result r = CoreThermalModel.Result.NONE;
        for (int i = 0; i < 20 * 60; i++) {
            r = t.step(T, half, 1, T.ratedPowerPerRod(), 0, 0.05);
        }
        double voids = r.voidFraction();
        assertTrue(voids > 0.5 && t.boilingVoid() > 0 && t.boilingVoid() < voids, "half uncovered and boiling: " + voids);
        // Saved and loaded: the whole core's void and the bubbles are restored apart, so the first step
        // after loading reads the same void and the power does not bump.
        CoreThermalModel loaded = new CoreThermalModel();
        loaded.restore(t.fuelTemperature(), t.coolantTemperature(), voids, t.boilingVoid());
        assertEquals(voids, loaded.voidFraction(), 1e-12, "restored as saved");
        CoreThermalModel.Result first = loaded.step(T, half, 1, T.ratedPowerPerRod(), 0, 0.05);
        assertEquals(voids, first.voidFraction(), 0.005, "and the first step does not move it: " + first.voidFraction());
        // The core's own reading is the thermal model's, restored with it.
        FissionCore core = new FissionCore();
        core.thermal().restore(885, 285, 0.3, 0.3);
        assertEquals(0.3, core.voidFraction(), 1e-12, "the core reads the restored void before its first tick");
    }

    @Test
    void heatPastTheWaterCeilingGoesToTheFuelNotNowhere() {
        CoreThermalModel t = new CoreThermalModel();
        // A save from the old model: water at 400 C, past the 335 C ceiling, with the outlet shut.
        t.restore(600, 400);
        CoreThermalModel.Boiling shut = new CoreThermalModel.Boiling(1, 0, 1.0e6);
        double fuelEnergy = t.fuelTemperature() * T.fuelHeatCapacity();
        double waterEnergy = t.coolantTemperature() * (T.coolantHeatCapacity() + T.structureHeatCapacity());
        t.step(T, shut, 1, 0, 0, 0.05);
        double after = t.fuelTemperature() * T.fuelHeatCapacity() + t.coolantTemperature() * (T.coolantHeatCapacity() + T.structureHeatCapacity());
        assertEquals(T.boilingTemperature() + T.maxSuperheat(), t.coolantTemperature(), 1e-9, "the water drops to its ceiling");
        assertEquals(fuelEnergy + waterEnergy, after, 0.5, "and what it could not hold is in the fuel (less a little radiation)");
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
        assertEquals(0, CoreDamageModel.rate(D, D.claddingFailure() - 100), 1e-12);
        assertEquals(D.claddingRate(), CoreDamageModel.rate(D, D.claddingFailure() + 800), 1e-9);
        assertTrue(CoreDamageModel.rate(D, D.fuelMelting() + 400) > 5 * CoreDamageModel.rate(D, D.fuelMelting() - 100),
                "molten fuel slumps fast");
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
