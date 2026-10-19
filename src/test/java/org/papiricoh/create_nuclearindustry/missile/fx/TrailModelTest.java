package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A smoke trail's points, spreading, fading and drifting. */
class TrailModelTest {

    private static TrailModel.Point at(long tick) {
        return new TrailModel.Point(tick, 100 + tick, 0, tick, 1, 1);
    }

    @Test
    void aPointEveryFiveTicks() {
        TrailModel trail = new TrailModel();
        assertTrue(trail.due(0));
        trail.add(at(0));
        assertFalse(trail.due(4));
        assertTrue(trail.due(TrailModel.SAMPLE_TICKS));
    }

    @Test
    void keepsAtMostItsCapDroppingTheOldest() {
        TrailModel trail = new TrailModel();
        int total = TrailModel.MAX_POINTS + 40;
        for (int i = 0; i < total; i++) trail.add(at(i * 5L));
        assertEquals(TrailModel.MAX_POINTS, trail.size());
        assertEquals(40 * 5L, trail.get(0).tick(), "the oldest went first");
        assertEquals((total - 1) * 5L, trail.newest().tick());
        for (int i = 1; i < trail.size(); i++) assertTrue(trail.get(i).tick() > trail.get(i - 1).tick(), "oldest first");
    }

    @Test
    void forgetsWhatOutlivedTheTrail() {
        TrailModel trail = new TrailModel();
        for (int i = 0; i < 20; i++) trail.add(at(i * 5L));
        trail.expire(100, 50);
        assertEquals(50, trail.get(0).tick());
        trail.expire(10_000, 50);
        assertTrue(trail.isEmpty());
        assertEquals(null, trail.newest());
    }

    @Test
    void smokeSpreadsAndThinsOut() {
        double life = 800;
        assertEquals(1, TrailModel.alpha(0, life), 1.0e-12);
        assertEquals(0, TrailModel.alpha(life, life), 1.0e-12);
        double lastW = 0, lastA = 2;
        for (int age = 0; age <= 2000; age += 20) {
            double w = TrailModel.width(age, 1.2);
            double a = TrailModel.alpha(age, life);
            assertTrue(w >= lastW && w <= TrailModel.MAX_WIDTH);
            assertTrue(a <= lastA && a >= 0);
            lastW = w;
            lastA = a;
        }
        assertTrue(TrailModel.width(400, 1.2) > 8, "a trail half a minute old is wide");
    }

    @Test
    void theWindBlowsHarderHighUpAndCarriesTheSmoke() {
        V3 low = TrailModel.wind(64, 1000);
        V3 high = TrailModel.wind(400, 1000);
        assertTrue(high.horizontalLength() > 2 * low.horizontalLength());
        assertEquals(0, low.y(), 1.0e-12);
        TrailModel.Point p = new TrailModel.Point(0, 300, 0, 0, 1, 1);
        V3 drifted = TrailModel.drifted(p, 200, high);
        assertEquals(high.horizontalLength() * 200, Math.hypot(drifted.x(), drifted.z()), 1.0e-9);
        assertTrue(drifted.y() < 300, "and it sinks a little");
    }
}
