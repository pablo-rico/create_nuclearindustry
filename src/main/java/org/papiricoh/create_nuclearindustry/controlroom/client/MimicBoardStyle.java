package org.papiricoh.create_nuclearindustry.controlroom.client;

import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorLayout;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorPaging;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorPaging.Box;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorStatus;

import java.util.List;

/**
 * A section of a 1970s hard-wired control board: seafoam enamel, an engraved nameplate with screws,
 * the state in a backlit annunciator window, page lamps, a grid of instruments (a nameplate that
 * lights amber or red, a digital panel meter and an edgewise meter) and a strip-chart recorder whose
 * paper runs down under red, blue and green pens.
 */
final class MimicBoardStyle implements MonitorStyle {
    static final MimicBoardStyle INSTANCE = new MimicBoardStyle();

    private static final int ENAMEL = 0xFFB4C5B8;
    private static final int GROOVE = 0xFF8A9E90;
    private static final int ENAMEL_LIGHT = 0xFFCAD7CD;
    private static final int PLATE = 0xFF1C201E;
    private static final int PLATE_EDGE = 0xFF4E5852;
    private static final int PLATE_TEXT = 0xFFF3F1E7;
    private static final int SCREW = 0xFF9AA39D;
    private static final int SCREW_SLOT = 0xFF3A403C;
    private static final int CASE = 0xFF2B302D;
    private static final int CASE_LIP = 0xFF4F5853;
    private static final int CARD = 0xFFF4F1E4;
    private static final int INK = 0xFF1A1A1A;
    private static final int POINTER = 0xFFD0281E;
    private static final int METER = 0xFF1A0C08;
    private static final int DIGIT = 0xFFFF9A3D;
    private static final int DIGIT_DIM = 0xFF9A6036;
    private static final int DIGIT_UNIT = 0xFFD07A34;
    private static final int LAMP_OFF = 0xFF454B47;
    private static final int LAMP_GOOD = 0xFF4CD95A;
    private static final int LAMP_WARN = 0xFFF3B23A;
    private static final int LAMP_BAD = 0xFFFF4A3A;
    private static final int WINDOW_WARN = 0xFFF3B23A;
    private static final int WINDOW_WARN_TEXT = 0xFF1A1200;
    private static final int WINDOW_BAD = 0xFFC8301F;
    private static final int WINDOW_BAD_TEXT = 0xFFFFF6EC;
    private static final int WINDOW_BAD_OFF = 0xFF5A1A14;
    private static final int WINDOW_BAD_OFF_TEXT = 0xFFFFB0A0;
    private static final int WINDOW_GOOD = 0xFF9BE38F;
    private static final int WINDOW_GOOD_TEXT = 0xFF0E2A10;
    private static final int PAPER = 0xFFF0EAD3;
    private static final int GRID_MINOR = 0xFFD2DEC4;
    private static final int GRID_MAJOR = 0xFF93B488;
    private static final int PAPER_INK = 0xFF2E5A34;

    private MimicBoardStyle() {}

    private static int lamp(MonitorRow.Tone tone) {
        return switch (tone) {
            case GOOD -> LAMP_GOOD;
            case WARN -> LAMP_WARN;
            case BAD -> LAMP_BAD;
            default -> LAMP_OFF;
        };
    }

    @Override
    public void draw(MonitorFrame f) {
        Painter p = new Painter(f);
        try {
            page(p, f);
        } finally {
            p.finish();
        }
    }

    private static void page(Painter p, MonitorFrame f) {
        float w = f.width();
        float h = f.height();
        if (f.single()) {
            single(p, f, w, h);
            return;
        }
        MonitorPaging.MbChrome chrome = MonitorPaging.mbChrome(w, h);
        float u = chrome.u();
        board(p, w, h, u);
        MonitorLayout layout = f.config().layout();
        boolean lastPage = f.page() == f.pages() - 1;
        MonitorPaging.Split split = MonitorPaging.split(layout, w, h, layout == MonitorLayout.AUTO && lastPage && f.pages() > 1);
        float top = chrome.header() + 2.5f * u;
        float bottom = h - chrome.margin();
        MonitorPaging.Boxes boxes = MonitorPaging.boxes(split, w, top, bottom, chrome.margin(), 1.5f * u);
        header(p, f, w, u);
        if (boxes.rows() != null) {
            tiles(p, f, MonitorDraw.readings(f.lines()), boxes.rows(), u);
        }
        if (boxes.graph() != null) {
            if (boxes.rows() != null && split == MonitorPaging.Split.SIDE) {
                // A groove between the board's two sections.
                float gx = (boxes.rows().x1() + boxes.graph().x0()) / 2;
                p.rect(gx - 0.25f * u, top, gx + 0.5f * u, bottom, GROOVE);
            }
            recorder(p, f, boxes.graph(), u);
        }
    }

