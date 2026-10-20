package org.papiricoh.create_nuclearindustry.missile.radar.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Vector drawing on a screen for the radar's scope and the command table's screen: thick lines, rings, filled discs, gradient
 * wedges and textured quads, in floating-point screen coordinates. One batch per {@link #begin}…{@link #close}, drawn straight
 * away so it layers correctly with what {@link GuiGraphics} draws before and after; additive batches glow like phosphor.
 */
public final class ScopeDraw implements AutoCloseable {
    private final BufferBuilder buffer;
    private final Matrix4f pose;
    private final boolean additive;

    private ScopeDraw(GuiGraphics g, boolean additive) {
        g.flush();
        this.additive = additive;
        this.pose = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        // Lines and fans are wound either way: nothing may be culled.
        RenderSystem.disableCull();
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    /** A batch drawn over what is already on screen. */
    public static ScopeDraw begin(GuiGraphics g, boolean additive) {
        return new ScopeDraw(g, additive);
    }

    @Override
    public void close() {
        MeshData mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private void vertex(float x, float y, int argb) {
        buffer.addVertex(pose, x, y, 0).setColor(argb);
    }

    public void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c0, int c1, int c2, int c3) {
        vertex(x0, y0, c0);
        vertex(x1, y1, c1);
        vertex(x2, y2, c2);
        vertex(x3, y3, c3);
    }

    public void rect(float x0, float y0, float x1, float y1, int argb) {
        quad(x0, y0, x0, y1, x1, y1, x1, y0, argb, argb, argb, argb);
    }

    /** A line {@code width} wide, its colour fading from {@code c0} to {@code c1}. */
    public void line(float x0, float y0, float x1, float y1, float width, int c0, int c1) {
        float dx = x1 - x0, dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-4f) return;
        float nx = -dy / length * width / 2, ny = dx / length * width / 2;
        quad(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, c0, c0, c1, c1);
    }

    public void line(float x0, float y0, float x1, float y1, float width, int argb) {
        line(x0, y0, x1, y1, width, argb, argb);
    }

    /** A ring of radius {@code r}; dashed draws every other segment. */
    public void circle(float cx, float cy, float r, float width, int argb, boolean dashed) {
        int segments = Math.max(24, Math.min(256, (int) (r * 0.9f)));
        for (int i = 0; i < segments; i++) {
            if (dashed && (i & 1) == 1) continue;
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            line(cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r, cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r, width, argb);
        }
    }

    /** An ellipse round ({@code cx, cy}): semi-axes {@code a} along {@code heading} (radians, 0 up the screen, clockwise) and {@code b} across. */
    public void ellipse(float cx, float cy, float a, float b, double heading, float width, int argb, boolean filled, int fill) {
        int segments = Math.max(16, Math.min(96, (int) (Math.max(a, b) * 1.2f)));
        float sin = (float) Math.sin(heading), cos = (float) Math.cos(heading);
        float px = 0, py = 0;
        for (int i = 0; i <= segments; i++) {
            double t = Math.PI * 2 * i / segments;
            float u = (float) Math.cos(t) * a, v = (float) Math.sin(t) * b;
            // u along the heading (screen direction (sin, -cos)), v across it (cos, sin).
            float x = cx + u * sin + v * cos, y = cy - u * cos + v * sin;
            if (i > 0) {
                if (filled) quad(cx, cy, px, py, x, y, x, y, fill, fill, fill, fill);
                line(px, py, x, y, width, argb);
            }
            px = x;
            py = y;
        }
    }

    /** A filled disc, its centre {@code inner} fading to {@code outer} at the rim. */
    public void disc(float cx, float cy, float r, int inner, int outer) {
        int segments = Math.max(8, Math.min(48, (int) (r * 2)));
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = cx + (float) Math.sin(a0) * r, y0 = cy - (float) Math.cos(a0) * r;
            float x1 = cx + (float) Math.sin(a1) * r, y1 = cy - (float) Math.cos(a1) * r;
            quad(cx, cy, x0, y0, x1, y1, x1, y1, inner, outer, outer, outer);
        }
    }

    /**
     * A sweep: the sector from bearing {@code from} to {@code to} (degrees, clockwise from up), radius {@code r}, its colour going
     * from {@code atFrom} to {@code atTo} across it.
     */
    public void wedge(float cx, float cy, float r, double from, double to, int atFrom, int atTo) {
        int segments = Math.max(4, (int) Math.ceil(Math.abs(to - from) / 2));
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments, t1 = (double) (i + 1) / segments;
            double a0 = Math.toRadians(from + (to - from) * t0), a1 = Math.toRadians(from + (to - from) * t1);
            int c0 = lerp(atFrom, atTo, (float) t0), c1 = lerp(atFrom, atTo, (float) t1);
            quad(cx, cy, cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r, cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r,
                    cx, cy, c0 & 0x00FFFFFF | ((c0 >>> 24) / 3) << 24, c0, c1, c1 & 0x00FFFFFF | ((c1 >>> 24) / 3) << 24);
        }
    }

    /** A colour with its alpha scaled by {@code alpha} (0-1). */
    public static int alpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) ((argb >>> 24) * alpha)));
        return (argb & 0x00FFFFFF) | a << 24;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    /** A texture stretched over a screen rectangle, tinted by {@code argb}; its own batch. */
    public static void texture(GuiGraphics g, ResourceLocation texture, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                               int argb) {
        g.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, x0, y0, 0).setUv(u0, v0).setColor(argb);
        buffer.addVertex(pose, x0, y1, 0).setUv(u0, v1).setColor(argb);
        buffer.addVertex(pose, x1, y1, 0).setUv(u1, v1).setColor(argb);
        buffer.addVertex(pose, x1, y0, 0).setUv(u1, v0).setColor(argb);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }
}
