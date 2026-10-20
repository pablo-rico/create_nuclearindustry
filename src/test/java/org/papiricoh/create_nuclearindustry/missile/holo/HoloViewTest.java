package org.papiricoh.create_nuclearindustry.missile.holo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloViewTest {
    @Test
    void theViewIsTheSmallestThatHoldsEverything() {
        assertEquals(1024, HoloView.pick(0));
        assertEquals(1024, HoloView.pick(400));
        assertEquals(2048, HoloView.pick(600));
        assertEquals(8192, HoloView.pick(3000));
        assertEquals(16384, HoloView.pick(1e6), "beyond the widest view it stays the widest");
    }

    @Test
    void arcsNeverRiseAboveTheLimit() {
        for (int view : HoloView.VIEWS) {
            for (double top : new double[]{0, 50, 300, 3000, 30000}) {
                float scale = HoloView.heightScale(view, 2.75f, top);
                assertTrue(scale * Math.max(50, top) <= HoloView.MAX_HEIGHT + 1e-4, "view " + view + ", top " + top);
                assertTrue(scale > 0);
            }
        }
    }

    @Test
    void wideViewsUseCoarserTerrain() {
        int last = -1;
        for (int view : HoloView.VIEWS) {
            int lod = HoloView.lod(view);
            assertTrue(lod >= last);
            last = lod;
        }
        assertEquals(0, HoloView.lod(1024));
        assertEquals(3, HoloView.lod(16384));
    }
}
