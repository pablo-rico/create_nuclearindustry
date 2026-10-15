package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** LED displays show readings in the monitors' units. */
class ReadingValueTest {

    private static MonitorReading reactor() {
        float[] v = new float[35];
        v[MonitorReading.R_TEMPERATURE] = 612;
        v[MonitorReading.R_RODS] = 0.42f;
        v[MonitorReading.R_PRESSURE] = 155.3f;
        return new MonitorReading(TargetKind.REACTOR, "Running", v, 0.15f, 0.5f);
    }

    @Test
    void everyReadingHasAUnitOrIsAPlainNumber() {
        for (GaugeReading reading : GaugeReading.values()) {
            assertTrue(reading == GaugeReading.CONTAINMENT_ACTIVITY || !ReadingValue.unit(reading).isBlank(), reading.name());
            assertTrue(ReadingValue.decimals(reading) >= 0 && ReadingValue.decimals(reading) <= 2, reading.name());
        }
    }

    @Test
    void valuesAreInTheirUnits() {
        assertEquals(612, ReadingValue.value(GaugeReading.REACTOR_TEMPERATURE, reactor()), 1e-3);
        assertEquals("°C", ReadingValue.unit(GaugeReading.REACTOR_TEMPERATURE));
        assertEquals(42, ReadingValue.value(GaugeReading.REACTOR_RODS, reactor()), 1e-3);
        assertEquals("%", ReadingValue.unit(GaugeReading.REACTOR_RODS));
        assertEquals(155.3, ReadingValue.value(GaugeReading.REACTOR_PRESSURE, reactor()), 1e-3);
        // The gauge's needle agrees with the number.
        assertEquals(GaugeReading.REACTOR_TEMPERATURE.fraction(reactor()),
                ReadingValue.value(GaugeReading.REACTOR_TEMPERATURE, reactor()) / 4000, 1e-6);
    }

    @Test
    void anotherMachineReadsNothing() {
        assertTrue(Double.isNaN(ReadingValue.value(GaugeReading.TURBINE_SPEED, reactor())));
        assertTrue(Double.isNaN(ReadingValue.value(GaugeReading.REACTOR_TEMPERATURE, MonitorReading.NONE)));
        assertFalse(Double.isNaN(ReadingValue.value(GaugeReading.REACTOR_XENON, reactor())));
    }

    @Test
    void theShutdownReadingsShowMarginAndBoron() {
        float[] v = new float[46];
        v[MonitorReading.R_COLD_MARGIN] = 2500;
        v[MonitorReading.R_BORON_PPM] = 625;
        MonitorReading sample = new MonitorReading(TargetKind.REACTOR, "Idle", v, 0, 0);
        assertEquals(2500, ReadingValue.value(GaugeReading.REACTOR_SHUTDOWN_MARGIN, sample), 1e-3);
        assertEquals("pcm", ReadingValue.unit(GaugeReading.REACTOR_SHUTDOWN_MARGIN));
        assertEquals(0.5, GaugeReading.REACTOR_SHUTDOWN_MARGIN.fraction(sample), 1e-6);
        assertEquals("ppm", ReadingValue.unit(GaugeReading.REACTOR_BORON));
        assertEquals(0.25, GaugeReading.REACTOR_BORON.fraction(sample), 1e-6);
        v[MonitorReading.R_COLD_MARGIN] = -800;
        assertEquals(0, GaugeReading.REACTOR_SHUTDOWN_MARGIN.fraction(sample), 1e-6, "a negative margin pins the needle at 0");
    }
}
