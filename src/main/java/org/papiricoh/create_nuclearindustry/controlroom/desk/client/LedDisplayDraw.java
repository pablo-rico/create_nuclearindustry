package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.client.ScreenDraw;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedLayout;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedText;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ReadingValue;
import org.papiricoh.create_nuclearindustry.controlroom.desk.SevenSegment;

/**
 * Draws an LED display's digits, unit and label on the window of its housings, from the slot of its
 * head: the window starts at the head's left edge as the operator sees it and runs 8 px per module
 * to their right. Segments go first and text last, in separate passes over all the displays of a
 * block, because the font closes the buffer the segments draw into.
 */
final class LedDisplayDraw {
    private static final float PX = 1 / 16f;
    /** Height of the window above the surface, just over the housing. */
    private static final float FACE_Y = 1.2f + 0.02f;
    private static final int LIT = 0xFFFF5A36;
    private static final int GHOST = 0xFF2B1712;
    private static final int UNIT = 0xFFE89A7A;
    private static final int LABEL = 0xFFB9CBB9;
    /** Font glyphs are about 8 units from the top of a capital to the bottom of a descender. */
    private static final float GLYPH = 8;

    private LedDisplayDraw() {}

    /**
     * From the head's slot (origin at its centre on the surface, x to the operator's left, y out, z
     * away from them) to the window as read: origin at its top-left corner, x right, y down, in px.
     */
    private static void toWindow(PoseStack ms) {
        ms.translate((LedLayout.CELL / 2 - LedLayout.MARGIN) * PX, FACE_Y * PX, LedLayout.HEIGHT / 2 * PX);
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        ms.scale(PX, -PX, PX);
    }

    static void segments(ControlModule head, int cells, PoseStack ms, MultiBufferSource buffers) {
        LedLayout layout = LedLayout.of(cells);
        LedText.Readout readout = LedText.format(head.value(), layout.digits(), ReadingValue.decimals(head.reading()));
        ms.pushPose();
        toWindow(ms);
        float w = layout.digitWidth();
        float h = layout.digitHeight();
        float t = Math.max(0.22f, h * 0.14f);
        for (int i = 0; i < layout.digits(); i++) {
            float left = layout.digitLeft(i);
            float top = layout.digitsTop();
            int mask = SevenSegment.mask(readout.cells().charAt(i));
            digit(ms, buffers, left, top, w, h, t, SevenSegment.ALL, GHOST, 0.0f);
            digit(ms, buffers, left, top, w, h, t, mask, LIT, 0.02f);
            float dotLeft = left + w + Math.max(0.05f, (layout.pitch() - w - t) / 2);
            ScreenDraw.rect(ms, buffers, dotLeft, top + h - t, dotLeft + t, top + h, i == readout.dotAfter() ? 0.02f : 0.0f,
                    i == readout.dotAfter() ? LIT : GHOST);
        }
        ms.popPose();
    }

    private static void digit(PoseStack ms, MultiBufferSource buffers, float l, float top, float w, float h, float t,
                              int mask, int color, float z) {
        float mid = top + h / 2;
        float in = t * 0.5f;
        if (SevenSegment.lit(mask, SevenSegment.A)) {
            ScreenDraw.rect(ms, buffers, l + in, top, l + w - in, top + t, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.G)) {
            ScreenDraw.rect(ms, buffers, l + in, mid - t / 2, l + w - in, mid + t / 2, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.D)) {
            ScreenDraw.rect(ms, buffers, l + in, top + h - t, l + w - in, top + h, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.F)) {
            ScreenDraw.rect(ms, buffers, l, top + in, l + t, mid, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.B)) {
            ScreenDraw.rect(ms, buffers, l + w - t, top + in, l + w, mid, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.E)) {
            ScreenDraw.rect(ms, buffers, l, mid, l + t, top + h - in, z, color);
        }
        if (SevenSegment.lit(mask, SevenSegment.C)) {
            ScreenDraw.rect(ms, buffers, l + w - t, mid, l + w, top + h - in, z, color);
        }
    }

    /** The unit, and on a display wide enough the label (the reading's name when none is set). */
    static void text(ControlModule head, int cells, Font font, PoseStack ms, MultiBufferSource buffers) {
        LedLayout layout = LedLayout.of(cells);
        ms.pushPose();
        toWindow(ms);
        String unit = ReadingValue.unit(head.reading());
        if (!unit.isEmpty()) {
            float scale = layout.unitHeight() / GLYPH;
            draw(font, ms, buffers, Component.literal(unit), layout.unitRight() - font.width(unit) * scale, layout.unitTop(),
                    scale, UNIT);
        }
        if (layout.label()) {
            float scale = layout.labelHeight() / GLYPH;
            Component name = head.label().isEmpty()
                    ? Component.translatable(head.reading().translationKey())
                    : Component.literal(head.label());
            String fitted = font.plainSubstrByWidth(name.getString(), (int) ((layout.width() - 2 * LedLayout.MARGIN) / scale));
            draw(font, ms, buffers, Component.literal(fitted), LedLayout.MARGIN, LedLayout.MARGIN, scale, LABEL);
        }
        ms.popPose();
    }

    private static void draw(Font font, PoseStack ms, MultiBufferSource buffers, Component text, float x, float y, float scale,
                             int color) {
        ms.pushPose();
        ms.translate(x, y, 0.03f);
        ms.scale(scale, scale, scale);
        font.drawInBatch(text, 0, 0, color, false, ms.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        ms.popPose();
    }
}
