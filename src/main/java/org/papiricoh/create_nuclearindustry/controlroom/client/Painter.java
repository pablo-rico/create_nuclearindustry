package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Paints a styled screen like a canvas: every rectangle covers those before it (a render type
 * painted in order, {@link MonitorRenderTypes#FLAT}) and text goes on top of all of them, held back until
 * {@link #finish} so the screen switches between the two only once. Coordinates are screen pixels,
 * y down; text is Minecraft's font at any scale, its 8-pixel cell from {@code y}.
 */
final class Painter {
    /** The plane everything is painted on, behind the text; the order paints it, not depth. */
    private static final float Z = -0.03f;
    /** The depth seal, a hair behind the paint so the paint is never hidden by it. */
    private static final float SEAL_Z = -0.035f;
    /**
     * In a Ponder scene each rectangle comes a hair nearer than the last instead, in the text
     * background's type: the scene's camera sorts by depth alone, so that keeps the painting order.
     */
    private static final float SORTED_FIRST = -0.07f;
    private static final float SORTED_STEP = 0.00002f;
    private static final float SORTED_LAST = -0.006f;
    /** The smallest text is shrunk to before it is cut short. */
    static final float MIN_SCALE = 0.6f;

    enum Align { LEFT, RIGHT, CENTER }

    private record Text(Matrix4f pose, String text, int argb) {}

    final PoseStack ms;
    final MultiBufferSource buffers;
    final Font font;
    private final boolean sorted;
    private float sortedZ = SORTED_FIRST;
    private final List<Text> texts = new ArrayList<>();
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;

    /** @param sorted a Ponder scene's screen, painted by depth ({@link #SORTED_FIRST}) */
    Painter(PoseStack ms, MultiBufferSource buffers, Font font, boolean sorted) {
        this.ms = ms;
        this.buffers = buffers;
        this.font = font;
        this.sorted = sorted;
    }

    Painter(MonitorFrame f) {
        this(f.ms(), f.buffers(), f.font(), f.monitor().getLevel() != net.minecraft.client.Minecraft.getInstance().level);
    }

    void rect(float x0, float y0, float x1, float y1, int argb) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        if (sorted) {
            sortedZ = Math.min(SORTED_LAST, sortedZ + SORTED_STEP);
            ScreenDraw.rect(ms, buffers, x0, y0, x1, y1, sortedZ, argb);
            return;
        }
        minX = Math.min(minX, x0);
        minY = Math.min(minY, y0);
        maxX = Math.max(maxX, x1);
        maxY = Math.max(maxY, y1);
        VertexConsumer consumer = buffers.getBuffer(MonitorRenderTypes.FLAT);
        Matrix4f pose = ms.last().pose();
        consumer.addVertex(pose, x0, y0, Z).setColor(argb).setLight(ScreenDraw.LIGHT);
        consumer.addVertex(pose, x0, y1, Z).setColor(argb).setLight(ScreenDraw.LIGHT);
        consumer.addVertex(pose, x1, y1, Z).setColor(argb).setLight(ScreenDraw.LIGHT);
        consumer.addVertex(pose, x1, y0, Z).setColor(argb).setLight(ScreenDraw.LIGHT);
    }

    /** A rectangle's outline, {@code t} thick, inside it. */
    void frame(float x0, float y0, float x1, float y1, float t, int argb) {
        rect(x0, y0, x1, y0 + t, argb);
        rect(x0, y1 - t, x1, y1, argb);
        rect(x0, y0 + t, x0 + t, y1 - t, argb);
        rect(x1 - t, y0 + t, x1, y1 - t, argb);
    }

    float width(String text) {
        return font.width(text);
    }

    /** Text from {@code x}, drawn at {@link #finish}; returns where it ends. */
    float text(String text, float x, float y, int argb, float scale) {
        texts.add(new Text(new Matrix4f(ms.last().pose()).translate(x, y, 0).scale(scale, scale, 1), text, argb));
        return x + font.width(text) * scale;
    }

    /** Seals what was painted with depth, then draws the text over everything. */
    void finish() {
        if (maxX > minX && maxY > minY) {
            VertexConsumer seal = buffers.getBuffer(MonitorRenderTypes.SEAL);
            Matrix4f pose = ms.last().pose();
            seal.addVertex(pose, minX, minY, SEAL_Z).setColor(0).setLight(ScreenDraw.LIGHT);
            seal.addVertex(pose, minX, maxY, SEAL_Z).setColor(0).setLight(ScreenDraw.LIGHT);
            seal.addVertex(pose, maxX, maxY, SEAL_Z).setColor(0).setLight(ScreenDraw.LIGHT);
            seal.addVertex(pose, maxX, minY, SEAL_Z).setColor(0).setLight(ScreenDraw.LIGHT);
        }
        for (Text text : texts) {
            font.drawInBatch(text.text(), 0, 0, text.argb(), false, text.pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
                    ScreenDraw.LIGHT);
        }
        texts.clear();
    }

    float text(String text, float x, float y, int argb) {
        return text(text, x, y, argb, 1);
    }

    void textRight(String text, float right, float y, int argb, float scale) {
        text(text, right - font.width(text) * scale, y, argb, scale);
    }

    void textCenter(String text, float center, float y, int argb, float scale) {
        text(text, center - font.width(text) * scale / 2, y, argb, scale);
    }

    /**
     * Text in at most {@code room} pixels at {@code scale}: shrunk down to {@link #MIN_SCALE}, then cut
     * short with "..", keeping its middle where it was. Returns the scale it was drawn at.
     */
    float fitted(String text, float x, float y, float room, int argb, float scale, Align align) {
        if (room <= 4) {
            return 0;
        }
        float textWidth = font.width(text);
        float shownScale = scale;
        String shown = text;
        if (textWidth * scale > room) {
            shownScale = Math.max(MIN_SCALE, room / textWidth);
            if (textWidth * shownScale > room) {
                String keep = text;
                while (!keep.isEmpty() && (font.width(keep) + font.width("..")) * shownScale > room) {
                    keep = keep.substring(0, keep.length() - 1);
                }
                shown = keep + "..";
            }
        }
        float y0 = y + (scale - shownScale) * 4;
        float shownWidth = font.width(shown) * shownScale;
        float x0 = switch (align) {
            case RIGHT -> x - shownWidth;
            case CENTER -> x - shownWidth / 2;
            default -> x;
        };
        text(shown, x0, y0, argb, shownScale);
        return shownScale;
    }

    float fitted(String text, float x, float y, float room, int argb, float scale) {
        return fitted(text, x, y, room, argb, scale, Align.LEFT);
    }
}
