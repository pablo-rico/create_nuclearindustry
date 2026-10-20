package org.papiricoh.create_nuclearindustry.missile.silo.client;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.gui.SiloLayout;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleNumbersTest {
    @Test
    void everyCellHasExclusiveEdgesAndItsOwnIndex() {
        for (int i = 0; i < SiloLayout.CELLS; i++) {
            assertEquals(i, ConsoleNumbers.cellAt(SiloLayout.cellX(), SiloLayout.cellY(i), SiloLayout.CELLS));
            assertEquals(i, ConsoleNumbers.cellAt(SiloLayout.cellX() + 17.99, SiloLayout.cellY(i) + 9.99, SiloLayout.CELLS));
        }
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX() - .01, SiloLayout.cellY(0), 18));
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX() + 18, SiloLayout.cellY(0), 18));
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX(), SiloLayout.baseY(), 18));
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX(), SiloLayout.cellY(18), 100));
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX(), SiloLayout.cellY(3), 3));
        assertEquals(-1, ConsoleNumbers.cellAt(SiloLayout.cellX(), SiloLayout.cellY(0), 0));
        assertEquals(-1, ConsoleNumbers.cellAt(Double.NaN, 100, 18));
    }

    @Test
    void countdownRoundsUpWithoutOverflow() {
        assertEquals(0, ConsoleNumbers.countdownSeconds(-1));
        assertEquals(0, ConsoleNumbers.countdownSeconds(0));
        assertEquals(1, ConsoleNumbers.countdownSeconds(1));
        assertEquals(1, ConsoleNumbers.countdownSeconds(20));
        assertEquals(2, ConsoleNumbers.countdownSeconds(21));
        assertEquals(107374183, ConsoleNumbers.countdownSeconds(Integer.MAX_VALUE));
    }

    @Test
    void coordinatesRejectPartialInputAndOverflow() {
        assertEquals(-30000000, ConsoleNumbers.coordinate("-30000000"));
        assertEquals(Integer.MAX_VALUE, ConsoleNumbers.coordinate("2147483647"));
        for (String invalid : new String[]{"", "-", "1.5", "2147483648", " 20", "NaN"})
            assertNull(ConsoleNumbers.coordinate(invalid), invalid);
    }

    @Test
    void readoutsStayCompactAndHandleUnavailableNumbers() {
        assertEquals("0.0", ConsoleNumbers.number(0));
        assertEquals("2.5", ConsoleNumbers.number(2.5));
        assertEquals("125", ConsoleNumbers.number(125));
        assertEquals("12.5k", ConsoleNumbers.number(12500));
        assertEquals("1.5M", ConsoleNumbers.number(1500000));
        assertEquals("—", ConsoleNumbers.number(Double.NaN));
        assertEquals("—", ConsoleNumbers.number(Double.POSITIVE_INFINITY));
    }
}
