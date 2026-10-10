package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;

import java.util.Locale;

/**
 * Lights an annunciator panel's windows by their alarm state (fast flash, steady, slow flash), rings
 * the first-out window in white and writes each window's label, plus the names of the two buttons.
 */
public class AnnunciatorPanelRenderer implements BlockEntityRenderer<AnnunciatorPanelBlockEntity> {
    /** Drawing units per texture pixel. */
    private static final float UNIT = 10;
    private static final int LIT = 0xFFF0703A;
    private static final int LIT_TEXT = 0xFF2A0E06;
    private static final int DARK_TEXT = 0xFF6E5A50;
    private static final int UNLINKED_TEXT = 0xFF3E3632;
    private static final int FIRST_OUT = 0xFFFFFFFF;
    private static final String KEY = "create_nuclearindustry.annunciator.";

    private final Font font;

    public AnnunciatorPanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(AnnunciatorPanelBlockEntity panel, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        long time = panel.getLevel() == null ? 0 : panel.getLevel().getGameTime();
        AnnunciatorLogic logic = panel.logic();
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, panel.facing());
        // Top-left corner of the face, y down, in tenths of a pixel.
        ms.translate(0, 1, 0.001);
        ms.scale(1 / (16 * UNIT), -1 / (16 * UNIT), 1 / (16 * UNIT));
        boolean[] lit = new boolean[AnnunciatorPanelBlockEntity.WINDOWS];
        for (int i = 0; i < lit.length; i++) {
            lit[i] = AnnunciatorLogic.lit(logic.state(i), time);
            float[] box = window(i);
            if (lit[i]) {
                ScreenDraw.rect(ms, buffers, box[0] + 2, box[1] + 2, box[2] - 2, box[3] - 2, -0.04f, LIT);
            }
            if (logic.firstOut() == i) {
                frame(ms, buffers, box, FIRST_OUT);
            }
        }
        // Text last: the font closes the shared buffer the rectangles draw into.
        for (int i = 0; i < lit.length; i++) {
            AnnunciatorPanelBlockEntity.Window window = panel.window(i);
            Component label = window.target() == null ? Component.literal("-")
                    : window.label().isEmpty()
                    ? Component.translatable(KEY + "short." + window.metric().name().toLowerCase(Locale.ROOT))
                    : Component.literal(window.label());
            int color = window.target() == null ? UNLINKED_TEXT : lit[i] ? LIT_TEXT : DARK_TEXT;
            fitted(ms, buffers, label, window(i), color);
        }
        fitted(ms, buffers, Component.translatable(KEY + "ack"), button(2, 7), 0xFFE8E2D0);
        fitted(ms, buffers, Component.translatable(KEY + "reset"), button(9, 14), 0xFFE8E2D0);
        ms.popPose();
    }

    /** A window's box in drawing units: left, top, right, bottom. Rows from the top, two columns. */
    private static float[] window(int index) {
        int column = index % 2;
        int row = index / 2;
        float left = (column == 0 ? 1 : 9) * UNIT;
        float top = (1 + row * 4) * UNIT;
        return new float[]{left, top, left + 6 * UNIT, top + 3 * UNIT};
    }

    private static float[] button(int fromPixel, int toPixel) {
        return new float[]{fromPixel * UNIT, 13 * UNIT, toPixel * UNIT, 15 * UNIT};
    }

    private static void frame(PoseStack ms, MultiBufferSource buffers, float[] box, int color) {
        float w = 1.2f;
        ScreenDraw.rect(ms, buffers, box[0], box[1], box[2], box[1] + w, -0.03f, color);
        ScreenDraw.rect(ms, buffers, box[0], box[3] - w, box[2], box[3], -0.03f, color);
        ScreenDraw.rect(ms, buffers, box[0], box[1], box[0] + w, box[3], -0.03f, color);
        ScreenDraw.rect(ms, buffers, box[2] - w, box[1], box[2], box[3], -0.03f, color);
    }

    /** Centred in the box, shrunk to fit its width. */
    private void fitted(PoseStack ms, MultiBufferSource buffers, Component text, float[] box, int color) {
        float room = box[2] - box[0] - 6;
        float width = Math.max(1, font.width(text));
        float scale = Math.min(1.0f, room / width);
        ms.pushPose();
        ms.translate((box[0] + box[2]) / 2, (box[1] + box[3]) / 2 - 4 * scale, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, text, 0, 0, color);
        ms.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
