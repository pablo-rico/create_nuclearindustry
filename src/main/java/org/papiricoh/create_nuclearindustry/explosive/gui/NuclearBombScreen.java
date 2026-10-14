package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 10, 0xFFE066);
        NuclearBombBlockEntity bomb = menu.getBomb();
        if (bomb != null && bomb.isDirty()) {
            float load = bomb.getRadiologicalLoad();
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.dirty_bomb.load",
                    String.format("%.0f", load)), leftPos + 14, topPos + 28, 0xDDDDDD);
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.dirty_bomb.radius",
                    DirtyWarheadStats.falloutRadius(load)), leftPos + 14, topPos + 40, 0xFFAA77);
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.dirty_bomb.intensity",
                    String.format("%.1f", DirtyWarheadStats.falloutIntensity(load))), leftPos + 14, topPos + 52, 0xFF7777);
            drawState(guiGraphics, bomb, leftPos + 14, topPos + 82);
        } else if (bomb != null) {
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.cores", bomb.getCoreCount(),
                    NuclearBombBlockEntity.MAX_CORES), leftPos + 14, topPos + 28, 0xDDDDDD);
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.enrichment",
                    String.format("%.1f", bomb.getAverageEnrichment())), leftPos + 14, topPos + 40, 0xDDDDDD);
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.power",
                    String.format("%.1f", bomb.getEstimatedPower())), leftPos + 14, topPos + 52, 0xFF7777);
            guiGraphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.radius",
                    bomb.getEstimatedRadius(), bomb.getEstimatedCraterRadius()), leftPos + 14, topPos + 64, 0xFFAA77);
            drawState(guiGraphics, bomb, leftPos + 14, topPos + 82);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.hint"),
                    leftPos + imageWidth / 2, topPos + NuclearBombMenu.SLOT_Y + 20, 0x8A8A8A);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** Armed and its countdown, or waiting for a redstone signal. */
    static void drawState(GuiGraphics graphics, NuclearBombBlockEntity bomb, int x, int y) {
        int countdown = bomb.getCountdown();
        Component state = countdown >= 0
                ? Component.translatable("gui.create_nuclearindustry.bomb.armed", String.format("%.1f", countdown / 20.0f))
                : Component.translatable("gui.create_nuclearindustry.bomb.waiting");
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, state, x, y, countdown >= 0 ? 0xFF4444 : 0x88FF88);
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
            int x = leftPos + NuclearBombMenu.SLOT_X + slot * 22;
            int y = topPos + NuclearBombMenu.SLOT_Y;
            guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF555555);
            guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2F2F2F);
            guiGraphics.fill(x, y, x + 16, y + 1, 0xFF6A6A6A);
            guiGraphics.fill(x, y, x + 1, y + 16, 0xFF6A6A6A);
            guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF151515);
            guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF151515);
        }
    }
}
