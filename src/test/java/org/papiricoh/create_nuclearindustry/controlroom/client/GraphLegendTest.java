package org.papiricoh.create_nuclearindustry.controlroom.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The graph's legend: two entries as screens always had them, a third beside them or below them. */
class GraphLegendTest {

    @Test
    void twoEntriesKeepTheirPlaces() {
        GraphLegend.Layout layout = GraphLegend.layout(3, 100, 2);
        assertEquals(1, layout.rows());
        assertEquals(2, layout.slots().length);
        assertEquals(new GraphLegend.Slot(3, 0, 48, false), layout.slots()[0], "the first on the left, in half the width");
        assertEquals(new GraphLegend.Slot(103, 0, 48, true), layout.slots()[1], "the second against the right edge");
        assertEquals(11, layout.height(), "the graph starts where it always did");
    }

    @Test
    void aThirdEntryFitsBesideTheOthersOnAWideGraph() {
        GraphLegend.Layout layout = GraphLegend.layout(0, 144, 3);
        assertEquals(1, layout.rows());
        assertNoOverlap(layout, 0, 144);
    }

    @Test
    void onANarrowGraphTheThirdGoesBelow() {
        GraphLegend.Layout layout = GraphLegend.layout(3, 64, 3);
        assertEquals(2, layout.rows(), "a single block's graph has no room for three side by side");
        assertEquals(1, layout.slots()[2].row());
        assertEquals(21, layout.height(), "the graph starts below both rows");
        assertNoOverlap(layout, 3, 64);
    }

    private static void assertNoOverlap(GraphLegend.Layout layout, float x, float width) {
        GraphLegend.Slot[] slots = layout.slots();
        for (GraphLegend.Slot slot : slots) {
            float start = slot.right() ? slot.x() - slot.room() : slot.x();
            assertTrue(start >= x && start + slot.room() <= x + width + 0.001f, "inside the graph: " + slot);
            assertTrue(slot.room() >= GraphLegend.MIN_SLOT, "room for some text: " + slot);
        }
        for (int i = 0; i < slots.length; i++) {
            for (int j = i + 1; j < slots.length; j++) {
                if (slots[i].row() != slots[j].row()) {
                    continue;
                }
                float a = slots[i].right() ? slots[i].x() - slots[i].room() : slots[i].x();
                float b = slots[j].right() ? slots[j].x() - slots[j].room() : slots[j].x();
                assertFalse(a < b + slots[j].room() && b < a + slots[i].room(), "entries " + i + " and " + j + " overlap");
            }
        }
    }
}
