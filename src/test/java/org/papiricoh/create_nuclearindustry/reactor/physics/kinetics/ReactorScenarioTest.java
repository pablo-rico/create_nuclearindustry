package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whole-core scenarios with the default numbers: a core built like the game's, cooled by boiling
 * water through its heat exchangers and ports. These pin what the redesign is for: withdrawing a rod
 * gives a new, steady power instead of a runaway, all rods out of a uranium core stays below melting,
 * losing the coolant damages it, and a big core behaves like a small one.
 */
class ReactorScenarioTest {
    private static final FissionCore.Params D = FissionCore.Params.DEFAULT;
    /** Without xenon, so steady states stay steady over the minutes these runs take. */
    private static final FissionCore.Params P = new FissionCore.Params(D.kinetics(), D.reactivity(), D.thermal(), D.damage(),
            D.sixGroups(), D.delayedTimeScale(), D.maxWithdrawalRate(), D.xenonWorth(), false, D.iodineHalfLife(),
            D.xenonHalfLife(), D.decayFraction(), D.decayTime());
    private static final double STEAM_MW_PER_MB = 2.0;
    private static final int TICKS_PER_SECOND = 20;

    /** A core: fuel rods, control channels and their height, heat exchangers and port pairs. */
    private record Core(int rods, int channels, int channelHeight, int exchangers, int portPairs, double moderation) {
        static Core testCore() {
            return new Core(6, 1, 3, 3, 1, 0);
        }

        static Core large() {
            return new Core(240, 12, 10, 100, 50, 0);
        }

        double maxAbsorption() {
            return Math.min(1, (double) channels * channelHeight / rods * 1.5);
        }

        double maxRemoval() {
            return Math.min(exchangers * 80, portPairs * 160) * STEAM_MW_PER_MB;
        }

        double chf() {
            return exchangers * 80 * STEAM_MW_PER_MB * 1.25;
        }
    }

    private static final class Run {
        final Core core;
        final FissionCore fission = new FissionCore();
        final double[] channels;
        double coverage = 1;
        double removalScale = 1;
        double reactivityOfFuel = 1.0;
        FissionCore.Params params = P;
        double peakFuel;

        Run(Core core, double insertion) {
            this.core = core;
            this.channels = new double[core.channels()];
            Arrays.fill(channels, insertion);
        }

        void insertAll(double x) {
            Arrays.fill(channels, x);
        }

        /** Moves one block of rod in channel 0 (positive out). */
        void withdrawBlocks(int blocks) {
            channels[0] = Math.max(0, Math.min(1, channels[0] - (double) blocks / core.channelHeight()));
        }

        void seconds(double s) {
            for (int i = 0; i < s * TICKS_PER_SECOND; i++) {
                CoreThermalModel.Boundary boundary = coverage <= 0 ? new CoreThermalModel.Dry()
                        : new CoreThermalModel.Boiling(coverage, core.maxRemoval() * removalScale, core.chf());
                fission.tick(params, new FissionCore.Inputs(core.rods(), true, 1.0, 650, reactivityOfFuel, 0, 0, 0, core.moderation(), 0,
                        ReactivityModel.Coolant.WATER, core.maxAbsorption(), channels, boundary, 0, 0.6, 0.002));
                peakFuel = Math.max(peakFuel, fission.fuelTemperature());
            }
        }

        double power() {
            return fission.power();
        }
    }

    /** A test core with two of its three rod blocks in, brought to its steady state from cold. */
    private static Run steadyTestCore() {
        Run run = new Run(Core.testCore(), 1.0 / 3);
        run.seconds(400);
        return run;
    }

    @Test
    void rodsInShutTheTestCoreDown() {
        Run run = new Run(Core.testCore(), 1.0);
        run.seconds(120);
        assertTrue(run.power() < 1.0e-3, "fully inserted rods hold it subcritical: " + run.power());
        assertTrue(run.fission.shutdownMargin() > 1000, "with a margin: " + run.fission.shutdownMargin());
    }

    @Test
    void theTestCoreSettlesWithOneBlockIn() {
        Run run = steadyTestCore();
        double before = run.power();
        run.seconds(30);
        assertTrue(before > 0.2 && before < 1.2, "a steady power: " + before);
        assertEquals(before, run.power(), 0.02 * before, "and it stays there");
        assertTrue(run.fission.fuelTemperature() < 1200, "at a sane temperature: " + run.fission.fuelTemperature());
    }

