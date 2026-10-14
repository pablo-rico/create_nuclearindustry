package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A thermonuclear explosion burns hotter and longer and leaves sooty smoke; a fission one looks as it always did. */
class ExplosionPaletteTest {

    private static MushroomCloudModel cloud(ExplosionPalette palette) {
        return new MushroomCloudModel(7, 250, 30, false, 0, palette);
    }

    @Test
    void eachStyleHasItsPalette() {
        assertSame(ExplosionPalette.FISSION, ExplosionPalette.of(DetonationStyle.FISSION));
        assertSame(ExplosionPalette.FISSION, ExplosionPalette.of(DetonationStyle.DIRTY));
        assertSame(ExplosionPalette.THERMONUCLEAR, ExplosionPalette.of(DetonationStyle.THERMONUCLEAR));
        for (DetonationStyle style : DetonationStyle.values()) {
            assertSame(style, DetonationStyle.byId(style.id()));
        }
        assertSame(DetonationStyle.FISSION, DetonationStyle.byId(99));
    }

    /** The fireball's colours as they were before palettes, for a fission warhead. */
    @Test
    void aFissionFireballKeepsItsOldColours() {
        MushroomCloudModel model = new MushroomCloudModel(7, 250, 30, false, 0);
        for (double s : new double[]{0, 0.5, 1, 2, 3, 3.9}) {
            List<MushroomCloudModel.Puff> puffs = model.puffs(s, 1);
            MushroomCloudModel.Puff outer = puffs.get(0);
            MushroomCloudModel.Puff core = puffs.get(1);
            assertEquals(30 * (1.0 + 0.5 * Math.min(1, s * 2)), outer.radius(), 1e-9);
            assertEquals(1.0 - 0.45 * Math.min(1, s / 3), outer.green(), 1e-6);
            assertEquals(0.95 - 0.8 * Math.min(1, s / 2), outer.blue(), 1e-6);
            assertEquals(0.9 - 0.5 * Math.min(1, s / 2), core.blue(), 1e-6);
            assertEquals(Math.min(1, (4 - s) / 2), outer.alpha(), 1e-6);
        }
        assertFalse(model.hasFireball(4));
        assertEquals(MushroomCloudModel.heat(10), model.heatAt(10), 1e-12);
        assertEquals(Math.min(360, 60 + 1.2 * 250), model.topHeight(), 1e-9);
    }

    @Test
    void aThermonuclearFireballIsBiggerWhiterAndLasts() {
        MushroomCloudModel h = cloud(ExplosionPalette.THERMONUCLEAR);
        MushroomCloudModel a = cloud(ExplosionPalette.FISSION);
        assertTrue(h.hasFireball(5) && !a.hasFireball(5), "it still burns after five seconds");
        MushroomCloudModel.Puff hBall = h.puffs(1, 1).get(0);
        MushroomCloudModel.Puff aBall = a.puffs(1, 1).get(0);
        assertTrue(hBall.radius() > aBall.radius() * 1.3);
        assertTrue(h.puffs(1, 1).get(1).blue() > a.puffs(1, 1).get(1).blue(), "its core is whiter");
        MushroomCloudModel.Puff late = h.puffs(5, 1).get(0);
        assertTrue(late.glowing() && late.green() < 0.6 && late.blue() < 0.2, "deepening to orange-red");
    }

    @Test
    void itsSmokeIsNearBlack() {
        double h = meanSmoke(cloud(ExplosionPalette.THERMONUCLEAR).puffs(60, 1));
        double a = meanSmoke(cloud(ExplosionPalette.FISSION).puffs(60, 1));
        assertTrue(h < 0.45 * a, "luminance " + h + " against " + a);
        assertTrue(ExplosionPalette.THERMONUCLEAR.dust().luminance() < ExplosionPalette.FISSION.dust().luminance());
    }

    @Test
    void aBiggerBombRaisesATallerCloud() {
        MushroomCloudModel h = new MushroomCloudModel(7, 750, 90, false, 0, ExplosionPalette.THERMONUCLEAR);
        assertTrue(h.topHeight() > 360, "above the fission cap: " + h.topHeight());
        assertTrue(h.topHeight() <= ExplosionPalette.THERMONUCLEAR.topHeightCap());
        assertTrue(h.capRadius() <= ExplosionPalette.THERMONUCLEAR.capRadiusCap());
    }

    private static double meanSmoke(List<MushroomCloudModel.Puff> puffs) {
        return puffs.stream().filter(p -> !p.glowing())
                .mapToDouble(p -> new ExplosionPalette.Rgb(p.red(), p.green(), p.blue()).luminance()).average().orElseThrow();
    }
}
