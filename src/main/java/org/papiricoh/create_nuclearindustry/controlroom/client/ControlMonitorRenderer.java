package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorLayout;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.List;

/**
 * Draws a whole control room screen from its origin monitor: a list of readings with gauges, a
 * two-minute history graph, or (on a single block) one big reading per page. What it shows and in
 * which order comes from the screen's {@link MonitorConfig}; the rows themselves from {@link MonitorRow}.
 */
public class ControlMonitorRenderer implements BlockEntityRenderer<ControlMonitorBlockEntity> {
    private static final float INSET = 1 / 16f;
    /** Size of one text pixel, in blocks. */
    private static final float PIXEL = 1 / 80f;
    private static final int LINE = 10;
    /** Below the title line. */
    private static final float TOP = 17;
    private static final int BACKDROP = 0xFF0B1511;
    private static final int TEXT = 0xFFB8F0C4;
    private static final int DIM = 0xFF6E8F7A;
    private static final int GOOD = 0xFF6FDF6E;
    private static final int WARN = 0xFFF0B040;
    private static final int BAD = 0xFFFF5A4A;
    private static final int SERIES_A = 0xFFFF7A50;
    private static final int SERIES_B = 0xFF58B8FF;
    private static final String KEY = MonitorRow.KEY;

    private final Font font;

