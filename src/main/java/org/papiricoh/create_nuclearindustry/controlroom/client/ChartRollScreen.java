package org.papiricoh.create_nuclearindustry.controlroom.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartData;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.Locale;

/**
 * Reads a torn-off chart: the whole trace across the paper, oldest on the left, with the time it
 * covers and each pen's reading and last value underneath. Read only.
 */
public class ChartRollScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/chart_roll.png");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 168;
    /** Paper area on the texture. */
    private static final int PAPER_X0 = 12, PAPER_Y0 = 20, PAPER_X1 = 244, PAPER_Y1 = 128;
    private static final int LEGEND_Y = 134;
    private static final String KEY = "create_nuclearindustry.chart_roll.";

    private final ChartData data;
    private final ChartBuffer chart;

    private ChartRollScreen(ChartData data) {
        super(Component.translatable("item.create_nuclearindustry.chart_roll"));
        this.data = data;
        this.chart = data.buffer();
    }

    public static void open(ChartData data) {
        Minecraft.getInstance().setScreen(new ChartRollScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        graphics.blit(TEXTURE, left, top, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        Component kind = Component.translatable(TargetKind.byName(data.kind()).translationKey());
        graphics.drawString(font, Component.translatable(KEY + "title", kind), left + 12, top + 7, 0xFF3A3324, false);
        drawTraces(graphics, left, top);
        String span = String.format(Locale.ROOT, "%.0f", data.spanTicks() / 20.0);
        graphics.drawString(font, Component.translatable(KEY + "span", span), left + PAPER_X0 + 2, top + PAPER_Y0 + 2, 0xFF6F6650, false);
        Component now = Component.translatable(KEY + "now");
        graphics.drawString(font, now, left + PAPER_X1 - 2 - font.width(now), top + PAPER_Y0 + 2, 0xFF6F6650, false);
        drawLegend(graphics, left, top);
    }

    private void drawTraces(GuiGraphics graphics, int left, int top) {
        int samples = chart.size();
        if (samples == 0) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "blank"), left + WIDTH / 2, top + 70, 0xFF8A8270);
            return;
        }
        int paperWidth = PAPER_X1 - PAPER_X0 - 2;
        int paperHeight = PAPER_Y1 - PAPER_Y0 - 2;
        for (int pen = ChartBuffer.MAX_PENS - 1; pen >= 0; pen--) {
            if (data.pen(pen) == null) {
                continue;
            }
            int color = ChartRecorderBlockEntity.PEN_COLORS[pen];
            int previousY = Integer.MIN_VALUE;
            for (int i = 0; i < samples; i++) {
                // The newest sample sits at the right edge, as on the recorder.
                int x = left + PAPER_X0 + 1 + Math.round((ChartBuffer.CAPACITY - samples + i) * (paperWidth - 1f) / (ChartBuffer.CAPACITY - 1));
                int y = top + PAPER_Y0 + 1 + Math.round((1 - chart.get(pen, i)) * (paperHeight - 1));
                int from = previousY == Integer.MIN_VALUE ? y : Math.min(y, previousY);
                int to = previousY == Integer.MIN_VALUE ? y : Math.max(y, previousY);
                graphics.fill(x, from, x + 1, to + 1, color);
                previousY = y;
            }
        }
    }

    private void drawLegend(GuiGraphics graphics, int left, int top) {
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            int y = top + LEGEND_Y + pen * 10;
            GaugeReading reading = data.pen(pen);
            graphics.fill(left + 13, y + 1, left + 19, y + 7, reading == null ? 0xFFB8B09A : ChartRecorderBlockEntity.PEN_COLORS[pen]);
            Component name = reading == null ? Component.translatable("create_nuclearindustry.chart_recorder.pen.off")
                    : Component.translatable(reading.translationKey());
            graphics.drawString(font, name, left + 24, y, 0xFF3A3324, false);
            if (reading != null && chart.size() > 0) {
                String last = String.format(Locale.ROOT, "%.0f%%", chart.latest(pen) * 100);
                graphics.drawString(font, Component.translatable(KEY + "last", last), left + WIDTH - 12 - font.width(Component.translatable(KEY + "last", last)),
                        y, 0xFF6F6650, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
