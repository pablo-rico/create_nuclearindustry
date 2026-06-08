package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;

public class NuclearBombScreen extends AbstractContainerScreen<NuclearBombMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 210;

    public NuclearBombScreen(NuclearBombMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
        guiGraphics.drawCenteredString(font, "Nuclear Bomb", leftPos + imageWidth / 2, topPos + 10, 0xFFE066);
        NuclearBombBlockEntity bomb = menu.getBomb();
        if (bomb != null) {
            int valid = bomb.getValidUraniumCount();
            guiGraphics.drawString(font, "Uranium: " + valid + "/" + NuclearBombBlockEntity.MAX_URANIUM_UNITS, leftPos + 14, topPos + 28, 0xDDDDDD);
            guiGraphics.drawString(font, String.format("Avg enrichment: %.1f%%", bomb.getAverageEnrichment()), leftPos + 14, topPos + 40, 0xDDDDDD);
            guiGraphics.drawString(font, String.format("Power: %.1f", bomb.getEstimatedPower()), leftPos + 14, topPos + 52, 0xFF7777);
            guiGraphics.drawString(font, "Radius H/V: " + bomb.getEstimatedRadius() + "/" + bomb.getEstimatedVerticalRadius(), leftPos + 14, topPos + 64, 0xFFAA77);
            int countdown = bomb.getCountdown();
            String state = countdown >= 0 ? String.format("Armed: %.1fs", countdown / 20.0f) : "Waiting for redstone";
            guiGraphics.drawString(font, state, leftPos + 14, topPos + 82, countdown >= 0 ? 0xFF4444 : 0x88FF88);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171717);
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + 1, 0xFF777777);
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + 1, topPos + imageHeight + 1, 0xFF777777);
        guiGraphics.fill(leftPos - 1, topPos + imageHeight - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF777777);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF777777);
        for (int slot = 0; slot < NuclearBombBlockEntity.SLOT_COUNT; slot++) {
            int x = leftPos + 62 + slot * 22;
            int y = topPos + 62;
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF555555);
            guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2F2F2F);
            guiGraphics.fill(x, y, x + 16, y + 1, 0xFF6A6A6A);
            guiGraphics.fill(x, y, x + 1, y + 16, 0xFF6A6A6A);
            guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF151515);
            guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF151515);
        }
    }
}
