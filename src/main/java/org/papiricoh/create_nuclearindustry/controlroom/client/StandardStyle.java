package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorLayout;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorStatus;

import java.util.List;

/**
 * The screen as it always looked: dark green glass, a title line with the state on the right, a
 * list of readings with gauges and a plain graph.
 */
final class StandardStyle implements MonitorStyle {
    static final StandardStyle INSTANCE = new StandardStyle();
    private static final int LINE = 10;
    /** Below the title line. */
    private static final float TOP = 17;
    private static final float[] LINE_Z = {-0.014f, -0.015f, -0.016f};

    private StandardStyle() {}

    @Override
    public void draw(MonitorFrame f) {
        float width = f.width();
        float height = f.height();
        MonitorLayout layout = f.config().layout();
        if (f.single()) {
            if (layout == MonitorLayout.CHART || layout == MonitorLayout.BOTH && f.page() == f.pages() - 1) {
                graph(f, 3, 3, width - 6, height - 6);
            } else if (f.lines().isEmpty()) {
                ScreenDraw.textCentered(f.ms(), f.buffers(), f.font(), MonitorDraw.label("no_rows"), width / 2, height / 2 - 4,
                        f.palette().dim());
            } else {
                big(f, f.lines().get(Math.floorMod(f.page(), f.lines().size())));
            }
            return;
        }
        title(f);
        float bodyHeight = height - TOP;
        switch (layout) {
            case LIST -> rows(f, 3, TOP, width - 6, bodyHeight);
            case CHART -> graph(f, 3, TOP, width - 6, bodyHeight - 3);
            case BOTH -> {
                if (width >= height) {
                    float listWidth = width * 0.5f;
                    rows(f, 3, TOP, listWidth - 6, bodyHeight);
                    graph(f, listWidth + 3, TOP, width - listWidth - 6, bodyHeight - 3);
                } else {
                    // Taller than wide: readings on top, graph below.
                    float listHeight = (float) Math.floor(bodyHeight * 0.5f / LINE) * LINE;
                    rows(f, 3, TOP, width - 6, listHeight);
                    graph(f, 3, TOP + listHeight + 2, width - 6, bodyHeight - listHeight - 5);
                }
            }
            default -> {
                if (f.page() == 1) {
                    graph(f, 3, TOP, width - 6, bodyHeight - 3);
                    return;
                }
                boolean split = width >= 160 && height >= 100;
                float listWidth = split ? width * 0.5f : width;
                rows(f, 3, TOP, listWidth - 6, bodyHeight);
                if (split) {
                    graph(f, listWidth + 3, TOP, width - listWidth - 6, bodyHeight - 3);
                }
            }
        }
    }

    /** The title line: the screen's own title or what it watches, and the target's state on the right. */
    private static void title(MonitorFrame f) {
        Font font = f.font();
        Component status = MonitorDraw.status(f.reading());
        ScreenDraw.textRight(f.ms(), f.buffers(), font, status, f.width() - 3, 3, f.palette().status(MonitorStatus.tone(f.reading().status())));
        // A long title shrinks, and is cut short if it still runs into the state.
        float room = f.width() - 6 - font.width(status) - 6;
        MonitorDraw.fitted(f.ms(), f.buffers(), font, MonitorDraw.title(f.reading(), f.config()), 3, 3, room, f.palette().titleText(), false);
        ScreenDraw.rect(f.ms(), f.buffers(), 2, 13, f.width() - 2, 14, -0.02f, f.palette().titleRule());
    }

    private static void rows(MonitorFrame f, float x, float y, float width, float height) {
        PoseStack ms = f.ms();
        MultiBufferSource buffers = f.buffers();
        Font font = f.font();
        List<MonitorRow.Line> lines = f.lines();
        if (lines.isEmpty()) {
            MonitorDraw.fitted(ms, buffers, font, MonitorDraw.label("no_rows"), x, y, width, f.palette().dim(), false);
            return;
        }
        int fit = Math.max(1, (int) (height / LINE));
        float barWidth = Math.min(46, width * 0.32f);
        for (int i = 0; i < Math.min(fit, lines.size()); i++) {
            MonitorRow.Line line = lines.get(i);
            Component label = Component.translatable(line.labelKey());
            Component value = MonitorDraw.value(line);
            int color = f.palette().tone(line.tone());
            float lineY = y + i * LINE;
            ScreenDraw.text(ms, buffers, font, label, x, lineY, f.palette().label());
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
    private static void big(MonitorFrame f, MonitorRow.Line line) {
        PoseStack ms = f.ms();
        MultiBufferSource buffers = f.buffers();
        Font font = f.font();
        float width = f.width();
        float height = f.height();
        ScreenDraw.textCentered(ms, buffers, font, Component.translatable(line.labelKey()), width / 2, 4, f.palette().label());
        Component value = MonitorDraw.value(line);
        float scale = Math.min(2.5f, (width - 6) / Math.max(1, font.width(value)));
        ms.pushPose();
        ms.translate(width / 2, height / 2 - 4 * scale, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, value, 0, 0, f.palette().tone(line.tone()));
        ms.popPose();
        if (line.gauge() >= 0) {
            ScreenDraw.bar(ms, buffers, 6, height - 22, width - 12, 6, line.gauge());
        }
        // A long status ("Cooling magnets") shrinks to fit the block.
        MonitorDraw.centred(ms, buffers, font, MonitorDraw.status(f.reading()), width / 2, height - 12, width - 4,
                f.palette().status(MonitorStatus.tone(f.reading().status())));
    }

    private static void graph(MonitorFrame f, float x, float y, float width, float height) {
        PoseStack ms = f.ms();
        MultiBufferSource buffers = f.buffers();
        List<Component> legend = MonitorDraw.legend(f.reading(), f.config());
        GraphLegend.Layout layout = GraphLegend.layout(x, width, legend.size());
        for (int i = 0; i < legend.size(); i++) {
            GraphLegend.Slot slot = layout.slots()[i];
            MonitorDraw.fitted(ms, buffers, f.font(), legend.get(i), slot.x(), y + slot.row() * GraphLegend.LINE, slot.room(),
                    f.palette().legend(i), slot.right());
        }
        float gy = y + layout.height();
        float gh = height - layout.height();
        if (gh < 8 || width < 20) {
            return;
        }
        ScreenDraw.rect(ms, buffers, x, gy, x + width, gy + gh, -0.03f, f.palette().graphBackground());
        for (int i = 1; i < 4; i++) {
            float lineY = gy + gh * i / 4f;
            ScreenDraw.rect(ms, buffers, x, lineY, x + width, lineY + 0.5f, -0.025f, f.palette().grid());
        }
        MonitorDraw.lines(ms, buffers, f.monitor(), x, gy, width, gh, MonitorDraw.lineColors(f.palette().series(), legend.size()),
                LINE_Z, 1);
    }
}
