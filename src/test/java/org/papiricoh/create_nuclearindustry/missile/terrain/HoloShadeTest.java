package org.papiricoh.create_nuclearindustry.missile.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The terrain's paint: green phosphor land with contours, cyan water, dimmer where it is only foretold. */
class HoloShadeTest {

    private static int red(int c) { return (c >> 16) & 0xFF; }
    private static int green(int c) { return (c >> 8) & 0xFF; }
    private static int blue(int c) { return c & 0xFF; }

    @Test
    void landIsGreenWaterCyanAndContoursShow() {
        byte[] d = new byte[TerrainTile.BYTES];
        int sea = 63;
        for (int j = 0; j < TerrainTile.SIDE; j++) {
            for (int i = 0; i < TerrainTile.SIDE; i++) {
                boolean water = i < 16;
                int h = water ? 40 : 64 + i * 2;
                TerrainTile.set(d, i, j, h, TerrainTile.surface(water ? TerrainKind.OCEAN : TerrainKind.PLAINS, water, j < 32));
            }
        }
        int scale = 2;
        int[] px = HoloShade.paint(d, 8, sea, scale);
        int n = TerrainTile.SIZE * scale;
        assertEquals(n * n, px.length);
        int water = px[10 * n + 4], land = px[10 * n + 100];
        assertTrue(blue(water) > red(water) && green(water) > red(water), "water is cyan");
        assertTrue(green(land) > red(land) && green(land) > blue(land), "land is phosphor green");
        assertTrue((land >>> 24) > 0 && (water >>> 24) > 0, "nothing is clear");
        // Below row 64 (sample 32) the land is only foretold: dimmer.
        int seen = px[20 * n + 100], foretold = px[(n - 4) * n + 100];
        assertTrue(HoloShade.brightness(seen) > HoloShade.brightness(foretold), "foretold land is dimmer");
        boolean contour = false;
        for (int x = 40; x < n - 1; x++) contour |= HoloShade.brightness(px[20 * n + x]) > HoloShade.brightness(px[20 * n + x + 1]) + 40;
        assertTrue(contour, "a rising slope crosses contour lines");
    }

    @Test
    void higherLandIsBrighter() {
        byte[] d = new byte[TerrainTile.BYTES];
        for (int j = 0; j < TerrainTile.SIDE; j++)
            for (int i = 0; i < TerrainTile.SIDE; i++)
                TerrainTile.set(d, i, j, j < 32 ? 70 : 140, TerrainTile.surface(TerrainKind.PLAINS, false, true));
        int[] px = HoloShade.paint(d, 512, 63, 1);
        assertTrue(HoloShade.brightness(px[50 * 64 + 10]) > HoloShade.brightness(px[5 * 64 + 10]), "the plateau glows brighter");
    }
}
