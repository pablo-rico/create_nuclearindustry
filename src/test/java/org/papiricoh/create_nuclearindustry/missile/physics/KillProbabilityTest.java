package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class KillProbabilityTest {
    private double pk(MissileFlight.Phase phase, double cos, double range, int radars, double age, double scale, double cap) {
        return KillProbability.of(new KillProbability.Inputs(0.8, phase, cos, range, radars, age, 1, 0.7, scale, cap));
    }
    @Test void factorsAndClamps() {
        var mid = MissileFlight.Phase.MIDCOURSE;
        assertEquals(0.8, pk(mid, 1, 0, 3, 4, 1, 1), 1e-12);
        assertEquals(0, pk(MissileFlight.Phase.BOOST, 1, 0, 3, 4, 1, 1));
        assertEquals(0, pk(MissileFlight.Phase.IMPACT, 1, 0, 3, 4, 1, 1));
        assertEquals(0, pk(mid, 1, 0, 0, 4, 1, 1));
        assertEquals(0.56, pk(MissileFlight.Phase.TERMINAL, 1, 0, 3, 4, 1, 1), 1e-12);
        assertEquals(0.95, pk(mid, 1, 0, 3, 4, 10, 0.95));
        assertEquals(0, pk(mid, 1, 0, 3, 4, -1, 1));
        assertEquals(pk(mid, 0, 0, 3, 4, 1, 1), pk(mid, -5, 0, 3, 4, 1, 1));
    }
    @Test void improvesWithGeometryRadarsAgeAndShorterRange() {
        var mid = MissileFlight.Phase.REENTRY;
        assertTrue(pk(mid, 1, 0, 3, 4, 1, 1) > pk(mid, 0, 0, 3, 4, 1, 1));
        for (int n = 0; n < 3; n++) assertTrue(pk(mid, 1, 0, n+1, 4, 1, 1) > pk(mid, 1, 0, n, 4, 1, 1));
        assertTrue(pk(mid, 1, 0, 3, 4, 1, 1) > pk(mid, 1, 0, 3, 0, 1, 1));
        assertTrue(pk(mid, 1, 0, 3, 4, 1, 1) > pk(mid, 1, 1, 3, 4, 1, 1));
    }
}
