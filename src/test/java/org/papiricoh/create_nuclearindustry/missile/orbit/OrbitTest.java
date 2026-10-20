package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import static org.junit.jupiter.api.Assertions.*;

class OrbitTest {
    private static final Orbit.Bounds BORDER = new Orbit.Bounds(-5000, -5000, 5000, 5000);

    @Test
    void aGeostationarySatelliteStaysOverItsAnchor() {
        Orbit geo = Orbit.geo(1200, -300, 12000, 100, BORDER);
        for (long t : new long[]{100, 5000, 1_000_000}) {
            V3 at = geo.position(t);
            assertEquals(1200, at.x(), 1e-9);
            assertEquals(-300, at.z(), 1e-9);
            assertEquals(Orbit.SEA_LEVEL + 12000, at.y(), 1e-9);
        }
        assertEquals(V3.ZERO, geo.velocity(0));
    }

    @Test
    void anchorsAreKeptInsideTheBorder() {
        Orbit geo = Orbit.geo(9000, -9000, 12000, 0, BORDER);
        assertEquals(5000, geo.anchorX(), 1e-9);
        assertEquals(-5000, geo.anchorZ(), 1e-9);
    }

    @Test
    void aLowSatelliteIsOverItsAnchorAtItsEpochAndFliesItsHeading() {
        Orbit leo = Orbit.leo(100, 200, Math.PI / 2, 1600, 1000, 10, BORDER);
        assertEquals(100, leo.position(1000).x(), 1e-9);
        assertEquals(200, leo.position(1000).z(), 1e-9);
        V3 later = leo.position(1010);
        assertEquals(200, later.x(), 1e-6);
        assertEquals(200, later.z(), 1e-6);
        assertEquals(10, leo.velocity(0).length(), 1e-9);
    }

    @Test
    void leavingTheBorderItComesBackOnTheFarSide() {
        Orbit leo = Orbit.leo(4990, 0, Math.PI / 2, 1600, 0, 10, BORDER);
        V3 at = leo.position(2);
        assertEquals(-4990, at.x(), 1e-6);
        for (int t = 0; t < 50_000; t += 37) assertTrue(BORDER.contains(leo.position(t).x(), leo.position(t).z()));
    }

    @Test
    void aSlantedTrackSweepsTheWorld() {
        Orbit leo = Orbit.leo(0, 0, Math.toRadians(30), 1600, 0, 20, BORDER);
        double footprint = 600;
        double earthRadius = footprint * footprint / (2 * 1600);
        assertEquals(footprint, leo.footprint(earthRadius), 1e-6);
        // A far corner of the world is passed over sooner or later.
        assertTrue(leo.nextPass(4500, -4500, 0, 2_000_000, earthRadius, 5) >= 0);
    }

    @Test
    void theUnwrappedPathIsStraightAndStartsWhereItIs() {
        Orbit leo = Orbit.leo(4990, 0, Math.PI / 2, 1600, 0, 10, BORDER);
        V3 now = leo.position(100);
        assertEquals(now.x(), leo.unwrapped(100, 100).x(), 1e-6);
        assertEquals(now.x() + 50, leo.unwrapped(105, 100).x(), 1e-6);
    }

    @Test
    void footprintGrowsWithAltitudeAndNoHorizonSeesEverything() {
        Orbit low = Orbit.leo(0, 0, 0, 1600, 0, 10, BORDER);
        Orbit high = Orbit.geo(0, 0, 12000, 0, BORDER);
        assertTrue(high.footprint(16000) > low.footprint(16000));
        assertTrue(Double.isInfinite(low.footprint(0)));
        assertTrue(low.covers(4000, 4000, 0, 0));
    }

    @Test
    void nbtAndNetworkRoundTrip() {
        Orbit leo = Orbit.leo(10, 20, 1.25, 1600, 77, 12, BORDER);
        assertEquals(leo, Orbit.load(leo.save()));
        io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        leo.write(buf);
        Orbit read = Orbit.read(buf);
        assertEquals(leo.position(5000).x(), read.position(5000).x(), 1e-2);
        assertEquals(leo.position(5000).z(), read.position(5000).z(), 1e-2);
    }

    @Test
    void anOrbitPathMeetsTheSatelliteWithoutWrapping() {
        Orbit leo = Orbit.leo(4990, 0, Math.PI / 2, 1600, 0, 10, BORDER);
        OrbitPath path = new OrbitPath(leo, 0, 400);
        assertEquals(5010, path.at(2).x(), 1e-6);
        assertEquals(10, path.velocityAt(0).length(), 1e-9);
        assertTrue(path.alive(399));
        assertFalse(path.alive(400));
    }
}