    // ------------------------------------------------------------------ pieces

    private static void board(Painter p, float w, float h, float u) {
        p.rect(0, 0, w, h, ENAMEL);
        p.frame(0, 0, w, h, u, GROOVE);
        p.rect(u, u, w - u, 1.5f * u, ENAMEL_LIGHT);
    }

    private static void screw(Painter p, float cx, float cy, float s) {
        p.rect(cx - s, cy - s, cx + s, cy + s, SCREW);
        p.rect(cx - s, cy - 0.25f * s, cx + s, cy + 0.25f * s, SCREW_SLOT);
    }

    private static void plate(Painter p, float x0, float y0, float x1, float y1, float s, int background, int edge) {
        p.rect(x0, y0, x1, y1, edge);
        p.rect(x0 + 0.5f * s, y0 + 0.5f * s, x1 - 0.5f * s, y1 - 0.5f * s, background);
    }

    /** The target's state in a backlit window: green, amber, or red flashing. */
    private static void annunciator(Painter p, MonitorFrame f, float x0, float y0, float x1, float y1, float s) {
        p.rect(x0, y0, x1, y1, CASE);
        MonitorRow.Tone tone = MonitorStatus.tone(f.reading().status());
        int background;
        int text;
        if (tone == MonitorRow.Tone.GOOD) {
            background = WINDOW_GOOD;
            text = WINDOW_GOOD_TEXT;
        } else if (tone == MonitorRow.Tone.BAD) {
            background = f.blink() ? WINDOW_BAD : WINDOW_BAD_OFF;
            text = f.blink() ? WINDOW_BAD_TEXT : WINDOW_BAD_OFF_TEXT;
        } else {
            background = WINDOW_WARN;
            text = WINDOW_WARN_TEXT;
        }
        float b = 1.25f * s;
        p.rect(x0 + b, y0 + b, x1 - b, y1 - b, background);
        // The frosted edge of the lens.
        p.rect(x0 + b, y0 + b, x1 - b, y0 + b + 0.5f * s, tone == MonitorRow.Tone.BAD ? background : ENAMEL_LIGHT);
        float height = y1 - y0 - 2 * b;
        float scale = Math.min(s, height / 9);
        p.fitted(MonitorDraw.upper(MonitorDraw.status(f.reading())), (x0 + x1) / 2, y0 + b + (height - 7 * scale) / 2,
                x1 - x0 - 2 * b - 3 * s, text, scale, Painter.Align.CENTER);
    }

    /** One small lamp per page, the shown one lit, or an engraved counter when there are too many. Returns the right edge. */
    private static float pageLamps(Painter p, float x, float y, float s, int page, int pages, int most) {
        if (pages <= 1) {
            return x;
        }
        if (pages > most) {
            String text = (page + 1) + "/" + pages;
            float width = p.width(text) * s + 4 * s;
            plate(p, x, y - 3.25f * s, x + width, y + 6.75f * s, s, PLATE, PLATE_EDGE);
            p.text(text, x + 2 * s, y - 1.75f * s, PLATE_TEXT, s);
            return x + width;
        }
        for (int k = 0; k < pages; k++) {
            float lx = x + k * 5 * s;
            p.rect(lx, y, lx + 3.5f * s, y + 3.5f * s, CASE);
            p.rect(lx + 0.5f * s, y + 0.5f * s, lx + 3 * s, y + 3 * s, k == page ? LAMP_WARN : LAMP_OFF);
        }
        return x + pages * 5 * s;
    }

