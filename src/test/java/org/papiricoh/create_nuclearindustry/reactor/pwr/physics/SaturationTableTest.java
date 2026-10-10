package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaturationTableTest {

    @Test
    void matchesTheSteamTables() {
        assertEquals(1.014, SaturationTable.pressure(100), 0.01);
        assertEquals(85.88, SaturationTable.pressure(300), 0.05);
        assertEquals(155.0, SaturationTable.pressure(344.8), 1.0);
        assertEquals(220.64, SaturationTable.pressure(374), 0.01);
    }

    @Test
    void boilingPointsOfAPressurizedWaterReactor() {
        // 155 bar primary boils at about 345 °C; a 70 bar steam generator at about 286 °C.
        assertEquals(344.8, SaturationTable.temperature(155), 0.5);
        assertEquals(285.8, SaturationTable.temperature(70), 0.5);
    }

    @Test
    void interpolatesBetweenPointsWithinAPercent() {
        // IAPWS: 345 °C -> 155.46 bar, 255 °C -> 43.25 bar
        assertEquals(155.46, SaturationTable.pressure(345), 155.46 * 0.01);
        assertEquals(43.25, SaturationTable.pressure(255), 43.25 * 0.01);
    }

    @Test
    void temperatureInvertsPressure() {
        for (double t = 100; t <= 374; t += 7.3) {
            assertEquals(t, SaturationTable.temperature(SaturationTable.pressure(t)), 1.0e-6, "at " + t);
        }
    }

    @Test
    void risesSteadily() {
        double previous = 0;
        for (double t = 100; t <= 374; t += 0.5) {
            double p = SaturationTable.pressure(t);
            assertTrue(p > previous, "pressure must rise with temperature at " + t);
            previous = p;
        }
    }

    @Test
    void clampsOutsideItsRange() {
        assertEquals(SaturationTable.minPressure(), SaturationTable.pressure(20), 0.0);
        assertEquals(SaturationTable.maxPressure(), SaturationTable.pressure(500), 0.0);
        assertEquals(SaturationTable.MIN_TEMPERATURE, SaturationTable.temperature(0.2), 0.0);
        assertEquals(SaturationTable.MAX_TEMPERATURE, SaturationTable.temperature(400), 0.0);
        assertEquals(SaturationTable.minPressure(), SaturationTable.pressure(Double.NaN), 0.0);
    }
}
