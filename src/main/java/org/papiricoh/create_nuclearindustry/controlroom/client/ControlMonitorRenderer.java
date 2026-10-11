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
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

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
            // Room left of the label; the gauge goes first when it does not fit, then the value shrinks.
            float room = width - font.width(row.label()) - 6;
            float valueWidth = font.width(row.value());
            boolean bar = row.gauge() >= 0 && valueWidth + barWidth + 4 <= room;
            float valueRight = bar ? x + width - barWidth - 4 : x + width;
            float valueRoom = bar ? room - barWidth - 4 : room;
            if (valueWidth > valueRoom && valueRoom > 4) {
                float scale = valueRoom / valueWidth;
                ms.pushPose();
                ms.translate(valueRight, lineY + (1 - scale) * 4, 0);
                ms.scale(scale, scale, 1);
                ScreenDraw.textRight(ms, buffers, font, row.value(), 0, 0, row.color());
                ms.popPose();
            } else {
                ScreenDraw.textRight(ms, buffers, font, row.value(), valueRight, lineY, row.color());
            }
            if (bar) {
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
                float rated = Math.max(1f, r.value(R_RATED_POWER));
                rows.add(row("power", fmt("%.0f MW", r.value(R_POWER)), r.value(R_POWER) / rated, r.value(R_POWER) > 1.05f * rated ? WARN : TEXT));
                if (r.value(R_CORE_DAMAGE) > 0) {
                    rows.add(row("damage", fmt("%.0f%%", r.value(R_CORE_DAMAGE) * 100), r.value(R_CORE_DAMAGE), BAD));
                }
                if (r.flag(R_PWR)) {
                    // A pressurized loop puts its own readings first: they are the ones that go wrong.
                    pressurizedRows(r, rows);
                }
                float target = r.value(R_ROD_TARGET);
                String rods = fmt("%.0f%%", r.value(R_RODS) * 100) + (target >= 0 ? fmt(" > %.0f%%", target * 100) : "");
                rows.add(row("rods", rods, r.value(R_RODS), TEXT));
                if (r.flag(R_PWR)) {
                    plantRows(r, rows);
                }
                rows.add(row("xenon", fmt("-%.0f%%", r.value(R_XENON) * 100), r.value(R_XENON) / 0.5f,
                        r.value(R_XENON) >= 0.15f ? WARN : TEXT));
                rows.add(row("coolant", fmt("%.0f%%", r.value(R_COOLANT) * 100), r.value(R_COOLANT),
                        r.value(R_COOLANT) < 0.2f ? BAD : TEXT));
                rows.add(row("reactivity", fmt("%+.0f pcm", r.value(R_NET_REACTIVITY)), -1, TEXT));
                rows.add(row("coolant_temperature", fmt("%.0f °C", r.value(R_COOLANT_TEMP)), r.value(R_COOLANT_TEMP) / 4000f, TEXT));
                rows.add(row("decay_heat", fmt("%.0f MW", r.value(R_DECAY_HEAT)), -1, TEXT));
                rows.add(row("steam", fmt("%.0f%%", r.value(R_STEAM) * 100), r.value(R_STEAM), TEXT));
                rows.add(row("fuel", fmt("%.0f%%", r.value(R_FUEL) * 100), r.value(R_FUEL), TEXT));
                rows.add(row("neutrons", fmt("%.0f", r.value(R_NEUTRONS)), r.value(R_NEUTRONS) / 1000f, TEXT));
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
            case TOKAMAK -> tokamakRows(r, rows);
            case AREA -> {
                float exposure = r.value(A_EXPOSURE);
                rows.add(row("exposure", fmt("%.2f rad/s", exposure), r.seriesA(), exposure >= 1 ? BAD : exposure >= 0.05f ? WARN : GOOD));
                rows.add(row("ground", fmt("%.2f rad/s", r.value(A_GROUND)), r.seriesB(), r.value(A_GROUND) >= 0.05f ? WARN : GOOD));
            }
            case STEAM_GENERATOR -> {
                float level = r.value(S_LEVEL);
                rows.add(row("level", fmt("%.0f%%", level * 100), level, level < 0.2f ? BAD : level < 0.35f ? WARN : TEXT));
                rows.add(row("steam_made", fmt("%.0f mB/t", r.value(S_STEAM)), r.value(S_STEAM) / SG_STEAM_MAX, TEXT));
                rows.add(row("heat", fmt("%.0f MW", r.value(S_HEAT)), r.value(S_HEAT) / SG_HEAT_MAX, TEXT));
                rows.add(row("primary_dt", fmt("%.0f °C", r.value(S_PRIMARY_DT)), r.value(S_PRIMARY_DT) / SG_DT_MAX, TEXT));
                rows.add(new Row(label("formed"), Component.translatable(KEY + (r.flag(S_FORMED) ? "yes" : "no")), -1,
                        r.flag(S_FORMED) ? GOOD : BAD));
            }
            case CONTAINMENT -> {
                float pressure = r.value(N_PRESSURE);
                rows.add(row("pressure", fmt("%.2f bar", pressure), pressure / CONTAINMENT_PRESSURE_MAX,
                        pressure >= 5 ? BAD : pressure >= 2 ? WARN : TEXT));
                float hydrogen = r.value(N_H2);
                rows.add(row("hydrogen", fmt("%.1f%%", hydrogen * 100), hydrogen / HYDROGEN_MAX,
                        hydrogen >= 0.04f ? BAD : hydrogen >= 0.02f ? WARN : TEXT));
                rows.add(new Row(label("sealed"), Component.translatable(KEY + (r.flag(N_SEALED) ? "yes" : "no")), -1,
                        r.flag(N_SEALED) ? GOOD : BAD));
                rows.add(new Row(label("vent"), Component.translatable(KEY + (r.flag(N_VENT) ? "open" : "closed")), -1,
                        r.flag(N_VENT) ? WARN : TEXT));
                rows.add(row("activity", fmt("%.2f", r.value(N_ACTIVITY)), activityFraction(r.value(N_ACTIVITY)),
                        r.value(N_ACTIVITY) > 0 ? WARN : TEXT));
                rows.add(row("volume", fmt("%.0f m³", r.value(N_VOLUME)), -1, TEXT));
            }
            default -> rows.add(new Row(Component.empty(), Component.empty(), -1, TEXT));
        }
        return rows;
    }

    /**
     * A tokamak's readings, most watched first so a small screen keeps them: why it tripped, the plasma
     * and what it gives, then what holds it (field, magnets, vacuum) and what feeds it.
     */
    private static void tokamakRows(MonitorReading r, List<Row> rows) {
        TokamakPhase phase = TokamakScales.phase(r.value(K_PHASE));
        if (r.flag(K_TRIPPED)) {
            rows.add(new Row(label("trip"), Component.translatable(TokamakScales.trip(r.value(K_TRIP)).langKey()), -1, BAD));
        }
        float plasma = r.value(K_TEMPERATURE);
        rows.add(row("plasma", fmt("%.0f °", plasma), r.seriesA(),
                plasma >= TokamakScales.DANGER_TEMPERATURE ? BAD : plasma >= TokamakScales.IGNITION_TEMPERATURE ? GOOD : TEXT));
        rows.add(row("power", fmt("%.0f MW", r.value(K_POWER)), r.seriesB(), TEXT));
        float gain = r.value(K_GAIN);
        rows.add(row("gain", TokamakScales.formatGain(gain), gain / TokamakScales.GAIN_MAX,
                phase != TokamakPhase.BURN ? TEXT : TokamakScales.ignited(gain) ? GOOD : gain < TokamakScales.NET_GAIN ? WARN : TEXT));
        float density = r.value(K_DENSITY);
        rows.add(row("density", fmt("%.0f%%", density * 100), density / TokamakScales.DENSITY_MAX,
                density >= 1.0f ? BAD : density >= 0.9f ? WARN : TEXT));
        float instability = r.value(K_INSTABILITY);
        rows.add(row("instability", fmt("%.0f%%", instability), instability / TokamakScales.DISRUPTION_INSTABILITY,
                instability >= TokamakScales.INSTABILITY_WARNING ? BAD : TEXT));
        float field = r.value(K_FIELD);
        rows.add(row("field", fmt("%.0f%%", field * 100), field, TEXT));
        float magnets = r.value(K_MAGNET_TEMPERATURE);
        boolean superconducting = magnets <= TokamakScales.SUPERCONDUCTING_LIMIT;
        rows.add(row("magnets", fmt(magnets < 100 ? "%.1f K" : "%.0f K", magnets), magnets / TokamakScales.MAGNET_TEMPERATURE_MAX,
                superconducting ? GOOD : field > 0.01f ? BAD : TEXT));
        float vacuum = r.value(K_VACUUM);
        rows.add(row("vacuum", TokamakScales.formatPressure(vacuum),
                (vacuum - TokamakScales.VACUUM_MIN) / (TokamakScales.VACUUM_MAX - TokamakScales.VACUUM_MIN),
                vacuum <= org.papiricoh.create_nuclearindustry.fusion.FusionConfig.vacuumLimit ? GOOD : phase.hasPlasma() ? BAD : TEXT));
        float heating = r.value(K_HEATING);
        rows.add(r.flag(K_HEATING_ON)
                ? row("heating", fmt("%.1f MW", heating), heating / TokamakScales.HEATING_MAX, TEXT)
                : new Row(label("heating"), Component.translatable(KEY + "off"), -1, DIM));
        float maxFuel = r.value(K_FUEL_RATE_MAX);
        rows.add(row("fuel_rate", fmt("%.2f mB/t", r.value(K_FUEL_RATE)), maxFuel > 0 ? r.value(K_FUEL_RATE) / maxFuel : 0, TEXT));
        float helium = r.value(K_LIQUID_HELIUM);
        rows.add(row("helium", fmt("%.0f%%", helium * 100), helium, helium < 0.1f ? WARN : TEXT));
        rows.add(row("confinement", fmt("%.0f%%", r.value(K_CONFINEMENT) * 100), r.value(K_CONFINEMENT),
                r.value(K_CONFINEMENT) < TokamakScales.MIN_BURN_CONFINEMENT ? WARN : TEXT));
        float ash = r.value(K_ASH);
        rows.add(row("ash", fmt("%.1f%%", ash * 100), ash / TokamakScales.ASH_MAX, ash >= 0.1f ? WARN : TEXT));
        rows.add(row("deuterium", fmt("%.0f%%", r.value(K_DEUTERIUM) * 100), r.value(K_DEUTERIUM), TEXT));
        rows.add(row("tritium", fmt("%.0f%%", r.value(K_TRITIUM) * 100), r.value(K_TRITIUM), TEXT));
    }

    /** Primary loop readings that need watching most: pressure, subcooling, pressurizer and flow. */
    private static void pressurizedRows(MonitorReading r, List<Row> rows) {
        float pressure = r.value(R_PRESSURE);
        rows.add(row("pressure", fmt("%.0f bar", pressure), pressure / PRESSURE_MAX,
                pressure >= 170 ? BAD : pressure < 100 ? WARN : TEXT));
        float subcooling = r.value(R_SUBCOOLING);
        rows.add(row("subcooling", fmt("%.1f °C", subcooling), subcooling / SUBCOOLING_MAX,
                subcooling < 10 ? BAD : subcooling < 20 ? WARN : GOOD));
        float level = r.value(R_PZR_LEVEL);
        rows.add(row("pzr_level", fmt("%.0f%%", level * 100), level, level < 0.2f || level > 0.9f ? WARN : TEXT));
        float flow = r.value(R_FLOW);
        rows.add(row("flow", fmt("%.0f mB/t", flow), flow / FLOW_MAX, flow <= 0 ? BAD : TEXT));
    }

    /** The rest of the plant around a pressurized reactor: legs, containment and the core's water. */
    private static void plantRows(MonitorReading r, List<Row> rows) {
        rows.add(row("legs", fmt("%.0f/%.0f °C", r.value(R_T_HOT), r.value(R_T_COLD)), r.value(R_T_HOT) / LEG_TEMPERATURE_MAX, TEXT));
        boolean relief = r.flag(R_RELIEF);
        rows.add(new Row(label("relief"), Component.translatable(KEY + (relief ? "open" : "closed")), -1, relief ? BAD : TEXT));
        float containment = r.value(R_CONT_PRESSURE);
        rows.add(row("containment", fmt("%.2f bar", containment), containment / CONTAINMENT_PRESSURE_MAX,
                containment >= 5 ? BAD : containment >= 2 ? WARN : TEXT));
        float hydrogen = r.value(R_H2);
        rows.add(row("hydrogen", fmt("%.1f%%", hydrogen * 100), hydrogen / HYDROGEN_MAX,
                hydrogen >= 0.04f ? BAD : hydrogen >= 0.02f ? WARN : TEXT));
        float vessel = r.value(R_VESSEL_LEVEL);
        rows.add(row("vessel", fmt("%.0f%%", vessel * 100), vessel, vessel < 0.9f ? BAD : TEXT));
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
