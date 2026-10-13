package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatBoardTest {

    private static ThreatBoard.Threat at(int x, int z, int radius, long impactAt, long expiresAt) {
        return new ThreatBoard.Threat(x, 64, z, radius, impactAt, expiresAt);
    }

    @Test
    void reachAddsTheListenRadiusToTheThreatRadius() {
        ThreatBoard.Threat threat = at(100, 0, 20, 50, 100);
        assertTrue(threat.reaches(20, 0, 60), "80 blocks away, 60 + 20 reach");
        assertFalse(threat.reaches(19, 0, 60), "81 blocks away is out of reach");
        assertTrue(threat.reaches(100, 0, 0), "a siren at the target hears it even listening nowhere");
    }

    @Test
    void reachIsHorizontal() {
        ThreatBoard.Threat high = new ThreatBoard.Threat(0, 300, 0, 0, 10, 20);
        assertTrue(high.reaches(0, 0, 1), "height does not matter");
    }

    @Test
    void soonestImpactInReachWins() {
        ThreatBoard board = new ThreatBoard();
        ThreatBoard.Threat late = at(10, 0, 0, 500, 600);
        ThreatBoard.Threat soon = at(-10, 0, 0, 200, 300);
        ThreatBoard.Threat farAway = at(5000, 0, 0, 10, 300);
        board.put("late", late);
        board.put("soon", soon);
        board.put("far", farAway);
        assertSame(soon, board.soonestReaching(0, 0, 50, 0));
        assertEquals(190, soon.ticksToImpact(10));
        assertEquals(0, soon.ticksToImpact(400), "never negative");
    }

    @Test
    void reportingAgainReplacesAndWithdrawingRemoves() {
        ThreatBoard board = new ThreatBoard();
        board.put("missile", at(0, 0, 0, 100, 140));
        board.put("missile", at(0, 0, 0, 90, 130));
        assertEquals(1, board.size());
        assertEquals(90, board.get("missile").impactAt());
        board.remove("missile");
        assertNull(board.soonestReaching(0, 0, 10, 0));
    }

    @Test
    void entriesNotReportedAgainExpire() {
        ThreatBoard board = new ThreatBoard();
        board.put("bomb", at(0, 0, 0, 100, 140));
        assertTrue(board.soonestReaching(0, 0, 10, 140) != null, "still there on its last tick");
        assertNull(board.soonestReaching(0, 0, 10, 141), "gone once it expires");
        assertEquals(0, board.size());
    }
}
