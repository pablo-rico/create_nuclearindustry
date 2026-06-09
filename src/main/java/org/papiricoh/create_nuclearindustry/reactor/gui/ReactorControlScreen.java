package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * GUI screen for reactor control panel.
 * Displays temperature, neutrons, fuel, and allows manual control rod adjustment.
 */
public class ReactorControlScreen extends AbstractContainerScreen<ReactorControlMenu> {

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 304;

    // Display positions
    private static final int TITLE_Y = 10;
    private static final int TEMP_Y = 34;
    private static final int NEUTRON_Y = 58;
    private static final int FUEL_Y = 82;
    private static final int POWER_Y = 106;
    private static final int STATE_Y = 130;
    private static final int ROD_Y = 144;

    // Gauge bar width
    private static final int GAUGE_WIDTH = 180;
    private static final int GAUGE_HEIGHT = 12;


    // Sync wait counter - waits for BlockEntity data to sync from server
    private int syncWaitCounter = 0;
    private static final int SYNC_WAIT_TICKS = 40; // 2 seconds at 20 ticks/sec

    public ReactorControlScreen(ReactorControlMenu menu, Inventory playerInventory, net.minecraft.network.chat.Component component) {
        super(menu, playerInventory, component);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    private ReactorBlockEntity getReactor() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        // Priority 1: Get from menu's reactor position (most reliable)
        if (menu.getReactorPos() != null) {
            var pos = menu.getReactorPos();
            var be = minecraft.level.getBlockEntity(pos);

            if (be instanceof ReactorBlockEntity reactor) {
                return reactor;
            }
        }

        // Priority 2: Get from stored position if menu pos is null
        var storedPos = org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock.lastReactorMenuPos;
        if (storedPos != null) {
            var be = minecraft.level.getBlockEntity(storedPos);
            if (be instanceof ReactorBlockEntity reactor) {
                return reactor;
            }
        }

        // Priority 3: Fallback - broad search (should not be needed)
        var player = minecraft.player;
        if (player != null) {
            var playerPos = player.blockPosition();
            for (int radius = 1; radius <= 16; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                        for (int y = -8; y <= 8; y++) {
                            var pos = playerPos.offset(x, y, z);
                            var be = minecraft.level.getBlockEntity(pos);
                            if (be instanceof ReactorBlockEntity reactor) {
                                return reactor;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        // Center the GUI on screen
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        // Render title and labels
        guiGraphics.drawCenteredString(this.font, "§6REACTOR CONTROL PANEL", leftPos + GUI_WIDTH / 2, topPos + TITLE_Y, 0xFFFFFF);

        // Get fresh reactor data every frame - don't rely on cached values
        var reactor = getReactor();

        // Increment sync counter each frame
        syncWaitCounter++;

        if (reactor == null) {
            guiGraphics.drawCenteredString(this.font, "§cReactor Not Found", leftPos + GUI_WIDTH / 2, topPos + 90, 0xFF0000);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // If reactor not formed, wait a moment for sync before showing error
        if (!reactor.isFormed()) {
            if (syncWaitCounter < SYNC_WAIT_TICKS) {
                guiGraphics.drawCenteredString(this.font, "§eLoading... (" + syncWaitCounter + "/" + SYNC_WAIT_TICKS + ")", leftPos + GUI_WIDTH / 2, topPos + 90, 0xFFFFFF);
                super.render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            } else {
                guiGraphics.drawCenteredString(this.font, "§cReactor Not Formed", leftPos + GUI_WIDTH / 2, topPos + 90, 0xFF0000);
                super.render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            }
        }

        // Reset counter once reactor is formed
        syncWaitCounter = 0;

        // Reactor status
        String state = reactor.getReactorState().getDisplayName();
        int stateColor = getStateColor(state);
        guiGraphics.drawString(this.font, "State: §r" + state, leftPos + 20, topPos + STATE_Y, stateColor);

        // Temperature (get fresh data)
        double temp = reactor.getCoreTemperature();
        String tempText = String.format("Temperature: §r%.0f°C", temp);
        int tempColor = getTempColor((float) temp);
        guiGraphics.drawString(this.font, tempText, leftPos + 20, topPos + TEMP_Y, tempColor);
        renderGaugeBar(guiGraphics, leftPos + 20, topPos + TEMP_Y + 12, (float) (temp / 4000.0));

        // Neutron level (get fresh data)
        double neutrons = reactor.getNeutronLevel();
        String neutronText = String.format("Neutrons: §r%.0f / 1000", neutrons);
        guiGraphics.drawString(this.font, neutronText, leftPos + 20, topPos + NEUTRON_Y, 0xFFFF00);
        renderGaugeBar(guiGraphics, leftPos + 20, topPos + NEUTRON_Y + 12, (float) (neutrons / 1000.0));

        // Fuel remaining (get fresh data)
        double fuel = reactor.getFuelRemaining();
        double fuelCapacity = reactor.getFuelCapacity();
        float fuelPercent = fuelCapacity <= 0.0 ? 0.0f : (float) (fuel / fuelCapacity);
        String fuelText = String.format("Fuel: §r%.1f / %.1f units", fuel, fuelCapacity);
        guiGraphics.drawString(this.font, fuelText, leftPos + 20, topPos + FUEL_Y, 0x00FF00);
        renderGaugeBar(guiGraphics, leftPos + 20, topPos + FUEL_Y + 12, fuelPercent);

        // Power output
        double power = reactor.getPowerOutput();
        String powerText = String.format("Power: §r%.1f MW  Steam: %.1f mB/t", power, reactor.getSteamGenerationRate());
        guiGraphics.drawString(this.font, powerText, leftPos + 20, topPos + POWER_Y, 0x0099FF);

        String rodText = String.format("Rods: §r%d/%d inserted §8(static %d, moving %d)",
                reactor.getInsertedControlRodSegments(),
                reactor.getExpectedControlRodSegments(),
                reactor.getStaticControlRodSegments(),
                reactor.getMovingControlRodSegments());
        guiGraphics.drawString(this.font, rodText, leftPos + 20, topPos + ROD_Y, 0xFFAA00);
        guiGraphics.drawString(this.font, String.format("Rod insertion: %.0f%%", reactor.getControlRodInsertionRatio() * 100.0f),
                leftPos + 20, topPos + ROD_Y + 12, 0xFFAA00);
        if (fuel > 0.0 && neutrons <= 1.0 && reactor.getControlRodInsertionRatio() >= 0.99f) {
            guiGraphics.drawString(this.font, "Loaded: rods suppressing reaction", leftPos + 20, topPos + ROD_Y + 24, 0xFFFFAA00);
        }
        guiGraphics.drawString(this.font, "Fuel In", leftPos + 20, topPos + 168, 0xCCCCCC);
        guiGraphics.drawString(this.font, "Spent", leftPos + 164, topPos + 168, 0xCCCCCC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw background panel
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1a1a1a);

        // Draw dark gray item slot areas
        guiGraphics.fill(leftPos + 12, topPos + 158, leftPos + 116, topPos + 206, 0xFF2B2B2B);
        guiGraphics.fill(leftPos + 156, topPos + 158, leftPos + 216, topPos + 206, 0xFF2B2B2B);
        guiGraphics.fill(leftPos + 40, topPos + 214, leftPos + 218, topPos + 294, 0xFF2B2B2B);

        // Draw border
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + 1, 0xFFAAAAAA);
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + 1, topPos + imageHeight + 1, 0xFFAAAAAA);
        guiGraphics.fill(leftPos - 1, topPos + imageHeight - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFFAAAAAA);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFFAAAAAA);
    }

    /**
     * Renders a horizontal gauge bar.
     */
    private void renderGaugeBar(GuiGraphics guiGraphics, int x, int y, float percentage) {
        percentage = Math.max(0, Math.min(1, percentage));

        // Background
        guiGraphics.fill(x, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, 0xFF333333);

        // Filled portion
        int filledWidth = (int) (GAUGE_WIDTH * percentage);
        int color = 0xFF00FF00;
        if (percentage > 0.75f) {
            color = 0xFFFF0000;
        } else if (percentage > 0.5f) {
            color = 0xFFFFAA00;
        }
        guiGraphics.fill(x, y, x + filledWidth, y + GAUGE_HEIGHT, color);

        // Border
        guiGraphics.fill(x - 1, y - 1, x + GAUGE_WIDTH + 1, y + 1, 0xFFAAAAAA);
        guiGraphics.fill(x - 1, y - 1, x + 1, y + GAUGE_HEIGHT + 1, 0xFFAAAAAA);
        guiGraphics.fill(x - 1, y + GAUGE_HEIGHT - 1, x + GAUGE_WIDTH + 1, y + GAUGE_HEIGHT + 1, 0xFFAAAAAA);
        guiGraphics.fill(x + GAUGE_WIDTH - 1, y - 1, x + GAUGE_WIDTH + 1, y + GAUGE_HEIGHT + 1, 0xFFAAAAAA);
    }

    /**
     * Renders the control rod position slider.
     */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        return super.mouseClicked(mouseX, mouseY, button);
    }




    /**
     * Gets the color for temperature gauge.
     */
    private int getTempColor(float temp) {
        if (temp < 500) return 0xFF00FF00;      // Green
        if (temp < 2000) return 0xFFFFAA00;     // Orange
        if (temp < 3500) return 0xFFFF5500;     // Red-orange
        return 0xFFFF0000;                      // Red
    }

    /**
     * Gets the color for reactor state text.
     */
    private int getStateColor(String state) {
        return switch (state) {
            case "Idle" -> 0xFF00CCFF;          // Cyan
            case "Fuel queued" -> 0xFFFFFF66;   // Yellow
            case "Suppressed" -> 0xFFFFAA00;    // Orange
            case "Running" -> 0xFF00FF00;       // Green
            case "Warning" -> 0xFFFFAA00;       // Orange
            case "Critical" -> 0xFFFF0000;      // Red
            case "MELTING" -> 0xFFFF00FF;       // Magenta
            default -> 0xFFCCCCCC;              // Gray
        };
    }
}
