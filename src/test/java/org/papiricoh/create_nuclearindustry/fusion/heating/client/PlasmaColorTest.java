package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlasmaColorTest {

    @Test
    void coldPlasmaIsDullRed() {
        float[] c = PlasmaColor.of(200);
        assertTrue(c[0] > c[1] && c[1] > c[2], "red dominates a cold plasma");
        assertTrue(c[0] < 0.8f, "and it is dull");
    }

    @Test
    void warmingPlasmaTurnsOrange() {
        float[] c = PlasmaColor.of(1800);
        assertTrue(c[0] > 0.9f && c[1] > 0.4f && c[1] < 0.7f && c[2] < 0.3f, "orange at 1800");
    }

    @Test
    void burningPlasmaIsWhite() {
        float[] c = PlasmaColor.of(4200);
        for (float channel : c) {
            assertTrue(channel > 0.9f, "white at burning temperature");
        }
    }

    @Test
    void dangerTemperatureIsViolet() {
        float[] c = PlasmaColor.of(PlasmaColor.DANGER_TEMPERATURE);
        assertTrue(c[2] > c[0] && c[0] > c[1], "violet: blue over red over green");
        float[] hotter = PlasmaColor.of(PlasmaColor.MAX_TEMPERATURE);
        assertTrue(hotter[1] < c[1], "deeper violet above the danger mark");
    }

    @Test
    void rampIsContinuousAndClamped() {
        float[] previous = PlasmaColor.of(0);
        for (double t = 0; t <= 6500; t += 10) {
            float[] c = PlasmaColor.of(t);
            for (int i = 0; i < 3; i++) {
                assertTrue(c[i] >= 0 && c[i] <= 1, "channel in range");
                assertTrue(Math.abs(c[i] - previous[i]) < 0.05f, "no jump at " + t);
            }
            previous = c;
        }
        assertArrayEquals(PlasmaColor.of(-50), PlasmaColor.of(20), 1e-6f);
        assertArrayEquals(PlasmaColor.of(9000), PlasmaColor.of(PlasmaColor.MAX_TEMPERATURE), 1e-6f);
    }

    @Test
    void noPlasmaDrawsNothing() {
        for (TokamakPhase phase : TokamakPhase.values()) {
            PlasmaLook look = PlasmaLook.of(phase, 3000, 0.5, 0, 0);
            assertEquals(phase.hasPlasma(), look != null, "drawn only with plasma: " + phase);
        }
    }

    @Test
    void breakdownFlashesMagenta() {
        PlasmaLook look = PlasmaLook.of(TokamakPhase.BREAKDOWN, 30, 0.05, 0, 3);
        assertNotNull(look);
        assertTrue(look.red() > look.green() && look.blue() > look.green(), "magenta: red and blue over green");
        assertTrue(look.coreAlpha() > 0.3f, "a bright flash");
    }

    @Test
    void denserPlasmaIsMoreOpaque() {
        PlasmaLook thin = PlasmaLook.of(TokamakPhase.BURN, 3000, 0.2, 0, 0);
        PlasmaLook dense = PlasmaLook.of(TokamakPhase.BURN, 3000, 0.8, 0, 0);
        assertNotNull(thin);
        assertNotNull(dense);
        assertTrue(dense.coreAlpha() > thin.coreAlpha() && dense.haloAlpha() > thin.haloAlpha());
        PlasmaLook empty = PlasmaLook.of(TokamakPhase.HEATING, 3000, 0.0, 0, 0);
        assertNotNull(empty);
        assertTrue(empty.coreAlpha() > 0, "even a thin plasma stays faintly visible");
    }

    @Test
    void instabilityMakesItFlicker() {
        double calmMin = 1;
        double calmMax = 0;
        double wildMin = 1;
        double wildMax = 0;
        for (double time = 0; time < 200; time += 0.37) {
            double calm = PlasmaLook.flicker(0, time);
            double wild = PlasmaLook.flicker(90, time);
            calmMin = Math.min(calmMin, calm);
            calmMax = Math.max(calmMax, calm);
            wildMin = Math.min(wildMin, wild);
            wildMax = Math.max(wildMax, wild);
        }
        assertTrue(calmMax - calmMin < 0.1, "a stable plasma only shimmers");
        assertTrue(wildMax - wildMin > 0.4, "an unstable one flickers hard");
        assertTrue(wildMin >= 0 && wildMax <= 1);
    }

    @Test
    void lookIsDeterministic() {
        assertEquals(PlasmaLook.of(TokamakPhase.BURN, 4000, 0.6, 40, 123.5), PlasmaLook.of(TokamakPhase.BURN, 4000, 0.6, 40, 123.5));
        assertNull(PlasmaLook.of(null, 4000, 0.6, 40, 1));
    }
}
