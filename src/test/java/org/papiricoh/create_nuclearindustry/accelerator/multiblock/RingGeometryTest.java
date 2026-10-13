package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The synchrotron ring's cells, their order and sectors. */
class RingGeometryTest {

    @Test
    void aRingHasAboutTwoPiRCells() {
        for (int r : new int[]{6, 8, 16, 32, 64}) {
            RingGeometry ring = RingGeometry.of(r);
            double expected = 2 * Math.PI * r;
            assertTrue(Math.abs(ring.cellCount() - expected) < 0.15 * expected + 4, "R=" + r + ": " + ring.cellCount());
        }
    }

    @Test
    void everyCellIsAtTheRadiusAndNoneRepeats() {
        RingGeometry ring = RingGeometry.of(12);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < ring.cellCount(); i++) {
            assertEquals(12, TokamakGeometry.rho(ring.dx(i), ring.dz(i)));
            assertTrue(seen.add(((long) ring.dx(i) << 32) ^ (ring.dz(i) & 0xffffffffL)));
        }
    }

    @Test
    void theBandSeparatesInsideFromOutside() {
        // Walking outwards along any row from the centre always crosses a ring cell.
        RingGeometry ring = RingGeometry.of(9);
        for (int dz = -8; dz <= 8; dz++) {
            boolean crossed = false;
            for (int dx = 0; dx <= 12; dx++) {
                crossed |= ring.isRing(dx, dz);
            }
            assertTrue(crossed, "row dz=" + dz);
        }
    }

    @Test
    void cellsGoRoundInOrderAndFillTheirSectors() {
        RingGeometry ring = RingGeometry.of(16);
        assertEquals(Math.round(2 * Math.PI * 16 / RingGeometry.CELLS_PER_SECTOR), ring.sectorCount());
        int[] perSector = new int[ring.sectorCount()];
        int last = -1;
        for (int i = 0; i < ring.cellCount(); i++) {
            assertTrue(ring.sector(i) >= last, "sectors never go back");
            last = ring.sector(i);
            perSector[ring.sector(i)]++;
        }
        for (int count : perSector) {
            assertTrue(count >= 4, "every sector has cells");
        }
    }

    @Test
    void cachedPerRadiusAndRefusesTinyRings() {
        assertSame(RingGeometry.of(10), RingGeometry.of(10));
        assertThrows(IllegalArgumentException.class, () -> RingGeometry.of(RingGeometry.MIN_RADIUS - 1));
    }
}
