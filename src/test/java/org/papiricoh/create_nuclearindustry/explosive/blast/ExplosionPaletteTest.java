package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Explosions differ only by physics: no weapon has colours of its own, and fire cools like a black body. */
class ExplosionPaletteTest {

    @Test
    void eachStyleHasItsPalette() {
        assertSame(ExplosionPalette.FISSION, ExplosionPalette.of(DetonationStyle.FISSION));
        assertSame(ExplosionPalette.FISSION, ExplosionPalette.of(DetonationStyle.DIRTY), "a dirty bomb is plain high explosive");
        assertSame(ExplosionPalette.THERMONUCLEAR, ExplosionPalette.of(DetonationStyle.THERMONUCLEAR));
        for (DetonationStyle style : DetonationStyle.values()) {
            assertSame(style, DetonationStyle.byId(style.id()));
        }
        assertSame(DetonationStyle.FISSION, DetonationStyle.byId(99));
    }

    /** Fission and thermonuclear share every colour: smoke, dust, condensation, flash, glow and fog. */
    @Test
    void noThemedColours() {
        ExplosionPalette a = ExplosionPalette.FISSION;
        ExplosionPalette h = ExplosionPalette.THERMONUCLEAR;
        assertEquals(a.smoke(), h.smoke());
        assertEquals(a.dust(), h.dust());
        assertEquals(a.condensation(), h.condensation());
        assertEquals(a.flash(), h.flash());
        assertEquals(a.glow(), h.glow());
        assertEquals(a.fog(), h.fog());
        // Smoke and dust are greys and browns, never tinted towards a hue of their own.
        for (ExplosionPalette.Rgb c : new ExplosionPalette.Rgb[]{a.smoke(), a.dust(), ExplosionPalette.SOOT, a.condensation()}) {
            float max = Math.max(c.red(), Math.max(c.green(), c.blue()));
            float min = Math.min(c.red(), Math.min(c.green(), c.blue()));
            assertTrue(max - min < 0.15f, "a near-neutral grey: " + c);
            assertTrue(c.red() >= c.blue() - 0.02f, "warm or neutral, never blue or green: " + c);
        }
    }

    /** What does differ is physical: the bigger device's fireball is larger, glows longer and its flash lasts. */
    @Test
    void aThermonuclearFireballIsBiggerAndLonger() {
        ExplosionPalette a = ExplosionPalette.FISSION;
        ExplosionPalette h = ExplosionPalette.THERMONUCLEAR;
        assertTrue(h.fireballScale() > a.fireballScale());
        assertTrue(h.fireballSeconds(90) > a.fireballSeconds(30));
        assertTrue(h.flashFade() > a.flashFade());
        assertTrue(a.fireballSeconds(60) > a.fireballSeconds(10), "a bigger fireball takes longer to cool");
    }

    /** Fire cools from white through yellow and orange to red: its green and blue fall away first. */
    @Test
    void fireCoolsLikeABlackBody() {
        ExplosionPalette.Rgb previous = ExplosionPalette.fire(1);
        assertTrue(previous.luminance() > 0.95, "white-hot");
        for (double heat = 0.95; heat >= 0; heat -= 0.05) {
            ExplosionPalette.Rgb c = ExplosionPalette.fire(heat);
            assertTrue(c.green() <= previous.green() + 1e-6 && c.blue() <= previous.blue() + 1e-6, "cooling at " + heat);
            assertTrue(c.red() >= c.green() && c.green() >= c.blue(), "a black body's order at " + heat);
            previous = c;
        }
        ExplosionPalette.Rgb orange = ExplosionPalette.fire(0.45);
        assertTrue(orange.red() > 0.9 && orange.green() > 0.4 && orange.green() < 0.8 && orange.blue() < 0.4);
    }
}
