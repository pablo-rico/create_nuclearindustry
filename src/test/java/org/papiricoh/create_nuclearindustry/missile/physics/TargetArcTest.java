package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class TargetArcTest {
    @Test void agreesWithEveryTrackerStep() {
        var params = BallisticModel.Params.DEFAULT;
        var flight = new MissileFlight(MissileDesign.LEGACY_PAD, V3.ZERO, new V3(1500, 0, 0), V3.ZERO);
        assertNull(TargetArc.of(flight, params.gravity()));
        for (int i = 0; i < 10000 && !flight.burnedOut(); i++) flight.step(params);
        var arc = TargetArc.of(flight, params.gravity());
        assertNotNull(arc);
        for (int n = 1; n < Math.min(100, arc.remaining()); n++) {
            flight.step(params);
            assertTrue(arc.at(n).distance(flight.position()) < 1e-9);
            assertTrue(arc.velocityAt(n).distance(flight.velocity()) < 1e-9);
        }
        assertFalse(arc.alive(arc.remaining()));
    }
}
