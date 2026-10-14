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
 * A 1980s plant computer's colour CRT page (a safety parameter display): black glass with scan
 * lines, a blue header band with the machine's tag and the time, box-drawn panels titled in their
 * border, a point-log table (dotted leaders, units in a column, segmented bar graphs, WRN and
 * blinking ALM flags), a trend with axes and a footer with the state, the alarm count and the page.
 */
final class ProcessComputerStyle implements MonitorStyle {
    static final ProcessComputerStyle INSTANCE = new ProcessComputerStyle();
    static final String KEY = "create_nuclearindustry.monitor.style.";

    private static final int GLASS = 0xFF05080C;
    private static final int SCAN = 0xFF0A1219;
    private static final int BAND = 0xFF0F2C82;
    private static final int BAND_TEXT = 0xFFFFFFFF;
    private static final int CLOCK = 0xFFFFFF55;
    private static final int CHIP = 0xFF55FFFF;
    private static final int CHIP_TEXT = 0xFF000000;
    private static final int FRAME = 0xFF2A8C9C;
    private static final int PANEL_TITLE = 0xFF55FFFF;
    private static final int LABEL = 0xFF5FD7E8;
    private static final int UNIT = 0xFF6FA0AE;
    private static final int LEADER = 0xFF1F4250;
    private static final int TEXT = 0xFFE8F0F2;
    private static final int DIM = 0xFF6C8A96;
    private static final int GOOD = 0xFF55FF66;
    private static final int WARN = 0xFFFFFF55;
    private static final int BAD = 0xFFFF5555;
    private static final int BAD_FILL = 0xFFD01818;
    private static final int BAD_FILL_TEXT = 0xFFFFFFFF;
    private static final int WARN_FILL = 0xFFFFFF55;
    private static final int WARN_FILL_TEXT = 0xFF000000;
    private static final int SEGMENT_OFF = 0xFF12303A;
    private static final int SEGMENT_ON = 0xFF3FD860;
    private static final int AXIS = 0xFF4F8C9C;
    private static final int GRID = 0xFF1E4A58;
    private static final int AXIS_TEXT = 0xFF6FA0AE;
    private static final String[] LETTERS = {"A", "B", "C"};

    private ProcessComputerStyle() {}

    private static int tone(MonitorRow.Tone tone) {
        return switch (tone) {
            case DIM -> DIM;
            case GOOD -> GOOD;
            case WARN -> WARN;
            case BAD -> BAD;
            default -> TEXT;
        };
    }

    private static int segment(MonitorRow.Tone tone) {
        return switch (tone) {
            case DIM -> DIM;
            case WARN -> WARN;
            case BAD -> BAD;
            default -> SEGMENT_ON;
        };
    }

