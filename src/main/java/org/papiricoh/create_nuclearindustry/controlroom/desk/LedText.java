package org.papiricoh.create_nuclearindustry.controlroom.desk;

import java.util.Locale;

/**
 * What a row of seven-segment digits shows for a value: right-aligned, with as many decimals as fit
 * (up to the reading's own), the decimal point lit on a digit rather than taking one. A value too big
 * for the digits reads "OL", as on a multimeter, and no value at all reads dashes.
 */
public final class LedText {
    private LedText() {}

    /** {@code cells} holds one character per digit; {@code dotAfter} is the digit carrying the point, or -1. */
    public record Readout(String cells, int dotAfter) {
        @Override
        public String toString() {
            if (dotAfter < 0) {
                return cells;
            }
            return cells.substring(0, dotAfter + 1) + "." + cells.substring(dotAfter + 1);
        }
    }

    public static Readout format(double value, int digits, int maxDecimals) {
        if (digits <= 0) {
            return new Readout("", -1);
        }
        if (Double.isNaN(value)) {
            return new Readout("-".repeat(digits), -1);
        }
        if (Double.isFinite(value)) {
            for (int decimals = Math.min(Math.max(0, maxDecimals), digits - 1); decimals >= 0; decimals--) {
                String text = String.format(Locale.ROOT, "%." + decimals + "f", Math.abs(value));
                String bare = text.replace(".", "");
                // A negative value that rounds to nothing shows no sign.
                boolean negative = value < 0 && bare.chars().anyMatch(c -> c != '0');
                int width = bare.length() + (negative ? 1 : 0);
                if (width <= digits) {
                    String cells = " ".repeat(digits - width) + (negative ? "-" : "") + bare;
                    return new Readout(cells, decimals > 0 ? digits - decimals - 1 : -1);
                }
            }
        }
        return new Readout(digits >= 2 ? " ".repeat(digits - 2) + "OL" : "-", -1);
    }
}
