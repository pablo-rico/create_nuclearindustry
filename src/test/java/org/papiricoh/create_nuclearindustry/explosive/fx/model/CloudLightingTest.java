package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sun's side of the cloud is bright, the far side in shade; at night only heat and fires light it. */
class CloudLightingTest {
    private static final float[] OUT = new float[3];

    private static double lum(CloudLighting.Sky sky, float nx, float ny, float nz, float occlusion, float heat) {
        CloudLighting.light(sky, 0.8f, 0.8f, 0.8f, nx, ny, nz, occlusion, heat, OUT);
        return 0.2126 * OUT[0] + 0.7152 * OUT[1] + 0.0722 * OUT[2];
    }

    @Test
    void theSunsSideIsBrighter() {
        CloudLighting.Sky noon = CloudLighting.sky(0.6, 0.8, 0, 1, 0);
        double lit = lum(noon, 1, 0, 0, 0, 0), shade = lum(noon, -1, 0, 0, 0, 0);
        assertTrue(lit > 1.3 * shade, lit + " against " + shade);
        assertTrue(shade > 0.3, "the shaded side is a mid grey, not black: " + shade);
        assertTrue(lum(noon, 0, 1, 0, 0, 0) > lum(noon, 0, -1, 0, 0, 0), "the top above the underside");
        assertTrue(lum(noon, 1, 0, 0, 0, 0) > lum(noon, 1, 0, 0, 1, 0), "deep inside is shaded");
    }

    @Test
    void aLowSunIsWarm() {
        CloudLighting.Sky dusk = CloudLighting.sky(1, 0.08, 0, 0.8, 0);
        CloudLighting.light(dusk, 0.8f, 0.8f, 0.8f, 1, 0, 0, 0, 0, OUT);
        assertTrue(OUT[0] > OUT[2] * 1.1, "reddened: " + OUT[0] + " " + OUT[2]);
    }

    @Test
    void atNightOnlyHeatAndFiresShow() {
        CloudLighting.Sky night = CloudLighting.sky(0, -0.6, 0.8, 0, 1);
        double cold = lum(night, 0, 1, 0, 0.5f, 0);
        assertTrue(cold < 0.16 && cold > 0.02, "dark, but faintly moonlit: " + cold);
        CloudLighting.light(night, 0.8f, 0.8f, 0.8f, 0, 1, 0, 0.8f, 0.8f, OUT);
        assertTrue(OUT[0] > 0.2 && OUT[0] > 2 * OUT[2], "a red glow inside a hot young cloud");
        double underside = lum(night, 0, -1, 0, 0, 0);
        assertTrue(underside > 2 * lum(night, 0, 1, 0, 0, 0), "the base is lit from below by the fires");
    }

    @Test
    void theImpostorsSunSide() {
        CloudLighting.Sky sky = CloudLighting.sky(1, 0.3, 0, 1, 0);
        assertTrue(CloudLighting.side(sky, 1, 0) > 0.9 && CloudLighting.side(sky, -1, 0) < 0.1);
    }
}
