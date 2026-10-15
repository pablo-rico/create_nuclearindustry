package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Target irradiation at the target station: below the antiproton threshold the beam makes isotopes. */
class TargetIrradiationModelTest {
    private static final AntimatterModel.Params AM = AntimatterModel.Params.DEFAULT;
    private static final double MIN = 1.0;
    private static final double NOMINAL = 60_000;

    @Test
    void irradiatesOnlyBetweenTheMinimumAndTheAntiprotonThreshold() {
        assertFalse(TargetIrradiationModel.irradiates(AM, 0.5, MIN), "too weak a beam");
        assertTrue(TargetIrradiationModel.irradiates(AM, 1.0, MIN));
        assertTrue(TargetIrradiationModel.irradiates(AM, 24.9, MIN));
        assertTrue(TargetIrradiationModel.irradiates(AM, AM.thresholdGev(), MIN), "at exactly the threshold no antiprotons yet");
        assertFalse(TargetIrradiationModel.irradiates(AM, 25.1, MIN), "above it the station makes antiprotons");
        assertFalse(TargetIrradiationModel.irradiates(AM, 0, 0), "no beam");
    }

    @Test
    void oneItemPerNominalBeamEnergy() {
        // 1 MW for a minute: 50 kJ a tick, 1200 ticks.
        double progress = 0;
        int made = 0;
        for (int tick = 0; tick < 1200; tick++) {
            TargetIrradiationModel.Result r = TargetIrradiationModel.tick(AM, progress, 10, 1000, MIN, NOMINAL, 64);
            made += r.made();
            progress = r.progress();
        }
        assertEquals(1, made);
        assertEquals(0, progress, 1e-6);
        assertEquals(1.0, TargetIrradiationModel.perMinute(NOMINAL, 1000), 1e-12);
        assertEquals(1200, TargetIrradiationModel.ticksLeft(0, NOMINAL, 1000));
        assertEquals(600, TargetIrradiationModel.ticksLeft(NOMINAL / 2, NOMINAL, 1000));
        assertEquals(Long.MAX_VALUE, TargetIrradiationModel.ticksLeft(0, NOMINAL, 0));
        assertEquals(0.5, TargetIrradiationModel.fraction(NOMINAL / 2, NOMINAL), 1e-12);
    }

    @Test
    void aBeamAboveTheThresholdLeavesTheTargetAlone() {
        TargetIrradiationModel.Result r = TargetIrradiationModel.tick(AM, 1234, 100, 6800, MIN, NOMINAL, 64);
        assertEquals(0, r.made());
        assertEquals(1234, r.progress(), 0.0, "progress waits, it is not lost");
    }

    @Test
    void aHugeBeamFinishesSeveralButNoMoreThanTheSlotHolds() {
        // 2.4 GW: 120 000 kJ a tick, two nominal items.
        TargetIrradiationModel.Result r = TargetIrradiationModel.tick(AM, 0, 10, 2.4e6, MIN, NOMINAL, 64);
        assertEquals(2, r.made(), "120 000 kJ in a tick");
        assertEquals(0, r.progress(), 1e-6);
        TargetIrradiationModel.Result capped = TargetIrradiationModel.tick(AM, 0, 10, 2.4e7, MIN, NOMINAL, 3);
        assertEquals(3, capped.made());
        assertEquals(0, capped.progress(), 0.0, "an emptied slot carries nothing");
        TargetIrradiationModel.Result empty = TargetIrradiationModel.tick(AM, 500, 10, 1000, MIN, NOMINAL, 0);
        assertEquals(0, empty.made());
        assertEquals(500, empty.progress(), 0.0);
    }
}
