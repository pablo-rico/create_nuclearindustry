package org.papiricoh.create_nuclearindustry.missile.radar;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The radar equation, the horizon, signatures, the impact prediction and its ellipse, and track names. */
class RadarModelTest {

    @Test
    void reachGrowsWithTheFourthRootOfSpeedAndSignature() {
        assertEquals(3000, RadarModel.range(3000, 32, 32, 1), 1e-9);
        assertEquals(3000 * Math.pow(2, 0.25), RadarModel.range(3000, 64, 32, 1), 1e-6, "twice the speed, 19% further");
        assertEquals(3000 * Math.pow(16, 0.25), RadarModel.range(3000, 32, 32, 16), 1e-6, "sixteen times brighter, twice as far");
        assertEquals(RadarModel.range(3000, -64, 32, 1), RadarModel.range(3000, 64, 32, 1), 1e-9, "either direction of turn");
        assertEquals(0, RadarModel.range(3000, 32, 32, 0), "nothing to see");
        assertEquals(Double.POSITIVE_INFINITY, RadarModel.range(0, 32, 32, 0.3), "a base range of 0 sees everything");
    }

    @Test
    void theHorizonHidesLowTargetsFarOff() {
        // A radar 7 blocks above sea level and a target 237 above it see each other about 3.2 km apart.
        double horizon = RadarModel.horizon(16000, 70, 300);
        assertEquals(Math.sqrt(2 * 16000 * 7) + Math.sqrt(2 * 16000 * 237), horizon, 1e-6);
        assertTrue(horizon > 3000 && horizon < 3500);
        assertTrue(RadarModel.horizon(16000, 70, 100) < 1700, "a launch beyond ~1.7 km is hidden until it climbs");
        assertEquals(Double.POSITIVE_INFINITY, RadarModel.horizon(0, 70, 64), "no horizon on a flat world");
        V3 radar = new V3(0, 70, 0);
        assertTrue(RadarModel.detects(radar, new V3(3000, 300, 0), 5000, 16000));
        assertFalse(RadarModel.detects(radar, new V3(3000, 90, 0), 5000, 16000), "below the horizon");
        assertFalse(RadarModel.detects(radar, new V3(3000, 300, 0), 2000, 16000), "out of reach");
        assertTrue(RadarModel.detects(radar, new V3(3000, 90, 0), 5000, 0), "with no horizon only reach matters");
    }

    @Test
    void aBurningRocketIsBrightAndALoneWarheadFaint() {
        assertEquals(8, RadarModel.signature(MissileFlight.Phase.BOOST));
        assertEquals(0.3, RadarModel.signature(MissileFlight.Phase.MIDCOURSE), 1e-9);
        assertEquals(0.6, RadarModel.signature(MissileFlight.Phase.REENTRY), 1e-9, "the air heating it doubles a reentry vehicle");
        assertEquals(RadarModel.signature(MissileFlight.Phase.REENTRY), RadarModel.signature(MissileFlight.Phase.TERMINAL));
        assertEquals(0, RadarModel.signature(MissileFlight.Phase.IMPACT));
        assertTrue(RadarModel.range(3000, 32, 32, RadarModel.signature(MissileFlight.Phase.BOOST))
                > 2 * RadarModel.range(3000, 32, 32, RadarModel.signature(MissileFlight.Phase.MIDCOURSE)));
    }

    @Test
    void theImpactPointIsWhereFreeFallEnds() {
        double g = 0.005;
        V3 pos = new V3(0, 563, 0), vel = new V3(2, 0, 1);
        // 500 blocks above the ground with no climb: t = sqrt(2·500/g) ≈ 447 ticks.
        double t = RadarModel.timeToGround(pos, vel, g, 63);
        assertEquals(Math.sqrt(2 * 500 / g), t, 1e-6);
        V3 impact = RadarModel.impactPoint(pos, vel, g, 63);
        assertEquals(2 * t, impact.x(), 1e-6);
        assertEquals(t, impact.z(), 1e-6);
        assertEquals(63, impact.y());
        assertTrue(RadarModel.timeToGround(new V3(0, 563, 0), new V3(0, 3, 0), g, 63) > t, "climbing takes longer to come down");
        assertEquals(0, RadarModel.timeToGround(new V3(0, 40, 0), new V3(0, 0, 0), g, 63), "already below the ground");
    }

    @Test
    void theErrorEllipseLiesAlongTheTrackAndTightensNearTheEnd() {
        V3 east = new V3(3, -1, 0);
        RadarModel.Ellipse early = RadarModel.ellipse(east, 400, false);
        RadarModel.Ellipse late = RadarModel.ellipse(east, 20, false);
        RadarModel.Ellipse boosting = RadarModel.ellipse(east, 400, true);
        assertTrue(early.along() > late.along(), "it tightens as the time to impact runs out");
        assertTrue(boosting.along() > early.along(), "and is wide while the engines still burn");
        assertTrue(early.along() > early.across(), "longer along the track than across it");
        assertEquals(Math.PI / 2, early.heading(), 1e-9, "heading east");
        // Along the track (east) the ellipse reaches further than across it (south).
        assertTrue(early.touches(0, 0, early.along() - 1, 0, 0.5));
        assertFalse(early.touches(0, 0, 0, early.along() - 1, 0.5));
        assertTrue(early.touches(0, 0, 0, early.across() + 10, 11), "a zone's radius counts");
        assertTrue(early.touches(0, 0, 1, 1, 0), "a zone centred inside");
    }

    @Test
    void designationsAreStableFourDigitNames() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String name = RadarModel.designation(id);
        assertTrue(name.matches("T-\\d{4}"), name);
        assertEquals(name, RadarModel.designation(UUID.fromString(id.toString())), "the same flight, the same name");
    }
}