    private static void header(Painter p, MonitorFrame f, float w, float u) {
        String status = MonitorDraw.upper(MonitorDraw.status(f.reading()));
        float statusWidth = Math.min(w * 0.42f, p.width(status) * u + 10 * u);
        float sx1 = w - 3 * u;
        float sx0 = sx1 - statusWidth;
        annunciator(p, f, sx0, 2.5f * u, sx1, 14.5f * u, u);
        float lamps = pageLamps(p, 4 * u, 6.75f * u, u, f.page(), f.pages(), 6);
        float left = f.pages() > 1 ? lamps + 3 * u : 3 * u;
        float right = sx0 - 4 * u;
        String title = MonitorDraw.upper(MonitorDraw.title(f.reading(), f.config()));
        float plateWidth = Math.min(p.width(title) * u + 16 * u, right - left);
        float x0 = w / 2 - plateWidth / 2;
        if (x0 < left || x0 + plateWidth > right) {
            x0 = left;
        }
        plate(p, x0, 2.5f * u, x0 + plateWidth, 14.5f * u, u, PLATE, PLATE_EDGE);
        screw(p, x0 + 3.5f * u, 8.5f * u, u);
        screw(p, x0 + plateWidth - 3.5f * u, 8.5f * u, u);
        p.fitted(title, x0 + plateWidth / 2, 4.5f * u, plateWidth - 14 * u, PLATE_TEXT, u, Painter.Align.CENTER);
        p.rect(2 * u, 16 * u, w - 2 * u, 16.75f * u, GROOVE);
    }

    // ------------------------------------------------------------------ instruments

    private static void tiles(Painter p, MonitorFrame f, List<MonitorDraw.Reading> readings, Box box, float u) {
        if (readings.isEmpty()) {
            p.fitted(MonitorDraw.upper(MonitorDraw.label("no_rows")), box.x0(), box.y0(), box.width(), INK, u);
            return;
        }
        MonitorPaging.MbRows layout = MonitorPaging.mbRows(readings.size(), box.width(), box.height(), u);
        int from = Math.min(readings.size(), f.page() * layout.perPage());
        List<MonitorDraw.Reading> shown = readings.subList(from, Math.min(readings.size(), from + layout.perPage()));
        if (shown.isEmpty()) {
            return;
        }
        if (layout.strips()) {
            strips(p, f, shown, box, u, layout.columns(), layout.rows());
            return;
        }
        float gap = 3 * u;
        float t = layout.scale();
        int columns = layout.columns();
        int rows = layout.rows();
        float tileWidth = (box.width() - (columns - 1) * gap) / columns;
        float tileHeight = (box.height() - (rows - 1) * gap) / rows;
        float y0 = box.y0();
        // Tiles never grow taller than a little over their own shape; the grid sits in the middle of the rest.
        float tallest = MonitorPaging.MB_TILE_H * t * 1.15f;
        if (tileHeight > tallest) {
            y0 += (tileHeight - tallest) * rows / 2;
            tileHeight = tallest;
        }
        // One nameplate size for the whole board: the longest name decides.
        float room = tileWidth - 3 * t - 4.5f * t;
        float nameScale = t;
        for (MonitorDraw.Reading reading : shown) {
            nameScale = Math.min(nameScale, room / Math.max(1, p.width(reading.label())));
        }
        nameScale = Math.max(0.6f, nameScale);
        for (int i = 0; i < shown.size(); i++) {
            tile(p, f, shown.get(i), box.x0() + (i % columns) * (tileWidth + gap), y0 + (i / columns) * (tileHeight + gap),
                    tileWidth, tileHeight, t, nameScale);
        }
    }

    /** The nameplate of an instrument, lit amber on a warning and red (flashing) on an alarm; returns its text colour. */
    private static int nameplate(Painter p, MonitorFrame f, MonitorRow.Tone tone, float x0, float y0, float x1, float y1, float s) {
        if (tone == MonitorRow.Tone.WARN) {
            plate(p, x0, y0, x1, y1, s, WINDOW_WARN, CASE);
            return WINDOW_WARN_TEXT;
        }
        if (tone == MonitorRow.Tone.BAD) {
            plate(p, x0, y0, x1, y1, s, f.blink() ? WINDOW_BAD : WINDOW_BAD_OFF, CASE);
            return f.blink() ? WINDOW_BAD_TEXT : WINDOW_BAD_OFF_TEXT;
        }
        plate(p, x0, y0, x1, y1, s, PLATE, PLATE_EDGE);
        return PLATE_TEXT;
    }

