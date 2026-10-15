package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cold feedwater in {@link CoreThermalModel}: what lets a shut-down core be fed and bled down below boiling. */
class CoreThermalFeedTest {
    private static final CoreThermalModel.Params T = CoreThermalModel.Params.DEFAULT;
    private static final double DT = 0.05;
    private static final int RODS = 6;
    /** Fifty mB a tick, the feed of a single mechanical pump at a modest speed, in mB per second. */
    private static final double FEED = 50 * 20;

    private static CoreThermalModel hotCore() {
        CoreThermalModel t = new CoreThermalModel();
        t.restore(285, 285);
        return t;
    }

    @Test
    void withoutFeedBoilingHoldsTheWaterAtSaturation() {
        CoreThermalModel t = hotCore();
        for (int i = 0; i < 20 * 300; i++) {
            t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6), RODS, 2.0, 0, DT);
        }
        assertEquals(T.boilingTemperature(), t.coolantTemperature(), 0.5, "boiling never cools below saturation");
        assertTrue(t.fuelTemperature() > T.boilingTemperature(), "and the decay heat keeps the fuel above it");
    }

    @Test
    void noFeedIsTheSameAsTheOldBoundary() {
        CoreThermalModel a = hotCore();
        CoreThermalModel b = hotCore();
        for (int i = 0; i < 2000; i++) {
            CoreThermalModel.Result ra = a.step(T, new CoreThermalModel.Boiling(0.8, 300, 400), RODS, 30.0, 0, DT);
            CoreThermalModel.Result rb = b.step(T, new CoreThermalModel.Boiling(0.8, 300, 400, 0), RODS, 30.0, 0, DT);
            assertEquals(ra, rb);
            assertEquals(0, ra.fed(), 0);
        }
        assertEquals(a.fuelTemperature(), b.fuelTemperature(), 0);
    }

    @Test
    void feedTakesFifteenDozenMegawattsFromAHotCore() {
        // 1000 mB/s x 0.0006 MJ/(mB K) x 265 K
        double sink = CoreThermalModel.feedSink(T, FEED, RODS, 285) * RODS;
        assertEquals(159, sink, 0.5);
        CoreThermalModel t = hotCore();
        CoreThermalModel.Result r = t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6, FEED), RODS, 2.0, 0, DT);
        assertEquals(159, r.fed(), 1);
        assertEquals(0, r.boiled(), 0, "below saturation nothing boils");
        assertEquals(r.fed(), r.removed(), 1.0e-9, "the feedwater's heat counts as removed");
    }

    @Test
    void feedingCoolsTheCoreBelowBoilingButNeverBelowItsOwnTemperature() {
        CoreThermalModel t = hotCore();
        double seconds = 0;
        double decayPerRod = 2.0;
        while (seconds < 600 && (t.fuelTemperature() >= 100 || t.coolantTemperature() >= 100)) {
            t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6, FEED), RODS, decayPerRod, 0, DT);
            seconds += DT;
        }
        assertTrue(seconds < 120, "fed and bled, a scrammed core is below 100 °C in under two minutes: " + seconds + " s");
        for (int i = 0; i < 20 * 600; i++) {
            t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6, FEED), RODS, 0, 0, DT);
        }
        assertEquals(CoreThermalModel.AMBIENT, t.coolantTemperature(), 0.5, "with no heat left it settles at the feed's temperature");
        assertTrue(t.coolantTemperature() >= CoreThermalModel.AMBIENT, "and never below it");
    }

    @Test
    void aSmallFeedHoldsACoreColdOnceItsDecayHeatHasFaded() {
        // Ten minutes after a SCRAM a six-rod core makes about 1.25 MW a rod.
        CoreThermalModel t = new CoreThermalModel();
        t.restore(60, 50);
        for (int i = 0; i < 20 * 900; i++) {
            t.step(T, new CoreThermalModel.Boiling(1, 1.0e6, 1.0e6, 12 * 20), RODS, 1.25, 0, DT);
        }
        assertTrue(t.coolantTemperature() < 100 && t.fuelTemperature() < 100, "12 mB/t keeps it cold: water "
                + t.coolantTemperature() + ", fuel " + t.fuelTemperature());
    }
}
