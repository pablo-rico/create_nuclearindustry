package org.papiricoh.create_nuclearindustry.missile.defense;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class EngagementPlannerTest {
    @Test void urgentThreatGetsTheEarliestShooterAndAmmoIsNeverReused() {
        UUID slow = UUID.randomUUID(), urgent = UUID.randomUUID();
        var threats = List.of(new EngagementPlanner.Threat(slow, 200, 0, 0), new EngagementPlanner.Threat(urgent, 40, 0, 0));
        var shooters = List.of(new EngagementPlanner.Shooter(0, 1), new EngagementPlanner.Shooter(1, 2));
        var plan = EngagementPlanner.plan(threats, shooters, Doctrine.SALVO_TWO, (t,s) -> s.index() == 0 ? 10 : 20);
        assertEquals(3, plan.size());
        assertEquals(urgent, plan.get(0).threat());
        assertEquals(0, plan.get(0).shooter());
        assertEquals(urgent, plan.get(1).threat());
        assertEquals(slow, plan.get(2).threat());
        assertEquals(1, plan.stream().filter(a -> a.shooter() == 0).count());
    }
    @Test void engagedDuplicateUnreachableAndEmptyAreSkipped() {
        var id = UUID.randomUUID();
        var threat = new EngagementPlanner.Threat(id, 10, 0, 0);
        var shooters = List.of(new EngagementPlanner.Shooter(0, 4));
        assertEquals(1, EngagementPlanner.plan(List.of(threat, threat), shooters, Doctrine.SHOOT_LOOK_SHOOT, (t,s)->3).size());
        assertTrue(EngagementPlanner.plan(List.of(new EngagementPlanner.Threat(id, 10, 1, 0)), shooters, Doctrine.SALVO_TWO, (t,s)->3).isEmpty());
        assertTrue(EngagementPlanner.plan(List.of(new EngagementPlanner.Threat(id, 10, 0, 1)), shooters, Doctrine.SALVO_TWO, (t,s)->3).isEmpty());
        assertTrue(EngagementPlanner.plan(List.of(threat), shooters, Doctrine.SALVO_TWO, (t,s)->-1).isEmpty());
        assertTrue(EngagementPlanner.plan(List.of(threat), List.of(new EngagementPlanner.Shooter(0, 0)), Doctrine.SALVO_TWO, (t,s)->3).isEmpty());
    }
}