    private static void tile(Painter p, MonitorFrame f, MonitorDraw.Reading row, float x, float y, float w, float h, float t,
                             float nameScale) {
        float plateHeight = 9 * t;
        int text = nameplate(p, f, row.tone(), x, y, x + w, y + plateHeight, t);
        float lampSize = 3 * t;
        float lx = x + w - 1.5f * t - lampSize;
        p.rect(lx, y + 3 * t, lx + lampSize, y + 3 * t + lampSize, lamp(row.tone()));
        float room = lx - x - 3 * t;
        p.fitted(row.label(), x + 1.5f * t + room / 2, y + (plateHeight - 7 * nameScale) / 2, room, text, nameScale, Painter.Align.CENTER);
        float caseTop = y + plateHeight + t;
        p.rect(x, caseTop, x + w, y + h, CASE);
        p.rect(x, caseTop, x + w, caseTop + 0.5f * t, CASE_LIP);
        boolean gauge = row.gauge() >= 0;
        float meterHeight = 10 * t;
        float dy0 = gauge ? caseTop + 1.5f * t : caseTop + (y + h - caseTop - meterHeight) / 2;
        float dx0 = x + 2 * t;
        float dx1 = x + w - 2 * t;
        digits(p, row, dx0, dy0, dx1, dy0 + meterHeight, t, 3 * t, 1.5f * t);
        if (gauge) {
            edgewise(p, dx0, dy0 + meterHeight + 1.5f * t, dx1, y + h - 1.5f * t, row.gauge(), t, false);
        }
    }

    /** A digital panel meter: amber digits and their unit, right-aligned in a dark window. */
    private static void digits(Painter p, MonitorDraw.Reading row, float x0, float y0, float x1, float y1, float most, float margin,
                               float inset) {
        p.rect(x0, y0, x1, y1, METER);
        float unitWidth = row.unit().isEmpty() ? 0 : p.width(row.unit()) + 2;
        float scale = Math.min(most, (x1 - x0 - margin) / Math.max(1, p.width(row.number()) + unitWidth));
        float vy = y0 + (y1 - y0 - 7 * scale) / 2;
        float right = x1 - inset;
        if (!row.unit().isEmpty()) {
            p.textRight(row.unit(), right, vy, DIGIT_UNIT, scale);
            right -= unitWidth * scale;
        }
        p.textRight(row.number(), right, vy, row.tone() == MonitorRow.Tone.DIM ? DIGIT_DIM : DIGIT, scale);
    }

    /** An edgewise meter: a cream scale card, ticks every 10% (long every 50%) and a red pointer. */
    private static void edgewise(Painter p, float x0, float y0, float x1, float y1, float fraction, float t, boolean numbers) {
        p.rect(x0, y0, x1, y1, CARD);
        float height = y1 - y0;
        float sx0 = x0 + 2 * t;
        float sx1 = x1 - 2 * t;
        for (int k = 0; k <= 10; k++) {
            float tx = sx0 + (sx1 - sx0) * k / 10;
            boolean major = k % 5 == 0;
            p.rect(tx - 0.25f * t, y0, tx + 0.25f * t + (major ? 0.25f * t : 0), y0 + height * (major ? 0.55f : 0.3f), INK);
        }
        float px = sx0 + (sx1 - sx0) * Math.max(0, Math.min(1, fraction));
        p.rect(px - 0.5f * t, y0, px + 0.5f * t, y1, POINTER);
        if (numbers) {
            float a = Math.max(0.6f, 0.6f * t);
            String[] labels = {"0", "50", "100"};
            for (int i = 0; i < 3; i++) {
                float tx = sx0 + (sx1 - sx0) * i / 2;
                float lw = p.width(labels[i]) * a;
                float lx = Math.min(Math.max(tx - lw / 2, x0 + 0.5f * t), x1 - 0.5f * t - lw);
                p.text(labels[i], lx, y0 + height * 0.6f, INK, a);
            }
        }
    }