    public ControlMonitorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ControlMonitorBlockEntity monitor, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!monitor.isOrigin()) {
            return;
        }
        int blocksWide = monitor.screenWidth();
        int blocksHigh = monitor.screenHeight();
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, monitor.facing());
        ScreenDraw.rect(ms, buffers, INSET, INSET, blocksWide - INSET, blocksHigh - INSET, 0, BACKDROP);
        ms.translate(INSET, blocksHigh - INSET, 0.001);
        ms.scale(PIXEL, -PIXEL, PIXEL);
        float width = (blocksWide - 2 * INSET) / PIXEL;
        float height = (blocksHigh - 2 * INSET) / PIXEL;
        draw(monitor, ms, buffers, width, height);
        ms.popPose();
    }

    private void draw(ControlMonitorBlockEntity monitor, PoseStack ms, MultiBufferSource buffers, float width, float height) {
        MonitorReading reading = monitor.getReading();
        if (reading.kind() == TargetKind.NONE) {
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "unlinked"), width / 2, height / 2 - 10, DIM);
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "unlinked.hint"), width / 2, height / 2 + 2, DIM);
            return;
        }
        if (reading.kind() == TargetKind.NO_SIGNAL) {
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "no_signal"), width / 2, height / 2 - 10, BAD);
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "no_signal." + reading.status()),
                    width / 2, height / 2 + 2, DIM);
            return;
        }
        MonitorConfig config = monitor.getConfig();
        List<MonitorRow.Line> lines = MonitorRow.visible(reading, config);
        MonitorLayout layout = config.layout();
        boolean single = monitor.screenWidth() == 1 && monitor.screenHeight() == 1;
        if (single) {
            int pages = monitor.pageCount();
            int page = Math.floorMod(monitor.getPage(), pages);
            if (layout == MonitorLayout.CHART || layout == MonitorLayout.BOTH && page == pages - 1) {
                drawGraph(monitor, ms, buffers, reading, config, 3, 3, width - 6, height - 6);
            } else if (lines.isEmpty()) {
                ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "no_rows"), width / 2, height / 2 - 4, DIM);
            } else {
                drawBig(ms, buffers, reading, lines.get(Math.floorMod(monitor.getPage(), lines.size())), width, height);
            }
            return;
        }
        drawTitle(ms, buffers, reading, config, width);
        float bodyHeight = height - TOP;
        switch (layout) {
            case LIST -> drawRows(ms, buffers, lines, 3, TOP, width - 6, bodyHeight);
            case CHART -> drawGraph(monitor, ms, buffers, reading, config, 3, TOP, width - 6, bodyHeight - 3);
            case BOTH -> {
                if (width >= height) {
                    float listWidth = width * 0.5f;
                    drawRows(ms, buffers, lines, 3, TOP, listWidth - 6, bodyHeight);
                    drawGraph(monitor, ms, buffers, reading, config, listWidth + 3, TOP, width - listWidth - 6, bodyHeight - 3);
                } else {
                    // Taller than wide: readings on top, graph below.
                    float listHeight = (float) Math.floor(bodyHeight * 0.5f / LINE) * LINE;
                    drawRows(ms, buffers, lines, 3, TOP, width - 6, listHeight);
                    drawGraph(monitor, ms, buffers, reading, config, 3, TOP + listHeight + 2, width - 6, bodyHeight - listHeight - 5);
                }
            }
            default -> {
                if (monitor.getPage() == 1) {
                    drawGraph(monitor, ms, buffers, reading, config, 3, TOP, width - 6, bodyHeight - 3);
                    return;
                }
                boolean split = width >= 160 && height >= 100;
                float listWidth = split ? width * 0.5f : width;
                drawRows(ms, buffers, lines, 3, TOP, listWidth - 6, bodyHeight);
                if (split) {
                    drawGraph(monitor, ms, buffers, reading, config, listWidth + 3, TOP, width - listWidth - 6, bodyHeight - 3);
                }
            }
        }
    }

    /** The title line: the screen's own title or what it watches, and the target's state on the right. */
    private void drawTitle(PoseStack ms, MultiBufferSource buffers, MonitorReading reading, MonitorConfig config, float width) {
        Component status = status(reading);
        ScreenDraw.textRight(ms, buffers, font, status, width - 3, 3, statusColor(reading));
        Component title = config.title().isEmpty()
                ? Component.translatable(reading.kind().translationKey())
                : Component.literal(config.title());
        // A long title shrinks, and is cut short if it still runs into the state.
        float room = width - 6 - font.width(status) - 6;
        fitted(ms, buffers, title, 3, 3, room, TEXT, false);
        ScreenDraw.rect(ms, buffers, 2, 13, width - 2, 14, -0.02f, 0xFF2C4A3A);
    }

    private void drawRows(PoseStack ms, MultiBufferSource buffers, List<MonitorRow.Line> lines, float x, float y, float width, float height) {
        if (lines.isEmpty()) {
            fitted(ms, buffers, Component.translatable(KEY + "no_rows"), x, y, width, DIM, false);
            return;
        }
        int fit = Math.max(1, (int) (height / LINE));
        float barWidth = Math.min(46, width * 0.32f);
        for (int i = 0; i < Math.min(fit, lines.size()); i++) {
            MonitorRow.Line line = lines.get(i);
            Component label = Component.translatable(line.labelKey());
            Component value = value(line);
            int color = color(line.tone());
            float lineY = y + i * LINE;
            ScreenDraw.text(ms, buffers, font, label, x, lineY, DIM);
            // Room left of the label; the gauge goes first when it does not fit, then the value shrinks.
            float room = width - font.width(label) - 6;
            float valueWidth = font.width(value);
            boolean bar = line.gauge() >= 0 && valueWidth + barWidth + 4 <= room;
            float valueRight = bar ? x + width - barWidth - 4 : x + width;
            float valueRoom = bar ? room - barWidth - 4 : room;
            if (valueWidth > valueRoom && valueRoom > 4) {
                float scale = valueRoom / valueWidth;
                ms.pushPose();
                ms.translate(valueRight, lineY + (1 - scale) * 4, 0);
                ms.scale(scale, scale, 1);
                ScreenDraw.textRight(ms, buffers, font, value, 0, 0, color);
                ms.popPose();
            } else {
                ScreenDraw.textRight(ms, buffers, font, value, valueRight, lineY, color);
            }
            if (bar) {
                ScreenDraw.bar(ms, buffers, x + width - barWidth, lineY + 1, barWidth, 6, line.gauge());
            }
        }
    }

    /** One reading as large as fits on a single block. */
    private void drawBig(PoseStack ms, MultiBufferSource buffers, MonitorReading reading, MonitorRow.Line line, float width, float height) {
        ScreenDraw.textCentered(ms, buffers, font, Component.translatable(line.labelKey()), width / 2, 4, DIM);
        Component value = value(line);
        float scale = Math.min(2.5f, (width - 6) / Math.max(1, font.width(value)));
        ms.pushPose();
        ms.translate(width / 2, height / 2 - 4 * scale, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, value, 0, 0, color(line.tone()));
        ms.popPose();
        if (line.gauge() >= 0) {
            ScreenDraw.bar(ms, buffers, 6, height - 22, width - 12, 6, line.gauge());
        }
        // A long status ("Cooling magnets") shrinks to fit the block.
        Component status = status(reading);
        float statusScale = Math.min(1.0f, (width - 4) / Math.max(1, font.width(status)));
        ms.pushPose();
        ms.translate(width / 2, height - 12 + (1 - statusScale) * 4, 0);
        ms.scale(statusScale, statusScale, 1);
        ScreenDraw.textCentered(ms, buffers, font, status, 0, 0, statusColor(reading));
        ms.popPose();
    }

    private void drawGraph(ControlMonitorBlockEntity monitor, PoseStack ms, MultiBufferSource buffers, MonitorReading reading,
                           MonitorConfig config, float x, float y, float width, float height) {
        Component[] legend = legend(reading, config);
        float half = width / 2 - 2;
        fitted(ms, buffers, legend[0], x, y, half, SERIES_A, false);
        fitted(ms, buffers, legend[1], x + width, y, half, SERIES_B, true);
        float gy = y + LINE + 1;
        float gh = height - LINE - 1;
        if (gh < 8 || width < 20) {
            return;
        }
        ScreenDraw.rect(ms, buffers, x, gy, x + width, gy + gh, -0.03f, 0xFF12221B);
        for (int i = 1; i < 4; i++) {
            float lineY = gy + gh * i / 4f;
            ScreenDraw.rect(ms, buffers, x, lineY, x + width, lineY + 0.5f, -0.025f, 0xFF223A2E);
        }
        int size = monitor.historySize();
        float step = width / (ControlMonitorBlockEntity.HISTORY - 1);
        float start = x + width - (size - 1) * step;
        for (int i = 0; i < size; i++) {
            plot(ms, buffers, start + i * step, step, gy, gh, monitor.historyB(i), i == 0 ? Float.NaN : monitor.historyB(i - 1), SERIES_B);
            plot(ms, buffers, start + i * step, step, gy, gh, monitor.historyA(i), i == 0 ? Float.NaN : monitor.historyA(i - 1), SERIES_A);
        }
    }

    /** One step of a series: a short flat segment, joined to the previous value by a vertical one. */
    private static void plot(PoseStack ms, MultiBufferSource buffers, float x, float step, float gy, float gh,
                             float value, float previous, int color) {
        float y = gy + gh - Math.max(0, Math.min(1, value)) * (gh - 1);
        ScreenDraw.rect(ms, buffers, x - step, y - 1, x, y, -0.015f, color);
        if (!Float.isNaN(previous)) {
            float py = gy + gh - Math.max(0, Math.min(1, previous)) * (gh - 1);
            ScreenDraw.rect(ms, buffers, x - step - 0.5f, Math.min(y, py) - 1, x - step + 0.5f, Math.max(y, py), -0.015f, color);
        }
    }

    /**
     * Text in at most {@code room} pixels from {@code x} (or ending at {@code x} when {@code right}):
     * shrunk down to 60% if needed, then cut short.
     */
    private void fitted(PoseStack ms, MultiBufferSource buffers, Component text, float x, float y, float room, int color, boolean right) {
        if (room <= 4) {
            return;
        }
        float textWidth = font.width(text);
        if (textWidth <= room) {
            if (right) {
                ScreenDraw.textRight(ms, buffers, font, text, x, y, color);
            } else {
                ScreenDraw.text(ms, buffers, font, text, x, y, color);
            }
            return;
        }
        float scale = Math.max(0.6f, room / textWidth);
        Component shown = text;
        if (textWidth * scale > room) {
            shown = Component.literal(font.plainSubstrByWidth(text.getString(), (int) (room / scale) - font.width("..")) + "..");
        }
        ms.pushPose();
        ms.translate(x, y + (1 - scale) * 4, 0);
        ms.scale(scale, scale, 1);
        if (right) {
            ScreenDraw.textRight(ms, buffers, font, shown, 0, 0, color);
        } else {
            ScreenDraw.text(ms, buffers, font, shown, 0, 0, color);
        }
        ms.popPose();
    }

    // ------------------------------------------------------------------ content

    private static Component value(MonitorRow.Line line) {
        return line.translated() ? Component.translatable(line.value()) : Component.literal(line.value());
    }

    private static int color(MonitorRow.Tone tone) {
        return switch (tone) {
            case DIM -> DIM;
            case GOOD -> GOOD;
            case WARN -> WARN;
            case BAD -> BAD;
            default -> TEXT;
        };
    }

    /** What the graph's two lines plot: the series the screen was set up with, or the target's usual pair. */
    private static Component[] legend(MonitorReading reading, MonitorConfig config) {
        Component[] usual = legend(reading.kind());
        return new Component[]{series(config.seriesA(), reading.kind(), usual[0]), series(config.seriesB(), reading.kind(), usual[1])};
    }

    private static Component series(GaugeReading chosen, TargetKind kind, Component usual) {
        return chosen != null && chosen.kind() == kind ? Component.translatable(chosen.translationKey()) : usual;
    }

    private static Component[] legend(TargetKind kind) {
        String[] keys = switch (kind) {
            case REACTOR -> new String[]{"temperature", "power"};
            case TURBINE -> new String[]{"steam_use", "speed"};
            case COOLING_TOWER -> new String[]{"condensing", "water"};
            case TOKAMAK -> new String[]{"plasma", "power"};
            case STEAM_GENERATOR -> new String[]{"level", "steam_made"};
            case CONTAINMENT -> new String[]{"pressure", "hydrogen"};
            default -> new String[]{"exposure", "ground"};
        };
        return new Component[]{label(keys[0]), label(keys[1])};
    }

    private static MutableComponent label(String key) {
        return Component.translatable(KEY + key);
    }

    private static Component status(MonitorReading reading) {
        String status = reading.status();
        return status.startsWith("create_nuclearindustry.") ? Component.translatable(status) : Component.literal(status);
    }

    private static int statusColor(MonitorReading reading) {
        String status = reading.status();
        if (status.contains("SCRAM") || status.equals("MELTING") || status.equals("Critical") || status.endsWith(".radiation")
                || status.endsWith(".tripped")) {
            return BAD;
        }
        if (status.equals("Running") || status.endsWith(".running") || status.endsWith(".clear") || status.endsWith(".burning")) {
            return GOOD;
        }
        return WARN;
    }

    @Override
    public AABB getRenderBoundingBox(ControlMonitorBlockEntity monitor) {
        BlockPos pos = monitor.getBlockPos();
        Direction right = monitor.right();
        BlockPos far = pos.relative(right, monitor.screenWidth() - 1).above(monitor.screenHeight() - 1);
        return new AABB(pos).minmax(new AABB(far));
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
