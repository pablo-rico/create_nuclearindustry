package org.papiricoh.create_nuclearindustry.controlroom;

/**
 * The layout rules a screen's style lays its pages out by, shared by the server (how many pages a
 * screen has) and the renderer (what each page shows and how big). No game classes and no font: the
 * server must work them out alike. All sizes are in screen pixels, 80 to a block, inside the frame.
 */
public final class MonitorPaging {
    /** Pixels in a block of screen. */
    public static final int BLOCK = 80;
    /** The frame round a screen, both sides together. */
    public static final int INSET = 10;

    /** Where a page puts its readings and its graph. */
    public enum Split { ROWS, GRAPH, SIDE, STACK }

    // Process computer: a row of the point-log table at row scale 1, and what a full row needs.
    public static final float PC_ROW = 10;
    public static final float PC_MIN_ROW_SCALE = 0.8f;
    public static final float PC_FULL_ROW = 200;
    // Mimic board: an instrument tile at tile scale 1, and the compact strips that replace small tiles.
    public static final float MB_TILE_W = 64;
    public static final float MB_TILE_H = 30;
    public static final float MB_MIN_TILE = 0.8f;
    public static final float MB_STRIP_H = 11;
    public static final float MB_STRIP_GAP = 2;

    private MonitorPaging() {}

    /** A screen's drawable width or height in pixels for {@code blocks} blocks. */
    public static float size(int blocks) {
        return blocks * BLOCK - INSET;
    }

    /** How much bigger than a 3x2 screen's the chrome of a {@code w} x {@code h} screen is drawn: 1 up to 3x2, at most 3. */
    public static float screenScale(float w, float h) {
        return clamp(Math.min(w / 230, h / 150), 1, 3);
    }

    /**
     * What a page of a screen of several blocks shows. The last page of an automatic screen is the
     * graph alone; its other pages list readings, with the graph beside them on a big enough screen.
     */
    public static Split split(MonitorLayout layout, float w, float h, boolean lastPage) {
        return switch (layout) {
            case LIST -> Split.ROWS;
            case CHART -> Split.GRAPH;
            case BOTH -> w >= h ? Split.SIDE : Split.STACK;
            default -> lastPage ? Split.GRAPH : w >= 160 && h >= 100 ? Split.SIDE : Split.ROWS;
        };
    }

    /** A box: left, top, right, bottom. */
    public record Box(float x0, float y0, float x1, float y1) {
        public float width() {
            return x1 - x0;
        }

        public float height() {
            return y1 - y0;
        }
    }

    /** Where the readings and the graph go in a body, {@code gap} apart; either may be null. */
    public record Boxes(Box rows, Box graph) {}

    public static Boxes boxes(Split split, float w, float top, float bottom, float margin, float gap) {
        return switch (split) {
            case ROWS -> new Boxes(new Box(margin, top, w - margin, bottom), null);
            case GRAPH -> new Boxes(null, new Box(margin, top, w - margin, bottom));
            case SIDE -> new Boxes(new Box(margin, top, w * 0.5f - gap, bottom), new Box(w * 0.5f + gap, top, w - margin, bottom));
            case STACK -> {
                float mid = top + (bottom - top) * 0.5f;
                yield new Boxes(new Box(margin, top, w - margin, mid - gap), new Box(margin, mid + gap, w - margin, bottom));
            }
        };
    }

    // ------------------------------------------------------------------ process computer

    /** The header band, the footer (0 when the screen is too short for one) and the body between them. */
    public record PcChrome(float u, float header, float footer, float bodyTop, float bodyBottom) {}

    public static PcChrome pcChrome(float w, float h) {
        float u = screenScale(w, h);
        float footer = h >= 110 * u ? 10 * u : 0;
        return new PcChrome(u, 11 * u, footer, 13 * u, h - footer - (footer > 0 ? u : 2 * u));
    }

    /** The inside of a panel whose title is cut into its top border. */
    public static Box pcPanelInner(Box panel, float u) {
        float border = Math.max(1, 0.75f * u);
        return new Box(panel.x0() + border + 2 * u, panel.y0() + 10 * u, panel.x1() - border - 2 * u, panel.y1() - border - u);
    }

    /** The scale of a table's rows and how many a page holds. */
    public record PcRows(float scale, int perPage) {}

    /**
     * Rows as big as the height allows up to the screen scale, but no wider than keeps the gauge
     * column (never below 1 for that); shrunk down to 0.8 to fit every row, then paged.
     */
    public static PcRows pcRows(float width, float height, int rows, float u) {
        if (rows <= 0) {
            return new PcRows(u, 1);
        }
        float scale = clamp(Math.min(height / (rows * PC_ROW), Math.max(1, width / PC_FULL_ROW)), PC_MIN_ROW_SCALE, u);
        return new PcRows(scale, Math.max(1, (int) (height / (PC_ROW * scale) + 1e-4f)));
    }

    // ------------------------------------------------------------------ mimic board

    /** The board's chrome scale, nameplate header height and margin. */
    public record MbChrome(float u, float header, float margin) {}

