package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How each style pages a screen's readings, the same on the server (page count) and the client
 * (what a page shows). Expected values come from the approved mock-ups.
 */
class MonitorPagingTest {
    private static final int REACTOR_ROWS = 14;

    @Test
    void theStandardStyleKeepsItsPages() {
        for (int[] size : new int[][]{{2, 1}, {3, 2}, {8, 8}}) {
            assertEquals(1, pages(MonitorTheme.STANDARD, MonitorLayout.LIST, size, REACTOR_ROWS));
            assertEquals(1, pages(MonitorTheme.STANDARD, MonitorLayout.BOTH, size, REACTOR_ROWS));
            assertEquals(1, pages(MonitorTheme.STANDARD, MonitorLayout.CHART, size, REACTOR_ROWS));
            assertEquals(2, pages(MonitorTheme.STANDARD, MonitorLayout.AUTO, size, REACTOR_ROWS), "readings, then the graph");
        }
    }

    @Test
    void aProcessComputerFitsAReactorOnAThreeByTwo() {
        assertEquals(1, pages(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.LIST, new int[]{3, 2}, REACTOR_ROWS));
        assertEquals(1, pages(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.BOTH, new int[]{3, 2}, REACTOR_ROWS));
        assertEquals(2, pages(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.AUTO, new int[]{3, 2}, REACTOR_ROWS));
        assertEquals(5, MonitorPaging.readingsPerPage(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.LIST, 150, 70, REACTOR_ROWS),
                "a two-block screen pages its table five rows at a time");
        assertEquals(3, pages(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.LIST, new int[]{2, 1}, REACTOR_ROWS));
        assertEquals(11, MonitorPaging.readingsPerPage(MonitorTheme.PROCESS_COMPUTER, MonitorLayout.BOTH, 70, 230, REACTOR_ROWS));
    }

    @Test
    void aMimicBoardPagesItsInstruments() {
        assertEquals(1, pages(MonitorTheme.MIMIC_BOARD, MonitorLayout.LIST, new int[]{3, 2}, REACTOR_ROWS), "fourteen tiles fit alone");
        assertEquals(8, MonitorPaging.readingsPerPage(MonitorTheme.MIMIC_BOARD, MonitorLayout.BOTH, 230, 150, REACTOR_ROWS),
                "beside the recorder, eight a page");
        assertEquals(2, pages(MonitorTheme.MIMIC_BOARD, MonitorLayout.BOTH, new int[]{3, 2}, REACTOR_ROWS));
        assertEquals(3, pages(MonitorTheme.MIMIC_BOARD, MonitorLayout.AUTO, new int[]{3, 2}, REACTOR_ROWS), "two of readings, one of graph");
        assertEquals(5, pages(MonitorTheme.MIMIC_BOARD, MonitorLayout.BOTH, new int[]{2, 1}, REACTOR_ROWS));
        assertEquals(1, pages(MonitorTheme.MIMIC_BOARD, MonitorLayout.BOTH, new int[]{4, 3}, REACTOR_ROWS), "a big board shows them all");
    }

    @Test
    void tilesShrinkThenGiveWayToStrips() {
        MonitorPaging.MbRows grid = MonitorPaging.mbRows(14, 224, 121, 1);
        assertFalse(grid.strips());
        assertEquals(4, grid.columns());
        assertEquals(4, grid.rows());
        assertEquals(0.84f, grid.scale(), 0.01f);
        assertEquals(14, grid.perPage());
        MonitorPaging.MbRows strips = MonitorPaging.mbRows(5, 64.5f, 46, 1);
        assertTrue(strips.strips(), "a small corner holds strips, not tiny tiles");
        assertEquals(3, strips.perPage());
    }

    @Test
    void theScaleGrowsWithTheScreen() {
        assertEquals(1, MonitorPaging.screenScale(230, 150), 1e-6f);
        assertEquals(1, MonitorPaging.screenScale(70, 70), 1e-6f, "never below 1");
        assertEquals(1.533f, MonitorPaging.screenScale(470, 230), 0.001f);
        assertEquals(2.739f, MonitorPaging.screenScale(630, 630), 0.001f);
        assertSame(MonitorPaging.Split.GRAPH, MonitorPaging.split(MonitorLayout.AUTO, 230, 150, true), "the last automatic page is the graph");
        assertSame(MonitorPaging.Split.SIDE, MonitorPaging.split(MonitorLayout.AUTO, 230, 150, false));
        assertSame(MonitorPaging.Split.ROWS, MonitorPaging.split(MonitorLayout.AUTO, 150, 70, false));
        assertSame(MonitorPaging.Split.STACK, MonitorPaging.split(MonitorLayout.BOTH, 70, 230, false));
    }

    @Test
    void unitsSplitOffTheirValues() {
        assertArrayEquals(new String[]{"870", "°C"}, MonitorPaging.splitUnit("870 °C"));
        assertArrayEquals(new String[]{"325/290", "°C"}, MonitorPaging.splitUnit("325/290 °C"));
        assertArrayEquals(new String[]{"35% > 35%", ""}, MonitorPaging.splitUnit("35% > 35%"), "the last word has digits");
        assertArrayEquals(new String[]{"Closed", ""}, MonitorPaging.splitUnit("Closed"));
        assertArrayEquals(new String[]{"1/1", "mB/t"}, MonitorPaging.splitUnit("1/1 mB/t"));
    }

    private static int pages(MonitorTheme theme, MonitorLayout layout, int[] size, int rows) {
        return MonitorPaging.pageCount(theme, layout, size[0], size[1], rows);
    }
}
