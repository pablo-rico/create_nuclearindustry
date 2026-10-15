package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The annihilation generator's burn and output in its three modes, and its chamber's field. */
class AnnihilationModelTest {
    private static final AnnihilationModel.Params P = AnnihilationModel.Params.DEFAULT;
    private static final PenningTrapModel.Params C = AnnihilationModel.CHAMBER;
    private static final AnnihilationModel.Mode ROTATION = AnnihilationModel.Mode.ROTATION;
    private static final AnnihilationModel.Mode ELECTRIC = AnnihilationModel.Mode.ELECTRIC;
    private static final AnnihilationModel.Mode SPLIT = AnnihilationModel.Mode.SPLIT;

    @Test
    void fullThrottleBurnsSixNanogramsAMinute() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, 100, ROTATION);
        assertEquals(6.0, out.burnt() * 1200, 1e-9);
        assertEquals(256, out.capacity(), 1e-9);
        assertEquals(0, out.energy(), 1e-9);
        assertEquals(1, out.fraction(), 1e-9);
    }

    @Test
    void electricModeMakesForgeEnergyInstead() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, 100, ELECTRIC);
        assertEquals(0, out.capacity(), 1e-9);
        assertEquals(16384, out.energy(), 1e-9);
        assertEquals(16384, out.energyFe());
        AnnihilationModel.Output split = AnnihilationModel.tick(P, 1, 100, SPLIT);
        assertEquals(128, split.capacity(), 1e-9);
        assertEquals(8192, split.energy(), 1e-9);
        assertEquals(out.burnt(), split.burnt(), 1e-15, "the mode only shares the output out");
    }

    @Test
    void zeroThrottleKeepsOnlyTheStandbyBurn() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 0, 100, ROTATION);
        assertEquals(P.ratedBurn() * P.standbyShare(), out.burnt(), 1e-12);
        assertEquals(P.standbyBurn(), out.burnt(), 1e-15);
        assertEquals(0, out.capacity(), 1e-9);
        assertEquals(0, out.fraction(), 1e-9);
        assertTrue(out.burning());
    }

    @Test
    void aThrottleBelowStandbyStillBurnsTheStandby() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 0.01, 100, ELECTRIC);
        assertEquals(P.standbyBurn(), out.burnt(), 1e-15);
        assertEquals(0.01 * 16384, out.energy(), 1e-6, "but gives only what was asked");
    }

    @Test
    void outputFollowsTheThrottle() {
        double last = -1;
        for (int signal = 0; signal <= 15; signal++) {
            AnnihilationModel.Output out = AnnihilationModel.tick(P, AnnihilationModel.throttle(signal), 100, ROTATION);
            assertTrue(out.capacity() > last || signal == 0, "more signal, more capacity");
            last = out.capacity();
        }
        assertEquals(128, AnnihilationModel.tick(P, 0.5, 100, ROTATION).capacity(), 1e-9);
    }

    @Test
    void runningShortGivesThatShare() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, P.ratedBurn() / 2, ROTATION);
        assertEquals(P.ratedBurn() / 2, out.burnt(), 1e-12);
        assertEquals(128, out.capacity(), 1e-9);
        assertEquals(0.5, out.fraction(), 1e-9);
        assertSame(AnnihilationModel.Output.NONE, AnnihilationModel.tick(P, 1, 0, ROTATION));
    }

    @Test
    void runningShortAtHalfThrottleGivesHalfOfThat() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 0.5, P.ratedBurn() / 4, SPLIT);
        assertEquals(P.ratedBurn() / 4, out.burnt(), 1e-12);
        assertEquals(0.25, out.fraction(), 1e-9);
        assertEquals(0.25 * 128, out.capacity(), 1e-9);
        assertEquals(0.25 * 8192, out.energy(), 1e-6);
    }

    @Test
    void tenNanogramsLastAlmostTwoMinutesFlatOut() {
        assertEquals(10.0 / 6.0, AnnihilationModel.minutesOn(P, 10, 1), 1e-9);
        assertEquals(6.0, AnnihilationModel.burnPerMinute(P, 1), 1e-9);
        assertEquals(6.0 * P.standbyShare(), AnnihilationModel.burnPerMinute(P, 0), 1e-9);
        assertEquals(10.0 / 6.0 / P.standbyShare(), AnnihilationModel.minutesOn(P, 10, 0), 1e-6, "standby lasts fifty times longer");
    }

    @Test
    void throttleFollowsTheAnalogSignal() {
        assertEquals(0, AnnihilationModel.throttle(0), 1e-9);
        assertEquals(1, AnnihilationModel.throttle(15), 1e-9);
        assertEquals(1, AnnihilationModel.throttle(99), 1e-9);
        assertEquals(0, AnnihilationModel.throttle(-3), 1e-9);
        assertEquals(7 / 15.0, AnnihilationModel.throttle(7), 1e-9);
    }

    @Test
    void modesCycleAndSkipEnergyWhenDisabled() {
        assertSame(ELECTRIC, ROTATION.next(P));
        assertSame(SPLIT, ELECTRIC.next(P));
        assertSame(ROTATION, SPLIT.next(P));
        AnnihilationModel.Params noFe = new AnnihilationModel.Params(P.ratedBurn(), P.ratedSpeed(), P.ratedCapacity(), 0, P.standbyShare());
        assertSame(ROTATION, ROTATION.next(noFe));
        assertSame(ROTATION, ELECTRIC.effective(noFe));
        AnnihilationModel.Output out = AnnihilationModel.tick(noFe, 1, 100, ELECTRIC);
        assertEquals(256, out.capacity(), 1e-9, "a saved electric generator turns when FE is disabled");
        assertEquals(0, out.energy(), 1e-9);
        assertSame(SPLIT, AnnihilationModel.Mode.byName("split"));
        assertSame(ROTATION, AnnihilationModel.Mode.byName("nonsense"));
        assertTrue(SPLIT.turnsShaft() && SPLIT.makesEnergy());
        assertFalse(ELECTRIC.turnsShaft());
        assertFalse(ROTATION.makesEnergy());
        assertEquals("electric", ELECTRIC.id());
    }

    @Test
    void theShaftTurnsOnlyWithCapacity() {
        assertEquals(256f, AnnihilationModel.shaftSpeed(P, AnnihilationModel.tick(P, 1, 100, ROTATION)));
        assertEquals(0f, AnnihilationModel.shaftSpeed(P, AnnihilationModel.tick(P, 0, 100, ROTATION)), "standby gives no speed");
        assertEquals(0f, AnnihilationModel.shaftSpeed(P, AnnihilationModel.tick(P, 1, 100, ELECTRIC)));
        assertEquals(0f, AnnihilationModel.shaftSpeed(P, AnnihilationModel.Output.NONE));
    }

    @Test
    void capacityRefreshesOnlyOnRealChanges() {
        assertFalse(AnnihilationModel.capacityChanged(256, 255));
        assertTrue(AnnihilationModel.capacityChanged(256, 240));
        assertTrue(AnnihilationModel.capacityChanged(0, 0.1), "starting always tells the network");
        assertTrue(AnnihilationModel.capacityChanged(0.1, 0), "and so does stopping");
        assertFalse(AnnihilationModel.capacityChanged(0, 0));
    }

    @Test
    void comparatorAndDroneFollowTheOutput() {
        assertEquals(0, AnnihilationModel.comparator(AnnihilationModel.Output.NONE));
        assertEquals(1, AnnihilationModel.comparator(AnnihilationModel.tick(P, 0, 100, ROTATION)), "standby still reads");
        assertEquals(15, AnnihilationModel.comparator(AnnihilationModel.tick(P, 1, 100, ROTATION)));
        assertEquals(0f, AnnihilationModel.droneVolume(false, 1));
        assertTrue(AnnihilationModel.droneVolume(true, 0) > 0);
        assertTrue(AnnihilationModel.droneVolume(true, 1) > AnnihilationModel.droneVolume(true, 0.5));
        assertTrue(AnnihilationModel.dronePitch(1) > AnnihilationModel.dronePitch(0));
    }

    @Test
    void theGeneratorFeedsItsOwnBufferFirst() {
        assertEquals(100, AnnihilationModel.selfFeed(16384, 100));
        assertEquals(50, AnnihilationModel.selfFeed(50, 100));
        assertEquals(0, AnnihilationModel.selfFeed(16384, 0));
        assertEquals(16384 * 20, AnnihilationModel.sourceCapacity(P), "one second of rated energy");
    }

    @Test
    void theChamberPullsUpToItsRoom() {
        assertEquals(AnnihilationModel.PULL_PER_TICK, AnnihilationModel.pull(C, 0, AnnihilationModel.PULL_PER_TICK), 1e-12);
        assertEquals(0.01, AnnihilationModel.pull(C, C.capacity() - 0.01, 1), 1e-9);
        assertEquals(0, AnnihilationModel.pull(C, C.capacity(), 1), 1e-12);
    }

    // ------------------------------------------------------------------ the chamber's field

    private static final int FULL = C.quenchTicks();
    private static final int GRACE = AnnihilationModel.LIT_GRACE_TICKS;

    @Test
    void anEmptyColdChamberIsJustATrap() {
        AnnihilationModel.Step off = AnnihilationModel.step(P, C, 1, 0, ROTATION, 0, FULL, 0);
        assertSame(TrapState.OFFLINE, off.state());
        assertFalse(off.annihilate() || off.lit() || off.output().burning());
        AnnihilationModel.Step on = AnnihilationModel.step(P, C, 1, 0, ROTATION, 10_000, FULL, 0);
        assertSame(TrapState.STABLE, on.state());
        assertEquals(PenningTrapModel.demandFe(C, 0), on.feUsed());
        assertFalse(on.lit(), "nothing to burn");
    }

    @Test
    void aColdChamberWithEnergyIgnites() {
        AnnihilationModel.Step step = AnnihilationModel.step(P, C, 1, 0.5, ROTATION, 10_000, FULL, 0);
        assertTrue(step.lit());
        assertTrue(step.output().burning());
        assertEquals(256, step.output().capacity(), 1e-9);
        assertTrue(step.feUsed() > 0, "the start pays its field");
        assertFalse(step.annihilate());
    }

    @Test
    void aLitChamberHoldsItselfWithoutEnergy() {
        double stored = 0.5;
        int quench = FULL;
        int lit = GRACE;
        for (int tick = 0; tick < 1200; tick++) {
            AnnihilationModel.Step step = AnnihilationModel.step(P, C, 0, stored, ELECTRIC, 0, quench, lit);
            assertFalse(step.annihilate(), "tick " + tick);
            assertEquals(0, step.feUsed());
            assertSame(TrapState.STABLE, step.state());
            stored -= step.output().burnt();
            quench = step.quenchTicksLeft();
            lit = step.litTicks();
        }
        assertEquals(GRACE, lit, "every burn renews the grace");
        assertEquals(0.5 - 1200 * P.standbyBurn(), stored, 1e-9, "a minute of standby");
    }

    @Test
    void antimatterInAColdChamberWithoutEnergyQuenchesThenAnnihilates() {
        double stored = 0.5;
        int quench = FULL;
        AnnihilationModel.Step step = null;
        for (int tick = 0; tick < FULL; tick++) {
            step = AnnihilationModel.step(P, C, 1, stored, ROTATION, 0, quench, 0);
            assertFalse(step.output().burning(), "no field, no burn");
            assertSame(TrapState.QUENCHING, step.state());
            quench = step.quenchTicksLeft();
            if (tick < FULL - 1) {
                assertFalse(step.annihilate());
            }
        }
        assertTrue(step.annihilate(), "after the quench countdown");
        assertFalse(step.lit());
    }

    @Test
    void runningDryPutsTheChamberOutSafely() {
        double stored = P.standbyBurn() / 2;
        AnnihilationModel.Step last = AnnihilationModel.step(P, C, 0, stored, ROTATION, 0, FULL, GRACE);
        assertEquals(stored, last.output().burnt(), 1e-15, "it burns its last crumbs");
        assertFalse(last.annihilate(), "nothing is left to annihilate");
        assertEquals(GRACE, last.litTicks(), "the last burn still raised the field");
        int lit = last.litTicks();
        AnnihilationModel.Step empty = null;
        for (int tick = 0; tick < GRACE; tick++) {
            empty = AnnihilationModel.step(P, C, 0, 0, ROTATION, 0, FULL, lit);
            assertFalse(empty.annihilate(), "tick " + tick);
            assertEquals(0, empty.feUsed(), "the collapsing field costs nothing");
            lit = empty.litTicks();
        }
        assertFalse(empty.lit(), "out after the grace window");
        AnnihilationModel.Step cold = AnnihilationModel.step(P, C, 0, 0, ROTATION, 0, FULL, lit);
        assertFalse(cold.lit() || cold.annihilate());
        assertSame(TrapState.OFFLINE, cold.state());
    }

    @Test
    void poweredMeansLitOrAbleToPayTheFullField() {
        assertTrue(AnnihilationModel.powered(C, 0.5, 0, GRACE));
        assertFalse(AnnihilationModel.powered(C, 0, 0, 0));
        int full = PenningTrapModel.demandFe(C, C.capacity());
        assertFalse(AnnihilationModel.powered(C, 0, full - 1, 0), "a cold start needs the full chamber's field");
        assertTrue(AnnihilationModel.powered(C, 0, full, 0));
        assertTrue(AnnihilationModel.powered(C, 0, 0, 1), "an empty chamber holds while its grace lasts");
    }

    @Test
    void theLegacyBooleanFormsKeepTheirOldMeaning() {
        assertTrue(AnnihilationModel.powered(C, 0.5, 0, true));
        assertFalse(AnnihilationModel.powered(C, 0, 0, true), "an empty chamber is out, whatever it said last");
        AnnihilationModel.Step empty = AnnihilationModel.step(P, C, 0, 0, ROTATION, 0, FULL, true);
        assertFalse(empty.lit(), "the boolean form has one tick of grace, used up at once");
        assertEquals(GRACE, AnnihilationModel.litTicksFromLegacy(true));
        assertEquals(0, AnnihilationModel.litTicksFromLegacy(false));
    }

    // ------------------------------------------------------------------ supply gaps (rotation, no cable)

    /**
     * Runs a rotation-mode generator with no Forge Energy at all: the chamber is fed {@code supply} ng
     * a tick (the generator's pull from a trap) except during {@code gapFrom..gapFrom+gapTicks}.
     * Returns the litTicks at the end; the chamber starts lit with {@code start} ng.
     */
    private static int runWithGap(double start, int gapFrom, int gapTicks, int total) {
        double stored = start;
        int quench = FULL;
        int lit = GRACE;
        for (int tick = 0; tick < total; tick++) {
            boolean gap = tick >= gapFrom && tick < gapFrom + gapTicks;
            // The generator only pulls while powered(), as its block entity does.
            if (!gap && AnnihilationModel.powered(C, stored, 0, lit)) {
                stored += AnnihilationModel.pull(C, stored, AnnihilationModel.PULL_PER_TICK);
            }
            AnnihilationModel.Step step = AnnihilationModel.step(P, C, 1, stored, ROTATION, 0, quench, lit);
            assertFalse(step.annihilate(), "tick " + tick);
            stored = Math.max(0, stored - step.output().burnt());
            if (!PenningTrapModel.loaded(stored)) {
                stored = 0;
            }
            quench = step.quenchTicksLeft();
            lit = step.litTicks();
        }
        return lit;
    }

    @Test
    void aChamberThatBurntItselfEmptySurvivesABriefSupplyGap() {
        // A nearly empty chamber (a tick of fuel) runs dry at once; supply comes back inside the grace.
        int lit = runWithGap(P.ratedBurn(), 0, GRACE - 5, 400);
        assertEquals(GRACE, lit, "the supply came back before the field collapsed: still burning");
    }

    @Test
    void aGapLongerThanTheGraceStillPutsItOut() {
        int lit = runWithGap(P.ratedBurn(), 0, GRACE + 20, GRACE + 40);
        assertEquals(0, lit, "with no cable for a black start, a long gap leaves it cold");
    }

    @Test
    void aFullChamberRidesOutItsOwnFuelPlusTheGrace() {
        // A full chamber lasts 200 ticks at full throttle; the grace adds to that.
        int burnTicks = (int) Math.floor(C.capacity() / P.ratedBurn());
        assertEquals(GRACE, runWithGap(C.capacity(), 0, burnTicks + GRACE - 2, burnTicks + GRACE + 100));
        assertEquals(0, runWithGap(C.capacity(), 0, burnTicks + GRACE + 5, burnTicks + GRACE + 10));
    }

    @Test
    void anEmptyLitChamberIsStableAndFree() {
        AnnihilationModel.Step step = AnnihilationModel.step(P, C, 1, 0, ROTATION, 0, FULL, 10);
        assertSame(TrapState.STABLE, step.state());
        assertEquals(1f, step.field());
        assertEquals(0, step.feUsed());
        assertEquals(9, step.litTicks(), "the grace counts down");
        assertFalse(step.output().burning());
        assertEquals(FULL, step.quenchTicksLeft());
    }

    @Test
    void graceIsClampedToTheWindow() {
        AnnihilationModel.Step step = AnnihilationModel.step(P, C, 1, 0, ROTATION, 0, FULL, 10_000);
        assertEquals(GRACE - 1, step.litTicks());
        AnnihilationModel.Step negative = AnnihilationModel.step(P, C, 1, 0, ROTATION, 0, FULL, -5);
        assertFalse(negative.lit());
    }

    @Test
    void anElectricGeneratorBlackStartsAndThenRunsAlone() {
        // One start from outside energy, then its own FE keeps the field buffer topped up for the next cold start.
        int capacity = PenningTrapModel.bufferCapacity(C);
        int buffer = PenningTrapModel.demandFe(C, C.capacity());
        double stored = C.capacity();
        int quench = FULL;
        int lit = 0;
        long exported = 0;
        for (int tick = 0; tick < 100; tick++) {
            AnnihilationModel.Step step = AnnihilationModel.step(P, C, 1, stored, ELECTRIC, buffer, quench, lit);
            assertFalse(step.annihilate());
            buffer -= step.feUsed();
            stored -= step.output().burnt();
            int made = step.output().energyFe();
            int self = AnnihilationModel.selfFeed(made, capacity - buffer);
            buffer += self;
            exported += made - self;
            quench = step.quenchTicksLeft();
            lit = step.litTicks();
        }
        assertEquals(capacity, buffer, "its own output filled its field buffer");
        assertTrue(exported > 99L * 16000, "and the rest went out");
        assertTrue(AnnihilationModel.powered(C, 0, buffer, 0), "ready for the next cold start");
    }
}
