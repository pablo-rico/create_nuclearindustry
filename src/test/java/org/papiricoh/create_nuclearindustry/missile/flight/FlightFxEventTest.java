package org.papiricoh.create_nuclearindustry.missile.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Events go by id: each comes back as itself, and an id this version does not know is no event. */
class FlightFxEventTest {
    @Test
    void everyEventRoundTripsByItsId() {
        for (FlightFxEvent e : FlightFxEvent.values()) assertEquals(e, FlightFxEvent.byId(e.id()));
    }

    @Test
    void satelliteEventsKeepTheirIds() {
        assertEquals(FlightFxEvent.ORBIT_INSERTION, FlightFxEvent.byId(12));
        assertEquals(FlightFxEvent.SATELLITE_KILLED, FlightFxEvent.byId(13));
        assertEquals(FlightFxEvent.SATELLITE_REENTRY, FlightFxEvent.byId(14));
    }

    @Test
    void unknownIdsAreNoEvent() {
        assertNull(FlightFxEvent.byId(-1));
        assertNull(FlightFxEvent.byId(FlightFxEvent.values().length));
        assertNull(FlightFxEvent.byId(127));
        assertNull(FlightFxEvent.byId((byte) 0x80));
    }

    @Test
    void theSatelliteFlagIsTheTopBitAndClashesWithNoOther() {
        assertEquals(0x80, FlightFxState.SATELLITE);
        int others = FlightFxState.BURNING | FlightFxState.RISING | FlightFxState.NUCLEAR_WARHEAD | FlightFxState.PUPPET
                | FlightFxState.BURNED_OUT | FlightFxState.INTERCEPTOR | FlightFxState.CANISTER;
        assertEquals(0, others & FlightFxState.SATELLITE);
        // Sent as a byte, read back masked.
        assertEquals(FlightFxState.SATELLITE, ((byte) FlightFxState.SATELLITE) & 0xFF);
    }
}
