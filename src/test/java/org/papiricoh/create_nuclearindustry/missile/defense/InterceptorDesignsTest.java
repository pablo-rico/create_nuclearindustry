package org.papiricoh.create_nuclearindustry.missile.defense;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class InterceptorDesignsTest {
    @Test void batteryMatchesRequestedPerformance() {
        for (double dv : new double[]{1, 9, 20, 40}) for (double acceleration : new double[]{0.05, 0.5, 4}) {
            var design = InterceptorDesigns.battery(dv, acceleration);
            assertEquals(1, design.stages().size());
            assertEquals(0.1, design.payloadMass());
            assertEquals(6, design.stages().getFirst().exhaustVelocity());
            assertEquals(dv, design.deltaV(), 1e-10);
            assertEquals(acceleration, design.stages().getFirst().thrust() / design.launchMass(), 1e-10);
        }
    }
    @Test void killVehicleOnlyReplacesPayload() {
        var original = MissileDesign.LEGACY_PAD;
        var kv = InterceptorDesigns.killVehicle(original, 0.4);
        assertEquals(original.stages(), kv.stages());
        assertEquals(original.cep(), kv.cep());
        assertEquals(0.4, kv.payloadMass());
    }
}
