package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Penning trap energy demand, states, quench countdown and readouts. */
class PenningTrapModelTest {
    private static final PenningTrapModel.Params S = PenningTrapModel.Params.SMALL;
    private static final int Q = S.quenchTicks();

    @Test
    void tiersMatchTheContract() {
        assertEquals(32, TrapTier.SMALL.defaults().capacity(), 1e-9);
        assertEquals(256, TrapTier.MEDIUM.defaults().capacity(), 1e-9);
        assertEquals(2048, TrapTier.LARGE.defaults().capacity(), 1e-9);
        assertEquals(20, PenningTrapModel.ratedDemand(TrapTier.SMALL.defaults()), 1e-9);
        assertEquals(80, PenningTrapModel.ratedDemand(TrapTier.MEDIUM.defaults()), 1e-9);
        assertEquals(250, PenningTrapModel.ratedDemand(TrapTier.LARGE.defaults()), 1e-9);
        for (TrapTier tier : TrapTier.values()) {
            assertEquals(60, tier.defaults().quenchTicks());
            assertEquals(tier, TrapTier.byId(tier.id()));
        }
        assertEquals(0x3FD8E8, TrapTier.SMALL.colour());
        assertEquals(0x9B5CFF, TrapTier.MEDIUM.colour());
        assertEquals(0xFFB23F, TrapTier.LARGE.colour());
        assertEquals(TrapTier.SMALL, TrapTier.byId("nonsense"));
    }

    @Test
    void demandRisesWithTheCharge() {
        assertEquals(8, PenningTrapModel.demand(S, 0), 1e-9);
        assertEquals(14, PenningTrapModel.demand(S, 16), 1e-9);
        assertEquals(20, PenningTrapModel.demand(S, 32), 1e-9);
        assertEquals(20, PenningTrapModel.demand(S, 1000), 1e-9, "over-full counts as full");
        assertEquals(9, PenningTrapModel.demandFe(S, 1), "rounded up");
        assertEquals(8, PenningTrapModel.demandFe(S, 0));
    }

    @Test
    void theBufferHoldsTenSecondsOfFullDemand() {
        assertEquals(4000, PenningTrapModel.bufferCapacity(S));
        assertEquals(16000, PenningTrapModel.bufferCapacity(PenningTrapModel.Params.MEDIUM));
        assertEquals(50000, PenningTrapModel.bufferCapacity(PenningTrapModel.Params.LARGE));
    }

