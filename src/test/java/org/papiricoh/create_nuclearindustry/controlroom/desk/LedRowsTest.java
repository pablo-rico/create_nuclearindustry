package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How LED modules side by side join into displays. */
class LedRowsTest {

    @Test
    void shortRunsAreOneDisplay() {
        for (int i = 0; i < 3; i++) {
            assertEquals(new LedRows.Span(0, 3), LedRows.pieceOf(i, 3, 8));
        }
        assertEquals(new LedRows.Span(0, 1), LedRows.pieceOf(0, 1, 8), "a module on its own");
    }

    @Test
    void longRunsAreCutFromTheLeft() {
        assertEquals(new LedRows.Span(0, 8), LedRows.pieceOf(7, 10, 8));
        assertEquals(new LedRows.Span(8, 2), LedRows.pieceOf(8, 10, 8));
        assertEquals(new LedRows.Span(8, 2), LedRows.pieceOf(9, 10, 8));
        assertEquals(new LedRows.Span(8, 8), LedRows.pieceOf(15, 16, 8));
        assertTrue(LedRows.pieceOf(9, 10, 8).contains(9));
    }

    @Test
    void wideDisplaysFitTheirWindowAndShowMore() {
        int digits = 0;
        for (int cells = 1; cells <= 8; cells++) {
            LedLayout layout = LedLayout.of(cells);
            assertEquals(cells * 8 - 1, layout.width(), 1e-6, "window width for " + cells);
            assertTrue(layout.digits() >= digits, "digits never shrink as the display grows");
            digits = layout.digits();
            assertTrue(layout.digits() <= LedLayout.MAX_DIGITS);
            assertTrue(layout.digitLeft(0) >= LedLayout.MARGIN - 1e-4, "first digit inside the window for " + cells);
            assertTrue(layout.digitsRight() <= layout.unitRight(), "digits left of the unit for " + cells);
            assertTrue(layout.unitRight() <= layout.width(), "unit inside the window for " + cells);
            assertTrue(layout.digitsTop() + layout.digitHeight() <= layout.height(), "digits inside the window for " + cells);
            assertTrue(layout.unitTop() + layout.unitHeight() <= layout.height(), "unit inside the window for " + cells);
            assertEquals(cells > 1, layout.label(), "a label strip once there is room");
            if (layout.label()) {
                assertTrue(LedLayout.MARGIN + layout.labelHeight() <= layout.digitsTop(), "label above the digits");
            }
        }
        assertEquals(4, LedLayout.of(1).digits());
        assertTrue(LedLayout.of(2).digits() > 4);
    }
}
