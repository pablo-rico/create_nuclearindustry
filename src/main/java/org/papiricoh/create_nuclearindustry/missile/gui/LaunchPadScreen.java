package org.papiricoh.create_nuclearindustry.missile.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

public class LaunchPadScreen extends AbstractContainerScreen<LaunchPadMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 210;

    public LaunchPadScreen(LaunchPadMenu menu, Inventory playerInventory, Component title) {
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
        guiGraphics.drawCenteredString(font, "Launch Pad", leftPos + imageWidth / 2, topPos + 10, 0xFFE066);
        LaunchPadBlockEntity pad = menu.getPad();
        if (pad != null) {
            int valid = pad.getValidUraniumCount();
            guiGraphics.drawString(font, "Missile: " + (pad.hasMissile() ? "ready" : "missing"), leftPos + 80, topPos + 30,
                    pad.hasMissile() ? 0x88FF88 : 0xFF7777);
            guiGraphics.drawString(font, "Uranium: " + valid, leftPos + 80, topPos + 42, 0xDDDDDD);

            BlockPos target = pad.getTarget();
            if (target != null) {
                guiGraphics.drawString(font, "Target: " + target.getX() + " " + target.getY() + " " + target.getZ(),
                        leftPos + 14, topPos + 88, 0x88CCFF);
                double distance = Math.sqrt(pad.getBlockPos().distSqr(target));
                guiGraphics.drawString(font, String.format("Distance: %.0f m", distance), leftPos + 14, topPos + 100, 0xAACCFF);
            } else {
                guiGraphics.drawString(font, "No target set", leftPos + 14, topPos + 88, 0xFF7777);
            }

            guiGraphics.drawString(font, String.format("Power: %.1f", pad.getEstimatedPower()), leftPos + 14, topPos + 100 + 12, 0xFF7777);
            guiGraphics.drawString(font, "Radius H/V: " + pad.getEstimatedRadius() + "/" + pad.getEstimatedVerticalRadius(),
                    leftPos + 14, topPos + 100 + 24, 0xFFAA77);

            int countdown = pad.getCountdown();
            String state = countdown >= 0 ? String.format("Launching: %.1fs", countdown / 20.0f)
                    : (pad.isReadyToLaunch() ? "Waiting for redstone" : "Not ready");
            guiGraphics.drawString(font, state, leftPos + 80, topPos + 30 + 24,
                    countdown >= 0 ? 0xFF4444 : (pad.isReadyToLaunch() ? 0x88FF88 : 0xFFAA55));
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
        drawSlot(guiGraphics, 26, 35);
        drawSlot(guiGraphics, 26, 62);
        for (int i = 0; i < LaunchPadBlockEntity.URANIUM_SLOTS; i++) {
            drawSlot(guiGraphics, 80 + i * 22, 62);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int relX, int relY) {
        int x = leftPos + relX;
        int y = topPos + relY;
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF555555);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2F2F2F);
        guiGraphics.fill(x, y, x + 16, y + 1, 0xFF6A6A6A);
        guiGraphics.fill(x, y, x + 1, y + 16, 0xFF6A6A6A);
        guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF151515);
        guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF151515);
    }
}
