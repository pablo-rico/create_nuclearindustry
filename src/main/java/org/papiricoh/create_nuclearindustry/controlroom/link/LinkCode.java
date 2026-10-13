package org.papiricoh.create_nuclearindustry.controlroom.link;

import java.util.Locale;

/**
 * The code an Advanced Redstone Link or a control module transmits on: four letters or digits, in
 * capitals. Anything else typed or sent is dropped, so "ab-12c" becomes "AB12". An empty code
 * joins no network. No game classes, so it is tested on its own.
 */
public final class LinkCode {
    public static final int LENGTH = 4;

    private LinkCode() {}

    /** The code as it is kept: capitals and digits only, at most {@link #LENGTH} of them. */
    public static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder code = new StringBuilder(LENGTH);
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (code.length() == LENGTH) {
                break;
            }
            if (c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                code.append(c);
            }
        }
        return code.toString();
    }

    /** Whether a code is complete: only complete codes transmit or listen. */
    public static boolean complete(String code) {
        return code != null && code.length() == LENGTH && sanitize(code).equals(code);
    }

    /** Whether a typed character may go into a code. */
    public static boolean allowed(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
    }
}
