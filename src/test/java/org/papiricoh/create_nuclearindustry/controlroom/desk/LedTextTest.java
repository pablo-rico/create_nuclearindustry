package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What an LED display's digits read for a value. */
class LedTextTest {

    private static String shown(double value, int digits, int decimals) {
        LedText.Readout readout = LedText.format(value, digits, decimals);
        assertEquals(digits, readout.cells().length(), "one character per digit");
        return readout.toString();
    }

    @Test
    void decimalsFillTheDigitsLeftOver() {
        assertEquals("152.5", shown(152.5, 4, 1));
        assertEquals("4000", shown(4000, 4, 0));
        assertEquals("  12", shown(12, 4, 0));
        assertEquals("   1.25", shown(1.25, 6, 2));
        assertEquals(" 0.00", shown(0.004, 4, 2));
    }

    @Test
    void theSignTakesADigit() {
        assertEquals("-3.20", shown(-3.2, 4, 2));
        assertEquals("-150", shown(-150, 4, 1));
        assertEquals(" 0.00", shown(-0.001, 4, 2), "no minus sign on a value that rounds to nothing");
    }

    @Test
    void roundingUpCanCostADecimal() {
        assertEquals("10.0", shown(9.996, 3, 2));
    }

    @Test
    void tooBigReadsOverload() {
        assertEquals("  OL", shown(12345, 4, 0));
        assertEquals("  OL", shown(Double.POSITIVE_INFINITY, 4, 0));
        assertEquals("    OL", shown(1.0e6, 6, 1));
    }

    @Test
    void noValueReadsDashes() {
        assertEquals("----", shown(Double.NaN, 4, 2));
    }

    @Test
    void everyCharacterShownHasSegments() {
        for (String text : new String[]{"0123456789", "-", "OL"}) {
            for (char c : text.toCharArray()) {
                assertEquals(true, SevenSegment.mask(c) != 0, "segments for " + c);
            }
        }
        assertEquals(SevenSegment.ALL, SevenSegment.mask('8'));
        assertEquals(0, SevenSegment.mask(' '));
    }
}
