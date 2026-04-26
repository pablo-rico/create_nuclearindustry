package org.papiricoh.create_nuclearindustry.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.physics.ReactorPhysicsSimulator;

/**
 * GUI screen for reactor control panel.
 * Displays temperature, neutrons, fuel, and allows manual control rod adjustment.
 */
public class ReactorControlScreen extends AbstractContainerScreen<ReactorControlMenu> {

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 220;

    // Display positions
    private static final int TITLE_Y = 10;
    private static final int TEMP_Y = 40;
    private static final int NEUTRON_Y = 70;
    private static final int FUEL_Y = 100;
    private static final int POWER_Y = 130;
    private static final int STATE_Y = 160;
    private static final int ROD_Y = 190;

    // Gauge bar width
    private static final int GAUGE_WIDTH = 180;
    private static final int GAUGE_HEIGHT = 12;

    // Control rod slider
    private float controlRodSliderValue = 0.5f;
    private boolean draggingSlider = false;
    private static final int SLIDER_Y = 200;
    private static final int SLIDER_WIDTH = 160;
    private static final int SLIDER_HEIGHT = 14;

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
            System.out.println("[ReactorControlScreen.getReactor] Level is null");
            return null;
        }

        // Priority 1: Get from menu's reactor position (most reliable)
        if (menu.getReactorPos() != null) {
            var pos = menu.getReactorPos();
            var be = minecraft.level.getBlockEntity(pos);

            if (be instanceof ReactorBlockEntity reactor) {
                System.out.println("[ReactorControlScreen.getReactor] Found reactor at " + pos + ", isFormed=" + reactor.isFormed());
                return reactor;
            } else {
                System.out.println("[ReactorControlScreen.getReactor] BlockEntity at " + pos + " is " + (be != null ? be.getClass().getSimpleName() : "null"));
            }
        }

        // Priority 2: Get from stored position if menu pos is null
        var storedPos = org.papiricoh.create_nuclearindustry.blocks.NuclearReactorControllerBlock.lastReactorMenuPos;
        if (storedPos != null) {
            var be = minecraft.level.getBlockEntity(storedPos);
            if (be instanceof ReactorBlockEntity reactor) {
                System.out.println("[ReactorControlScreen.getReactor] Found reactor at stored pos " + storedPos + ", isFormed=" + reactor.isFormed());
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
                                System.out.println("[ReactorControlScreen.getReactor] Found reactor at fallback search " + pos + ", isFormed=" + reactor.isFormed());
                                return reactor;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("[ReactorControlScreen.getReactor] No reactor found!");
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
            System.out.println("[ReactorControlScreen] Reactor not formed yet, syncWaitCounter=" + syncWaitCounter);
            if (syncWaitCounter < SYNC_WAIT_TICKS) {
                guiGraphics.drawCenteredString(this.font, "§eLoading... (" + syncWaitCounter + "/" + SYNC_WAIT_TICKS + ")", leftPos + GUI_WIDTH / 2, topPos + 90, 0xFFFFFF);
                super.render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            } else {
                System.out.println("[ReactorControlScreen] Waited " + SYNC_WAIT_TICKS + " ticks, reactor still not formed");
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
        String fuelText = String.format("Fuel: §r%.1f%%", fuel);
        guiGraphics.drawString(this.font, fuelText, leftPos + 20, topPos + FUEL_Y, 0x00FF00);
        renderGaugeBar(guiGraphics, leftPos + 20, topPos + FUEL_Y + 12, (float) (fuel / 100.0));

        // Power output
        double power = reactor.getPowerOutput();
        String powerText = String.format("Power: §r%.1f MW", power);
        guiGraphics.drawString(this.font, powerText, leftPos + 20, topPos + POWER_Y, 0x0099FF);

        // Control rod position
        float rodPos = reactor.getControlRodPosition();
        String rodText = String.format("Control Rod: §r%.0f%%", rodPos * 100);
        guiGraphics.drawString(this.font, rodText, leftPos + 20, topPos + ROD_Y, 0xFFAA00);

        // Draw slider
        renderControlRodSlider(guiGraphics, leftPos + 20, topPos + SLIDER_Y, mouseX, mouseY);

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
    private void renderControlRodSlider(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        var reactor = getReactor();
        float currentPos = reactor != null ? reactor.getControlRodPosition() : 0.5f;

        // Background
        guiGraphics.fill(x, y, x + SLIDER_WIDTH, y + SLIDER_HEIGHT, 0xFF333333);

        // Slider track
        guiGraphics.fill(x + 2, y + 5, x + SLIDER_WIDTH - 2, y + 9, 0xFF666666);

        // Slider button
        int sliderX = x + 2 + (int) ((SLIDER_WIDTH - 4) * currentPos);
        guiGraphics.fill(sliderX - 3, y, sliderX + 3, y + SLIDER_HEIGHT, draggingSlider ? 0xFFFFAA00 : 0xFFAAAAAA);

        // Labels
        guiGraphics.drawString(this.font, "Inserted", x + 5, y - 12, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "Withdrawn", x + SLIDER_WIDTH - 60, y - 12, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on control rod slider
        int sliderLeft = leftPos + 20;
        int sliderTop = topPos + SLIDER_Y;

        if (mouseX >= sliderLeft && mouseX <= sliderLeft + SLIDER_WIDTH &&
            mouseY >= sliderTop && mouseY <= sliderTop + SLIDER_HEIGHT && button == 0) {
            draggingSlider = true;
            updateSliderPosition(mouseX, sliderLeft);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlider) {
            int sliderLeft = leftPos + 20;
            updateSliderPosition(mouseX, sliderLeft);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateSliderPosition(double mouseX, int sliderLeft) {
        float newValue = (float) ((mouseX - sliderLeft - 2) / (SLIDER_WIDTH - 4));
        newValue = Math.max(0, Math.min(1, newValue));
        // TODO: Send packet to server to update reactor control rod position
        // For now, update local reactor reference if available
        var reactor = getReactor();
        if (reactor != null && reactor.isFormed()) {
            reactor.setControlRodPosition(newValue);
        }
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
            case "IDLE" -> 0xFF00CCFF;          // Cyan
            case "RUNNING" -> 0xFF00FF00;       // Green
            case "WARNING" -> 0xFFFFAA00;       // Orange
            case "CRITICAL" -> 0xFFFF0000;      // Red
            case "MELTING" -> 0xFFFF00FF;       // Magenta
            default -> 0xFFCCCCCC;              // Gray
        };
    }
}
