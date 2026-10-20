package org.papiricoh.create_nuclearindustry.missile.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A plume's shape with the air round it. */
class PlumeModelTest {
    private static final double TOP = 400;

    @Test
    void theAirThinsToAboutAOnTwentiethAtTheTop() {
        assertEquals(1, PlumeModel.airDensity(40, TOP), 1.0e-12);
        assertEquals(1, PlumeModel.airDensity(63, TOP), 1.0e-12);
        assertEquals(Math.exp(-3), PlumeModel.airDensity(TOP, TOP), 1.0e-9);
        assertTrue(PlumeModel.airDensity(700, TOP) < 0.01);
    }

    @Test
    void thePlumeWidensAndLengthensAsItClimbs() {
        double lastEnd = 0, lastLength = 0;
        for (double y = 70; y <= 800; y += 30) {
            PlumeModel.Plume p = PlumeModel.of(false, 0.4, y - 70, y, TOP, 1);
            assertTrue(p.endRadius() >= lastEnd - 1.0e-12, "wider at " + y);
            assertTrue(p.length() >= lastLength - 1.0e-12, "longer at " + y);
            lastEnd = p.endRadius();
            lastLength = p.length();
        }
        double ground = PlumeModel.of(false, 0.4, 0, 70, TOP, 1).endRadius();
        double space = PlumeModel.of(false, 0.4, 900, 1000, TOP, 1).endRadius();
        assertTrue(space > 3 * ground, "it balloons out in the void: " + ground + " -> " + space);
        assertTrue(PlumeModel.expansion(0) <= PlumeModel.MAX_EXPANSION);
    }

    @Test
    void shockDiamondsOnlyNearTheGround() {
        PlumeModel.Plume low = PlumeModel.of(true, 0.4, 5, 75, TOP, 1);
        PlumeModel.Plume high = PlumeModel.of(true, 0.4, PlumeModel.DIAMOND_HEIGHT + 1, 200, TOP, 1);
        assertTrue(low.diamonds() > 0 && low.diamondStrength() > 0.8, "a liquid engine shows bright diamonds at lift-off");
        assertEquals(0, high.diamonds());
        assertEquals(0, high.diamondStrength(), 1.0e-12);
        assertTrue(PlumeModel.diamondStrength(30) > PlumeModel.diamondStrength(90), "fading as it climbs");
    }

    @Test
    void solidMotorsSmokeMoreAndLiquidEnginesShowBrighterDiamonds() {
        PlumeModel.Plume solid = PlumeModel.of(false, 0.4, 10, 80, TOP, 1);
        PlumeModel.Plume liquid = PlumeModel.of(true, 0.4, 10, 80, TOP, 1);
        assertTrue(solid.smoke() > 2 * liquid.smoke());
        assertTrue(liquid.diamondStrength() > solid.diamondStrength());
    }

    @Test
    void anEngineOffHasNoFlame() {
        PlumeModel.Plume off = PlumeModel.of(false, 0.4, 10, 80, TOP, 0);
        assertEquals(0, off.brightness(), 1.0e-12);
        assertEquals(0, off.diamonds());
    }

    @Test
    void flickerStaysNearOne() {
        for (int i = 0; i < 1000; i++) {
            double f = PlumeModel.flicker(i * 0.37, 12345);
            assertTrue(f > 0.8 && f < 1.2, "flicker " + f);
        }
    }
}
