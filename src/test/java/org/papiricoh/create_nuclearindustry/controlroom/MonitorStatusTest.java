package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/** The colour of a target's state on a screen's title line. */
class MonitorStatusTest {

    @Test
    void tripsAreBadRunningIsGoodTheRestIsAWarning() {
        assertSame(MonitorRow.Tone.BAD, MonitorStatus.tone("SCRAM"));
        assertSame(MonitorRow.Tone.BAD, MonitorStatus.tone("MELTING"));
        assertSame(MonitorRow.Tone.BAD, MonitorStatus.tone("create_nuclearindustry.monitor.status.tripped"));
        assertSame(MonitorRow.Tone.GOOD, MonitorStatus.tone("Running"));
        assertSame(MonitorRow.Tone.GOOD, MonitorStatus.tone("create_nuclearindustry.monitor.status.burning"));
        assertSame(MonitorRow.Tone.WARN, MonitorStatus.tone("Idle"));
    }
}
