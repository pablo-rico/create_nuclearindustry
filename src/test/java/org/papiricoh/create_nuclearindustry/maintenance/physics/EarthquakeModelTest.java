package org.papiricoh.create_nuclearindustry.maintenance.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Natural earthquakes: frequency, magnitudes, reach and ground motion. */
class EarthquakeModelTest {

    @Test
    void chancePerTickSpreadsTheDailyRate() {
        assertEquals(1.0 / 24000, EarthquakeModel.chancePerTick(1.0), 1e-15);
        assertEquals(0.0, EarthquakeModel.chancePerTick(-1.0));
    }

    @Test
    void magnitudesFollowGutenbergRichter() {
        assertEquals(3.0, EarthquakeModel.magnitude(0.0, 3, 7, 1), 1e-9);
        assertEquals(7.0, EarthquakeModel.magnitude(1.0, 3, 7, 1), 1e-3);
        // Ninety percent of quakes stay under magnitude 4 with b = 1.
        double ninetieth = EarthquakeModel.magnitude(0.9, 3, 7, 1);
        assertTrue(ninetieth > 3.9 && ninetieth < 4.0, "the 90th percentile is about magnitude 4: " + ninetieth);
        assertEquals(5.0, EarthquakeModel.magnitude(0.5, 5, 5, 1), 1e-12, "a single magnitude range gives that magnitude");
    }

    @Test
    void biggerQuakesReachFurtherAndShakeHarder() {
        assertEquals(16.0, EarthquakeModel.feltRadius(3), 1e-12);
        assertEquals(64.0, EarthquakeModel.feltRadius(5), 1e-12);
        assertEquals(1.0, EarthquakeModel.groundMotion(3, 0), 1e-12);
        assertEquals(100.0, EarthquakeModel.groundMotion(5, 0), 1e-9);
        assertEquals(20.0, EarthquakeModel.groundMotion(5, 64), 1e-9);
        assertTrue(EarthquakeModel.groundMotion(5, 200) < EarthquakeModel.groundMotion(5, 100));
    }

    @Test
    void tripAndSeverity() {
        assertTrue(EarthquakeModel.trips(5, 4));
        assertFalse(EarthquakeModel.trips(3, 4));
        assertFalse(EarthquakeModel.trips(100, 0), "a zero threshold turns the trip off");
        assertEquals(1.0, EarthquakeModel.severity(4, 4), 1e-12);
        assertEquals(3.0, EarthquakeModel.severity(400, 4), 1e-12);
        assertEquals(0.0, EarthquakeModel.severity(0, 4), 1e-12);
    }

    @Test
    void detectorsReadBiggerQuakesAsBiggerBlasts() {
        assertEquals(10.0f, EarthquakeModel.detectorPower(3), 1e-5);
        assertEquals(160.0f, EarthquakeModel.detectorPower(5), 1e-3);
    }
}
