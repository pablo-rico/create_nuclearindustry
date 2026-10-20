package org.papiricoh.create_nuclearindustry.missile.radar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The radar screen's regions stay on the sheet and apart, so the texture and the drawing never fight. */
class RadarLayoutTest {

    @Test
    void regionsStayOnTheSheetAndApart() {
        List<RadarLayout.Rect> all = RadarLayout.ALL;
        for (RadarLayout.Rect r : all) {
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= RadarLayout.WIDTH && r.bottom() <= RadarLayout.HEIGHT, r.name() + " on the sheet");
            assertTrue(r.w() > 0 && r.h() > 0, r.name());
        }
        for (int i = 0; i < all.size(); i++)
            for (int j = i + 1; j < all.size(); j++)
                assertFalse(all.get(i).overlaps(all.get(j)), all.get(i).name() + " and " + all.get(j).name() + " overlap");
    }

    @Test
    void theScopeAndItsBezelFitTheirSquare() {
        RadarLayout.Rect ppi = RadarLayout.PPI;
        assertTrue(RadarLayout.PPI_RADIUS < RadarLayout.BEZEL);
        assertTrue(RadarLayout.PPI_CX - RadarLayout.BEZEL >= ppi.x() && RadarLayout.PPI_CX + RadarLayout.BEZEL <= ppi.right());
        assertTrue(RadarLayout.PPI_CY - RadarLayout.BEZEL >= ppi.y() && RadarLayout.PPI_CY + RadarLayout.BEZEL <= ppi.bottom());
    }

    @Test
    void theTableHoldsItsRows() {
        assertEquals(RadarLayout.TABLE.h(), RadarLayout.HEADER + RadarLayout.ROW * RadarLayout.TABLE_ROWS + 1);
        assertTrue(RadarLayout.COL_FLAG < RadarLayout.TABLE.w() - 4);
    }
}
