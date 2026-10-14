package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.papiricoh.create_nuclearindustry.controlroom.client.ScreenDraw;
import org.papiricoh.create_nuclearindustry.controlroom.desk.BarGraph;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.CoreMap;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.PlasmaSection;
import org.papiricoh.create_nuclearindustry.controlroom.desk.TrendRing;

/**
 * Draws the pictures of the screen modules on their glass, full bright: a bar graph's segments, a trend
 * recorder's trace, a core map and a plasma section. A screen is drawn from its slot, or for a merged
 * screen from slot 0 over the whole block. Rectangles go in one pass and text (the core map's north
 * mark) in a later one, since the font closes the buffer the rectangles draw into.
 */
final class ModuleScreenDraw {
    private static final float PX = 1 / 16f;
    /** Just over the housings' glass. */
    private static final float FACE_Y = 1.2f + 0.02f;
    private static final float GLYPH = 8;

    private static final int BACKGROUND = 0xFF080B0D;
    private static final int BEZEL = 0xFF2A3236;

    private ModuleScreenDraw() {}

    /** Where a screen's window is on its slot (or block), in px. */
    record Window(float centreX, float centreZ, float width, float height) {
        /** One module's window: a bar graph's housing is 6 px wide, the others 8, all 7 deep. */
        static Window single(ModuleType type) {
            return new Window(0, 0, type == ModuleType.BAR_GRAPH ? 5 : 7, 6);
        }

        /**
         * A merged screen over the four slots of a block, from slot 0: the block's centre is 4 px to the
         * operator's right and half a row further away.
         */
        static Window merged(float rowPitch) {
            return new Window(-4, rowPitch / 2, 15, rowPitch + 6);
        }
    }

    /**
     * From a slot (origin at its centre on the surface, x to the operator's left, y out, z away from them)
     * to a window as read: origin at its top-left corner, x right, y down, in px.
     */
    private static void toWindow(PoseStack ms, Window window) {
        ms.translate((window.centreX() + window.width() / 2) * PX, FACE_Y * PX, (window.centreZ() + window.height() / 2) * PX);
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        ms.scale(PX, -PX, PX);
    }

    /** The picture of a screen module; {@code time} in ticks for what moves. */
    static void picture(ControlModule module, Window window, boolean merged, float time, PoseStack ms, MultiBufferSource buffers) {
        ms.pushPose();
        toWindow(ms, window);
        float w = window.width();
        float h = window.height();
        if (merged) {
            // One glass over the four housings and the gaps between them.
            ScreenDraw.rect(ms, buffers, -0.5f, -0.5f, w + 0.5f, h + 0.5f, -0.01f, BEZEL);
            ScreenDraw.rect(ms, buffers, 0, 0, w, h, 0, BACKGROUND);
        } else if (module.active()) {
            ScreenDraw.rect(ms, buffers, 0, 0, w, h, 0, BACKGROUND);
        }
        switch (module.type()) {
            case BAR_GRAPH -> barGraph(module, w, h, ms, buffers);
            case TREND_RECORDER -> trend(module, w, h, ms, buffers);
            case CORE_MAP -> coreMap(module.display(), w, h, ms, buffers);
            case PLASMA_SECTION -> plasma(module.display(), w, h, merged, time, ms, buffers);
            default -> {
            }
        }
        ms.popPose();
    }

