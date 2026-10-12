package org.papiricoh.create_nuclearindustry.fusion.cryo;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.HeliumWellModel;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.HeliumWellModel.Step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeliumWellModelTest {
    /** The shipped default: 0.02 mB/t at rated speed. */
    private static final double RATE = 0.02;

    @Test
    void outputScalesWithSpeedAndCapsAtRated() {
        assertEquals(0.0, HeliumWellModel.output(RATE, 0));
        assertEquals(0.01, HeliumWellModel.output(RATE, 0.5), 1e-12);
        assertEquals(RATE, HeliumWellModel.output(RATE, 1.0), 1e-12);
        assertEquals(RATE, HeliumWellModel.output(RATE, 3.0), 1e-12, "faster than rated gives no more");
        assertEquals(0.0, HeliumWellModel.output(-1, 1.0), "a negative rate draws nothing");
        assertEquals(0.0, HeliumWellModel.output(Double.NaN, 1.0));
    }

    @Test
    void fractionsAddUpToWholeMillibuckets() {
        double balance = 0;
        int released = 0;
        for (int tick = 0; tick < 1_000; tick++) {
            Step step = HeliumWellModel.tick(balance, RATE, 1_000);
            balance = step.balance();
            released += step.released();
            assertTrue(balance >= 0 && balance < 1.0, "the balance stays below one mB, got " + balance);
        }
        // 1000 ticks at 0.02 mB/t is 20 mB; rounding may leave the last one in the balance.
        assertTrue(released == 20 || released == 19, "1000 ticks release about 20 mB, got " + released);
        assertEquals(20.0, released + balance, 1e-6, "nothing is lost to rounding");
    }

    @Test
    void aFullTankStopsTheWell() {
        Step step = HeliumWellModel.tick(0.5, RATE, 0);
        assertEquals(0, step.released());
        assertEquals(0.5, step.balance(), 1e-12, "with no room nothing accrues");
        for (int tick = 0; tick < 10_000; tick++) {
            step = HeliumWellModel.tick(step.balance(), RATE, 0);
        }
        assertEquals(0.5, step.balance(), 1e-12, "the balance does not grow while the tank is full");
    }

    @Test
    void releasesNoMoreThanFitsAndCapsTheBalance() {
        Step step = HeliumWellModel.tick(0.0, 5.5, 2);
        assertEquals(2, step.released(), "only what fits leaves");
        assertEquals(1.0, step.balance(), 1e-12, "the rest is capped at one mB");
        Step big = HeliumWellModel.tick(0.0, 5.5, 100);
        assertEquals(5, big.released());
        assertEquals(0.5, big.balance(), 1e-12);
    }

    @Test
    void badBalancesAreSanitised() {
        assertEquals(0.0, HeliumWellModel.tick(Double.NaN, 0, 10).balance());
        assertEquals(0.0, HeliumWellModel.tick(-3, 0, 10).balance());
        assertEquals(1.0, HeliumWellModel.tick(50, 0, 10).balance());
        assertEquals(0, HeliumWellModel.tick(0, Double.POSITIVE_INFINITY, 10).released());
    }

    @Test
    void perMinuteIsTwelveHundredTicks() {
        assertEquals(24.0, HeliumWellModel.perMinute(RATE), 1e-9);
    }
}
