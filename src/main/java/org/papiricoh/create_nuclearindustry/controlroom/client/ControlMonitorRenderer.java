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
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.*;

/**
 * Draws a whole control room screen from its origin monitor: a list of readings with gauges, a
 * two-minute history graph, or (on a single block) one big reading per page.
 */
public class ControlMonitorRenderer implements BlockEntityRenderer<ControlMonitorBlockEntity> {
    private static final float INSET = 1 / 16f;
    /** Size of one text pixel, in blocks. */
    private static final float PIXEL = 1 / 80f;
    private static final int LINE = 10;
    private static final int BACKDROP = 0xFF0B1511;
    private static final int TEXT = 0xFFB8F0C4;
    private static final int DIM = 0xFF6E8F7A;
    private static final int GOOD = 0xFF6FDF6E;
    private static final int WARN = 0xFFF0B040;
    private static final int BAD = 0xFFFF5A4A;
    private static final int SERIES_A = 0xFFFF7A50;
    private static final int SERIES_B = 0xFF58B8FF;
    private static final String KEY = "create_nuclearindustry.monitor.";

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
        List<Row> rows = rows(reading);
        boolean single = monitor.screenWidth() == 1 && monitor.screenHeight() == 1;
        if (single) {
            drawBig(ms, buffers, reading, rows.get(Math.floorMod(monitor.getPage(), rows.size())), width, height);
            return;
        }
        // title line: what it is and its state
        ScreenDraw.text(ms, buffers, font, Component.translatable(reading.kind().translationKey()), 3, 3, TEXT);
        Component status = status(reading);
        ScreenDraw.textRight(ms, buffers, font, status, width - 3, 3, statusColor(reading));
        ScreenDraw.rect(ms, buffers, 2, 13, width - 2, 14, -0.02f, 0xFF2C4A3A);
        float top = 17;
        if (monitor.getPage() == 1) {
            drawGraph(monitor, ms, buffers, reading, 3, top, width - 6, height - top - 3);
            return;
        }
        boolean split = width >= 160 && height >= 100;
        float listWidth = split ? width * 0.5f : width;
        drawRows(ms, buffers, rows, 3, top, listWidth - 6, height - top);
        if (split) {
            drawGraph(monitor, ms, buffers, reading, listWidth + 3, top, width - listWidth - 6, height - top - 3);
        }
    }

    private void drawRows(PoseStack ms, MultiBufferSource buffers, List<Row> rows, float x, float y, float width, float height) {
        int fit = Math.max(1, (int) (height / LINE));
        float barWidth = Math.min(46, width * 0.32f);
        for (int i = 0; i < Math.min(fit, rows.size()); i++) {
            Row row = rows.get(i);
            float lineY = y + i * LINE;
            ScreenDraw.text(ms, buffers, font, row.label(), x, lineY, DIM);
            float valueRight = row.gauge() >= 0 ? x + width - barWidth - 4 : x + width;
            ScreenDraw.textRight(ms, buffers, font, row.value(), valueRight, lineY, row.color());
            if (row.gauge() >= 0) {
                ScreenDraw.bar(ms, buffers, x + width - barWidth, lineY + 1, barWidth, 6, row.gauge());
            }
        }
    }

    /** One reading as large as fits on a single block. */
    private void drawBig(PoseStack ms, MultiBufferSource buffers, MonitorReading reading, Row row, float width, float height) {
        ScreenDraw.textCentered(ms, buffers, font, row.label(), width / 2, 4, DIM);
        Component value = row.value();
        float scale = Math.min(2.5f, (width - 6) / Math.max(1, font.width(value)));
        ms.pushPose();
        ms.translate(width / 2, height / 2 - 4 * scale, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, value, 0, 0, row.color());
        ms.popPose();
        if (row.gauge() >= 0) {
            ScreenDraw.bar(ms, buffers, 6, height - 22, width - 12, 6, row.gauge());
        }
        ScreenDraw.textCentered(ms, buffers, font, status(reading), width / 2, height - 12, statusColor(reading));
    }

    private void drawGraph(ControlMonitorBlockEntity monitor, PoseStack ms, MultiBufferSource buffers, MonitorReading reading,
                           float x, float y, float width, float height) {
        Component[] legend = legend(reading.kind());
        ScreenDraw.text(ms, buffers, font, legend[0], x, y, SERIES_A);
        ScreenDraw.textRight(ms, buffers, font, legend[1], x + width, y, SERIES_B);
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

    // ------------------------------------------------------------------ content

    private record Row(Component label, Component value, float gauge, int color) {}

    private static List<Row> rows(MonitorReading r) {
        List<Row> rows = new ArrayList<>();
        switch (r.kind()) {
            case REACTOR -> {
                float temperature = r.value(R_TEMPERATURE);
                rows.add(row("temperature", fmt("%.0f °C", temperature), temperature / 4000f,
                        temperature >= 3500 ? BAD : temperature >= 2000 ? WARN : TEXT));
                rows.add(row("power", fmt("%.0f MW", r.value(R_POWER)), r.value(R_POWER) / 1000f, TEXT));
                float target = r.value(R_ROD_TARGET);
                String rods = fmt("%.0f%%", r.value(R_RODS) * 100) + (target >= 0 ? fmt(" > %.0f%%", target * 100) : "");
                rows.add(row("rods", rods, r.value(R_RODS), TEXT));
                rows.add(row("xenon", fmt("-%.0f%%", r.value(R_XENON) * 100), r.value(R_XENON) / 0.5f,
                        r.value(R_XENON) >= 0.15f ? WARN : TEXT));
                rows.add(row("coolant", fmt("%.0f%%", r.value(R_COOLANT) * 100), r.value(R_COOLANT),
                        r.value(R_COOLANT) < 0.2f ? BAD : TEXT));
                rows.add(row("reactivity", fmt("x%.2f", r.value(R_REACTIVITY)), -1, TEXT));
                rows.add(row("decay_heat", fmt("%.0f MW", r.value(R_DECAY_HEAT)), -1, TEXT));
                rows.add(row("steam", fmt("%.0f%%", r.value(R_STEAM) * 100), r.value(R_STEAM), TEXT));
                rows.add(row("fuel", fmt("%.0f%%", r.value(R_FUEL) * 100), r.value(R_FUEL), TEXT));
                rows.add(row("neutrons", fmt("%.0f", r.value(R_NEUTRONS)), r.value(R_NEUTRONS) / 2000f, TEXT));
                rows.add(row("stress", fmt("%.0f%%", r.value(R_STRESS)), r.value(R_STRESS) / 100f,
                        r.value(R_STRESS) >= 65 ? BAD : TEXT));
            }
            case TURBINE -> {
                rows.add(row("speed", fmt("%.0f RPM", r.value(T_SPEED)), r.value(T_SPEED) / 256f, TEXT));
                rows.add(row("steam_use", fmt("%.0f/%.0f mB/t", r.value(T_USE), r.value(T_LIMIT)), r.seriesA(), TEXT));
                rows.add(row("capacity", fmt("%.0f su", r.value(T_CAPACITY)), -1, TEXT));
                rows.add(row("steam_tank", fmt("%.0f%%", r.value(T_STEAM) * 100), r.value(T_STEAM), TEXT));
                rows.add(new Row(label("exhaust"), Component.translatable(KEY + (r.flag(T_EXHAUST) ? "yes" : "no")), -1, TEXT));
            }
            case COOLING_TOWER -> {
                rows.add(row("condensing", fmt("%.0f/%.0f mB/t", r.value(C_USE), r.value(C_CAPACITY)), r.seriesA(), TEXT));
                rows.add(row("water", fmt("%.0f%%", r.value(C_WATER) * 100), r.value(C_WATER), TEXT));
                rows.add(row("steam_tank", fmt("%.0f%%", r.value(C_STEAM) * 100), r.value(C_STEAM),
                        r.value(C_STEAM) > 0.9f ? WARN : TEXT));
                rows.add(row("fans", fmt("%.0f", r.value(C_FANS)), -1, TEXT));
            }
            case TOKAMAK -> {
                float plasma = r.value(K_TEMPERATURE);
                rows.add(row("plasma", fmt("%.0f °", plasma), r.seriesA(),
                        plasma >= 5000 ? BAD : plasma >= 1500 ? GOOD : TEXT));
                rows.add(row("power", fmt("%.0f MW", r.value(K_POWER)), r.seriesB(), TEXT));
                rows.add(row("confinement", fmt("%.0f%%", r.value(K_CONFINEMENT) * 100), r.value(K_CONFINEMENT),
                        r.value(K_CONFINEMENT) < 0.65f ? WARN : TEXT));
                rows.add(row("instability", fmt("%.0f%%", r.value(K_INSTABILITY)), r.value(K_INSTABILITY) / 100f,
                        r.value(K_INSTABILITY) >= 60 ? BAD : TEXT));
                rows.add(row("deuterium", fmt("%.0f%%", r.value(K_DEUTERIUM) * 100), r.value(K_DEUTERIUM), TEXT));
                rows.add(row("tritium", fmt("%.0f%%", r.value(K_TRITIUM) * 100), r.value(K_TRITIUM), TEXT));
                rows.add(new Row(label("fuel"), Component.translatable(KEY + (r.flag(K_FUEL) ? "yes" : "no")), -1, TEXT));
            }
            case AREA -> {
                float exposure = r.value(A_EXPOSURE);
                rows.add(row("exposure", fmt("%.2f rad/s", exposure), r.seriesA(), exposure >= 1 ? BAD : exposure >= 0.05f ? WARN : GOOD));
                rows.add(row("ground", fmt("%.2f rad/s", r.value(A_GROUND)), r.seriesB(), r.value(A_GROUND) >= 0.05f ? WARN : GOOD));
            }
            default -> rows.add(new Row(Component.empty(), Component.empty(), -1, TEXT));
        }
        return rows;
    }

    private static Component[] legend(TargetKind kind) {
        String[] keys = switch (kind) {
            case REACTOR -> new String[]{"temperature", "power"};
            case TURBINE -> new String[]{"steam_use", "speed"};
            case COOLING_TOWER -> new String[]{"condensing", "water"};
            case TOKAMAK -> new String[]{"plasma", "power"};
            default -> new String[]{"exposure", "ground"};
        };
        return new Component[]{label(keys[0]), label(keys[1])};
    }

    private static Row row(String label, String value, float gauge, int color) {
        return new Row(label(label), Component.literal(value), gauge, color);
    }

    private static MutableComponent label(String key) {
        return Component.translatable(KEY + key);
    }

    private static String fmt(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    private static Component status(MonitorReading reading) {
        String status = reading.status();
        return status.startsWith("create_nuclearindustry.") ? Component.translatable(status) : Component.literal(status);
    }

    private static int statusColor(MonitorReading reading) {
        String status = reading.status();
        if (status.contains("SCRAM") || status.equals("MELTING") || status.equals("Critical") || status.endsWith(".radiation")) {
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
