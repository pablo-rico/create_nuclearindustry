package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;

/**
 * Draws a chart recorder's paper in the window of its front face: the grid scrolls left with every
 * sample and each pen leaves its trace, newest at the right edge where the pen heads sit. Below the
 * window a strip shows how much paper is left.
 */
public class ChartRecorderRenderer implements BlockEntityRenderer<ChartRecorderBlockEntity> {
    /** Paper window on the front texture, in pixels from the bottom-left corner. */
    private static final float WINDOW_X0 = 2, WINDOW_X1 = 14, WINDOW_Y0 = 4, WINDOW_Y1 = 14;
    /** Drawing units per texture pixel. */
    private static final float UNIT = 10;
    private static final int PAPER = 0xFFEDE6CC;
    private static final int GRID_MAJOR = 0xFFB4C7AA;
    private static final int GRID_MINOR = 0xFFD3DCC2;
    private static final int ROLLER = 0xFF2A2F2C;
    private static final int GRID_EVERY = 10;
    private static final String KEY = "create_nuclearindustry.chart_recorder.";

    private final Font font;

    public ChartRecorderRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ChartRecorderBlockEntity recorder, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, recorder.facing());
        // Top-left of the paper window, y down, in units of a tenth of a pixel.
        ms.translate(WINDOW_X0 / 16, WINDOW_Y1 / 16, 0.001);
        ms.scale(1 / (16 * UNIT), -1 / (16 * UNIT), 1 / (16 * UNIT));
        float width = (WINDOW_X1 - WINDOW_X0) * UNIT;
        float height = (WINDOW_Y1 - WINDOW_Y0) * UNIT;
        if (!recorder.running()) {
            ScreenDraw.rect(ms, buffers, 0, 0, width, height, -0.05f, ROLLER);
            paperGauge(recorder, ms, buffers, width, height);
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(KEY + "no_paper_short"), width / 2, height / 2 - 4, 0xFFFF5A4A);
            ms.popPose();
            return;
        }
        ScreenDraw.rect(ms, buffers, 0, 0, width, height, -0.05f, PAPER);
        drawGrid(recorder, ms, buffers, width, height);
        drawTraces(recorder, ms, buffers, width, height);
        paperGauge(recorder, ms, buffers, width, height);
        if (recorder.kind() == TargetKind.NONE) {
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable("create_nuclearindustry.monitor.unlinked"),
                    width / 2, height / 2 - 4, 0xFF8A8270);
        }
        ms.popPose();
    }

    private static float step(float width) {
        return width / (ChartRecorderBlockEntity.VISIBLE - 1);
    }

    /** Value lines every quarter, time lines every few samples that move with the paper. */
    // Depths, back to front: paper -0.05, grid -0.04, traces -0.03, pen heads -0.02, text 0.

    private static void drawGrid(ChartRecorderBlockEntity recorder, PoseStack ms, MultiBufferSource buffers, float width, float height) {
        for (int i = 1; i < 4; i++) {
            float y = height * i / 4f;
            ScreenDraw.rect(ms, buffers, 0, y - 0.4f, width, y + 0.4f, -0.04f, i == 2 ? GRID_MAJOR : GRID_MINOR);
        }
        float step = step(width);
        long newest = recorder.drawn();
        for (int back = 0; back < ChartRecorderBlockEntity.VISIBLE; back++) {
            if (Math.floorMod(newest - back, GRID_EVERY) == 0) {
                float x = width - back * step;
                boolean major = Math.floorMod(newest - back, GRID_EVERY * 5) == 0;
                ScreenDraw.rect(ms, buffers, x - 0.4f, 0, x + 0.4f, height, -0.04f, major ? GRID_MAJOR : GRID_MINOR);
            }
        }
    }

    private static void drawTraces(ChartRecorderBlockEntity recorder, PoseStack ms, MultiBufferSource buffers, float width, float height) {
        ChartBuffer chart = recorder.chart();
        int shown = Math.min(chart.size(), ChartRecorderBlockEntity.VISIBLE);
        int first = chart.size() - shown;
        float step = step(width);
        for (int pen = ChartBuffer.MAX_PENS - 1; pen >= 0; pen--) {
            if (recorder.pen(pen) == null) {
                continue;
            }
            int color = ChartRecorderBlockEntity.PEN_COLORS[pen];
            float previousY = Float.NaN;
            for (int i = 0; i < shown; i++) {
                float x = width - (shown - 1 - i) * step;
                float y = valueY(chart.get(pen, first + i), height);
                ScreenDraw.rect(ms, buffers, Math.max(0, x - step), y - 0.6f, x, y + 0.6f, -0.03f - pen * 0.001f, color);
                if (!Float.isNaN(previousY)) {
                    ScreenDraw.rect(ms, buffers, x - step - 0.6f, Math.min(y, previousY) - 0.6f, x - step + 0.6f,
                            Math.max(y, previousY) + 0.6f, -0.03f - pen * 0.001f, color);
                }
                previousY = y;
            }
            // The pen head, riding the right edge of the paper.
            float head = valueY(shown > 0 ? chart.latest(pen) : 0, height);
            ScreenDraw.rect(ms, buffers, width - 3, head - 2, width, head + 2, -0.02f, 0xFF1E1E1E);
            ScreenDraw.rect(ms, buffers, width - 2.4f, head - 1.4f, width - 0.6f, head + 1.4f, -0.019f, color);
        }
    }

    private static float valueY(float fraction, float height) {
        return height - 1 - Math.max(0, Math.min(1, fraction)) * (height - 2);
    }

    /** The tray below the window: dark when empty, filling with paper as it is topped up. */
    private static void paperGauge(ChartRecorderBlockEntity recorder, PoseStack ms, MultiBufferSource buffers, float width, float height) {
        float top = height + (WINDOW_Y0 - 3) * UNIT + 2;
        float bottom = height + (WINDOW_Y0 - 1.5f) * UNIT;
        ScreenDraw.rect(ms, buffers, 0, top, width, bottom, -0.04f, 0xFF161A18);
        float fill = width * recorder.paper() / ChartRecorderBlockEntity.MAX_PAPER;
        ScreenDraw.rect(ms, buffers, 0, top, fill, bottom, -0.035f, recorder.paper() < 8 ? 0xFFE8A93A : PAPER);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
