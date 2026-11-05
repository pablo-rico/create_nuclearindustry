package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A conventional blast: a brief fireball and smoke that lingers by its charge; thermobaric burns wide and long. */
class BlastFxModelTest {
    private static BlastFxModel of(ConventionalKind kind, double charge) {
        return new BlastFxModel(kind, charge, 1 + 0.9 * Math.cbrt(charge), kind == ConventionalKind.THERMOBARIC ? 40 : 0, false, 5,
                ExplosionPalette.DUST);
    }

    @Test
    void biggerChargesSmokeLongerAndHigher() {
        BlastFxModel tnt = of(ConventionalKind.HIGH_EXPLOSIVE, 1);
        BlastFxModel bay = of(ConventionalKind.HIGH_EXPLOSIVE, 192);
        assertTrue(bay.smokeSeconds() >= 60 && bay.smokeSeconds() <= 125, "a minute or two: " + bay.smokeSeconds());
        assertTrue(tnt.smokeSeconds() < bay.smokeSeconds() && tnt.columnHeight() < bay.columnHeight());
        assertTrue(bay.smokePuffs(48) > tnt.smokePuffs(48));
        assertTrue(bay.fireballSeconds() < 2, "a high-explosive fireball is brief");
    }

    @Test
    void thermobaricHazesThenBurnsWideAndLong() {
        BlastFxModel he = of(ConventionalKind.HIGH_EXPLOSIVE, 16);
        BlastFxModel tb = of(ConventionalKind.THERMOBARIC, 16);
        assertNotNull(tb.haze(0.1), "the fuel spreads first");
        assertNull(he.haze(0.1));
        assertEquals(0, tb.fireballWidth(0.05), "not lit yet");
        double t = tb.hazeSeconds() + 0.5;
        assertTrue(tb.fireballWidth(t) > 1.8 * he.fireballWidth(0.5), "much wider");
        assertTrue(tb.fireballWidth(t) > 2 * tb.fireballHeight(t), "and flat, rolling along the ground");
        assertTrue(tb.fireballSeconds() > 2 * he.fireballSeconds(), "burning longer");
    }

    @Test
    void aDirtyBombLooksLikeHighExplosive() {
        assertEquals(ConventionalKind.HIGH_EXPLOSIVE, of(ConventionalKind.DIRTY, 24).kind());
        assertEquals(of(ConventionalKind.HIGH_EXPLOSIVE, 24).smokeSeconds(), of(ConventionalKind.DIRTY, 24).smokeSeconds());
    }

    @Test
    void smokeRisesAndIsGoneInTheEnd() {
        BlastFxModel bay = of(ConventionalKind.HIGH_EXPLOSIVE, 192);
        double[] high = {0};
        int[] n = {0};
        bay.smoke(30, 40, (x, y, z, r, tile, spin, red, g, b, a) -> {
            high[0] = Math.max(high[0], y);
            n[0]++;
        });
        assertTrue(n[0] > 10 && high[0] > 0.5 * bay.columnHeight());
        int[] after = {0};
        bay.smoke(bay.smokeSeconds() + 1, 40, (x, y, z, r, tile, spin, red, g, b, a) -> after[0]++);
        assertEquals(0, after[0]);
        assertTrue(bay.over(bay.smokeSeconds() + 2));
    }
}
