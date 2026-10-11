package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The annihilation generator's burn and output in its three modes. */
class AnnihilationModelTest {
    private static final AnnihilationModel.Params P = AnnihilationModel.Params.DEFAULT;

    @Test
    void fullThrottleBurnsSixNanogramsAMinute() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, 100, AnnihilationModel.Mode.ROTATION);
        assertEquals(6.0, out.burnt() * 1200, 1e-9);
        assertEquals(256, out.capacity(), 1e-9);
        assertEquals(0, out.energy(), 1e-9);
    }

    @Test
    void electricModeMakesForgeEnergyInstead() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, 100, AnnihilationModel.Mode.ELECTRIC);
        assertEquals(0, out.capacity(), 1e-9);
        assertEquals(16384, out.energy(), 1e-9);
        AnnihilationModel.Output split = AnnihilationModel.tick(P, 1, 100, AnnihilationModel.Mode.SPLIT);
        assertEquals(128, split.capacity(), 1e-9);
        assertEquals(8192, split.energy(), 1e-9);
    }

    @Test
    void zeroThrottleKeepsOnlyTheStandbyBurn() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 0, 100, AnnihilationModel.Mode.ROTATION);
        assertEquals(P.ratedBurn() * P.standbyShare(), out.burnt(), 1e-12);
        assertEquals(0, out.capacity(), 1e-9);
    }

    @Test
    void runningShortGivesThatShare() {
        AnnihilationModel.Output out = AnnihilationModel.tick(P, 1, P.ratedBurn() / 2, AnnihilationModel.Mode.ROTATION);
        assertEquals(P.ratedBurn() / 2, out.burnt(), 1e-12);
        assertEquals(128, out.capacity(), 1e-9);
        assertSame(AnnihilationModel.Output.NONE, AnnihilationModel.tick(P, 1, 0, AnnihilationModel.Mode.ROTATION));
    }

    @Test
    void tenNanogramsLastAlmostTwoMinutesFlatOut() {
        assertEquals(10.0 / 6.0, AnnihilationModel.minutesOn(P, 10, 1), 1e-9);
    }
}
