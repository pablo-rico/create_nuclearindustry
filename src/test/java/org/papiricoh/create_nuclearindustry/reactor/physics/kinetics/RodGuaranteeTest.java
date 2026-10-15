package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.ThoriumBreeding;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rod guarantee: control rods inserted at least equal to the fuel blocks (counted along their S
 * curve, {@link RodWorth#effectiveRods}) hold any fuel subcritical, cold and without xenon, in the
 * most reactive core the game allows: graphite round every fuel column, a full beryllium reflector
 * and heavy water. With the default boron effectiveness (1.5) one rod block per fuel block is worth
 * 1.5 rod banks (33000 pcm) for uranium, which beats 90% uranium (about 27500 pcm with everything
 * that adds to it) by 5500 pcm.
 */
class RodGuaranteeTest {
    private static final ReactivityModel.Params R = ReactivityModel.Params.DEFAULT;
    private static final double BORON_ABSORPTION = 1.5;
    private static final double CAP = 4.0;
    private static final double SHAPE = R.rodWorthShape();
    /** Margin the guarantee must leave in the worst core, pcm. */
    private static final double MINIMUM = 2000;

    /** Fresh fuel's reactivity relative to 5% uranium, as the simulator rates an assembly of it. */
    private static double reactivityFor(double enrichment) {
        return Math.max(0.25, 1.0 + 0.25 * (Math.log(Math.max(3.0, enrichment) / 5.0) / Math.log(2.0)));
    }

    private record Fuel(String name, double reactivity, double noDefect, double controlWorth) {}

    private static final Fuel[] WORST = {
            new Fuel("90% uranium", reactivityFor(90), 0, FuelKind.URANIUM.controlWorth()),
            new Fuel("20% uranium", reactivityFor(20), 0, FuelKind.URANIUM.controlWorth()),
            new Fuel("MOX", reactivityFor(FuelKind.MOX_EQUIVALENT_ENRICHMENT) * FuelKind.MOX.reactivityBonus(), 0,
                    FuelKind.MOX.controlWorth()),
            // Thorium at the peak of its breeding, its most reactive point.
            new Fuel("thorium", reactivityFor(FuelKind.THORIUM_EQUIVALENT_ENRICHMENT) * FuelKind.THORIUM.reactivityBonus()
                    * ThoriumBreeding.reactivityMultiplier(ThoriumBreeding.peakBurnFraction()), 1, FuelKind.THORIUM.controlWorth()),
    };

    /** Cold, xenon-free shutdown margin of a fully moderated, reflected, heavy-water core with these rods. */
    private static double coldMargin(Fuel fuel, int fuelRods, int rodCapacity, double insertion) {
        double absorption = RodWorth.absorption(rodCapacity, fuelRods, BORON_ABSORPTION, fuel.controlWorth(), CAP);
        double[] channels = new double[Math.max(1, rodCapacity / 3)];
        Arrays.fill(channels, insertion);
        FissionCore core = new FissionCore();
        FissionCore.Params p = new FissionCore.Params(PointKinetics.Params.DEFAULT, R, CoreThermalModel.Params.DEFAULT,
                CoreDamageModel.Params.DEFAULT, true, 1.0, 100.0, 2800.0, false, 3600.0, 5400.0, 0.07, 20000.0);
        core.tick(p, new FissionCore.Inputs(fuelRods, true, 1.0, 650, fuel.reactivity(), 0, fuel.noDefect(), 0, 1.0, 1.0,
                ReactivityModel.Coolant.HEAVY_WATER, absorption, channels, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6), 0, 0.6,
                0.002));
        return core.coldShutdownMargin(false);
    }

    /** Insertion of every channel at which {@code capacity} rod blocks are worth {@code fuelRods} effective rods. */
    private static double insertionFor(int fuelRods, int capacity) {
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (RodWorth.effectiveRods(new double[] {mid}, capacity, SHAPE) < fuelRods) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }

    @Test
    void asManyRodsInAsFuelHoldTheWorstCoreSubcritical() {
        for (Fuel fuel : WORST) {
            double margin = coldMargin(fuel, 12, 12, 1.0);
            assertTrue(margin >= MINIMUM, fuel.name() + ": rods in equal to the fuel leave " + margin + " pcm");
        }
    }

    @Test
    void theWorstCaseMarginsMatchTheTable() {
        // 33000 cw - (fuel + 1500 graphite + 1000 reflector + 500 heavy water)
        assertEquals(5490, coldMargin(WORST[0], 12, 12, 1.0), 20);
        assertEquals(12000, coldMargin(WORST[1], 12, 12, 1.0), 20);
        assertEquals(9330, coldMargin(WORST[2], 12, 12, 1.0), 20);
        assertEquals(16410, coldMargin(WORST[3], 12, 12, 1.0), 20);
    }

    @Test
    void moreRodsPartlyInCountTheSameAsFewerAllTheWayIn() {
        int fuel = 12;
        for (int capacity : new int[] {12, 15, 18, 24, 30}) {
            double insertion = insertionFor(fuel, capacity);
            assertEquals(fuel, RodWorth.effectiveRods(new double[] {insertion, insertion}, capacity, SHAPE), 1.0e-6);
            for (Fuel f : WORST) {
                double margin = coldMargin(f, fuel, capacity, insertion);
                assertTrue(margin >= MINIMUM, f.name() + " with " + capacity + " rod blocks at " + insertion + ": " + margin + " pcm");
            }
        }
    }

    @Test
    void fewerRodsThanFuelDoNotGuaranteeIt() {
        // Half as many rod blocks as fuel cannot hold 90% uranium with graphite, beryllium and heavy water.
        assertTrue(coldMargin(WORST[0], 12, 6, 1.0) < 0, "a weak bank leaves the richest core critical when cold");
    }

    @Test
    void absorptionIsNoLongerCappedAtOneBank() {
        assertEquals(1.5, RodWorth.absorption(12, 12, BORON_ABSORPTION, 1.0, CAP), 1.0e-9);
        assertEquals(0.75, RodWorth.absorption(6, 12, BORON_ABSORPTION, 1.0, CAP), 1.0e-9);
        assertEquals(CAP, RodWorth.absorption(48, 12, BORON_ABSORPTION, 1.0, CAP), 1.0e-9, "only the guard caps it");
        assertEquals(0, RodWorth.absorption(0, 12, BORON_ABSORPTION, 1.0, CAP), 0);
        assertEquals(0, RodWorth.absorption(12, 0, BORON_ABSORPTION, 1.0, CAP), 0);
        assertEquals(1.5 * 22000, ReactivityModel.rodBank(R, 1.5), 1.0e-6);
    }

    @Test
    void effectiveRodsFollowTheSCurve() {
        assertEquals(12, RodWorth.effectiveRods(new double[] {1, 1, 1, 1}, 12, SHAPE), 1.0e-9);
        assertEquals(0, RodWorth.effectiveRods(new double[] {0, 0}, 12, SHAPE), 1.0e-9);
        assertEquals(6, RodWorth.effectiveRods(new double[] {0.5, 0.5}, 12, SHAPE), 1.0e-9, "half in is half the worth");
        assertEquals(6, RodWorth.effectiveRods(new double[] {1, 0}, 12, SHAPE), 1.0e-9);
        assertTrue(RodWorth.effectiveRods(new double[] {0.25}, 12, SHAPE) < 3, "the ends of a rod are worth less");
    }
}
