package org.papiricoh.create_nuclearindustry.missile.radar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.controlroom.client.MonitorRenderTypes;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarScopeBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Draws a radar scope's screen from its origin block: a round phosphor plan view as big as the screen allows, range rings, the
 * beam turning with the radar's antenna, and the echoes it lights, fading until it comes round again, each with its velocity, its
 * predicted impact and its name. Coloured shapes go through {@link MonitorRenderTypes#FLAT}, asked for just before each shape;
 * the text goes last, so no buffer is used after another kind was drawn.
 */
public class RadarScopeRenderer implements BlockEntityRenderer<RadarScopeBlockEntity> {
    private static final float INSET = 1 / 16f;
    /** One screen pixel, in blocks. */
    private static final float PIXEL = 1 / 96f;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final int GREEN = 0xFF7DFF9A, RED = 0xFFFF4A3A, AMBER = 0xFFFFB347;

    private final Font font;
    /** Per screen, when the beam last painted each echo (client ticks) and where. */
    private final Map<RadarScopeBlockEntity, Phosphor> phosphor = new WeakHashMap<>();

    private static final class Phosphor {
        final Map<UUID, float[]> painted = new HashMap<>();
        double lastSweep = Double.NaN;
    }

    public RadarScopeRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(RadarScopeBlockEntity scope, float partial, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        if (!scope.isOrigin()) return;
        float w = scope.screenWidth(), h = scope.screenHeight();
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-scope.facing().toYRot()));
        ms.translate(-0.5, -0.5, 0.5 + 0.002);
        // Screen pixels from the bottom-left corner, y down from the top.
        ms.translate(INSET, h - INSET, 0);
        ms.scale(PIXEL, -PIXEL, PIXEL);
        float width = (w - 2 * INSET) / PIXEL, height = (h - 2 * INSET) / PIXEL;
        draw(scope, ms, buffers, width, height, partial);
        ms.popPose();
    }

    private VertexConsumer flat(MultiBufferSource buffers) {
        return buffers.getBuffer(MonitorRenderTypes.FLAT);
    }

    private static void quad(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c0, int c1) {
        v.addVertex(m, x0, y0, 0).setColor(c0).setLight(LIGHT);
        v.addVertex(m, x1, y1, 0).setColor(c0).setLight(LIGHT);
        v.addVertex(m, x2, y2, 0).setColor(c1).setLight(LIGHT);
        v.addVertex(m, x3, y3, 0).setColor(c1).setLight(LIGHT);
        // Both windings, whatever the pose's handedness.
        v.addVertex(m, x3, y3, 0).setColor(c1).setLight(LIGHT);
        v.addVertex(m, x2, y2, 0).setColor(c1).setLight(LIGHT);
        v.addVertex(m, x1, y1, 0).setColor(c0).setLight(LIGHT);
        v.addVertex(m, x0, y0, 0).setColor(c0).setLight(LIGHT);
    }

    private static void line(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float width, int c0, int c1) {
        float dx = x1 - x0, dy = y1 - y0, length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-4f) return;
        float nx = -dy / length * width / 2, ny = dx / length * width / 2;
        quad(v, m, x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, c0, c1);
    }

    private static void ring(VertexConsumer v, Matrix4f m, float cx, float cy, float r, float width, int argb, boolean dashed) {
        int segments = Math.max(24, Math.min(160, (int) r));
        for (int i = 0; i < segments; i++) {
            if (dashed && (i & 1) == 1) continue;
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            line(v, m, cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r, cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r,
                    width, argb, argb);
        }
    }

    private static void disc(VertexConsumer v, Matrix4f m, float cx, float cy, float r, int inner, int outer) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            quad(v, m, cx, cy, cx, cy, cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r, cx + (float) Math.sin(a0) * r,
                    cy - (float) Math.cos(a0) * r, inner, outer);
        }
    }

    private static int alpha(int argb, float a) {
        return (argb & 0xFFFFFF) | Mth.clamp((int) ((argb >>> 24) * a), 0, 255) << 24;
    }

    private void draw(RadarScopeBlockEntity scope, PoseStack ms, MultiBufferSource buffers, float width, float height, float partial) {
        RadarScopeBlockEntity.Picture picture = scope.picture();
        Matrix4f m = ms.last().pose();
        float cx = width / 2, cy = height / 2, r = Math.min(width, height) / 2 - 3;
        List<Runnable> texts = new ArrayList<>();
        // Housing, tube and glass.
        quad(flat(buffers), m, 0, 0, 0, height, width, height, width, 0, 0xFF1E2418, 0xFF1E2418);
        disc(flat(buffers), m, cx, cy, r + 2.5f, 0xFF6A7570, 0xFF4A5450);
        disc(flat(buffers), m, cx, cy, r, 0xFF0D2A14, 0xFF04120A);
        double time = scope.getLevel() == null ? 0 : scope.getLevel().getGameTime() + partial;
        if (picture.status() < 0) {
            Component text = Component.translatable("radar.create_nuclearindustry.scope." + (picture.status() == -1 ? "unlinked" : "no_signal"));
            texts.add(() -> text(ms, buffers, text, cx - font.width(text) / 2f, cy - 4, 0xFF3C8A50));
            texts.forEach(Runnable::run);
            return;
        }
        float scale = r / picture.range();
        VertexConsumer v = flat(buffers);
        for (int i = 1; i <= 4; i++) ring(v, m, cx, cy, r * i / 4, 0.6f, 0x6652D27A, false);
        line(v, m, cx, cy - r, cx, cy + r, 0.5f, 0x4452D27A, 0x4452D27A);
        line(v, m, cx - r, cy, cx + r, cy, 0.5f, 0x4452D27A, 0x4452D27A);
        if (picture.zoneRadius() > 0) {
            float zx = cx + picture.zoneDx() * scale, zy = cy + picture.zoneDz() * scale;
            if (Math.hypot(zx - cx, zy - cy) < r) ring(v, m, zx, zy, Math.min(r * 2, picture.zoneRadius() * scale), 0.8f, 0xCCFFB347, true);
        }
        // The beam, turning with the antenna.
        boolean on = picture.status() == 1 && picture.speed() != 0;
        double sweep = on ? Mth.positiveModulo(time * picture.speed() * 0.3, 360) : Double.NaN;
        if (on) {
            int steps = 20;
            double trail = picture.speed() >= 0 ? -40 : 40;
            for (int i = 0; i < steps; i++) {
                double a0 = Math.toRadians(sweep + trail * i / steps), a1 = Math.toRadians(sweep + trail * (i + 1) / steps);
                int c0 = alpha(0xFF40FF70, 0.35f * (1 - (float) i / steps)), c1 = alpha(0xFF40FF70, 0.35f * (1 - (float) (i + 1) / steps));
                quad(v, m, cx, cy, cx, cy, cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r, cx + (float) Math.sin(a0) * r,
                        cy - (float) Math.cos(a0) * r, c0, c1);
            }
            double a = Math.toRadians(sweep);
            line(v, m, cx, cy, cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, 1.2f, 0xEEB8FFC8, 0x8840FF70);
        }
        // Echoes: lit as the beam crosses them, fading after.
        Phosphor state = phosphor.computeIfAbsent(scope, s -> new Phosphor());
        double period = picture.speed() == 0 ? 40 : 360 / (Math.abs(picture.speed()) * 0.3);
        double since = Math.max(0, Math.min(40, time - picture.sample()));
        for (RadarScopeBlockEntity.Echo echo : picture.echoes()) {
            float ex = echo.dx() + echo.vx() * (float) since, ez = echo.dz() + echo.vz() * (float) since;
            double bearing = Mth.positiveModulo(Math.toDegrees(Math.atan2(ex, -ez)), 360);
            float[] painted = state.painted.get(echo.id());
            boolean crossed = on && !Double.isNaN(state.lastSweep) && crossed(bearing, state.lastSweep, sweep, picture.speed() >= 0);
            if (painted == null || crossed || !ClientConfig.radarPhosphor) state.painted.put(echo.id(), new float[]{ex, ez, (float) time});
        }
        state.lastSweep = sweep;
        state.painted.keySet().removeIf(id -> picture.echoes().stream().noneMatch(e -> e.id().equals(id)));
        v = flat(buffers);
        for (RadarScopeBlockEntity.Echo echo : picture.echoes()) {
            float[] painted = state.painted.get(echo.id());
            if (painted == null) continue;
            float glow = ClientConfig.radarPhosphor ? (float) (0.25 + 0.75 * Math.exp(-(time - painted[2]) / (period * 0.7))) : 1;
            float px = cx + painted[0] * scale, py = cy + painted[1] * scale;
            if (Math.hypot(px - cx, py - cy) > r) continue;
            int colour = echo.threat() ? RED : GREEN;
            line(v, m, px, py, px + echo.vx() * 600 * scale, py + echo.vz() * 600 * scale, 0.7f, alpha(colour, 0.7f * glow), alpha(colour, 0.05f));
            float ix = cx + echo.ix() * scale, iy = cy + echo.iz() * scale;
            if (Math.hypot(ix - cx, iy - cy) < r) {
                int ring = echo.threat() ? RED : AMBER;
                line(v, m, ix - 2, iy - 2, ix + 2, iy + 2, 0.8f, alpha(ring, 0.9f), alpha(ring, 0.9f));
                line(v, m, ix - 2, iy + 2, ix + 2, iy - 2, 0.8f, alpha(ring, 0.9f), alpha(ring, 0.9f));
            }
            disc(v, m, px, py, 4.5f, alpha(colour, 0.35f * glow), alpha(colour, 0));
            disc(v, m, px, py, 1.8f, alpha(colour, glow), alpha(colour, 0.6f * glow));
            String name = echo.designation();
            float fx = px + 4, fy = py - 7, fg = glow;
            texts.add(() -> small(ms, buffers, name, fx, fy, alpha(echo.threat() ? RED : GREEN, Math.max(0.4f, fg))));
        }
        float range = picture.range();
        texts.add(() -> small(ms, buffers, range >= 1000 ? String.format(Locale.ROOT, "%.0fk", range / 1000) : String.format(Locale.ROOT, "%.0f", range),
                cx + r * 0.72f, cy + r * 0.72f, 0xAA68D88A));
        if (picture.status() != 1) {
            Component off = Component.translatable("radar.create_nuclearindustry.screen." + (picture.status() == 2 ? "static" : "offline"));
            texts.add(() -> text(ms, buffers, off, cx - font.width(off) / 2f, cy - 4, 0xFFFF8A70));
        } else if (picture.unacked() && ((int) (time / 8) & 1) == 0) {
            Component alarm = Component.translatable("radar.create_nuclearindustry.screen.threat");
            texts.add(() -> text(ms, buffers, alarm, cx - font.width(alarm) / 2f, cy + r * 0.5f, RED));
        }
        // Text last of all.
        texts.forEach(Runnable::run);
    }

    private static boolean crossed(double b, double from, double to, boolean clockwise) {
        double span = clockwise ? Mth.positiveModulo(to - from, 360) : Mth.positiveModulo(from - to, 360);
        double off = clockwise ? Mth.positiveModulo(b - from, 360) : Mth.positiveModulo(from - b, 360);
        return span < 180 && off <= span;
    }

    private void text(PoseStack ms, MultiBufferSource buffers, Component text, float x, float y, int argb) {
        font.drawInBatch(text, x, y, argb, false, new Matrix4f(ms.last().pose()).translate(0, 0, -0.5f), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
    }

    private void small(PoseStack ms, MultiBufferSource buffers, String text, float x, float y, int argb) {
        Matrix4f pose = new Matrix4f(ms.last().pose()).translate(x, y, -0.5f).scale(0.5f, 0.5f, 1);
        font.drawInBatch(text, 0, 0, argb, false, pose, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(RadarScopeBlockEntity scope) {
        BlockPos pos = scope.getBlockPos();
        Direction right = scope.right();
        BlockPos far = pos.relative(right, scope.screenWidth() - 1).above(scope.screenHeight() - 1);
        return new AABB(pos).minmax(new AABB(far));
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