    /** Small boards: a nameplate beside a digital meter, a thin pointer track under the meter. */
    private static void strips(Painter p, MonitorFrame f, List<MonitorDraw.Reading> rows, Box box, float u, int columns, int perColumn) {
        float stripHeight = MonitorPaging.MB_STRIP_H * u;
        float gap = MonitorPaging.MB_STRIP_GAP * u;
        float columnWidth = (box.width() - (columns - 1) * 3 * u) / columns;
        float plateWidth = columnWidth * 0.5f;
        float nameScale = u;
        for (MonitorDraw.Reading row : rows) {
            nameScale = Math.min(nameScale, (plateWidth - 7 * u) / Math.max(1, p.width(row.label())));
        }
        nameScale = Math.max(0.6f, nameScale);
        for (int i = 0; i < rows.size(); i++) {
            MonitorDraw.Reading row = rows.get(i);
            float x = box.x0() + (i / perColumn) * (columnWidth + 3 * u);
            float y = box.y0() + (i % perColumn) * (stripHeight + gap);
            int text = nameplate(p, f, row.tone(), x, y, x + plateWidth, y + stripHeight, u);
            float lampSize = 2.5f * u;
            p.rect(x + plateWidth - 1.5f * u - lampSize, y + (stripHeight - lampSize) / 2, x + plateWidth - 1.5f * u,
                    y + (stripHeight + lampSize) / 2, lamp(row.tone()));
            p.fitted(row.label(), x + 2 * u, y + (stripHeight - 7 * nameScale) / 2, plateWidth - 7 * u, text, nameScale);
            p.rect(x + plateWidth + u, y, x + columnWidth, y + stripHeight, CASE);
            float dx0 = x + plateWidth + 2 * u;
            float dx1 = x + columnWidth - u;
            float dy1 = y + stripHeight - (row.gauge() >= 0 ? 3 * u : u);
            digits(p, row, dx0, y + u, dx1, dy1, Math.min(u, (dy1 - y - 2 * u) / 7), 2 * u, u);
            if (row.gauge() >= 0) {
                p.rect(dx0, dy1 + 0.5f * u, dx1, y + stripHeight - 0.5f * u, CARD);
                float px = dx0 + u + (dx1 - dx0 - 2 * u) * Math.max(0, Math.min(1, row.gauge()));
                p.rect(px - 0.5f * u, dy1 + 0.5f * u, px + 0.5f * u, y + stripHeight - 0.5f * u, POINTER);
            }
        }
    }

    // ------------------------------------------------------------------ strip-chart recorder

    private static void recorder(Painter p, MonitorFrame f, Box box, float u) {
        float x0 = box.x0();
        float y0 = box.y0();
        float x1 = box.x1();
        float y1 = box.y1();
        p.rect(x0, y0, x1, y1, CASE);
        p.rect(x0, y0, x1, y0 + 0.75f * u, CASE_LIP);
        float bezel = 2.5f * u;
        List<Component> legend = MonitorDraw.legend(f.reading(), f.config());
        int n = legend.size();
        int[] pens = MonitorDraw.lineColors(MonitorPalette.MIMIC_BOARD.series(), n);
        int[] plateInk = MonitorPalette.MIMIC_BOARD.guiSeries();
        // The legend plate along the bottom of the case.
        float legendHeight = 10 * u;
        float ly0 = y1 - bezel - legendHeight;
        plate(p, x0 + bezel, ly0, x1 - bezel, y1 - bezel, u, PLATE, PLATE_EDGE);
        float slot = (x1 - x0 - 2 * bezel - 3 * u) / n;
        float widest = 0;
        float total = 0;
        for (Component name : legend) {
            float width = p.width(MonitorDraw.upper(name));
            widest = Math.max(widest, width);
            total += width;
        }
        if (slot - 8 * u < widest * u) {
            // A small recorder: the names alone in their pens' colours, at one shared size.
            float gap = 6 * u;
            float room = x1 - x0 - 2 * bezel - 4 * u - gap * (n - 1);
            float scale = Math.max(0.6f, Math.min(u, room / Math.max(1, total)));
            float x = x0 + bezel + 2 * u;
            for (int i = 0; i < n; i++) {
                String name = MonitorDraw.upper(legend.get(i));
                float width = p.width(name) * scale;
                float r = total * scale <= room ? width : room / n;
                p.fitted(name, x, ly0 + (legendHeight - 7 * scale) / 2, r, plateInk[i], scale);
                x += Math.min(width, r) + gap;
            }
        } else {
            for (int i = 0; i < n; i++) {
                float sx = x0 + bezel + 2 * u + i * slot;
                p.rect(sx, ly0 + 4 * u, sx + 4 * u, ly0 + 6 * u, plateInk[i]);
                p.fitted(MonitorDraw.upper(legend.get(i)), sx + 5.5f * u, ly0 + 1.5f * u, slot - 8 * u, PLATE_TEXT, u);
            }
        }
        float wx0 = x0 + bezel;
        float wx1 = x1 - bezel;
        float wy0 = y0 + bezel;
        float wy1 = ly0 - 1.5f * u;
        if (wy1 - wy0 < 12 || wx1 - wx0 < 20) {
            return;
        }
        float a = Math.max(0.6f, 0.6f * u);
        float thickness = Math.max(1, 0.9f * u);
        ControlMonitorBlockEntity monitor = f.monitor();
        if (MonitorPaging.recorderVertical(wx1 - wx0, wy1 - wy0)) {
            vertical(p, monitor, pens, wx0, wy0, wx1, wy1, u, a, thickness, f.detail());
        } else {
            horizontal(p, monitor, pens, wx0, wy0, wx1, wy1, u, a, thickness, f.detail());
        }
    }

