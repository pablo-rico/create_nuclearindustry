package org.papiricoh.create_nuclearindustry.controlroom.client;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every screen style stays readable: contrast between text and what it sits on (WCAG ratios), three
 * graph lines told apart by hue, and only opaque colours, which the depth-sorted screen needs.
 */
class MonitorPaletteTest {

    @Test
    void textIsReadableOnEveryStyle() {
        for (MonitorTheme theme : MonitorTheme.values()) {
            MonitorPalette p = MonitorPalette.of(theme);
            int title = p.titleBand() != 0 ? p.titleBand() : p.backdrop();
            atLeast(7, p.text(), p.backdrop(), theme + " values");
            atLeast(4.5, p.label(), p.backdrop(), theme + " labels");
            atLeast(3, p.dim(), p.backdrop(), theme + " dim text");
            for (MonitorRow.Tone tone : new MonitorRow.Tone[]{MonitorRow.Tone.GOOD, MonitorRow.Tone.WARN, MonitorRow.Tone.BAD}) {
                atLeast(3, p.tone(tone), p.backdrop(), theme + " " + tone + " values");
                atLeast(3, p.status(tone), title, theme + " " + tone + " state on the title line");
            }
            atLeast(4.5, p.titleText(), title, theme + " title");
            if (p.tileWarn() != 0) {
                atLeast(4.5, p.tileWarnText(), p.tileWarn(), theme + " warning tile");
                atLeast(4.5, p.tileBadText(), p.tileBad(), theme + " alarm tile");
            }
            for (int i = 0; i < 3; i++) {
                atLeast(3, p.series()[i], p.graphBackground(), theme + " line " + i);
                atLeast(3, p.legend(i), p.backdrop(), theme + " legend " + i);
            }
        }
    }

    @Test
    void theThreeLinesAreToldApartByHue() {
        for (MonitorTheme theme : MonitorTheme.values()) {
            int[] series = MonitorPalette.of(theme).series();
            for (int i = 0; i < 3; i++) {
                for (int j = i + 1; j < 3; j++) {
                    float gap = Math.abs(hue(series[i]) - hue(series[j]));
                    gap = Math.min(gap, 360 - gap);
                    assertTrue(gap >= 60, theme + " lines " + i + " and " + j + " are only " + gap + " degrees apart");
                }
            }
        }
    }

    @Test
    void everyColourIsOpaqueAndEveryStyleLooksItsOwn() {
        Set<Integer> backdrops = new HashSet<>();
        for (MonitorTheme theme : MonitorTheme.values()) {
            MonitorPalette p = MonitorPalette.of(theme);
            int[] colours = {p.backdrop(), p.titleBand(), p.titleText(), p.titleRule(), p.text(), p.label(), p.dim(), p.good(),
                    p.warn(), p.bad(), p.statusGood(), p.statusWarn(), p.statusBad(), p.tileWarn(), p.tileWarnText(), p.tileBad(),
                    p.tileBadText(), p.graphBackground(), p.grid(), p.legendText()};
            for (int colour : colours) {
                assertTrue(colour == 0 || colour >>> 24 == 0xFF, theme + " colour " + Integer.toHexString(colour) + " is see-through");
            }
            for (int i = 0; i < 3; i++) {
                assertEquals(0xFF, p.series()[i] >>> 24, theme + " line " + i);
                assertEquals(0xFF, p.guiSeries()[i] >>> 24, theme + " wrench screen line " + i);
            }
            assertTrue(backdrops.add(p.backdrop()), theme + " has a backdrop of its own");
        }
    }

    private static void atLeast(double ratio, int foreground, int background, String what) {
        double contrast = contrast(foreground, background);
        assertTrue(contrast >= ratio, what + ": contrast " + String.format("%.2f", contrast) + " under " + ratio);
    }

    static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16) + 0.7152 * channel(argb >> 8) + 0.0722 * channel(argb);
    }

    private static double channel(int value) {
        double c = (value & 0xFF) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static float hue(int argb) {
        float[] hsb = java.awt.Color.RGBtoHSB(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, null);
        return hsb[0] * 360;
    }
}
