package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.blast.ShockFront;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Three shakes in order: the ground's tremor, the shock's jolt and the rumble after it; harder close by. */
class CameraShakeTest {
    private static double peak(CameraShake.Strength s, double seismicAt, double shockAt, double from, double to) {
        double[] out = new double[3];
        double max = 0;
        for (double t = from; t < to; t += 0.01) {
            CameraShake.sample(s, t - seismicAt, t - shockAt, 0.3, out);
            max = Math.max(max, Math.abs(out[0]) + Math.abs(out[1]) + Math.abs(out[2]));
        }
        return max;
    }

    @Test
    void nothingBeforeItArrives() {
        CameraShake.Strength s = CameraShake.nuclear(300, 1e6, 768);
        double[] out = new double[3];
        CameraShake.sample(s, -0.1, -2, 0, out);
        assertArrayEquals(new double[]{0, 0, 0}, out, 1e-12);
        assertTrue(!CameraShake.active(s, -0.1, -2));
    }

    @Test
    void theTremorComesFirstThenTheJoltThenTheRumble() {
        double d = 500;
        double seismic = ShockFront.seismicArrivalTicks(d) / 20;
        double shock = ShockFront.arrivalTicks(d, 8, 768) / 20;
        assertTrue(seismic < shock);
        CameraShake.Strength s = CameraShake.nuclear(d, 1e6, 768);
        assertTrue(peak(s, seismic, shock, seismic, shock) > 0, "felt before the shock");
        double jolt = peak(s, seismic, shock, shock, shock + 0.5);
        double rumble = peak(s, seismic, shock, shock + 4, shock + 10);
        assertTrue(jolt > rumble && rumble > 0, jolt + " then " + rumble);
        assertTrue(!CameraShake.active(s, 100, 100), "and all over in the end");
    }

    @Test
    void harderCloseByAndNeverWild() {
        double near = peak(CameraShake.nuclear(100, 1e6, 768), 0, 0, 0, 1);
        double far = peak(CameraShake.nuclear(700, 1e6, 768), 0, 0, 0, 1);
        double beyond = peak(CameraShake.nuclear(3000, 1e6, 768), 0, 0, 0, 1);
        assertTrue(near > far && far > beyond, near + " " + far + " " + beyond);
        assertTrue(near <= 3 * CameraShake.MAX_DEGREES);
        double bang = peak(CameraShake.conventional(10, 200, 30), 0, 0, 0, 1);
        assertTrue(bang > 0 && bang < near, "a conventional bang shakes less than a nuke");
        assertTrue(CameraShake.conventional(500, 200, 30).none(), "and not at all from afar");
    }
}