    /** Paper running down, newest at the top, under a printed scale with each pen's index on it. */
    private static void vertical(Painter p, ControlMonitorBlockEntity monitor, int[] pens, float wx0, float wy0, float wx1, float wy1,
                                 float u, float a, float thickness, boolean detail) {
        float band = 9 * u;
        p.rect(wx0, wy0, wx1, wy0 + band, CARD);
        float px0 = wx0 + 3 * u;
        float px1 = wx1 - 3 * u;
        for (int k = 0; k <= 10; k++) {
            float tx = px0 + (px1 - px0) * k / 10;
            p.rect(tx - 0.25f * u, wy0 + band - (k % 5 == 0 ? 2.5f * u : 1.5f * u), tx + 0.25f * u, wy0 + band, INK);
        }
        String[] labels = {"0", "50", "100"};
        for (int i = 0; i < 3; i++) {
            float tx = px0 + (px1 - px0) * i / 2;
            float lw = p.width(labels[i]) * a;
            p.text(labels[i], Math.min(Math.max(tx - lw / 2, wx0 + 0.5f * u), wx1 - 0.5f * u - lw), wy0 + u, INK, a);
        }
        float py0 = wy0 + band + 0.75f * u;
        float py1 = wy1;
        p.rect(wx0, py0, wx1, py1, PAPER);
        // The pen carriage's rail between the scale and the paper.
        p.rect(wx0, wy0 + band, wx1, py0, CASE);
        for (int k = 0; k <= 10; k++) {
            boolean major = k % 5 == 0;
            if (!major && !detail) {
                continue;
            }
            float tx = px0 + (px1 - px0) * k / 10;
            p.rect(tx - 0.25f * u, py0, tx + 0.25f * u + (major ? 0.25f * u : 0), py1, major ? GRID_MAJOR : GRID_MINOR);
        }
        for (int k = 1; k < 12; k++) {
            boolean major = k % 6 == 0;
            if (!major && !detail) {
                continue;
            }
            float ty = py0 + (py1 - py0) * k / 12;
            p.rect(wx0, ty, wx1, ty + (major ? 0.75f * u : 0.5f * u), major ? GRID_MAJOR : GRID_MINOR);
            if (major && py1 - py0 > 40 * u) {
                p.text("-" + k / 6 + " MIN", px0 + u, ty - 6 * a, PAPER_INK, a);
            }
        }
        int size = monitor.historySize();
        float step = (py1 - py0) / (ControlMonitorBlockEntity.HISTORY - 1);
        for (int line = 2; line >= 0; line--) {
            if (pens[line] == 0) {
                continue;
            }
            float previous = Float.NaN;
            float latest = Float.NaN;
            for (int i = size - 1; i >= 0; i--) {
                float value = MonitorDraw.sample(monitor, line, i);
                if (Float.isNaN(value)) {
                    previous = Float.NaN;
                    continue;
                }
                float y = py0 + (size - 1 - i) * step;
                float x = px0 + (px1 - px0) * Math.max(0, Math.min(1, value));
                if (Float.isNaN(latest)) {
                    latest = x;
                }
                p.rect(x - thickness / 2, y, x + thickness / 2, Math.min(py1, y + step + 0.01f), pens[line]);
                if (!Float.isNaN(previous) && Math.abs(previous - x) > 0.01f) {
                    p.rect(Math.min(previous, x) - thickness / 2, y, Math.max(previous, x) + thickness / 2, y + thickness, pens[line]);
                }
                previous = x;
            }
            if (!Float.isNaN(latest)) {
                // The pen's index: a small triangle on the scale's lower edge at its latest value.
                for (int j = 0; j < 3; j++) {
                    p.rect(latest - (j + 0.5f) * u * 0.6f, wy0 + band - 3 * u + j * u * 0.8f, latest + (j + 0.5f) * u * 0.6f,
                            wy0 + band - 2.2f * u + j * u * 0.8f, pens[line]);
                }
            }
        }
    }