    public static MbChrome mbChrome(float w, float h) {
        float u = screenScale(w, h);
        return new MbChrome(u, 17 * u, 3 * u);
    }

    /**
     * How a board lays out its readings: a grid of instrument tiles ({@code strips} false) at tile
     * scale {@code scale}, or compact nameplate-and-meter strips in one or two columns.
     */
    public record MbRows(boolean strips, int columns, int rows, float scale, int perPage) {}

    /**
     * Tiles at the biggest scale that shows them all, down to 0.8; below that as many as fit a page.
     * When tiles would show fewer than four a page (or fewer than there are), strips instead.
     */
    public static MbRows mbRows(int count, float width, float height, float u) {
        int n = Math.max(1, count);
        float gap = 3 * u;
        int bestColumns = 0;
        int bestRows = 0;
        float best = -1;
        for (int columns = 1; columns <= n; columns++) {
            int rows = (n + columns - 1) / columns;
            float tileWidth = (width - (columns - 1) * gap) / columns;
            float tileHeight = (height - (rows - 1) * gap) / rows;
            float scale = Math.min(Math.min(tileWidth / MB_TILE_W, tileHeight / MB_TILE_H), 3);
            if (scale >= MB_MIN_TILE && scale > best + 1e-6f) {
                best = scale;
                bestColumns = columns;
                bestRows = rows;
            }
        }
        int perPage;
        if (best > 0) {
            perPage = n;
        } else {
            bestColumns = Math.max(1, (int) ((width + gap) / (MB_TILE_W * MB_MIN_TILE + gap)));
            bestRows = Math.max(1, (int) ((height + gap) / (MB_TILE_H * MB_MIN_TILE + gap)));
            float tileWidth = (width - (bestColumns - 1) * gap) / bestColumns;
            float tileHeight = (height - (bestRows - 1) * gap) / bestRows;
            best = Math.min(tileWidth / MB_TILE_W, tileHeight / MB_TILE_H);
            perPage = bestColumns * bestRows;
        }
        if (perPage >= Math.min(n, 4)) {
            return new MbRows(false, bestColumns, bestRows, best, perPage);
        }
        float stripHeight = MB_STRIP_H * u;
        float stripGap = MB_STRIP_GAP * u;
        int rows = Math.max(1, (int) ((height + stripGap) / (stripHeight + stripGap)));
        int columns = width >= 2 * 70 * u + 3 * u ? 2 : 1;
        return new MbRows(true, columns, rows, u, columns * rows);
    }

    /** A strip-chart recorder runs its paper down unless its window is much wider than tall. */
    public static boolean recorderVertical(float width, float height) {
        return height >= 0.4f * width;
    }

    // ------------------------------------------------------------------ pages

    /** Readings one page of a screen of several blocks shows in {@code theme}: all {@code count} when they fit. */
    public static int readingsPerPage(MonitorTheme theme, MonitorLayout layout, float w, float h, int count) {
        Split split = split(layout, w, h, false);
        switch (theme) {
            case PROCESS_COMPUTER -> {
                PcChrome chrome = pcChrome(w, h);
                Box rows = boxes(split, w, chrome.bodyTop(), chrome.bodyBottom(), 2 * chrome.u(), chrome.u()).rows();
                if (rows == null) {
                    return count;
                }
                Box inner = pcPanelInner(rows, chrome.u());
                return Math.min(count, pcRows(inner.width(), inner.height(), count, chrome.u()).perPage());
            }
            case MIMIC_BOARD -> {
                MbChrome chrome = mbChrome(w, h);
                Box rows = boxes(split, w, chrome.header() + 2.5f * chrome.u(), h - chrome.margin(), chrome.margin(),
                        1.5f * chrome.u()).rows();
                if (rows == null) {
                    return count;
                }
                return Math.min(count, mbRows(count, rows.width(), rows.height(), chrome.u()).perPage());
            }
            default -> {
                return count;
            }
        }
    }

    /**
     * Pages of a screen of several blocks: as many as its readings need, plus the graph's for an
     * automatic one; a graph-only screen has one. The standard style always fits its readings on one.
     */
    public static int pageCount(MonitorTheme theme, MonitorLayout layout, int blocksWide, int blocksHigh, int count) {
        if (layout == MonitorLayout.CHART) {
            return 1;
        }
        int perPage = Math.max(1, readingsPerPage(theme, layout, size(blocksWide), size(blocksHigh), count));
        int n = Math.max(1, count);
        return (n + perPage - 1) / perPage + (layout == MonitorLayout.AUTO ? 1 : 0);
    }

    /** "870 °C" into "870" and "°C": a last word with no digit after one that has; otherwise the whole value and no unit. */
    public static String[] splitUnit(String value) {
        int space = value.lastIndexOf(' ');
        if (space > 0) {
            String head = value.substring(0, space);
            String tail = value.substring(space + 1);
            if (!tail.isEmpty() && tail.chars().noneMatch(Character::isDigit) && head.chars().anyMatch(Character::isDigit)) {
                return new String[]{head, tail};
            }
        }
        return new String[]{value, ""};
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
