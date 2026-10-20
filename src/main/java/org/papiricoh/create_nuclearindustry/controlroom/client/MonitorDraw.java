package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorPaging;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Drawing every {@link MonitorStyle} shares: text that fits its room, the graph's lines, what a screen says. */
final class MonitorDraw {
    static final String KEY = MonitorRow.KEY;

    private MonitorDraw() {}

    /** Text as shown in some room: shrunk to at most 60%, then cut short with "..". */
    record Fitted(Component text, float scale, float width) {}

    static Fitted fit(Font font, Component text, float room) {
        float textWidth = font.width(text);
        if (textWidth <= room) {
            return new Fitted(text, 1, textWidth);
        }
        float scale = Math.max(0.6f, room / textWidth);
        Component shown = text;
        if (textWidth * scale > room) {
            shown = Component.literal(font.plainSubstrByWidth(text.getString(), (int) (room / scale) - font.width("..")) + "..");
        }
        return new Fitted(shown, scale, font.width(shown) * scale);
    }

    /**
     * Text in at most {@code room} pixels from {@code x} (or ending at {@code x} when {@code right}):
     * shrunk down to 60% if needed, then cut short. Returns the width it took.
     */
    static float fitted(PoseStack ms, MultiBufferSource buffers, Font font, Component text, float x, float y, float room, int color,
                        boolean right) {
        if (room <= 4) {
            return 0;
        }
        Fitted fitted = fit(font, text, room);
        if (fitted.scale() == 1) {
            if (right) {
                ScreenDraw.textRight(ms, buffers, font, text, x, y, color);
            } else {
                ScreenDraw.text(ms, buffers, font, text, x, y, color);
            }
            return fitted.width();
        }
        ms.pushPose();
        ms.translate(x, y + (1 - fitted.scale()) * 4, 0);
        ms.scale(fitted.scale(), fitted.scale(), 1);
        if (right) {
            ScreenDraw.textRight(ms, buffers, font, fitted.text(), 0, 0, color);
        } else {
            ScreenDraw.text(ms, buffers, font, fitted.text(), 0, 0, color);
        }
        ms.popPose();
        return fitted.width();
    }

    /** Text centred on {@code center}, shrunk to fit {@code room} (never grown). */
    static void centred(PoseStack ms, MultiBufferSource buffers, Font font, Component text, float center, float y, float room, int color) {
        float scale = Math.min(1.0f, room / Math.max(1, font.width(text)));
        ms.pushPose();
        ms.translate(center, y + (1 - scale) * 4, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, text, 0, 0, color);
        ms.popPose();
    }