    /** A wide window: paper running left under a scale on its right edge. */
    private static void horizontal(Painter p, ControlMonitorBlockEntity monitor, int[] pens, float wx0, float wy0, float wx1, float wy1,
                                   float u, float a, float thickness, boolean detail) {
        float band = p.width("100") * a + 4 * u;
        p.rect(wx1 - band, wy0, wx1, wy1, CARD);
        float py0 = wy0 + 2 * u;
        float py1 = wy1 - 2 * u;
        float px0 = wx0;
        float px1 = wx1 - band - 0.75f * u;
        p.rect(px0, wy0, px1, wy1, PAPER);
        for (int k = 0; k <= 10; k++) {
            boolean major = k % 5 == 0;
            if (!major && !detail) {
                continue;
            }
            float ty = py1 - (py1 - py0) * k / 10;
            p.rect(px0, ty, px1, ty + (major ? 0.75f * u : 0.5f * u), major ? GRID_MAJOR : GRID_MINOR);
        }
        String[] labels = {"0", "50", "100"};
        for (int i = 0; i < 3; i++) {
            float ty = py1 - (py1 - py0) * i / 2;
            p.text(labels[i], wx1 - band + 3 * u, ty - 3.5f * a, INK, a);
        }
        int size = monitor.historySize();
        float step = (px1 - px0) / (ControlMonitorBlockEntity.HISTORY - 1);
        for (int line = 2; line >= 0; line--) {
            if (pens[line] == 0) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                float value = MonitorDraw.sample(monitor, line, i);
                if (Float.isNaN(value)) {
                    continue;
                }
                float x = px1 - (size - 1 - i) * step;
                float y = py1 - (py1 - py0) * Math.max(0, Math.min(1, value));
                p.rect(x - step, y - thickness / 2, x, y + thickness / 2, pens[line]);
            }
        }
    }

    // ------------------------------------------------------------------ a single block

    private static void single(Painter p, MonitorFrame f, float w, float h) {
        board(p, w, h, 1);
        MonitorLayout layout = f.config().layout();
        if (layout == MonitorLayout.CHART || layout == MonitorLayout.BOTH && f.page() == f.pages() - 1) {
            recorder(p, f, new Box(3, 3, w - 3, h - 3), 1);
            return;
        }
        if (f.lines().isEmpty()) {
            p.fitted(MonitorDraw.upper(MonitorDraw.label("no_rows")), w / 2, h / 2 - 4, w - 6, INK, 1, Painter.Align.CENTER);
            return;
        }
        List<MonitorDraw.Reading> readings = MonitorDraw.readings(f.lines());
        MonitorDraw.Reading row = readings.get(Math.floorMod(f.page(), readings.size()));
        int text = nameplate(p, f, row.tone(), 3, 2.5f, w - 3, 13.5f, 1);
        screw(p, 6.5f, 8, 1);
        screw(p, w - 6.5f, 8, 1);
        p.fitted(row.label(), w / 2, 4.5f, w - 18, text, 1, Painter.Align.CENTER);
        float caseTop = 15.5f;
        float caseBottom = 49;
        p.rect(3, caseTop, w - 3, caseBottom, CASE);
        p.rect(3, caseTop, w - 3, caseTop + 0.5f, CASE_LIP);
        digits(p, row, 5, caseTop + 2, w - 5, caseTop + 15, 1.5f, 4, 2);
        if (row.gauge() >= 0) {
            edgewise(p, 5, caseTop + 17, w - 5, caseBottom - 2, row.gauge(), 1, true);
        }
        annunciator(p, f, 3, 51.5f, w - 3, 63.5f, 1);
        float lamps = Math.min(f.pages(), 12) * 5 - 1.5f;
        pageLamps(p, w / 2 - lamps / 2, 65.25f, 1, f.page(), f.pages(), 12);
    }
}
