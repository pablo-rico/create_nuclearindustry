package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

/**
 * Reactor control panel. Readouts are drawn over the generated background texture;
 * control rods themselves are physical blocks, so the only control here is the SCRAM switch.
 */
public class ReactorControlScreen extends AbstractContainerScreen<ReactorControlMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/reactor_control.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 304;

    private static final int TITLE_Y = 10;
    private static final int TEMP_Y = 34;
    private static final int NEUTRON_Y = 58;
    private static final int FUEL_Y = 82;
    private static final int POWER_Y = 112;
    private static final int STATE_Y = 124;
    private static final int STRESS_Y = 136;
    private static final int ROD_Y = 148;
    private static final int GAUGE_WIDTH = 180;
    private static final int GAUGE_HEIGHT = 12;

    private Button scramButton;

    public ReactorControlScreen(ReactorControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        scramButton = addRenderableWidget(Button.builder(scramLabel(), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ReactorControlMenu.BUTTON_TOGGLE_SCRAM);
                    }
                })
                .bounds(leftPos + 207, topPos + 177, 41, 19)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.create_nuclearindustry.reactor.scram.tooltip")))
                .build());
    }

    private Component scramLabel() {
        return Component.translatable(menu.isManualScram()
                ? "gui.create_nuclearindustry.reactor.reset"
                : "gui.create_nuclearindustry.reactor.scram");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        scramButton.setMessage(scramLabel());
        scramButton.active = menu.isReactorFormed();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("gui.create_nuclearindustry.reactor.title"),
                GUI_WIDTH / 2, TITLE_Y, 0xFFE8C26A);
        graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.reactor.fuel_in"), 20, 170, 0xFFCCCCCC, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.reactor.spent"), 164, 170, 0xFFCCCCCC, false);

        if (menu.getReactorPos() == null || !menu.isReactorFormed()) {
            String key = menu.getReactorPos() == null
                    ? "gui.create_nuclearindustry.reactor.missing"
                    : "gui.create_nuclearindustry.reactor.unformed";
            graphics.drawCenteredString(font, Component.translatable(key), GUI_WIDTH / 2, 90, 0xFFFF5555);
            return;
        }

        double temp = menu.getCoreTemperature();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.temperature", Math.round(temp)), TEMP_Y, tempColor(temp));
        gauge(graphics, TEMP_Y + 12, (float) (temp / 4000.0));

        double neutrons = menu.getNeutronLevel();
        double maxNeutrons = ReactorPhysicsSimulator.getMaxNeutronLevel();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.neutrons",
                Math.round(neutrons), Math.round(maxNeutrons)), NEUTRON_Y, 0xFFFFFF55);
        gauge(graphics, NEUTRON_Y + 12, (float) (neutrons / maxNeutrons));

        double fuel = menu.getFuelRemaining();
        double capacity = menu.getFuelCapacity();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.fuel",
                String.format("%.1f", fuel), String.format("%.1f", capacity), String.format("%.1f", menu.getFuelEnrichment())),
                FUEL_Y, 0xFF55FF55);
        gauge(graphics, FUEL_Y + 12, capacity <= 0 ? 0 : (float) (fuel / capacity));

        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.power",
                String.format("%.1f", menu.getPowerOutput()), String.format("%.1f", menu.getSteamGenerationRate())), POWER_Y, 0xFF55BBFF);
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.state", menu.getSyncedReactorStateName()),
                STATE_Y, stateColor(menu.getSyncedReactorStateName()));

        Component stress = Component.translatable("gui.create_nuclearindustry.reactor.stress", Math.round(menu.getThermalStress()));
        if (menu.isThermalExcursionActive()) {
            stress = stress.copy().append(" ").append(Component.translatable("gui.create_nuclearindustry.reactor.excursion"));
        }
        text(graphics, stress, STRESS_Y, stressColor(menu.getThermalStress()));

        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.rods",
                menu.getInsertedControlRodSegments(), menu.getExpectedControlRodSegments(),
                Math.round(menu.getControlRodInsertionRatio() * 100)), ROD_Y, 0xFFFFAA00);
        if (menu.isManualScram()) {
            text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.scram_engaged"), ROD_Y + 10, 0xFFFF5555);
        }
    }

    private void text(GuiGraphics graphics, Component text, int y, int color) {
        graphics.drawString(font, text, 20, y, color, false);
    }

    private void gauge(GuiGraphics graphics, int y, float fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        int color = fraction > 0.75f ? 0xFFE04A3A : fraction > 0.5f ? 0xFFE8A93A : 0xFF6FBF4E;
        graphics.fill(20, y, 20 + Math.round(GAUGE_WIDTH * fraction), y + GAUGE_HEIGHT - 3, color);
    }

    private static int tempColor(double temp) {
        if (temp < 500) return 0xFF55FF55;
        if (temp < 2000) return 0xFFFFAA00;
        if (temp < 3500) return 0xFFFF7733;
        return 0xFFFF3333;
    }

    private static int stressColor(double stress) {
        if (stress < 35) return 0xFF55FF55;
        if (stress < 65) return 0xFFFFFF55;
        if (stress < 90) return 0xFFFFAA00;
        return 0xFFFF3333;
    }

    private static int stateColor(String state) {
        return switch (state) {
            case "Idle" -> 0xFF55CCFF;
            case "Fuel queued" -> 0xFFFFFF66;
            case "Suppressed", "Warning" -> 0xFFFFAA00;
            case "Running" -> 0xFF55FF55;
            case "Critical" -> 0xFFFF3333;
            case "MELTING" -> 0xFFFF55FF;
            default -> 0xFFCCCCCC;
        };
    }
}
