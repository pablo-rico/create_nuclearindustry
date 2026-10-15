package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Withdrawing rods as a real plant does: the shutdown banks come out quickly while the core is deep
 * below critical, and only the last {@code freeWithdrawalMargin} creeps up at the rod drives' speed.
 */
class RodWithdrawalTest {
    private static final double RATE = 100;
    private static final double MARGIN = 1000;
    private static final double TICK = FissionCore.TICK;

    @Test
    void insertingIsImmediate() {
        assertEquals(20000, FissionCore.withdraw(5000, 20000, 18000, RATE, MARGIN, TICK), 1e-9);
    }

    @Test
    void deepBelowCriticalTheRodsComeOutAtOnce() {
        // Rest of the core +18000; rods from 30000 to 22000 leave it 4000 short: still well shut down.
        assertEquals(22000, FissionCore.withdraw(30000, 22000, 18000, RATE, MARGIN, TICK), 1e-9);
    }

    @Test
    void aBigPullStopsShortOfCriticalAndThenCreeps() {
        // Pulling everything: free down to 1000 pcm short of critical, no further this tick.
        double first = FissionCore.withdraw(30000, 0, 18000, RATE, MARGIN, TICK);
        assertEquals(19000, first, 1e-9);
        double second = FissionCore.withdraw(first, 0, 18000, RATE, MARGIN, TICK);
        assertEquals(19000 - RATE * TICK, second, 1e-9);
    }

    @Test
    void nearCriticalEveryWithdrawalIsRateLimited() {
        // Already only 200 pcm short: the drives' speed applies.
        assertEquals(18200 - RATE * TICK, FissionCore.withdraw(18200, 17000, 18000, RATE, MARGIN, TICK), 1e-9);
    }

    @Test
    void noMarginRateLimitsEveryWithdrawal() {
        assertEquals(30000 - RATE * TICK, FissionCore.withdraw(30000, 0, 18000, RATE, 0, TICK), 1e-9);
    }

    @Test
    void theFirstTickTakesTheRodsWhereTheyStand() {
        assertEquals(7000, FissionCore.withdraw(Double.NaN, 7000, 18000, RATE, MARGIN, TICK), 1e-9);
    }

    /** The game's 6-rod test core with every rod in, then every rod pulled out by a piston at once. */
    @Test
    void aShutDownCoreReachesCriticalInSecondsButNeverJumpsPastIt() {
        FissionCore core = new FissionCore();
        double[] channels = {1.0};
        Runnable tick = () -> core.tick(FissionCore.Params.DEFAULT, new FissionCore.Inputs(6, true, 1.0, 650, 1.0, 0, 0, 0, 0, 0,
                ReactivityModel.Coolant.WATER, RodWorth.absorption(3, 6, 1.5, 1.0, 4.0), channels,
                new CoreThermalModel.Boiling(1, 600, 750), 0, 0.6, 0.002, 0));
        for (int i = 0; i < 20; i++) {
            tick.run();
        }
        assertTrue(core.reactivity() < -2000, "shut down with every rod in: " + core.reactivity());

        Arrays.fill(channels, 0.0);
        tick.run();
        double afterPull = core.reactivity();
        assertTrue(afterPull > -1100 && afterPull < -900, "the pull frees all but the last 1000 pcm at once: " + afterPull);
        for (int i = 0; i < 20; i++) {
            tick.run();
        }
        double creep = core.reactivity() - afterPull;
        assertTrue(creep <= RATE + 1, "then it creeps at the drives' speed: +" + creep + " pcm in a second");
    }
}
