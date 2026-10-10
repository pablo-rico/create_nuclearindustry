package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselIntegrityTest {

    @Test
    void failsAfterLongEnoughOverpressure() {
        VesselIntegrity vessel = new VesselIntegrity();
        for (int t = 0; t < 99; t++) {
            assertFalse(vessel.tick(300, 250, 100));
        }
        assertTrue(vessel.tick(300, 250, 100), "the hundredth tick breaks it");
        assertTrue(vessel.breached());
        assertFalse(vessel.tick(300, 250, 100), "it only fails once");
    }

    @Test
    void shortSpikesDoNoHarm() {
        VesselIntegrity vessel = new VesselIntegrity();
        for (int cycle = 0; cycle < 50; cycle++) {
            for (int t = 0; t < 40; t++) {
                vessel.tick(300, 250, 100);
            }
            for (int t = 0; t < 40; t++) {
                vessel.tick(150, 250, 100);
            }
        }
        assertFalse(vessel.breached());
    }

    @Test
    void repairResetsIt() {
        VesselIntegrity vessel = new VesselIntegrity();
        for (int t = 0; t < 100; t++) {
            vessel.tick(300, 250, 100);
        }
        vessel.repair();
        assertFalse(vessel.breached());
    }
}
