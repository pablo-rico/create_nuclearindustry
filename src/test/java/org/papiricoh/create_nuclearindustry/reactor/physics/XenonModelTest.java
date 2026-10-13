package org.papiricoh.create_nuclearindustry.reactor.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XenonModelTest {
    private static final double IODINE_HALF_LIFE = 3600;
    private static final double XENON_HALF_LIFE = 5400;

    private static XenonModel runAt(double flux, int ticks) {
        XenonModel model = new XenonModel();
        for (int i = 0; i < ticks; i++) {
            model.tick(flux, IODINE_HALF_LIFE, XENON_HALF_LIFE);
        }
        return model;
    }

    @Test
    void cleanCoreHasNoPoison() {
        XenonModel model = runAt(0.0, 10_000);
        assertEquals(0.0, model.xenon());
        assertEquals(0.0, model.penalty(0.3));
    }

    @Test
    void steadyPowerSettlesWhereBurnoutBalancesProduction() {
        XenonModel model = runAt(1.0, 200_000);
        assertEquals(1.0, model.iodine(), 1e-6);
        assertEquals(1.0 / (1.0 + XenonModel.BURNOUT), model.xenon(), 1e-6);
    }

    @Test
    void xenonPeaksAfterShutdownThenDecays() {
        XenonModel model = runAt(0.75, 200_000);
        double steady = model.xenon();
        double peak = steady;
        int peakTick = 0;
        for (int tick = 1; tick <= 60_000; tick++) {
            model.tick(0.0, IODINE_HALF_LIFE, XENON_HALF_LIFE);
            if (model.xenon() > peak) {
                peak = model.xenon();
                peakTick = tick;
            }
        }
        assertTrue(peak > steady * 1.3, "the iodine pit should rise well above the running level: " + peak + " vs " + steady);
        assertTrue(peakTick > 1200 && peakTick < 12_000, "the peak should come minutes after the shutdown, came at " + peakTick);
        assertTrue(model.xenon() < steady * 0.2, "it should be mostly gone after 50 minutes, left " + model.xenon());
    }

    @Test
    void higherFluxBurnsXenonAway() {
        double low = runAt(0.25, 200_000).xenon() / 0.25;
        double high = runAt(1.0, 200_000).xenon() / 1.0;
        assertTrue(high < low, "per unit of flux, a hot core holds less xenon: " + high + " vs " + low);
    }

    @Test
    void penaltyIsCappedAtTheWorth() {
        XenonModel model = new XenonModel();
        model.set(1.0, 2.0);
        assertEquals(0.3, model.penalty(0.3), 1e-12);
        model.set(0.0, XenonModel.REFERENCE_XENON / 2);
        assertEquals(0.15, model.penalty(0.3), 1e-12);
    }
}