    /**
     * The graph's lines over a plot area, newest sample at the right edge, the third first so the
     * first lies on top. {@code colors} and {@code z} hold one entry per line; a line with no
     * colour (0) is not drawn, nor are the samples it has nothing for (NaN).
     */
    static void lines(PoseStack ms, MultiBufferSource buffers, ControlMonitorBlockEntity monitor, float x, float gy, float width, float gh,
                      int[] colors, float[] z, float thickness) {
        int size = monitor.historySize();
        float step = width / (ControlMonitorBlockEntity.HISTORY - 1);
        float start = x + width - (size - 1) * step;
        for (int line = 2; line >= 0; line--) {
            if (colors[line] == 0) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                float value = sample(monitor, line, i);
                float previous = i == 0 ? Float.NaN : sample(monitor, line, i - 1);
                plot(ms, buffers, start + i * step, step, gy, gh, value, previous, colors[line], z[line], thickness);
            }
        }
    }

    /** Line {@code line}'s sample {@code index}: NaN where it plots nothing. */
    static float sample(ControlMonitorBlockEntity monitor, int line, int index) {
        return switch (line) {
            case 0 -> monitor.historyA(index);
            case 1 -> monitor.historyB(index);
            default -> monitor.historyC(index);
        };
    }

    /** One step of a line: a short flat segment, joined to the previous value by a vertical one. */
    static void plot(PoseStack ms, MultiBufferSource buffers, float x, float step, float gy, float gh, float value, float previous,
                     int color, float z, float thickness) {
        if (Float.isNaN(value)) {
            return;
        }
        float y = gy + gh - Math.max(0, Math.min(1, value)) * (gh - 1);
        ScreenDraw.rect(ms, buffers, x - step, y - thickness, x, y, z, color);
        if (!Float.isNaN(previous)) {
            float py = gy + gh - Math.max(0, Math.min(1, previous)) * (gh - 1);
            float half = thickness / 2;
            ScreenDraw.rect(ms, buffers, x - step - half, Math.min(y, py) - thickness, x - step + half, Math.max(y, py), z, color);
        }
    }

    // ------------------------------------------------------------------ content

    static Component value(MonitorRow.Line line) {
        return line.translated() ? Component.translatable(line.value()) : Component.literal(line.value());
    }

    static Component status(MonitorReading reading) {
        String status = reading.status();
        return status.startsWith("create_nuclearindustry.") ? Component.translatable(status) : Component.literal(status);
    }

    /** The screen's own title, or what it watches. */
    static Component title(MonitorReading reading, MonitorConfig config) {
        return config.title().isEmpty() ? Component.translatable(reading.kind().translationKey()) : Component.literal(config.title());
    }

    /**
     * What the graph's lines plot: the series the screen was set up with or the target's usual pair,
     * then the third line if one is chosen for this kind of target.
     */
    static List<Component> legend(MonitorReading reading, MonitorConfig config) {
        Component[] usual = usual(reading.kind());
        List<Component> legend = new ArrayList<>(3);
        legend.add(series(config.seriesA(), reading.kind(), usual[0]));
        legend.add(series(config.seriesB(), reading.kind(), usual[1]));
        if (config.seriesC() != null && config.seriesC().kind() == reading.kind()) {
            legend.add(Component.translatable(config.seriesC().translationKey()));
        }
        return legend;
    }

    private static Component series(GaugeReading chosen, TargetKind kind, Component usual) {
        return chosen != null && chosen.kind() == kind ? Component.translatable(chosen.translationKey()) : usual;
    }

    private static Component[] usual(TargetKind kind) {
        String[] keys = switch (kind) {
            case REACTOR -> new String[]{"temperature", "power"};
            case TURBINE -> new String[]{"steam_use", "speed"};
            case COOLING_TOWER -> new String[]{"condensing", "water"};
            case TOKAMAK -> new String[]{"plasma", "power"};
            case STEAM_GENERATOR -> new String[]{"level", "steam_made"};
            case CONTAINMENT -> new String[]{"pressure", "hydrogen"};
            case ANTIMATTER_TRAP -> new String[]{"antimatter", "trap_buffer"};
            case ANNIHILATION_GENERATOR -> new String[]{"gen_output", "antimatter"};
            case THERMAL_ENGINE -> new String[]{"eng_thrust", "eng_nozzle"};
            case ANTIMATTER_ENGINE -> new String[]{"eng_thrust", "antimatter"};
            case RADAR -> new String[]{"radar_tracks", "radar_threats"};
            default -> new String[]{"exposure", "ground"};
        };
        return new Component[]{label(keys[0]), label(keys[1])};
    }

    static MutableComponent label(String key) {
        return Component.translatable(KEY + key);
    }

    /** A reading as the styled screens print it: its name in capitals and its value split from its unit. */
    record Reading(String label, String number, String unit, float gauge, MonitorRow.Tone tone) {}

    static List<Reading> readings(List<MonitorRow.Line> lines) {
        List<Reading> readings = new ArrayList<>(lines.size());
        for (MonitorRow.Line line : lines) {
            String value = line.translated() ? upper(Component.translatable(line.value())) : line.value();
            String[] parts = MonitorPaging.splitUnit(value);
            readings.add(new Reading(upper(Component.translatable(line.labelKey())), parts[0], parts[1], line.gauge(), line.tone()));
        }
        return readings;
    }

    static String upper(Component text) {
        return text.getString().toUpperCase(Locale.ROOT);
    }

    /** The short tag a plant computer files a machine under. */
    static String code(TargetKind kind) {
        return switch (kind) {
            case REACTOR -> "RX";
            case TURBINE -> "TG";
            case TOKAMAK -> "TK";
            case STEAM_GENERATOR -> "SG";
            case CONTAINMENT -> "CNT";
            case COOLING_TOWER -> "CT";
            case ANTIMATTER_TRAP -> "PT";
            case ANNIHILATION_GENERATOR -> "AG";
            case THERMAL_ENGINE -> "NTR";
            case ANTIMATTER_ENGINE -> "AMR";
            case RADAR -> "RDR";
            default -> "RAD";
        };
    }

    /** The colours of the lines a screen draws: none for a third line nobody chose. */
    static int[] lineColors(int[] palette, int count) {
        return new int[]{palette[0], palette[1], count > 2 ? palette[2] : 0};
    }
}
