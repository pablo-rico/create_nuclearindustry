package org.papiricoh.create_nuclearindustry.missile.silo.client;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleTextTest {
    private static final ToIntFunction<String> WIDTH = text -> text.codePoints().map(c -> c == 'W' ? 7 : c == 'i' ? 1 : 3).sum();

    @Test
    void keepsExactFitsAndUsesMeasuredWidths() {
        assertEquals("Wiii", ConsoleText.trim("Wiii", 10, WIDTH));
        assertEquals("W…", ConsoleText.trim("WWiii", 10, WIDTH));
        assertEquals("iii…", ConsoleText.trim("iiiWW", 6, WIDTH));
    }

    @Test
    void handlesEmptyAndNarrowColumns() {
        assertEquals("", ConsoleText.trim("", 10, WIDTH));
        assertEquals("", ConsoleText.trim("Wide", -1, WIDTH));
        assertEquals("", ConsoleText.trim("Wide", 0, WIDTH));
        assertEquals("", ConsoleText.trim("Wide", 2, WIDTH));
        assertEquals("…", ConsoleText.trim("Wide", 3, WIDTH));
    }

    @Test
    void neverSplitsSupplementaryCharactersOrLeavesSpaceBeforeEllipsis() {
        assertEquals("🚀…", ConsoleText.trim("🚀WWW", 6, WIDTH));
        assertEquals("i…", ConsoleText.trim("i WWW", 7, WIDTH));
    }

    @Test
    void everyTrimmedResultStaysWithinItsPixelBudget() {
        for (String value : new String[]{"WWiii", "TNT cannot mix with other charge", "Etapa 2 · Δv 🚀", "", "   WWW"})
            for (int width = 0; width <= 150; width++) {
                String result = ConsoleText.trim(value, width, WIDTH);
                assertTrue(WIDTH.applyAsInt(result) <= width, result + " exceeds " + width);
                if (WIDTH.applyAsInt(value) <= width) assertEquals(value, result);
                else if (width >= WIDTH.applyAsInt("…")) assertTrue(result.endsWith("…"));
            }
    }
}
