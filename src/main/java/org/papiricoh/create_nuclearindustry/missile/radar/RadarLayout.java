package org.papiricoh.create_nuclearindustry.missile.radar;

import java.util.List;

/**
 * Where everything sits on the radar's screen, in texture pixels. {@code tools/texturegen/tex_missile.py} paints
 * {@code radar_screen.png} from the same numbers, so keep both in step; JUnit checks the regions stay apart and on the sheet.
 */
public final class RadarLayout {
    public static final int WIDTH = 384, HEIGHT = 256;

    /** A rectangle of the screen: {@code x, y} its top left, {@code w × h} its size. */
    public record Rect(String name, int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }

        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public boolean overlaps(Rect o) {
            return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
        }
    }

    public static final Rect TITLE = new Rect("title", 6, 4, 372, 13);
    /** The scope's square, its bezel included. */
    public static final Rect PPI = new Rect("ppi", 6, 20, 212, 212);
    /** The scope glass: a circle of {@link #PPI_RADIUS} round ({@link #PPI_CX}, {@link #PPI_CY}); the bezel runs out to {@link #BEZEL}. */
    public static final int PPI_CX = 112, PPI_CY = 126, PPI_RADIUS = 99, BEZEL = 105;

    public static final Rect ZOOM_OUT = new Rect("zoom_out", 6, 236, 15, 15);
    public static final Rect ZOOM_IN = new Rect("zoom_in", 23, 236, 15, 15);
    public static final Rect RANGE = new Rect("range", 40, 236, 64, 15);
    public static final Rect CENTRE = new Rect("centre", 106, 236, 30, 15);
    public static final Rect ZONE = new Rect("zone", 138, 236, 80, 15);

    public static final Rect TABLE = new Rect("table", 224, 20, 154, 92);
    /** The table's header band and rows: {@link #TABLE_ROWS} rows of {@link #ROW} pixels under a header of {@link #HEADER}. */
    public static final int HEADER = 11, ROW = 10, TABLE_ROWS = 8;
    public static final Rect DETAIL = new Rect("detail", 224, 116, 154, 62);
    public static final Rect PROFILE = new Rect("profile", 224, 182, 154, 50);
    public static final Rect CODE = new Rect("code", 224, 236, 44, 15);
    public static final Rect FILTER = new Rect("filter", 270, 236, 50, 15);
    public static final Rect ACK = new Rect("ack", 322, 236, 56, 15);

    /** Every region, for the layout test. */
    public static final List<Rect> ALL = List.of(TITLE, PPI, ZOOM_OUT, ZOOM_IN, RANGE, CENTRE, ZONE, TABLE, DETAIL, PROFILE, CODE, FILTER, ACK);

    /** Column starts in the track table, from its left edge. */
    public static final int COL_NAME = 3, COL_PHASE = 42, COL_RANGE = 70, COL_ETA = 108, COL_FLAG = 144;

    private RadarLayout() {
    }
}
