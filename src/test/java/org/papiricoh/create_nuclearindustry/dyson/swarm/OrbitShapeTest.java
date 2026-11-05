package org.papiricoh.create_nuclearindustry.dyson.swarm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An orbit's shape stays in range whatever it is given. */
class OrbitShapeTest {
    @Test
    void outOfRangeValuesAreClamped() {
        OrbitShape s = new OrbitShape(true, 500, -5, 725);
        assertEquals(OrbitShape.MAX_RADIUS, s.radius());
        assertEquals(0, s.inclination());
        assertEquals(5, s.node(), 1.0e-4);
        OrbitShape nan = new OrbitShape(true, Float.NaN, Float.POSITIVE_INFINITY, Float.NaN);
        assertEquals(OrbitShape.MIN_RADIUS, nan.radius());
        assertEquals(0, nan.inclination());
        assertEquals(0, nan.node());
    }

    @Test
    void aTerminalMaySendOnlyInRangeValues() {
        assertTrue(OrbitShape.valid(20, 45, 180));
        assertFalse(OrbitShape.valid(5, 45, 180));
        assertFalse(OrbitShape.valid(20, 91, 180));
        assertFalse(OrbitShape.valid(20, 45, Float.NaN));
        assertFalse(OrbitShape.validIndex(4));
        assertFalse(OrbitShape.validIndex(-1));
    }

    @Test
    void onlyTheFirstOrbitStartsOpen() {
        assertTrue(OrbitShape.defaults(0).enabled());
        for (int i = 1; i < OrbitShape.COUNT; i++) assertFalse(OrbitShape.defaults(i).enabled());
    }
}
