package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Flat, full-bright drawing on the front face of a control room block. */
final class ScreenDraw {
    static final int LIGHT = LightTexture.FULL_BRIGHT;

    private ScreenDraw() {}

    /**
     * Moves the pose onto the front face of a block facing {@code facing}: origin at the bottom-left
     * corner as the viewer sees it, x to the viewer's right, y up, z out of the face, in block units.
     */
    static void onFrontFace(PoseStack ms, Direction facing) {
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        ms.translate(-0.5, -0.5, 0.5 + 0.002);
    }

    /** A filled rectangle; drawn with both windings so it shows whatever the pose's handedness. */
    static void rect(PoseStack ms, MultiBufferSource buffers, float x0, float y0, float x1, float y1, float z, int argb) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.textBackground());
        Matrix4f pose = ms.last().pose();
        vertex(consumer, pose, x0, y0, z, argb);
        vertex(consumer, pose, x0, y1, z, argb);
        vertex(consumer, pose, x1, y1, z, argb);
        vertex(consumer, pose, x1, y0, z, argb);
        vertex(consumer, pose, x1, y0, z, argb);
        vertex(consumer, pose, x1, y1, z, argb);
        vertex(consumer, pose, x0, y1, z, argb);
        vertex(consumer, pose, x0, y0, z, argb);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int argb) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setLight(LIGHT);
    }

    static void text(PoseStack ms, MultiBufferSource buffers, Font font, Component text, float x, float y, int argb) {
        font.drawInBatch(text, x, y, argb, false, ms.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
    }

    static void textRight(PoseStack ms, MultiBufferSource buffers, Font font, Component text, float right, float y, int argb) {
        text(ms, buffers, font, text, right - font.width(text), y, argb);
    }

    static void textCentered(PoseStack ms, MultiBufferSource buffers, Font font, Component text, float center, float y, int argb) {
        text(ms, buffers, font, text, center - font.width(text) / 2.0f, y, argb);
    }

    /** A horizontal gauge: dark trough with the filled part coloured by how full it is. */
    static void bar(PoseStack ms, MultiBufferSource buffers, float x, float y, float width, float height, float fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        rect(ms, buffers, x, y, x + width, y + height, -0.02f, 0xFF1F2E28);
        int color = fraction > 0.85f ? 0xFFE04A3A : fraction > 0.6f ? 0xFFE8A93A : 0xFF6FBF4E;
        rect(ms, buffers, x, y, x + width * fraction, y + height, -0.01f, color);
    }
}
