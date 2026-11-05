package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionQuality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each step up in quality spends more; far clouds are drawn with fewer, bigger puffs. */
class QualityPresetTest {

    @Test
    void eachLevelSpendsMore() {
        QualityPreset previous = null;
        for (ExplosionQuality q : ExplosionQuality.values()) {
            QualityPreset p = QualityPreset.of(q);
            if (previous != null) {
                assertTrue(p.totalPuffs() > previous.totalPuffs(), q + " puffs");
                assertTrue(p.rebuildHz() >= previous.rebuildHz(), q + " rebuilds");
                assertTrue(p.particleBudget() > previous.particleBudget(), q + " particles");
                assertTrue(p.particleScale() > previous.particleScale(), q + " particle scale");
                assertTrue(p.fullDetail() >= previous.fullDetail(), q + " detail distance");
            }
            previous = p;
        }
        assertTrue(!QualityPreset.LOW.heatHaze() && QualityPreset.MEDIUM.heatHaze());
        assertEquals(QualityPreset.MEDIUM, QualityPreset.of(null));
        assertTrue(QualityPreset.MEDIUM.rebuildHz() >= 8 && QualityPreset.MEDIUM.rebuildHz() <= 12, "about 10 Hz at medium");
    }

    @Test
    void farCloudsUseFewerBiggerPuffs() {
        QualityPreset p = QualityPreset.MEDIUM;
        assertEquals(1, p.lod(100));
        assertTrue(p.lod(3000) < p.lod(1000) && p.lod(100000) >= QualityPreset.MIN_LOD);
        double lod = p.lod(2000);
        int n = QualityPreset.scaled(1000, 1, lod);
        double area = n * Math.pow(QualityPreset.lodGrowth(lod), 2);
        assertEquals(1000, area, 30, "the same sky covered");
    }

    @Test
    void theFireballRebuildsEveryFrame() {
        QualityPreset p = QualityPreset.MEDIUM;
        assertEquals(0, p.rebuildSeconds(1, 3));
        assertEquals(0.1, p.rebuildSeconds(60, 3), 1e-9);
    }
}
