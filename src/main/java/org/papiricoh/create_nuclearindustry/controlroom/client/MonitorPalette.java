package org.papiricoh.create_nuclearindustry.controlroom.client;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;

/**
 * The main colours of a {@link MonitorTheme}, by what they are for: what the standard screen draws
 * with, the backdrop and messages of every style, and the lines' colours in the wrench screen. Each
 * style keeps the colours of its own pieces (plates, meters, paper) with it. Every colour is opaque,
 * since screens paint layer over layer. 0 means the style has no such thing (no title band, no
 * tiles). Kept free of game classes so the contrast can be tested.
 *
 * @param titleBand  behind the title line, or 0 for none (a rule under it instead)
 * @param titleRule  the rule under the title line
 * @param label      reading names
 * @param statusGood the target's state on the title line; it sits on the band when there is one
 * @param tileWarn   behind a value in warning, or 0 for none; {@code tileWarnText} is the value on it
 * @param legendText the graph's legend, or 0 to write each entry in its line's colour
 * @param series     the graph's three lines on the screen
 * @param guiSeries  the same lines named in the wrench screen, light enough for its dark background
 */
public record MonitorPalette(int backdrop, int titleBand, int titleText, int titleRule, int text, int label, int dim,
                             int good, int warn, int bad, int statusGood, int statusWarn, int statusBad,
                             int tileWarn, int tileWarnText, int tileBad, int tileBadText,
                             int graphBackground, int grid, int legendText, int[] series, int[] guiSeries) {

    public static final MonitorPalette STANDARD = new MonitorPalette(0xFF0B1511, 0, 0xFFB8F0C4, 0xFF2C4A3A,
            0xFFB8F0C4, 0xFF6E8F7A, 0xFF6E8F7A,
            0xFF6FDF6E, 0xFFF0B040, 0xFFFF5A4A, 0xFF6FDF6E, 0xFFF0B040, 0xFFFF5A4A,
            0, 0, 0, 0,
            0xFF12221B, 0xFF223A2E, 0,
            new int[]{0xFFFF7A50, 0xFF58B8FF, 0xFFC68CFF}, new int[]{0xFFFF7A50, 0xFF58B8FF, 0xFFC68CFF});

    /**
     * A plant computer's CRT page: black glass, a blue header band, cyan labels, white data and
     * EGA-bright traces. Its decoration lives in {@link ProcessComputerStyle}.
     */
    public static final MonitorPalette PROCESS_COMPUTER = new MonitorPalette(0xFF05080C, 0xFF0F2C82, 0xFFFFFFFF, 0xFF2A8C9C,
            0xFFE8F0F2, 0xFF5FD7E8, 0xFF6C8A96,
            0xFF55FF66, 0xFFFFFF55, 0xFFFF5555, 0xFF55FF66, 0xFFFFFF55, 0xFFFF5555,
            0, 0, 0, 0,
            0xFF05080C, 0xFF1E4A58, 0,
            new int[]{0xFFFFFF55, 0xFF55FFFF, 0xFFFF55FF}, new int[]{0xFFFFFF55, 0xFF55FFFF, 0xFFFF55FF});

    /**
     * A hard-wired board: seafoam enamel, black engraved nameplates, annunciator windows and strip-chart
     * paper with red, blue and green pens. Its decoration lives in {@link MimicBoardStyle}.
     */
    public static final MonitorPalette MIMIC_BOARD = new MonitorPalette(0xFFB4C5B8, 0xFF1C201E, 0xFFF3F1E7, 0xFF8A9E90,
            0xFF101412, 0xFF36423B, 0xFF55635A,
            0xFF135C25, 0xFF7A4A00, 0xFFA81E12, 0xFF4CD95A, 0xFFF3B23A, 0xFFFF4A3A,
            0xFFF3B23A, 0xFF1A1200, 0xFFC8301F, 0xFFFFF6EC,
            0xFFF0EAD3, 0xFF93B488, 0xFF101412,
            new int[]{0xFFD02A20, 0xFF2050C8, 0xFF1E8A3A}, new int[]{0xFFFF5A4E, 0xFF6E96FF, 0xFF4CC86A});

    public static MonitorPalette of(MonitorTheme theme) {
        return switch (theme) {
            case PROCESS_COMPUTER -> PROCESS_COMPUTER;
            case MIMIC_BOARD -> MIMIC_BOARD;
            default -> STANDARD;
        };
    }

    /** A reading's value in {@code tone}. */
    public int tone(MonitorRow.Tone tone) {
        return switch (tone) {
            case DIM -> dim;
            case GOOD -> good;
            case WARN -> warn;
            case BAD -> bad;
            default -> text;
        };
    }

    /** The target's state on the title line in {@code tone}. */
    public int status(MonitorRow.Tone tone) {
        return switch (tone) {
            case GOOD -> statusGood;
            case BAD -> statusBad;
            default -> statusWarn;
        };
    }

    /** The colour {@code text} is drawn in for the legend entry of line {@code index}. */
    public int legend(int index) {
        return legendText != 0 ? legendText : series[index];
    }
}
