package org.papiricoh.create_nuclearindustry.dyson.swarm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sun's path matches the one the game draws it on. */
class SunPathTest {
    private static final double EPS = 1.0e-9;

    /** {@code DimensionType.timeOfDay}: the celestial angle of a day time. */
    private static double celestial(long dayTime) {
        double d = (dayTime / 24000.0 - 0.25) - Math.floor(dayTime / 24000.0 - 0.25);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (d * 2.0 + e) / 3.0;
    }

    @Test
    void noonIsOverhead() {
        double[] out = new double[3];
        SunPath.direction(celestial(6000), out);
        assertEquals(0, out[0], 1.0e-6);
        assertEquals(1, out[1], 1.0e-6);
        assertEquals(0, out[2], EPS);
        assertEquals(90, SunPath.elevationDegrees(celestial(6000)), 1.0e-4);
    }

    @Test
    void itRisesInTheEastAndSetsInTheWest() {
        double[] out = new double[3];
        SunPath.direction(celestial(0), out);
        assertTrue(out[0] > 0.9 && out[1] > 0, "just after dawn, low in the east");
        SunPath.direction(celestial(12000), out);
        assertTrue(out[0] < -0.9 && out[1] > 0, "just before dusk, low in the west");
        SunPath.direction(celestial(18000), out);
        assertEquals(-1, out[1], 1.0e-6);
    }

    @Test
    void itIsUpByDayAndDownByNight() {
        for (long t = 0; t < 24000; t += 250) {
            double sin = SunPath.sinElevation(celestial(t));
            if (t >= 500 && t <= 11500) assertTrue(sin > 0, "up at " + t);
            if (t >= 13500 && t <= 22500) assertTrue(sin < 0, "down at " + t);
        }
    }

    @Test
    void directionIsAUnitVector() {
        double[] out = new double[3];
        for (double c = 0; c < 1; c += 0.037) {
            SunPath.direction(c, out);
            assertEquals(1, Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]), EPS);
            assertEquals(out[1], SunPath.sinElevation(c), EPS);
        }
    }

    /** Iris's sun: turned -90 about y, then by the pack's path rotation about z, then by the sky angle about x. */
    private static double[] irisSun(double celestial, double pathRotation) {
        double t = Math.toRadians(celestial * 360), p = Math.toRadians(pathRotation);
        double[] v = {0, Math.cos(t), Math.sin(t)};
        v = new double[] {v[0] * Math.cos(p) - v[1] * Math.sin(p), v[0] * Math.sin(p) + v[1] * Math.cos(p), v[2]};
        return new double[] {-v[2], v[1], v[0]};
    }

    @Test
    void aShaderPackTipsThePathAsIrisDoes() {
        double[] out = new double[3];
        for (double rotation : new double[] {0, -40, 30}) {
            for (double c = 0; c < 1; c += 0.07) {
                SunPath.direction(c, rotation, out);
                double[] iris = irisSun(c, rotation);
                for (int k = 0; k < 3; k++) assertEquals(iris[k], out[k], 1.0e-9, "rotation " + rotation + " at " + c);
                assertEquals(1, Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]), 1.0e-9);
            }
        }
        double[] plain = new double[3];
        SunPath.direction(0.1, plain);
        SunPath.direction(0.1, 0, out);
        for (int k = 0; k < 3; k++) assertEquals(plain[k], out[k], 1.0e-12, "no tilt is the game's own sun");
    }
}
