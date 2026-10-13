package org.papiricoh.create_nuclearindustry.radiation.contamination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkContaminationTest {
    private static final long HALF_LIFE = 240_000;

    @Test
    void halvesEveryHalfLife() {
        assertEquals(8f, ChunkContamination.decay(8f, 0, HALF_LIFE), 1e-6);
        assertEquals(4f, ChunkContamination.decay(8f, HALF_LIFE, HALF_LIFE), 1e-5);
        assertEquals(2f, ChunkContamination.decay(8f, 2 * HALF_LIFE, HALF_LIFE), 1e-5);
    }

    @Test
    void zeroHalfLifeNeverDecays() {
        assertEquals(8f, ChunkContamination.decay(8f, 10 * HALF_LIFE, 0), 1e-6);
    }

    @Test
    void decayIsLazyFromTheLastUpdate() {
        ChunkContamination record = new ChunkContamination(6f, 1000, 0, 0);
        assertEquals(3f, record.levelAt(1000 + HALF_LIFE, HALF_LIFE), 1e-5);
        assertEquals(6f, record.levelAt(500, HALF_LIFE), 1e-6, "reading before the last update must not grow it");
    }

    @Test
    void severityClimbsLogarithmically() {
        assertEquals(0, ChunkContamination.severity(0f));
        assertEquals(0, ChunkContamination.severity(0.049f));
        assertEquals(1, ChunkContamination.severity(0.05f));
        assertEquals(4, ChunkContamination.severity(1f));
        assertEquals(6, ChunkContamination.severity(5f));
        assertEquals(ChunkContamination.MAX_SEVERITY, ChunkContamination.severity(1000f));
        for (float level = 0; level < 20; level += 0.01f) {
            assertTrue(ChunkContamination.severity(level) <= ChunkContamination.severity(level + 0.01f));
        }
    }
}
