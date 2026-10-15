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
    /** Fuel temperature where the cladding fails: the first damage. */
    private static final double DAMAGE = D.damage().claddingFailure();

    /** A core: fuel rods, control channels and their height, heat exchangers and port pairs. */
    private record Core(int rods, int channels, int channelHeight, int exchangers, int portPairs, double moderation) {
        static Core testCore() {
            return new Core(6, 1, 3, 3, 1, 0);
        }

        static Core large() {
            return new Core(240, 12, 10, 100, 50, 0);
        }

        /** Rod banks the rods are worth: no longer capped at one, only by the 4-bank guard. */
        double maxAbsorption() {
            return RodWorth.absorption(channels * channelHeight, rods, 1.5, 1.0, 4.0);
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
        /** A steam tank between the core and its turbine, mB: -1 for none (the removal is the exchangers'). */
        double tank = -1;
        double tankCapacity = 16000;
        double turbineDraw;
        double steamCarry;
        double minPower = Double.MAX_VALUE;
        double maxPower;
        /** Cold water fed beyond the steam, mB per second (feed and bleed). */
        double feedFlow;
        /** Boron dissolved in the water, ppm. */
        double boronPpm;

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
            minPower = Double.MAX_VALUE;
            maxPower = 0;
            for (int i = 0; i < s * TICKS_PER_SECOND; i++) {
                double removal = core.maxRemoval() * removalScale;
                if (tank >= 0) {
                    removal = Math.min(removal, Math.max(0, tankCapacity - tank) * STEAM_MW_PER_MB);
                }
                CoreThermalModel.Boundary boundary = coverage <= 0 ? new CoreThermalModel.Dry()
                        : new CoreThermalModel.Boiling(coverage, removal, core.chf(), feedFlow);
                fission.tick(params, new FissionCore.Inputs(core.rods(), true, 1.0, 650, reactivityOfFuel, 0, 0, 0, core.moderation(), 0,
                        ReactivityModel.Coolant.WATER, core.maxAbsorption(), channels, boundary, 0, 0.6, 0.002, boronPpm));
                if (tank >= 0) {
                    steamCarry += fission.thermalResult().boiled() / STEAM_MW_PER_MB;
                    int steam = (int) steamCarry;
                    steamCarry -= steam;
                    tank = Math.min(tankCapacity, tank + steam);
                    // The turbine takes its steam in gulps, once a second: the outlet shuts whenever the tank is full.
                    if (i % TICKS_PER_SECOND == 0) {
                        tank = Math.max(0, tank - turbineDraw * TICKS_PER_SECOND);
                    }
                }
                peakFuel = Math.max(peakFuel, fission.fuelTemperature());
                minPower = Math.min(minPower, fission.power());
                maxPower = Math.max(maxPower, fission.power());
            }
        }

        /** Peak-to-peak swing of the power over the last run, as a share of its peak. */
        double powerSwing() {
            return maxPower <= 0 ? 0 : (maxPower - minPower) / maxPower;
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
        assertTrue(run.peakFuel > DAMAGE && run.fission.damage() > 0, "fresh fuel holds far more reactivity than its heat takes back: "
                + run.peakFuel + " °C, damage " + run.fission.damage());
    }

    @Test
    void aCoreWithNoControlRodsOverheats() {
        Run run = new Run(new Core(12, 0, 3, 6, 3, 0), 0);
        run.seconds(600);
        assertTrue(run.peakFuel > DAMAGE && run.fission.damage() > 0, "nothing holds it back: " + run.peakFuel);
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
        assertTrue(run.peakFuel > DAMAGE && run.fission.damage() > 0, "but the water coming back brings every bit of reactivity "
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
        assertTrue(run.peakFuel > DAMAGE && run.fission.damage() > 0, "and that is not enough for 20% fuel with graphite: "
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
        assertTrue(run.fission.fuelTemperature() < DAMAGE - 300, "still cool: " + run.fission.fuelTemperature());
    }

    @Test
    void aScramSoonAfterPullingEveryRodRecovers() {
        Run run = steadyCore();
        run.insertAll(0);
        run.seconds(20);
        run.insertAll(1);
        run.seconds(300);
        assertTrue(run.peakFuel < DAMAGE, "the rod drives free their reactivity slowly enough to SCRAM in time: " + run.peakFuel);
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

    /**
     * The steam has nowhere to go: the water is driven to its superheat ceiling and flashes to steam,
     * and the void shuts the chain reaction down within seconds. The decay heat then has no water to
     * go to and creeps into the fuel, so the outlet must be freed, but a core at half power is not
     * damaged by it.
     */
    @Test
    void aBlockedSteamOutletShutsTheCoreDownOnItsOwnSteam() {
        Run run = steadyCore();
        double fuel = run.fission.fuelTemperature();
        double ceiling = D.thermal().boilingTemperature() + D.thermal().maxSuperheat();
        run.removalScale = 0;
        run.seconds(5);
        assertTrue(run.fission.fuelTemperature() - fuel < 150, "no jump: " + (run.fission.fuelTemperature() - fuel));
        assertEquals(ceiling, run.fission.coolantTemperature(), 1, "the water stops at its ceiling");
        run.seconds(30);
        assertTrue(run.power() < 0.1, "the flashing water pulls the power down by itself: " + run.power());
        double afterShutdown = run.fission.fuelTemperature();
        run.seconds(600);
        assertTrue(run.fission.fuelTemperature() > afterShutdown + 100, "the decay heat has nowhere to go but the fuel: "
                + afterShutdown + " -> " + run.fission.fuelTemperature());
        assertTrue(run.peakFuel < DAMAGE && run.fission.damage() == 0, "without damage from half power: " + run.peakFuel);
    }

    /**
     * The reactor that prompted this: twelve 20% fuel blocks, nine rod blocks, and only two heat
     * exchangers' worth of steam. The power cannot exceed what leaves as steam, so pulling more rods
     * used to change nothing while the water silently soaked the reactivity. Now the water stops at
     * its ceiling and each block out heats the fuel by hundreds of degrees, until it is damaged.
     */
    @Test
    void aSteamBoundCoreStillAnswersItsRods() {
        Run run = new Run(new Core(12, 3, 3, 2, 3, 0), 1.0);
        run.reactivityOfFuel = 1.5;
        run.seconds(60);
        double removal = run.core.maxRemoval();
        double[] fuelByBlocksOut = new double[10];
        double peakPower = 0;
        for (int out = 1; out <= 9; out++) {
            for (int c = 0; c < 3; c++) {
                int inChannel = out / 3 + (c < out % 3 ? 1 : 0);
                run.channels[c] = 1.0 - inChannel / 3.0;
            }
            run.seconds(300);
            // Settled: the last minute of each step, after the withdrawal's transient has passed.
            run.seconds(60);
            peakPower = Math.max(peakPower, run.maxPower * run.fission.ratedPower());
            fuelByBlocksOut[out] = run.fission.fuelTemperature();
            if (run.fission.damage() > 0) {
                break;
            }
        }
        assertTrue(peakPower < removal * 1.05, "settled, the power never beats the steam that leaves: " + peakPower + " of " + removal);
        assertTrue(fuelByBlocksOut[4] > 500 && fuelByBlocksOut[5] > fuelByBlocksOut[4] + 300,
                "the first block past criticality is a few hundred degrees, the next is hundreds more: " + Arrays.toString(fuelByBlocksOut));
        assertTrue(run.fission.damage() > 0 && run.peakFuel > DAMAGE, "and the rods out damage it, steam bound or not: "
                + Arrays.toString(fuelByBlocksOut));
    }

    /** The turbine takes less than the core makes: the tank fills, the outlet chokes, and the power holds steady. */
    @Test
    void aChokedSteamTankDoesNotMakeThePowerSawtooth() {
        // Nine rod blocks for twelve of fuel are worth 1.125 banks now that the absorption is not capped at
        // one: 0.4175 in holds the same 9360 pcm 0.45 did, so the core makes more steam than the turbine takes.
        Run run = new Run(new Core(12, 3, 3, 6, 3, 0), 0.4175);
        run.tank = 0;
        run.turbineDraw = 100;
        run.seconds(300);
        assertTrue(run.tank > run.tankCapacity * 0.9, "the turbine cannot keep up: " + run.tank);
        run.seconds(60);
        assertTrue(run.power() > 0.2, "the core runs on at what the turbine takes: " + run.power());
        assertTrue(run.powerSwing() < 0.05, "steadily: " + run.powerSwing() * 100 + "% peak to peak");
        assertTrue(run.fission.damage() == 0 && run.fission.fuelTemperature() < DAMAGE, "no harm done: " + run.fission.fuelTemperature());
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
        assertTrue(run.peakFuel > DAMAGE, "graphite keeps moderating when the water boils: " + run.peakFuel);
    }

    /** The six-rod test core after an hour near full power, scrammed, its decay heat boiling away for half a minute. */
    private static Run scrammedAfterAnHour() {
        Run run = new Run(Core.testCore(), 0.52);
        run.seconds(3600);
        assertTrue(run.power() > 0.5, "it ran: " + run.power());
        run.insertAll(1);
        run.seconds(30);
        assertTrue(run.power() < 0.05, "and the SCRAM stopped it, but for the delayed neutrons' tail: " + run.power());
        return run;
    }

    private static boolean cold(Run run) {
        return run.fission.fuelTemperature() < ShutdownState.COLD_TEMPERATURE
                && run.fission.coolantTemperature() < ShutdownState.COLD_TEMPERATURE;
    }

    @Test
    void withoutFeedAScrammedCoreStaysHot() {
        Run run = scrammedAfterAnHour();
        run.seconds(600);
        assertEquals(D.thermal().boilingTemperature(), run.fission.coolantTemperature(), 1,
                "its decay heat boils the water, which never cools below saturation");
        assertTrue(run.fission.fuelTemperature() > D.thermal().boilingTemperature(), "and the fuel stays above it");
    }

    @Test
    void feedingAndBleedingTakesAScrammedCoreCold() {
        Run run = scrammedAfterAnHour();
        run.feedFlow = 50 * TICKS_PER_SECOND;
        int seconds = 0;
        while (!cold(run) && seconds < 600) {
            run.seconds(1);
            seconds++;
        }
        assertTrue(seconds < 120, "50 mB/t of cold water fed and bled takes it below 100 °C in under two minutes: " + seconds + " s, "
                + run.fission.fuelTemperature() + " / " + run.fission.coolantTemperature());
        run.seconds(600);
        assertTrue(cold(run) && run.power() < 1.0e-3, "and holds it there, shut down: " + run.fission.fuelTemperature() + ", "
                + run.power());
        assertEquals(ShutdownState.COLD_SHUTDOWN, ShutdownState.classify(run.power() > 1.0e-3, run.fission.reactivity(),
                run.fission.coldShutdownMargin(false), 1000, run.fission.fuelTemperature(), run.fission.coolantTemperature(), 1));
    }

    /**
     * Twenty percent fuel with one rod block for every two of fuel: the rods (16500 pcm) cannot hold
     * its 18000 pcm when cold, so with every rod in it still warms itself up to criticality. 600 ppm
     * of boron (3000 pcm) keep it shut down and cold with margin to spare.
     */
    @Test
    void boronHoldsARichCoreShutWhenCold() {
        Core core = new Core(6, 1, 3, 3, 1, 0);
        Run bare = new Run(core, 1.0);
        bare.reactivityOfFuel = 1.5;
        bare.seconds(600);
        assertTrue(bare.fission.coldShutdownMargin(true) < 0, "the rods alone are short: " + bare.fission.coldShutdownMargin(true));
        assertTrue(bare.fission.fuelTemperature() > 100, "so it heats itself up with every rod in: " + bare.fission.fuelTemperature());

        Run borated = new Run(core, 1.0);
        borated.reactivityOfFuel = 1.5;
        borated.boronPpm = 600;
        borated.seconds(600);
        assertTrue(borated.power() < 1.0e-3 && cold(borated), "boron holds it shut down and cold: " + borated.power() + ", "
                + borated.fission.fuelTemperature());
        assertEquals(1500, borated.fission.coldShutdownMargin(true), 20, "with 16500 + 3000 - 18000 pcm to spare");
        assertEquals(-3000, borated.fission.breakdown().parts().boron(), 1, "the boron's part of the reactivity");
        assertEquals(borated.fission.breakdown().parts().total(), borated.fission.reactivity(), 1.0e-6, "the parts still add up");
    }
}
