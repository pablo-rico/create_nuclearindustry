package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How a rocket is heard: late, near or far, and the boom of a warhead passing close. */
class RocketAcousticsTest {

    @Test
    void soundArrivesAtSeventeenBlocksATick() {
        assertEquals(0, RocketAcoustics.delayTicks(0));
        assertEquals(1, RocketAcoustics.delayTicks(17.15));
        assertEquals(58, RocketAcoustics.delayTicks(1000));
    }

    @Test
    void theNearRoarGivesWayToTheDistantRumble() {
        assertEquals(1, RocketAcoustics.nearVolume(10), 1.0e-12);
        assertEquals(0, RocketAcoustics.nearVolume(500), 1.0e-12);
        assertTrue(RocketAcoustics.farVolume(10) < 0.01, "no distant rumble on top of the roar");
        assertTrue(RocketAcoustics.farVolume(300) > RocketAcoustics.nearVolume(300));
        assertTrue(RocketAcoustics.farVolume(1500) > 0.05, "a launch is heard kilometres away");
        assertEquals(0, RocketAcoustics.farVolume(RocketAcoustics.FAR_REACH), 1.0e-12);
        for (double d = 0; d < 3000; d += 25) {
            double near = RocketAcoustics.nearVolume(d), far = RocketAcoustics.farVolume(d);
            assertTrue(near >= 0 && near <= 1 && far >= 0 && far <= 1);
        }
    }

    @Test
    void oneShotsFadeWithDistance() {
        assertEquals(1, RocketAcoustics.oneShotVolume(0, 1000), 1.0e-12);
        assertTrue(RocketAcoustics.oneShotVolume(200, 1000) > RocketAcoustics.oneShotVolume(600, 1000));
        assertEquals(0, RocketAcoustics.oneShotVolume(1000, 1000), 1.0e-12);
    }

    @Test
    void aFastCloseWarheadBoomsOnceAtItsClosest() {
        RocketAcoustics.Passage passage = new RocketAcoustics.Passage();
        int booms = 0;
        // Straight past at 6 blocks a tick, 30 blocks off at its closest.
        for (int t = -40; t <= 40; t++) {
            double along = 6 * t;
            if (passage.update(Math.hypot(along, 30), 6, true)) {
                booms++;
                assertEquals(1, t, "heard as it starts to draw away");
            }
        }
        assertEquals(1, booms);
        assertFalse(passage.finish(), "and only once");
    }

    @Test
    void noBoomTooSlowTooFarOrOnTheWayUp() {
        RocketAcoustics.Passage slow = new RocketAcoustics.Passage();
        RocketAcoustics.Passage far = new RocketAcoustics.Passage();
        RocketAcoustics.Passage climbing = new RocketAcoustics.Passage();
        for (int t = -40; t <= 40; t++) {
            assertFalse(slow.update(Math.hypot(3 * t, 20), 3, true));
            assertFalse(far.update(Math.hypot(6 * t, 100), 6, true));
            assertFalse(climbing.update(Math.hypot(6 * t, 20), 6, false));
        }
        assertFalse(slow.finish() || far.finish() || climbing.finish());
    }

    @Test
    void aWarheadThatLandsCloseStillBooms() {
        RocketAcoustics.Passage passage = new RocketAcoustics.Passage();
        for (int t = 0; t < 20; t++) assertFalse(passage.update(200 - 8 * t, 8, true));
        assertTrue(passage.finish());
    }
}
