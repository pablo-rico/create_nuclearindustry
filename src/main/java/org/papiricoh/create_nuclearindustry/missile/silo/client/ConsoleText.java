package org.papiricoh.create_nuclearindustry.missile.silo.client;

import java.util.function.ToIntFunction;

/** Pixel-width trimming shared by the consoles; never splits a Unicode code point. */
public final class ConsoleText {
    private ConsoleText() {}

    public static String trim(String text, int width, ToIntFunction<String> measure) {
        if (width <= 0) return "";
        if (measure.applyAsInt(text) <= width) return text;
        String ellipsis = "…";
        if (measure.applyAsInt(ellipsis) > width) return "";
        int end = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (measure.applyAsInt(text.substring(0, next) + ellipsis) > width) break;
            end = next;
        }
        return text.substring(0, end).stripTrailing() + ellipsis;
    }
}