    @Test
    void aWellFedTrapIsStableAndLosesNothing() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, 4000, Q);
        assertEquals(TrapState.STABLE, step.state());
        assertEquals(20, step.feUsed());
        assertEquals(1f, step.field(), 1e-6);
        assertEquals(Q, step.quenchTicksLeft());
        assertFalse(step.annihilate());
        assertTrue(PenningTrapModel.holds(step.field()));
    }

    @Test
    void anEmptyTrapWithoutPowerIsOfflineAndHarmless() {
        int left = Q;
        for (int i = 0; i < 1000; i++) {
            PenningTrapModel.Step step = PenningTrapModel.tick(S, 0, 0, left);
            assertEquals(TrapState.OFFLINE, step.state());
            assertFalse(step.annihilate());
            assertEquals(Q, step.quenchTicksLeft());
            left = step.quenchTicksLeft();
        }
    }

    @Test
    void anEmptyTrapWithPowerIsReady() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 0, 4000, Q);
        assertEquals(TrapState.STABLE, step.state());
        assertEquals(8, step.feUsed());
        assertTrue(PenningTrapModel.holds(step.field()));
        assertFalse(PenningTrapModel.holds(PenningTrapModel.tick(S, 0, 3, Q).field()));
    }

    @Test
    void aLowBufferWarnsBeforeTheFieldGoes() {
        // 25% of 4000 is 1000: 1010 - 20 is below it.
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, 1010, Q);
        assertEquals(TrapState.LOW_POWER, step.state());
        assertFalse(step.annihilate());
        assertEquals(TrapState.STABLE, PenningTrapModel.tick(S, 32, 1100, Q).state());
    }

    @Test
    void aLoadedTrapWithoutPowerQuenchesForThreeSecondsThenAnnihilates() {
        int left = Q;
        int ticks = 0;
        PenningTrapModel.Step step;
        do {
            step = PenningTrapModel.tick(S, 10, 0, left);
            left = step.quenchTicksLeft();
            ticks++;
            if (!step.annihilate()) {
                assertEquals(TrapState.QUENCHING, step.state());
            }
        } while (!step.annihilate() && ticks < 1000);
        assertEquals(60, ticks);
        assertTrue(step.state().alarming());
    }

    @Test
    void anyShortfallQuenches() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, 19, Q);
        assertEquals(TrapState.QUENCHING, step.state());
        assertEquals(19, step.feUsed());
        assertEquals(0.95f, step.field(), 1e-6);
        assertEquals(Q - 1, step.quenchTicksLeft());
    }

    @Test
    void restoringPowerStopsTheQuenchAndItRecoversSlowly() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, 4000, 10);
        assertEquals(TrapState.LOW_POWER, step.state(), "still recovering");
        assertEquals(11, step.quenchTicksLeft());
        assertFalse(step.annihilate());
        int left = step.quenchTicksLeft();
        for (int i = 0; i < 100; i++) {
            left = PenningTrapModel.tick(S, 32, 4000, left).quenchTicksLeft();
        }
        assertEquals(Q, left);
        assertEquals(TrapState.STABLE, PenningTrapModel.tick(S, 32, 4000, left).state());
    }

    @Test
    void flickeringPowerDoesNotResetTheCountdown() {
        int left = Q;
        boolean annihilated = false;
        // Two ticks off, one on: the countdown loses one tick every three.
        for (int i = 0; i < 1000 && !annihilated; i++) {
            PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, i % 3 == 0 ? 4000 : 0, left);
            left = step.quenchTicksLeft();
            annihilated = step.annihilate();
        }
        assertTrue(annihilated);
    }

    @Test
    void anEmptiedTrapStopsQuenching() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 0, 0, 5);
        assertEquals(TrapState.OFFLINE, step.state());
        assertEquals(Q, step.quenchTicksLeft());
        assertFalse(step.annihilate());
    }

    @Test
    void outOfRangeCountdownsAreClamped() {
        assertEquals(Q, PenningTrapModel.tick(S, 32, 4000, 9999).quenchTicksLeft());
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, 0, -5);
        assertTrue(step.annihilate());
        assertEquals(0, step.quenchTicksLeft());
    }

    @Test
    void negativeBuffersAreTreatedAsEmpty() {
        PenningTrapModel.Step step = PenningTrapModel.tick(S, 32, -50, Q);
        assertEquals(0, step.feUsed());
        assertEquals(0f, step.field(), 1e-6);
    }

    @Test
    void bufferAndAnnihilationTimes() {
        assertEquals(200, PenningTrapModel.bufferTicksLeft(S, 32, 4000));
        assertEquals(400, PenningTrapModel.bufferTicksLeft(S, 32, 4000, 10));
        assertEquals(Integer.MAX_VALUE, PenningTrapModel.bufferTicksLeft(S, 32, 4000, 20));
        assertEquals(260, PenningTrapModel.ticksToAnnihilation(S, 32, 4000, Q, 0));
        assertEquals(Integer.MAX_VALUE, PenningTrapModel.ticksToAnnihilation(S, 0, 0, Q, 0), "empty traps never blow");
        assertEquals(Integer.MAX_VALUE, PenningTrapModel.ticksToAnnihilation(S, 32, 0, Q, 100));
        assertEquals(30, PenningTrapModel.ticksToAnnihilation(S, 32, 0, 30, 0));
    }

    @Test
    void comparatorFollowsTheFill() {
        assertEquals(0, PenningTrapModel.comparator(S, 0));
        assertEquals(1, PenningTrapModel.comparator(S, 0.01));
        assertEquals(8, PenningTrapModel.comparator(S, 16));
        assertEquals(15, PenningTrapModel.comparator(S, 32));
        assertEquals(15, PenningTrapModel.comparator(S, 64));
    }

    @Test
    void fillAndSpace() {
        assertEquals(0.5, PenningTrapModel.fill(S, 16), 1e-9);
        assertEquals(16, PenningTrapModel.space(S, 16), 1e-9);
        assertEquals(0, PenningTrapModel.space(S, 40), 1e-9);
        assertEquals(32, PenningTrapModel.space(S, -3), 1e-9);
        assertFalse(PenningTrapModel.loaded(0));
        assertFalse(PenningTrapModel.loaded(1e-12));
        assertTrue(PenningTrapModel.loaded(1e-6));
    }

    @Test
    void theCloudGrowsWithTheCubeRootOfTheCharge() {
        assertEquals(0, PenningTrapModel.cloudScale(S, 0), 1e-9);
        assertEquals(0.5, PenningTrapModel.cloudScale(S, 4), 1e-9);
        assertEquals(1, PenningTrapModel.cloudScale(S, 32), 1e-9);
    }

    @Test
    void theHumFollowsFieldAndCharge() {
        assertEquals(0, PenningTrapModel.humVolume(S, 32, 0f), 1e-6);
        assertTrue(PenningTrapModel.humVolume(S, 32, 1f) > PenningTrapModel.humVolume(S, 0, 1f));
        assertTrue(PenningTrapModel.humPitch(S, 32) > PenningTrapModel.humPitch(S, 0));
        assertEquals(0.8f, PenningTrapModel.humPitch(S, 0), 1e-6);
    }

    @Test
    void statesRoundTripByOrdinal() {
        for (TrapState state : TrapState.values()) {
            assertEquals(state, TrapState.byOrdinal(state.ordinal()));
        }
        assertEquals(TrapState.OFFLINE, TrapState.byOrdinal(-1));
        assertEquals(TrapState.OFFLINE, TrapState.byOrdinal(99));
        assertFalse(TrapState.STABLE.alarming());
    }
}
