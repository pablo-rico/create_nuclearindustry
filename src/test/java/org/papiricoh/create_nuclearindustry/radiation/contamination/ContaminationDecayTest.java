package org.papiricoh.create_nuclearindustry.radiation.contamination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaminationDecayTest {
    private static final double MAX = 50, CLEAN = 0.01;

    @Test
    void theMaximumIsGoneAfterTheConfiguredDays() {
        long halfLife = ContaminationDecay.halfLifeTicks(7, MAX, CLEAN);
        long sevenDays = 7L * ContaminationDecay.TICKS_PER_DAY;
        assertTrue(ChunkContamination.decay((float) MAX, sevenDays - 2 * halfLife / 10, halfLife) >= CLEAN, "still there just before");
        assertTrue(ChunkContamination.decay((float) MAX, sevenDays + 1, halfLife) < CLEAN, "gone after seven days");
        assertEquals(sevenDays, ContaminationDecay.ticksToClear(MAX, CLEAN, halfLife), halfLife / 100.0);
    }

    @Test
    void weakerContaminationClearsSooner() {
        long halfLife = ContaminationDecay.halfLifeTicks(7, MAX, CLEAN);
        assertTrue(ContaminationDecay.ticksToClear(1, CLEAN, halfLife) < ContaminationDecay.ticksToClear(20, CLEAN, halfLife));
        assertEquals(0, ContaminationDecay.ticksToClear(0.005, CLEAN, halfLife));
    }

    @Test
    void itFallsFastestAtFirst() {
        long halfLife = ContaminationDecay.halfLifeTicks(7, MAX, CLEAN);
        float afterOneDay = ChunkContamination.decay((float) MAX, ContaminationDecay.TICKS_PER_DAY, halfLife);
        assertTrue(MAX - afterOneDay > MAX / 2, "more than half goes on the first day, got " + afterOneDay);
    }

    @Test
    void zeroDaysNeverDecays() {
        assertEquals(0, ContaminationDecay.halfLifeTicks(0, MAX, CLEAN));
        assertEquals(-1, ContaminationDecay.ticksToClear(MAX, CLEAN, 0));
    }

    @Test
    void moreDaysMeansALongerHalfLife() {
        assertTrue(ContaminationDecay.halfLifeTicks(30, MAX, CLEAN) > ContaminationDecay.halfLifeTicks(7, MAX, CLEAN));
        assertTrue(ContaminationDecay.halfLifeTicks(7, 0, CLEAN) > 0, "a zero maximum still gives a valid half-life");
    }
}
