package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the control room makes of a tokamak's readings: stored values, alarm logic and text. */
class TokamakScalesTest {

    @Test
    void anInfiniteGainIsStoredAsIgnited() {
        assertEquals(TokamakScales.GAIN_IGNITED, TokamakScales.storedGain(Double.POSITIVE_INFINITY));
        assertTrue(TokamakScales.ignited(TokamakScales.storedGain(Double.POSITIVE_INFINITY)));
        assertEquals(4.5f, TokamakScales.storedGain(4.5), 1e-6);
        assertFalse(TokamakScales.ignited(TokamakScales.storedGain(4.5)));
        assertEquals(0.0f, TokamakScales.storedGain(Double.NaN));
        assertEquals(0.0f, TokamakScales.storedGain(-1));
        assertEquals("Q ∞", TokamakScales.formatGain(TokamakScales.GAIN_IGNITED));
        assertEquals("Q 4.5", TokamakScales.formatGain(4.5f));
        assertEquals("Q 12", TokamakScales.formatGain(12.3f));
    }

    @Test
    void anOperatorShutdownIsNoTrip() {
        assertFalse(TokamakScales.tripped(TokamakPhase.OFFLINE, TripCause.OPERATOR));
        assertFalse(TokamakScales.tripped(TokamakPhase.RAMP_DOWN, TripCause.OPERATOR));
        assertFalse(TokamakScales.tripped(TokamakPhase.OFFLINE, TripCause.NONE));
        assertTrue(TokamakScales.tripped(TokamakPhase.RAMP_DOWN, TripCause.MAGNETS_WARM));
        assertTrue(TokamakScales.tripped(TokamakPhase.OFFLINE, TripCause.VACUUM_LOST), "a refused start also shows");
        assertFalse(TokamakScales.tripped(TokamakPhase.PUMP_DOWN, TripCause.VACUUM_LOST), "started again: the trip is over");
        assertFalse(TokamakScales.tripped(TokamakPhase.BURN, TripCause.DISRUPTION));
    }

    @Test
    void theGainAlarmJudgesABurningPlasmaOnly() {
        float threshold = TokamakScales.gainThreshold(TokamakScales.gainSetting(TokamakScales.NET_GAIN));
        assertEquals(3.0f, threshold, 1e-6);
        assertTrue(TokamakScales.gainLow(TokamakPhase.BURN, 2.0f, threshold));
        assertTrue(TokamakScales.gainLow(TokamakPhase.BURN, 3.0f, threshold), "at the threshold");
        assertFalse(TokamakScales.gainLow(TokamakPhase.BURN, 5.0f, threshold));
        assertFalse(TokamakScales.gainLow(TokamakPhase.BURN, TokamakScales.GAIN_IGNITED, threshold), "an ignited plasma has gain to spare");
        assertFalse(TokamakScales.gainLow(TokamakPhase.HEATING, 0.5f, threshold), "while heating up a low gain is expected");
        assertFalse(TokamakScales.gainLow(TokamakPhase.OFFLINE, 0.0f, threshold));
    }

    @Test
    void magnetsMatterOnlyWhileEnergised() {
        assertFalse(TokamakScales.magnetsInUse(TokamakPhase.COOL_DOWN, 0.0f), "cooling down from room temperature is no alarm");
        assertTrue(TokamakScales.magnetsInUse(TokamakPhase.ENERGIZE, 0.0f));
        assertTrue(TokamakScales.magnetsInUse(TokamakPhase.BURN, 1.0f));
        assertTrue(TokamakScales.magnetsInUse(TokamakPhase.RAMP_DOWN, 0.5f), "still carrying current on the way down");
        assertFalse(TokamakScales.magnetsInUse(TokamakPhase.OFFLINE, 0.0f));
    }

    @Test
    void thresholdSettingsRoundTrip() {
        assertEquals(15, TokamakScales.vacuumSetting(-3.5));
        assertEquals(-3.5f, TokamakScales.vacuumThreshold(15), 1e-6);
        assertEquals(0, TokamakScales.vacuumSetting(-9));
        assertEquals(100, TokamakScales.vacuumSetting(TokamakScales.VACUUM_MAX));
        assertEquals(20, TokamakScales.magnetSetting(10.0));
        assertEquals(10.0f, TokamakScales.magnetThreshold(20), 1e-6);
        assertEquals(15, TokamakScales.gainSetting(3.0));
    }

    @Test
    void pressuresReadShort() {
        assertEquals("1e5 Pa", TokamakScales.formatPressure(5.0));
        assertEquals("1e-3 Pa", TokamakScales.formatPressure(-3.0));
        assertEquals("3e-4 Pa", TokamakScales.formatPressure(Math.log10(3.0e-4)));
        assertEquals("1e-3 Pa", TokamakScales.formatPressure(Math.log10(9.8e-4)), "rounds up into the next decade");
        assertEquals("-", TokamakScales.formatPressure(Double.NaN));
    }

    @Test
    void storedPhasesAndCausesDecodeSafely() {
        assertSame(TokamakPhase.BURN, TokamakScales.phase(TokamakPhase.BURN.ordinal()));
        assertSame(TokamakPhase.OFFLINE, TokamakScales.phase(99));
        assertSame(TripCause.MAGNETS_WARM, TokamakScales.trip(TripCause.MAGNETS_WARM.ordinal()));
        assertSame(TripCause.NONE, TokamakScales.trip(-1));
    }

    @Test
    void warningsSitBeforeTheProtections() {
        assertTrue(TokamakScales.INSTABILITY_WARNING < TokamakScales.DISRUPTION_INSTABILITY);
        assertTrue(TokamakScales.IGNITION_TEMPERATURE < TokamakScales.DANGER_TEMPERATURE);
        assertTrue(TokamakScales.DANGER_TEMPERATURE < TokamakScales.MAX_TEMPERATURE);
        assertTrue(TokamakScales.PLASMA_VACUUM_LIMIT > TokamakScales.VACUUM_MIN);
    }
}
