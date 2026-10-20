package org.papiricoh.create_nuclearindustry.missile.silo;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.*;

class RocketAdvisorTest {
    @Test void allColumnsThroughEightAgreeWithParser() { enumerate(new ArrayList<>()); }
    private void enumerate(List<RocketComposition.Section> column) {
        boolean parsed;
        try { RocketComposition.parse(column); parsed = true; }
        catch (RocketComposition.Invalid e) { parsed = false; }
        boolean advised = RocketAdvisor.advise(column).stream().allMatch(a -> a.action() == LaunchDiagram.Advice.INFO);
        assertEquals(parsed, advised, column.toString());
        if (column.size() == 8) return;
        for (var section : RocketComposition.Section.values()) {
            column.add(section);
            enumerate(column);
            column.removeLast();
        }
    }
    @Test void suggestsFirstMissingSectionAndFirstMismatch() {
        var empty = RocketAdvisor.advise(List.of()).getFirst();
        assertEquals(LaunchDiagram.Advice.ADD, empty.action());
        assertEquals(1 << ENGINE.ordinal(), empty.expectedMask());
        var missingTank = RocketAdvisor.advise(List.of(ENGINE)).getFirst();
        assertEquals(1, missingTank.index());
        assertEquals(1 << TANK.ordinal(), missingTank.expectedMask());
        var mismatch = RocketAdvisor.advise(List.of(ENGINE, GUIDANCE)).getFirst();
        assertEquals(LaunchDiagram.Advice.REPLACE, mismatch.action());
        assertEquals(1, mismatch.index());
        var missingWarhead = RocketAdvisor.advise(List.of(ENGINE, TANK, GUIDANCE)).getFirst();
        assertEquals(3, missingWarhead.index());
        assertEquals((1 << WARHEAD.ordinal()) | (1 << KILL_VEHICLE.ordinal()), missingWarhead.expectedMask());
    }
    @Test void validRocketOffersAnotherStageUntilThree() {
        var advice = RocketAdvisor.advise(List.of(ENGINE, TANK, GUIDANCE, WARHEAD)).getFirst();
        assertEquals(LaunchDiagram.Advice.INFO, advice.action());
        assertEquals(2, advice.index());
        assertEquals("add_stage", advice.key());
        assertEquals(1, advice.arg());
        assertTrue(RocketAdvisor.advise(List.of(ENGINE, TANK, ENGINE, TANK, ENGINE, TANK, GUIDANCE, WARHEAD)).isEmpty());
    }
}
