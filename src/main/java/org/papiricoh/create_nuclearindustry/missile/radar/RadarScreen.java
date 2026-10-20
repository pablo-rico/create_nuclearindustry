package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The radar's screen: its tracks, soonest impact first. */
public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 242;
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff17242c);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0xffd9eee5, false);
        int y = 24;
        for (RadarTrack.Snapshot track : menu.tracks()) {
            g.drawString(font, track.designation() + "  " + (track.eta() + 19) / 20 + " s", 8, y, 0xff62efab, false);
            y += 10;
        }
    }
}