    private static String text(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    /** The time of day on the level, as the plant computer's clock shows it. */
    private static String clock(MonitorFrame f) {
        long time = f.monitor().getLevel() == null ? 6000 : Math.floorMod(f.monitor().getLevel().getDayTime(), 24000L);
        long hours = (time / 1000 + 6) % 24;
        long minutes = time % 1000 * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
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
        p.rect(0, 0, w, h, GLASS);
        if (f.detail()) {
            for (float y = 1; y < h; y += 2) {
                p.rect(0, y, w, y + 0.5f, SCAN);
            }
        }
        if (f.single()) {
            single(p, f);
            return;
        }
        MonitorPaging.PcChrome chrome = MonitorPaging.pcChrome(w, h);
        float u = chrome.u();
        boolean lastPage = f.page() == f.pages() - 1;
        MonitorLayout layout = f.config().layout();
        MonitorPaging.Split split = MonitorPaging.split(layout, w, h, layout == MonitorLayout.AUTO && lastPage && f.pages() > 1);
        MonitorPaging.Boxes boxes = MonitorPaging.boxes(split, w, chrome.bodyTop(), chrome.bodyBottom(), 2 * u, u);
        List<MonitorDraw.Reading> readings = MonitorDraw.readings(f.lines());
        if (boxes.rows() != null) {
            Box inner = MonitorPaging.pcPanelInner(boxes.rows(), u);
            MonitorPaging.PcRows rows = MonitorPaging.pcRows(inner.width(), inner.height(), readings.size(), u);
            int from = Math.min(readings.size(), f.page() * rows.perPage());
            List<MonitorDraw.Reading> shown = readings.subList(from, Math.min(readings.size(), from + rows.perPage()));
            panel(p, boxes.rows(), u, text("plant_data"), null);
            float[] bar = table(p, f, shown, inner, rows.scale());
            if (bar[1] > 0) {
                barScale(p, boxes.rows(), u, bar[0], bar[1]);
            }
        }
        header(p, f, w, u, chrome.footer() == 0);
        if (boxes.graph() != null) {
            panel(p, boxes.graph(), u, text("trend"), text("trend_caption"));
            trend(p, f, MonitorPaging.pcPanelInner(boxes.graph(), u), u, false);
        }
        if (chrome.footer() > 0) {
            footer(p, f, readings, w, h, u);
        }
    }

    // ------------------------------------------------------------------ pieces

    /** Inverse-video text: a filled cell a pixel round the glyphs. Returns its right edge. */
    private static float chip(Painter p, String text, float x, float y, float s, int background, int foreground) {
        float width = p.width(text) * s + 2 * s;
        p.rect(x, y - s, x + width, y + 8 * s, background);
        p.text(text, x + 1.5f * s, y, foreground, s);
        return x + width;
    }

    /** The target's state: green text, black on yellow, or white on red blinking to a red outline. Returns its left edge. */
    private static float statusChip(Painter p, MonitorFrame f, float x, float y, float s, boolean right, float room) {
        String text = MonitorDraw.upper(MonitorDraw.status(f.reading()));
        float width = p.width(text) * s + 3 * s;
        if (room > 0 && width > room) {
            float shrunk = Math.max(0.6f * s, s * room / width);
            y += (s - shrunk) * 4;
            s = shrunk;
            width = p.width(text) * s + 3 * s;
        }
        float x0 = right ? x - width : x;
        MonitorRow.Tone tone = MonitorStatus.tone(f.reading().status());
        if (tone == MonitorRow.Tone.BAD) {
            if (f.blink()) {
                p.rect(x0, y - s, x0 + width, y + 8 * s, BAD_FILL);
                p.text(text, x0 + 1.5f * s, y, BAD_FILL_TEXT, s);
            } else {
                p.frame(x0, y - s, x0 + width, y + 8 * s, Math.max(1, 0.75f * s), BAD);
                p.text(text, x0 + 1.5f * s, y, BAD, s);
            }
        } else if (tone == MonitorRow.Tone.WARN) {
            p.rect(x0, y - s, x0 + width, y + 8 * s, WARN_FILL);
            p.text(text, x0 + 1.5f * s, y, WARN_FILL_TEXT, s);
        } else {
            p.text(text, x0 + 1.5f * s, y, GOOD, s);
        }
        return x0;
    }

    private static void header(Painter p, MonitorFrame f, float w, float u, boolean withStatus) {
        p.rect(0, 0, w, 11 * u, BAND);
        float y = 2 * u;
        float x = chip(p, MonitorDraw.code(f.reading().kind()), 2 * u, y, u, CHIP, CHIP_TEXT);
        float right = w - 2.5f * u;
        if (withStatus) {
            right = statusChip(p, f, right, y, u, true, w * 0.45f) - 4 * u;
            if (f.pages() > 1) {
                String page = (f.page() + 1) + "/" + f.pages();
                p.textRight(page, right, y, CHIP, u);
                right -= p.width(page) * u + 4 * u;
            }
        } else {
            String time = clock(f);
            p.textRight(time, right, y, CLOCK, u);
            right -= p.width(time) * u + 6 * u;
        }
        p.fitted(MonitorDraw.upper(MonitorDraw.title(f.reading(), f.config())), x + 4 * u, y, right - x - 4 * u, BAND_TEXT, u);
    }

    private static void footer(Painter p, MonitorFrame f, List<MonitorDraw.Reading> readings, float w, float h, float u) {
        float y0 = h - 10 * u;
        p.rect(0, y0, w, y0 + Math.max(1, 0.75f * u), FRAME);
        float y = y0 + 2.5f * u;
        float x = 3 * u;
        String status = text("status");
        p.text(status, x, y, LABEL, u);
        x += p.width(status + " ") * u + u;
        statusChip(p, f, x, y, u, false, 0);
        String page = text("page", f.page() + 1, f.pages());
        p.textRight(page, w - 3 * u, y, TEXT, u);
        int alarms = 0;
        boolean bad = false;
        for (MonitorDraw.Reading reading : readings) {
            if (reading.tone() == MonitorRow.Tone.WARN || reading.tone() == MonitorRow.Tone.BAD) {
                alarms++;
                bad |= reading.tone() == MonitorRow.Tone.BAD;
            }
        }
        String count = alarms > 0 ? text("alarms", alarms) : text("no_alarms");
        float ax = w - 3 * u - p.width(page) * u - 8 * u - p.width(count) * u;
        float statusEnd = x + p.width(MonitorDraw.upper(MonitorDraw.status(f.reading()))) * u + 10 * u;
        if (ax > statusEnd) {
            p.text(count, ax, y, bad ? BAD : alarms > 0 ? WARN : DIM, u);
        }
    }

    /** A single-line box with its title, and maybe a caption, cut into its top border. */
    private static void panel(Painter p, Box box, float u, String title, String caption) {
        float border = Math.max(1, 0.75f * u);
        float lineY = box.y0() + 3.5f * u;
        p.frame(box.x0(), lineY, box.x1(), box.y1(), border, FRAME);
        float tx = box.x0() + 5 * u;
        float tw = p.width(title) * u;
        p.rect(tx - 2 * u, lineY, tx + tw + u, lineY + border, GLASS);
        p.text(title, tx, box.y0(), PANEL_TITLE, u);
        if (caption != null) {
            float cw = p.width(caption) * u;
            float cx = box.x1() - 5 * u - cw;
            if (cx > tx + tw + 6 * u) {
                p.rect(cx - 2 * u, lineY, cx + cw + u, lineY + border, GLASS);
                p.text(caption, cx, box.y0(), DIM, u);
            }
        }
    }

    // ------------------------------------------------------------------ table

    /**
     * Readings as a point-log table: NAME ........ value unit FLAG [bar graph]. Returns where the bar
     * column starts and how wide it is (0 when there is no room for it).
     */
    private static float[] table(Painter p, MonitorFrame f, List<MonitorDraw.Reading> rows, Box box, float r) {
        if (rows.isEmpty()) {
            p.fitted(MonitorDraw.upper(MonitorDraw.label("no_rows")), box.x0(), box.y0(), box.width(), DIM, r);
            return new float[]{0, 0};
        }
        float x0 = box.x0();
        float x1 = box.x1();
        float tableWidth = x1 - x0;
        float labelWidth = 0;
        float numberWidth = 0;
        float anyNumber = 0;
        float unitWidth = 0;
        for (MonitorDraw.Reading row : rows) {
            labelWidth = Math.max(labelWidth, p.width(row.label()));
            anyNumber = Math.max(anyNumber, p.width(row.number()));
            if (!row.unit().isEmpty()) {
                numberWidth = Math.max(numberWidth, p.width(row.number()));
                unitWidth = Math.max(unitWidth, p.width(row.unit()));
            }
        }
        labelWidth *= r;
        numberWidth = (numberWidth > 0 ? numberWidth : anyNumber) * r;
        unitWidth *= r;
        String flagBad = text("flag.bad");
        String flagWarn = text("flag.warn");
        float flagWidth = (Math.max(p.width(flagBad), p.width(flagWarn)) + 3) * r;
        float gap = 4 * r;
        float cluster = numberWidth + (unitWidth > 0 ? 3 * r + unitWidth : 0);
        float minLabel = Math.min(labelWidth, 40 * r);
        float barWidth = Math.min(tableWidth - Math.min(labelWidth, tableWidth * 0.45f) - gap * 3 - cluster - flagWidth - 2 * r, 90 * r);
        boolean showBar = barWidth >= 24 * r;
        boolean showFlag = tableWidth - minLabel - gap * 2 - cluster - flagWidth >= 0;
        float right = x1;
        float barX = 0;
        if (showBar) {
            barX = right - barWidth;
            right = barX - gap;
        }
        float flagX = 0;
        if (showFlag) {
            flagX = right - flagWidth;
            right = flagX - gap;
        }
        float unitX = unitWidth > 0 ? right - unitWidth : right;
        float valueRight = unitWidth > 0 ? unitX - 3 * r : right;
        float pitch = MonitorPaging.PC_ROW * r;
        for (int i = 0; i < rows.size(); i++) {
            MonitorDraw.Reading row = rows.get(i);
            float y = box.y0() + i * pitch + r;
            float valueLeft = valueRight - p.width(row.number()) * r;
            float labelRoom = Math.min(valueLeft - x0 - 6 * r, labelWidth);
            p.fitted(row.label(), x0, y, labelRoom, LABEL, r);
            float labelEnd = x0 + Math.min(p.width(row.label()) * r, labelRoom);
            // Leader dots on a fixed grid, so the rows' dots line up.
            float dot = x0 + (float) Math.ceil((labelEnd + 3 * r - x0) / (3 * r)) * 3 * r;
            for (; dot < valueLeft - 3 * r; dot += 3 * r) {
                p.rect(dot, y + 6 * r, dot + r, y + 7 * r, LEADER);
            }
            if (showFlag && row.tone() == MonitorRow.Tone.BAD && f.blink()) {
                p.rect(flagX, y - r, flagX + flagWidth, y + 8 * r, BAD_FILL);
            }
            if (showBar && row.gauge() >= 0) {
                segmentBar(p, barX, y + r, barWidth, 5 * r, row.gauge(), segment(row.tone()), r);
            }
            p.text(row.number(), valueLeft, y, tone(row.tone()), r);
            if (!row.unit().isEmpty()) {
                p.text(row.unit(), unitX, y, UNIT, r);
            }
            if (showFlag && row.tone() == MonitorRow.Tone.BAD) {
                p.text(flagBad, flagX + 1.5f * r, y, f.blink() ? BAD_FILL_TEXT : BAD, r);
            } else if (showFlag && row.tone() == MonitorRow.Tone.WARN) {
                p.text(flagWarn, flagX + 1.5f * r, y, WARN, r);
            }
        }
        return new float[]{barX, showBar ? barWidth : 0};
    }

    /** "0" and "100%" over the bar column, cut into the panel's top border. */
    private static void barScale(Painter p, Box panel, float u, float barX, float barWidth) {
        float border = Math.max(1, 0.75f * u);
        float scale = 0.75f * u;
        boolean both = barWidth >= (p.width("0") + p.width("100%")) * scale + 6 * u;
        String[] labels = both ? new String[]{"0", "100%"} : new String[]{"%"};
        for (int i = 0; i < labels.length; i++) {
            float lw = p.width(labels[i]) * scale;
            float lx = both && i == 0 ? barX : barX + barWidth - lw;
            p.rect(lx - u, panel.y0() + 3.5f * u, lx + lw + u, panel.y0() + 3.5f * u + border, GLASS);
            p.text(labels[i], lx, panel.y0() + u, DIM, scale);
        }
    }

    /** A bar graph of 2-pixel segments, lit from the left as far as {@code fraction}. */
    private static void segmentBar(Painter p, float x, float y, float width, float height, float fraction, int on, float r) {
        float pitch = 3 * r;
        int count = Math.max(1, (int) ((width + r) / pitch));
        int lit = Math.round(Math.max(0, Math.min(1, fraction)) * count);
        for (int k = 0; k < count; k++) {
            float sx = x + k * pitch;
            p.rect(sx, y, sx + 2 * r, y + height, k < lit ? on : SEGMENT_OFF);
        }
    }

    // ------------------------------------------------------------------ trend

    private static void trend(Painter p, MonitorFrame f, Box box, float u, boolean compact) {
        float x0 = box.x0();
        float y0 = box.y0();
        float x1 = box.x1();
        float y1 = box.y1();
        float a = Math.max(0.6f, 0.75f * u);
        List<Component> legend = MonitorDraw.legend(f.reading(), f.config());
        int n = legend.size();
        compact |= y1 - y0 < 60 * u;
        boolean oneRow = compact || (x1 - x0 - (n - 1) * 4 * u) / n >= 40 * u;
        int legendRows = oneRow ? 1 : (n + 1) / 2;
        float legendHeight = legendRows * 10 * u;
        float labelWidth = compact ? 0 : p.width("100") * a + 2 * u;
        float px0 = x0 + labelWidth;
        float px1 = x1 - u;
        float py0 = compact ? y0 + u : y0 + 4 * a;
        float py1 = y1 - legendHeight - (7 * a + 3 * u) - (compact ? 0 : u);
        if (py1 - py0 < 10 || px1 - px0 < 20) {
            return;
        }
        float border = Math.max(1, 0.75f * u);
        if (f.detail()) {
            float dot = Math.max(0.75f, 0.5f * u);
            float step = 3 * u;
            for (int k = 1; k <= 3; k++) {
                float gy = py1 - (py1 - py0) * k / 4;
                for (float gx = px0 + step; gx < px1; gx += step) {
                    p.rect(gx, gy, gx + dot, gy + dot, GRID);
                }
                float gx = px0 + (px1 - px0) * k / 4;
                for (float gy2 = py0; gy2 < py1; gy2 += step) {
                    p.rect(gx, gy2, gx + dot, gy2 + dot, GRID);
                }
            }
        }
        p.rect(px0 - border, py0, px0, py1 + border, AXIS);
        p.rect(px0 - border, py1, px1, py1 + border, AXIS);
        for (int k = 0; k < 5; k++) {
            float gy = py1 - (py1 - py0) * k / 4;
            p.rect(px0 - border - 2 * u, gy, px0 - border, gy + border, AXIS);
        }
        float thickness = Math.max(1, 0.75f * u);
        int[] pens = MonitorDraw.lineColors(MonitorPalette.PROCESS_COMPUTER.series(), n);
        traces(p, f.monitor(), px0, py0, px1, py1, pens, thickness);
        if (!compact) {
            String[][] marks = {{"100", "4"}, {"50", "2"}, {"0", "0"}};
            for (String[] mark : marks) {
                float gy = py1 - (py1 - py0) * Integer.parseInt(mark[1]) / 4;
                p.textRight(mark[0], px0 - border - 3 * u, gy - 3.5f * a, AXIS_TEXT, a);
            }
        }
        float ty = py1 + border + 2 * u;
        p.text("-2M", px0, ty, AXIS_TEXT, a);
        p.textRight("0", px1, ty, AXIS_TEXT, a);
        if (!compact && px1 - px0 > 3 * p.width("-1M") * a + 8 * u) {
            p.textCenter("-1M", (px0 + px1) / 2, ty, AXIS_TEXT, a);
        }
        legend(p, f.monitor(), legend, pens, x0, x1, y1 - legendHeight + 1.5f * u, u, oneRow, compact);
    }

    /** Step lines, newest sample on the right; the third first so the first lies on top. */
    private static void traces(Painter p, ControlMonitorBlockEntity monitor, float px0, float py0, float px1, float py1, int[] pens,
                               float thickness) {
        int size = monitor.historySize();
        float step = (px1 - px0) / (ControlMonitorBlockEntity.HISTORY - 1);
        float span = py1 - py0 - thickness;
        for (int line = 2; line >= 0; line--) {
            if (pens[line] == 0) {
                continue;
            }
            float previous = Float.NaN;
            for (int i = 0; i < size; i++) {
                float value = MonitorDraw.sample(monitor, line, i);
                if (Float.isNaN(value)) {
                    previous = Float.NaN;
                    continue;
                }
                float x = px1 - (size - 1 - i) * step;
                float y = py1 - Math.max(0, Math.min(1, value)) * span;
                p.rect(i == 0 ? x : x - step, y - thickness, x, y, pens[line]);
                if (!Float.isNaN(previous)) {
                    p.rect(x - step - thickness / 2, Math.min(y, previous) - thickness, x - step + thickness / 2, Math.max(y, previous),
                            pens[line]);
                }
                previous = y;
            }
        }
    }

    /** Each line: a letter chip in its colour, its name and its latest value as a share of its scale. */
    private static void legend(Painter p, ControlMonitorBlockEntity monitor, List<Component> names, int[] pens, float x0, float x1,
                               float y, float u, boolean oneRow, boolean compact) {
        int n = names.size();
        if (compact) {
            float gap = 5 * u;
            float total = 0;
            for (Component name : names) {
                total += p.width(MonitorDraw.upper(name));
            }
            float scale = Math.max(0.6f, Math.min(u, (x1 - x0 - gap * (n - 1)) / Math.max(1, total)));
            float roomEach = (x1 - x0 - gap * (n - 1)) / n;
            boolean fits = total * scale + gap * (n - 1) <= x1 - x0;
            float x = x0;
            for (int i = 0; i < n; i++) {
                String name = MonitorDraw.upper(names.get(i));
                float width = p.width(name) * scale;
                float room = fits ? width : roomEach;
                p.fitted(name, x, y + (u - scale) * 4, room, pens[i], scale);
                x += Math.min(width, room) + gap;
            }
            return;
        }
        float slot = oneRow ? (x1 - x0 - (n - 1) * 3 * u) / n : (x1 - x0 - 4 * u) / 2;
        int last = monitor.historySize() - 1;
        for (int i = 0; i < n; i++) {
            float lx = oneRow ? x0 + i * (slot + 3 * u) : x0 + (i % 2) * (slot + 4 * u);
            float ly = oneRow ? y : y + (i / 2) * 10 * u;
            float end = chip(p, LETTERS[i], lx, ly, u, pens[i], CHIP_TEXT);
            String name = MonitorDraw.upper(names.get(i));
            float latest = last < 0 ? Float.NaN : MonitorDraw.sample(monitor, i, last);
            String value = Float.isNaN(latest) ? "--" : Math.round(Math.max(0, Math.min(1, latest)) * 100) + "%";
            float valueWidth = p.width(value) * u;
            float room = slot - (end - lx) - 3 * u;
            if (room - valueWidth - 4 * u >= p.width(name) * u * 0.8f) {
                p.textRight(value, lx + slot, ly, TEXT, u);
                room -= valueWidth + 4 * u;
            }
            p.fitted(name, end + 3 * u, ly, room, pens[i], u);
        }
    }

    // ------------------------------------------------------------------ a single block

    private static void single(Painter p, MonitorFrame f) {
        float w = f.width();
        float h = f.height();
        p.rect(0, 0, w, 11, BAND);
        MonitorLayout layout = f.config().layout();
        boolean graph = layout == MonitorLayout.CHART || layout == MonitorLayout.BOTH && f.page() == f.pages() - 1;
        float x = chip(p, MonitorDraw.code(f.reading().kind()), 2, 2, 1, CHIP, CHIP_TEXT);
        if (graph) {
            p.text(text("trend"), x + 3, 2, BAND_TEXT);
            trend(p, f, new Box(3, 13, w - 3, h - 2), 1, true);
            return;
        }
        if (f.lines().isEmpty()) {
            p.fitted(MonitorDraw.upper(MonitorDraw.label("no_rows")), w / 2, h / 2 - 4, w - 6, DIM, 1, Painter.Align.CENTER);
            return;
        }
        List<MonitorDraw.Reading> readings = MonitorDraw.readings(f.lines());
        MonitorDraw.Reading row = readings.get(Math.floorMod(f.page(), readings.size()));
        String page = (f.page() + 1) + "/" + f.pages();
        p.textRight(page, w - 2.5f, 2, DIM, 1);
        p.fitted(row.label(), x + 3, 2, w - x - 3 - p.width(page) - 6, BAND_TEXT, 1);
        // The value as a faceplate shows it: the number big, its unit small on the same baseline.
        float unitWidth = row.unit().isEmpty() ? 0 : p.width(row.unit());
        float right = w - 4 - (row.unit().isEmpty() ? 0 : unitWidth + 1);
        float scale = Math.min(2.5f, (right - 4) / Math.max(1, p.width(row.number())));
        float vy = 16 + (2.5f - scale) * 4;
        p.textRight(row.number(), right, vy, tone(row.tone()), scale);
        if (!row.unit().isEmpty()) {
            p.text(row.unit(), right + 1, vy + 7 * scale - 7, UNIT);
        }
        if (row.gauge() >= 0) {
            segmentBar(p, 4, 40, w - 8, 6, row.gauge(), segment(row.tone()), 1);
            for (int k = 0; k < 5; k++) {
                float tx = 4 + (w - 8 - 1) * k / 4;
                p.rect(tx, 47, tx + 1, k % 2 == 0 ? 49 : 48, AXIS);
            }
            p.text("0", 4, 50, AXIS_TEXT, 0.6f);
            p.textRight("100", w - 4, 50, AXIS_TEXT, 0.6f);
        }
        float y0 = h - 12;
        String status = MonitorDraw.upper(MonitorDraw.status(f.reading()));
        MonitorRow.Tone tone = MonitorStatus.tone(f.reading().status());
        if (tone == MonitorRow.Tone.BAD && f.blink()) {
            p.rect(3, y0, w - 3, h - 2, BAD_FILL);
            p.fitted(status, w / 2, y0 + 1.5f, w - 10, BAD_FILL_TEXT, 1, Painter.Align.CENTER);
        } else if (tone == MonitorRow.Tone.WARN) {
            p.rect(3, y0, w - 3, h - 2, WARN_FILL);
            p.fitted(status, w / 2, y0 + 1.5f, w - 10, WARN_FILL_TEXT, 1, Painter.Align.CENTER);
        } else {
            int color = tone == MonitorRow.Tone.BAD ? BAD : GOOD;
            p.frame(3, y0, w - 3, h - 2, 1, color);
            p.fitted(status, w / 2, y0 + 1.5f, w - 10, color, 1, Painter.Align.CENTER);
        }
    }
}