    /** Text over a screen's picture: the core map's north mark. */
    static void text(ControlModule module, Window window, Font font, PoseStack ms, MultiBufferSource buffers) {
        if (module.type() != ModuleType.CORE_MAP || CoreMap.side(module.display()) == 0) {
            return;
        }
        ms.pushPose();
        toWindow(ms, window);
        float size = Math.min(2.0f, window.height() / 5);
        float scale = size / GLYPH;
        ms.translate(0.35f, 0.3f, 0.03f);
        ms.scale(scale, scale, scale);
        font.drawInBatch(Component.literal("N"), 0, 0, 0xFF9FB4C8, false, ms.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        ms.popPose();
    }

    // ------------------------------------------------------------------ bar graph

    private static void barGraph(ControlModule module, float w, float h, PoseStack ms, MultiBufferSource buffers) {
        float top = 0.4f;
        float bottom = h - 0.4f;
        float pitch = (bottom - top) / BarGraph.SEGMENTS;
        int lit = module.active() ? BarGraph.lit(module.shown()) : 0;
        for (int i = 0; i < BarGraph.SEGMENTS; i++) {
            float y1 = bottom - i * pitch;
            BarGraph.Zone zone = BarGraph.zone(i, module.setting());
            boolean on = i < lit;
            ScreenDraw.rect(ms, buffers, 2.0f, y1 - pitch * 0.72f, 3.6f, y1, on ? 0.02f : 0.01f, on ? litColor(zone) : ghostColor(zone));
        }
        for (int q = 0; q <= 4; q++) {
            float y = bottom - q / 4.0f * (bottom - top);
            ScreenDraw.rect(ms, buffers, 0.6f, y - 0.06f, 1.4f, y + 0.06f, 0.01f, 0xFF3A4A3E);
        }
        float alarm = bottom - module.setting() / 100.0f * (bottom - top);
        ScreenDraw.rect(ms, buffers, 0.3f, alarm - 0.08f, 1.8f, alarm + 0.08f, 0.02f, module.active() ? 0xFFFF4A3A : 0xFF5A2A22);
    }

    private static int litColor(BarGraph.Zone zone) {
        return switch (zone) {
            case GREEN -> 0xFF4CE05A;
            case AMBER -> 0xFFFFB13A;
            case RED -> 0xFFFF4A3A;
        };
    }

    private static int ghostColor(BarGraph.Zone zone) {
        return switch (zone) {
            case GREEN -> 0xFF132617;
            case AMBER -> 0xFF2A2210;
            case RED -> 0xFF2B1712;
        };
    }

    // ------------------------------------------------------------------ trend recorder

    private static void trend(ControlModule module, float w, float h, PoseStack ms, MultiBufferSource buffers) {
        float left = 0.4f;
        float right = w - 0.4f;
        float top = 0.4f;
        float bottom = h - 0.4f;
        int grid = 0xFF12301A;
        for (int q = 1; q < 4; q++) {
            float y = bottom - q / 4.0f * (bottom - top);
            ScreenDraw.rect(ms, buffers, left, y - 0.03f, right, y + 0.03f, 0.005f, grid);
        }
        float step = (right - left) / (TrendRing.CAPACITY - 1);
        for (int i = 0; i < TrendRing.CAPACITY; i += 12) {
            float x = right - i * step;
            ScreenDraw.rect(ms, buffers, x - 0.03f, top, x + 0.03f, bottom, 0.005f, grid);
        }
        byte[] samples = module.display();
        int n = samples.length;
        float previousX = 0;
        float previousY = 0;
        boolean previous = false;
        for (int i = 0; i < n; i++) {
            float value = TrendRing.value(samples[i]);
            if (Float.isNaN(value)) {
                previous = false;
                continue;
            }
            float x = right - (n - 1 - i) * step;
            float y = bottom - value * (bottom - top);
            if (previous) {
                line(ms, buffers, previousX, previousY, x, y, 0.18f, 0.02f, 0xFF6CFF8A);
            }
            previous = true;
            previousX = x;
            previousY = y;
        }
        if (previous && n > 0 && !Float.isNaN(TrendRing.value(samples[n - 1]))) {
            ScreenDraw.rect(ms, buffers, previousX - 0.22f, previousY - 0.22f, previousX + 0.22f, previousY + 0.22f, 0.03f, 0xFFCFFFD8);
        }
    }

    // ------------------------------------------------------------------ core map

    private static void coreMap(byte[] map, float w, float h, PoseStack ms, MultiBufferSource buffers) {
        int side = CoreMap.side(map);
        if (side == 0) {
            return;
        }
        float usable = Math.min(w, h) - 1.0f;
        float cell = usable / side;
        float gap = Math.max(0.08f, cell * 0.12f);
        float x0 = (w - side * cell) / 2 + 0.3f;
        float y0 = (h - side * cell) / 2;
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                float l = x0 + column * cell + gap / 2;
                float t = y0 + row * cell + gap / 2;
                float r = l + cell - gap;
                float b = t + cell - gap;
                CoreMap.Kind kind = CoreMap.kind(map, column, row);
                float value = CoreMap.value(map, column, row);
                if (kind == CoreMap.Kind.CONTROL) {
                    ScreenDraw.rect(ms, buffers, l, t, r, b, 0.01f, 0xFF14243A);
                    if (!Float.isNaN(value) && value > 0) {
                        // The rods come in from the top.
                        ScreenDraw.rect(ms, buffers, l, t, r, t + (b - t) * value, 0.02f, 0xFF5AA0FF);
                    }
                } else {
                    ScreenDraw.rect(ms, buffers, l, t, r, b, 0.01f, cellColor(kind, value));
                }
            }
        }
    }

    private static int cellColor(CoreMap.Kind kind, float value) {
        return switch (kind) {
            case FUEL -> Float.isNaN(value) ? 0xFF2A332D
                    : value < 0.5f ? PlasmaSection.mix(0xFF4CD06A, 0xFFE8B040, value * 2)
                    : PlasmaSection.mix(0xFFE8B040, 0xFFB04030, (value - 0.5f) * 2);
            case MODERATOR -> 0xFF4A4F55;
            case REFLECTOR -> 0xFF8FA3AE;
            case EXCHANGER -> 0xFF2F6F8F;
            case CONTROL -> 0xFF14243A;
            case EMPTY -> 0xFF0E1214;
        };
    }

    // ------------------------------------------------------------------ plasma section

    private static void plasma(byte[] data, float w, float h, boolean merged, float time, PoseStack ms, MultiBufferSource buffers) {
        PlasmaSection.Look look = PlasmaSection.decode(data);
        if (look == null) {
            return;
        }
        float cx = w / 2;
        float cy = h / 2;
        float ax = w * 0.3f;
        float ay = h * 0.4f;
        int strips = merged ? 28 : 14;
        float wall = merged ? 0.35f : 0.22f;
        // The vessel: an upright oval wall.
        ellipseRing(ms, buffers, cx, cy, ax, ay, wall, strips, 0.01f, 0xFF56636C);
        // Coils round it, lit while they carry current.
        int coil = look.coils() ? 0xFF3FC8FF : 0xFF243038;
        float c = merged ? 0.9f : 0.5f;
        for (float[] at : new float[][]{{cx - ax - 1.4f * c, cy}, {cx + ax + 1.4f * c, cy}, {cx, cy - ay - 1.2f * c},
                {cx, cy + ay + 1.2f * c}, {cx - ax - c, cy - ay}, {cx + ax + c, cy - ay}, {cx - ax - c, cy + ay}, {cx + ax + c, cy + ay}}) {
            ScreenDraw.rect(ms, buffers, at[0] - c / 2, at[1] - c / 2, at[0] + c / 2, at[1] + c / 2, 0.01f, coil);
        }
        if (look.plasma()) {
            float px = ax * 0.72f * look.radius();
            float py = ay * 0.92f * look.radius();
            for (int line = 0; line < look.fieldLines(); line++) {
                float grow = 1.12f + 0.11f * line;
                dottedEllipse(ms, buffers, cx, cy, Math.min(ax * 0.95f, px * grow), Math.min(ay * 0.95f, py * grow),
                        merged ? 0.16f : 0.1f, merged ? 28 : 18, time * 0.05f * (line % 2 == 0 ? 1 : -1), 0xFF2E8FB0);
            }
            int halo = PlasmaSection.mix(BACKGROUND, look.color(), look.brightness() * 0.35f);
            int core = PlasmaSection.mix(BACKGROUND, look.color(), look.brightness());
            int hot = PlasmaSection.mix(core, 0xFFFFFFFF, 0.5f * look.brightness());
            filledEllipse(ms, buffers, cx, cy, px * 1.25f, py * 1.18f, strips, look.wobble(), time, 0.015f, halo);
            filledEllipse(ms, buffers, cx, cy, px, py, strips, look.wobble(), time, 0.02f, core);
            filledEllipse(ms, buffers, cx, cy, px * 0.45f, py * 0.45f, strips / 2, look.wobble(), time, 0.025f, hot);
        }
        if (look.tripped()) {
            float t = merged ? 0.35f : 0.2f;
            ScreenDraw.rect(ms, buffers, 0, 0, w, t, 0.03f, 0xFFFF4A3A);
            ScreenDraw.rect(ms, buffers, 0, h - t, w, h, 0.03f, 0xFFFF4A3A);
            ScreenDraw.rect(ms, buffers, 0, 0, t, h, 0.03f, 0xFFFF4A3A);
            ScreenDraw.rect(ms, buffers, w - t, 0, w, h, 0.03f, 0xFFFF4A3A);
        }
    }

    /** An ellipse filled with horizontal strips, each shaken sideways by {@code wobble} of its half-width. */
    private static void filledEllipse(PoseStack ms, MultiBufferSource buffers, float cx, float cy, float rx, float ry, int strips,
                                      float wobble, float time, float z, int color) {
        float height = 2 * ry / strips;
        for (int k = 0; k < strips; k++) {
            float y0 = cy - ry + k * height;
            float mid = (y0 + height / 2 - cy) / ry;
            float half = rx * (float) Math.sqrt(Math.max(0, 1 - mid * mid));
            float shift = wobble * rx * (float) Math.sin(time * 0.9f + k * 1.3f);
            ScreenDraw.rect(ms, buffers, cx - half + shift, y0, cx + half + shift, y0 + height + 0.01f, z, color);
        }
    }

    /** The outline of an ellipse, {@code thickness} wide, as strips of its left and right edges plus its top and bottom. */
    private static void ellipseRing(PoseStack ms, MultiBufferSource buffers, float cx, float cy, float rx, float ry, float thickness,
                                    int strips, float z, int color) {
        float height = 2 * ry / strips;
        for (int k = 0; k < strips; k++) {
            float y0 = cy - ry + k * height;
            float mid = (y0 + height / 2 - cy) / ry;
            float half = rx * (float) Math.sqrt(Math.max(0, 1 - mid * mid));
            float inner = Math.max(0, half - thickness * (1 + 2 * Math.abs(mid) * Math.abs(mid)));
            ScreenDraw.rect(ms, buffers, cx - half, y0, cx - inner, y0 + height + 0.01f, z, color);
            ScreenDraw.rect(ms, buffers, cx + inner, y0, cx + half, y0 + height + 0.01f, z, color);
        }
        ScreenDraw.rect(ms, buffers, cx - rx * 0.4f, cy - ry - thickness / 2, cx + rx * 0.4f, cy - ry + thickness / 2, z, color);
        ScreenDraw.rect(ms, buffers, cx - rx * 0.4f, cy + ry - thickness / 2, cx + rx * 0.4f, cy + ry + thickness / 2, z, color);
    }

    /** Dots round an ellipse, turning with {@code phase}: a field line. */
    private static void dottedEllipse(PoseStack ms, MultiBufferSource buffers, float cx, float cy, float rx, float ry, float dot,
                                      int dots, float phase, int color) {
        for (int i = 0; i < dots; i++) {
            double angle = phase + i * 2 * Math.PI / dots;
            float x = cx + rx * (float) Math.cos(angle);
            float y = cy + ry * (float) Math.sin(angle);
            ScreenDraw.rect(ms, buffers, x - dot / 2, y - dot / 2, x + dot / 2, y + dot / 2, 0.012f, color);
        }
    }

    /** A straight line {@code thickness} wide, as a quad drawn with both windings. */
    private static void line(PoseStack ms, MultiBufferSource buffers, float x0, float y0, float x1, float y1, float thickness, float z,
                             int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0e-4f) {
            return;
        }
        float nx = -dy / length * thickness / 2;
        float ny = dx / length * thickness / 2;
        VertexConsumer consumer = buffers.getBuffer(RenderType.textBackground());
        Matrix4f pose = ms.last().pose();
        float[][] corners = {{x0 + nx, y0 + ny}, {x1 + nx, y1 + ny}, {x1 - nx, y1 - ny}, {x0 - nx, y0 - ny}};
        for (int i = 0; i < 4; i++) {
            consumer.addVertex(pose, corners[i][0], corners[i][1], z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        }
        for (int i = 3; i >= 0; i--) {
            consumer.addVertex(pose, corners[i][0], corners[i][1], z).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        }
    }
}
