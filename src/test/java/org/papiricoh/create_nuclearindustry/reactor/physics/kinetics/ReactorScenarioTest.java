package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whole-core scenarios with the default numbers: a core built like the game's, cooled by boiling
 * water through its heat exchangers and ports. These pin what the redesign is for: withdrawing a rod
 * gives a new, steady power instead of a runaway, but fresh fuel holds far more reactivity than its
 * heat can take back, so a core needs its rods: with every rod out (or none built) it overheats and
 * is damaged, half drained or not. Losing the coolant damages it too, and a big core behaves like a
 * small one.
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

    /** Rod insertion that holds the large core at about half its rated power. */
    private static final double HALF_POWER = 0.55;

    /** A large core on its rods at about half power, brought to its steady state from cold. */
    private static Run steadyCore() {
        Run run = new Run(Core.large(), HALF_POWER);
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
    void aCoreSettlesOnItsRods() {
        Run run = steadyCore();
        double before = run.power();
        run.seconds(30);
        assertTrue(before > 0.2 && before < 1.0, "a steady power: " + before);
        assertEquals(before, run.power(), 0.02 * before, "and it stays there");
        assertTrue(run.fission.fuelTemperature() < 1000, "at a sane temperature: " + run.fission.fuelTemperature());
        assertEquals(0, run.fission.reactivity(), 20, "critical: its net reactivity is nil");
    }

    @Test
    void everyRodOutOfAFreshUraniumCoreDamagesIt() {
        Run run = steadyCore();
        run.insertAll(0);
        run.seconds(600);
        assertTrue(run.peakFuel > 2000 && run.fission.damage() > 0, "fresh fuel holds far more reactivity than its heat takes back: "
                + run.peakFuel + " °C, damage " + run.fission.damage());
    }

    @Test
    void aCoreWithNoControlRodsOverheats() {
        Run run = new Run(new Core(12, 0, 3, 6, 3, 0), 0);
        run.seconds(600);
        assertTrue(run.peakFuel > 2000 && run.fission.damage() > 0, "nothing holds it back: " + run.peakFuel);
    }

    @Test
    void aRodlessCoreRunningLowOnWaterSurgesWhenItIsRefilled() {
        Run run = new Run(new Core(12, 3, 3, 6, 3, 0), 0);
        run.coverage = 0.45;
        run.seconds(300);
        assertTrue(run.fission.damage() == 0, "uncovering the fuel takes its moderator away: " + run.peakFuel + " °C, power "
                + run.power());
        run.coverage = 1;
        run.seconds(300);
        assertTrue(run.peakFuel > 2000 && run.fission.damage() > 0, "but the water coming back brings every bit of reactivity "
                + "back with nothing to hold it: " + run.peakFuel + " °C, damage " + run.fission.damage());
    }

    /**
     * The reactor that prompted the new values: twelve 20% fuel blocks with some graphite, nine rod
     * blocks, all of them out, and a tank at about an eighth. With the old values it looked stable
     * because the fuel stood half out of the water; rich fuel with graphite now has more reactivity
     * than the missing water takes away, so it damages itself even uncovered.
     */
    @Test
    void aSmallGraphiteCoreOfRichFuelWithEveryRodOutDamagesItselfEvenOnLowWater() {
        Run run = new Run(new Core(12, 3, 3, 6, 3, 0.25), 0);
        run.reactivityOfFuel = 1.5;
        run.coverage = 0.48;
        run.seconds(300);
        ReactivityModel.Parts parts = run.fission.breakdown().parts();
        assertEquals(parts.total(), run.fission.reactivity(), 1e-6, "the parts add up to the net reactivity");
        assertTrue(parts.rods() == 0 && parts.voids() < -5000, "the uncovered fuel is all that holds it: " + parts);
        assertTrue(run.peakFuel > 2000 && run.fission.damage() > 0, "and that is not enough for 20% fuel with graphite: "
                + run.peakFuel + " C, damage " + run.fission.damage());
    }

    @Test
    void oneBlockOutOfALargeCoreRaisesItsPowerOneStep() {
        Run run = steadyCore();
        double before = run.power();
        run.withdrawBlocks(1);
        run.seconds(120);
        double after = run.power();
        run.seconds(30);
        double change = after / before - 1;
        assertTrue(change > 0.02 && change < 0.30, "one block out: " + (change * 100) + "% (" + before + " -> " + after + ")");
        assertEquals(after, run.power(), 0.01 * after, "settled");
        assertTrue(run.fission.fuelTemperature() < 1200, "still cool: " + run.fission.fuelTemperature());
    }

    @Test
    void aScramSoonAfterPullingEveryRodRecovers() {
        Run run = steadyCore();
        run.insertAll(0);
        run.seconds(20);
        run.insertAll(1);
        run.seconds(300);
        assertTrue(run.peakFuel < 2000, "the rod drives free their reactivity slowly enough to SCRAM in time: " + run.peakFuel);
        assertTrue(run.power() < 0.01 && run.fission.fuelTemperature() < 450, "and it shuts down: " + run.power() + ", "
                + run.fission.fuelTemperature());
    }

    @Test
    void aSmallAndALargeCoreBehaveAlike() {
        Core small = new Core(6, 1, 3, 3, 2, 0);
        Core large = new Core(240, 40, 3, 120, 80, 0);
        Run a = new Run(small, 0.45);
        Run b = new Run(large, 0.45);
        a.seconds(400);
        b.seconds(400);
        assertEquals(a.fission.fuelTemperature(), b.fission.fuelTemperature(), 0.01 * a.fission.fuelTemperature());
        assertTrue(a.power() > 0.2, "at power: " + a.power());
        assertEquals(a.power(), b.power(), 0.01 * a.power());
    }

    @Test
    void losingTheCoolantLeavesTheCoreHeatingUpForAnHour() {
        Run run = new Run(Core.large(), 0.52);
        run.seconds(7200);
        assertTrue(run.power() > 0.8, "running near full power for two hours first: " + run.power());
        double running = run.fission.fuelTemperature();
        run.coverage = 0;
        run.peakFuel = 0;
        run.seconds(1800);
        assertTrue(run.power() < 1.0e-3, "a dry core stops its chain reaction");
        assertTrue(run.peakFuel > running + 600, "but its decay heat keeps heating it, with no water to take it: " + running
                + " -> " + run.peakFuel);
        assertTrue(run.fission.fuelTemperature() > 1500, "and half an hour later it is still hot: " + run.fission.fuelTemperature());
    }

    @Test
    void aBlockedSteamOutletHeatsTheCoreGradually() {
        Run run = steadyCore();
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
        Run run = new Run(Core.large(), HALF_POWER);
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
