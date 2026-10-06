package org.papiricoh.create_nuclearindustry.radiation.contamination.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.DoseReading;

/**
 * Dose readout in a screen corner: rate, current dose with a bar up to radiation sickness
 * (coloured by the symptom stage) and the lifetime total. Shown only while the server says
 * there is a rate or a dose; it draws from the synced reading and asks the server nothing.
 */
public final class DoseHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 92;
    private static final int MARGIN = 4;
    private static final int[] STAGE_COLOURS = {0xFF55FF55, 0xFFFFFF55, 0xFFFFAA00, 0xFFFF5555};

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "dose_hud"), new DoseHudLayer());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!ClientConfig.doseHud || player == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        DoseReading reading = player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD);
        if (reading == null || !reading.visible()) {
            return;
        }
        Font font = minecraft.font;
        int height = 3 * (font.lineHeight + 1) + 6;
        int x = ClientConfig.hudCorner.right() ? graphics.guiWidth() - WIDTH - MARGIN : MARGIN;
        int y = ClientConfig.hudCorner.bottom() ? graphics.guiHeight() - height - MARGIN : MARGIN;
        graphics.fill(x, y, x + WIDTH, y + height, 0x90000000);

        float rate = reading.rate();
        int rateColour = rate >= 1 ? 0xFFFF5555 : rate >= 0.1 ? 0xFFFFAA00 : 0xFF55FF55;
        int line = y + 3;
        graphics.drawString(font, Component.translatable("hud.create_nuclearindustry.rate",
                String.format(java.util.Locale.ROOT, "%.2f", rate)), x + 3, line, rateColour, true);
        line += font.lineHeight + 1;

        int stageColour = STAGE_COLOURS[Math.max(0, Math.min(STAGE_COLOURS.length - 1, reading.stage()))];
        graphics.drawString(font, Component.translatable("hud.create_nuclearindustry.dose", reading.dose()), x + 3, line, 0xFFFFFFFF, true);
        line += font.lineHeight;
        int barWidth = WIDTH - 6;
        graphics.fill(x + 3, line, x + 3 + barWidth, line + 2, 0xFF303030);
        graphics.fill(x + 3, line, x + 3 + barWidth * reading.bar() / 100, line + 2, stageColour);
        line += 3;

        graphics.drawString(font, Component.translatable("hud.create_nuclearindustry.total", reading.total()), x + 3, line, 0xFFAAAAAA, true);
    }
}
