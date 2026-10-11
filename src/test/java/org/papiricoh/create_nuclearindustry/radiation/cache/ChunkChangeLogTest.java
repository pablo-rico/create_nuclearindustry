package org.papiricoh.create_nuclearindustry.radiation.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkChangeLogTest {

    @Test
    void aResultIsStaleOnlyAfterAChangeInItsChunks() {
        ChunkChangeLog log = new ChunkChangeLog();
        log.mark(0, 0, 10);
        long since = log.issue();
        assertFalse(log.changedSince(since, -1, -1, 1, 1), "changes before the result do not count");
        log.mark(5, 5, 11);
        assertFalse(log.changedSince(since, -1, -1, 1, 1), "a change elsewhere does not count");
        assertTrue(log.changedSince(since, 4, 4, 6, 6), "a change in the rectangle does");
        log.mark(1, -1, 12);
        assertTrue(log.changedSince(since, -1, -1, 1, 1), "and a later change in it makes it stale");
    }

    @Test
    void repeatedChangesToAStaleChunkCostNoNewSequence() {
        ChunkChangeLog log = new ChunkChangeLog();
        log.issue();
        log.mark(2, 3, 1);
        long sequence = log.sequence();
        for (int i = 0; i < 100; i++) {
            log.mark(2, 3, 1 + i);
        }
        assertEquals(sequence, log.sequence(), "nothing cached since: the chunk is already newer than every result");
        long since = log.issue();
        log.mark(2, 3, 200);
        assertEquals(sequence + 1, log.sequence(), "once a result is taken the next change counts again");
        assertTrue(log.changedSince(since, 2, 3, 2, 3));
    }

    @Test
    void oldChangesAreForgotten() {
        ChunkChangeLog log = new ChunkChangeLog();
        log.mark(0, 0, 10);
        log.mark(1, 0, 50);
        log.mark(-3, 7, 90);
        assertEquals(3, log.size());
        log.forgetBefore(60);
        assertEquals(1, log.size(), "only the change at 90 is kept");
        assertTrue(log.changedSince(0, -3, 7, -3, 7));
        assertFalse(log.changedSince(0, 0, 0, 1, 0));
    }

    @Test
    void negativeAndLargeChunkCoordinatesDoNotCollide() {
        ChunkChangeLog log = new ChunkChangeLog();
        long since = log.issue();
        log.mark(-1, 0, 1);
        assertFalse(log.changedSince(since, 0, -1, 0, -1));
        assertFalse(log.changedSince(since, 1_875_000, 0, 1_875_000, 0));
        assertTrue(log.changedSince(since, -1, 0, -1, 0));
    }
}