    @Test
    void pullingTheLastBlockOfTheTestCoreNeverRunsAway() {
        Run run = steadyTestCore();
        run.withdrawBlocks(1);
        run.seconds(400);
        double settled = run.power();
        run.seconds(30);
        assertEquals(settled, run.power(), 0.03 * settled, "it settles again");
        assertTrue(run.peakFuel < 2000, "all rods out of a uranium core stays well short of damage: " + run.peakFuel);
        assertEquals(0, run.fission.damage(), 1e-9);
    }

    @Test
    void oneBlockOutOfALargeCoreAddsAFewPercent() {
        Run run = new Run(Core.large(), 0.3);
        run.seconds(400);
        double before = run.power();
        run.withdrawBlocks(1);
        run.seconds(120);
        double after = run.power();
        run.seconds(30);
        double change = after / before - 1;
        assertTrue(change > 0.005 && change < 0.10, "one block out: " + (change * 100) + "% (" + before + " -> " + after + ")");
        assertEquals(after, run.power(), 0.01 * after, "settled");
        assertTrue(run.fission.fuelTemperature() < 1200, "still cool: " + run.fission.fuelTemperature());
    }

    @Test
    void allRodsOutThenInRecovers() {
        Run run = new Run(Core.large(), 0.3);
        run.seconds(200);
        run.insertAll(0);
        run.seconds(60);
        run.insertAll(1);
        run.seconds(300);
        assertTrue(run.peakFuel < 2000, "pulling every rod is no meltdown in a uranium core: " + run.peakFuel);
        assertTrue(run.power() < 0.01 && run.fission.fuelTemperature() < 450, "and it shuts down: " + run.power() + ", "
                + run.fission.fuelTemperature());
    }

    @Test
    void aSmallAndALargeCoreBehaveAlike() {
        Core small = new Core(6, 1, 3, 3, 2, 0);
        Core large = new Core(240, 40, 3, 120, 80, 0);
        Run a = new Run(small, 1.0 / 3);
        Run b = new Run(large, 1.0 / 3);
        a.seconds(400);
        b.seconds(400);
        assertEquals(a.fission.fuelTemperature(), b.fission.fuelTemperature(), 0.01 * a.fission.fuelTemperature());
        assertEquals(a.power(), b.power(), 0.01 * a.power());
    }

    @Test
    void losingTheCoolantDamagesTheCore() {
        Run run = new Run(Core.large(), 0.1);
        run.seconds(300);
        assertTrue(run.power() > 0.8, "running near full power first: " + run.power());
        run.coverage = 0;
        run.seconds(1800);
        assertTrue(run.power() < 1.0e-3, "a dry core stops its chain reaction");
        assertTrue(run.peakFuel > 2000 && run.fission.damage() > 0, "but its decay heat damages it: " + run.peakFuel + ", "
                + run.fission.damage());
    }

    @Test
    void aBlockedSteamOutletHeatsTheCoreGradually() {
        Run run = steadyTestCore();
        double fuel = run.fission.fuelTemperature();
        run.removalScale = 0;
        run.seconds(5);
        assertTrue(run.fission.fuelTemperature() - fuel < 150, "no jump: " + (run.fission.fuelTemperature() - fuel));
        run.seconds(600);
        assertTrue(run.power() < 0.2, "the hot water pulls the power down by itself: " + run.power());
        assertTrue(run.peakFuel < 2000, "without damage: " + run.peakFuel);
    }

    @Test
    void xenonPullsASteadyCoreDownOverTheFollowingHours() {
        Run run = new Run(Core.large(), 0.3);
        run.params = D;
        run.seconds(300);
        double early = run.power();
        run.seconds(3600);
        assertTrue(run.power() < early, "xenon builds up and takes reactivity: " + early + " -> " + run.power());
        assertTrue(run.fission.breakdown().xenon() < -500, "about a thousand pcm: " + run.fission.breakdown().xenon());
    }

    @Test
    void aGraphiteCoreWithEveryRodOutIsDangerous() {
        Core core = new Core(6, 1, 3, 3, 1, 1.0);
        Run run = new Run(core, 0);
        run.seconds(600);
        assertTrue(run.peakFuel > 2000, "graphite keeps moderating when the water boils: " + run.peakFuel);
    }
}
