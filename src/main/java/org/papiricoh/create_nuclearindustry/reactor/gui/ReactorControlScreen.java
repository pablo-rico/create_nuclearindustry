package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Reactor control panel. Readouts are drawn over the generated background texture;
 * control rods themselves are physical blocks, so the controls here only drive the SCRAM signal.
 */
public class ReactorControlScreen extends AbstractContainerScreen<ReactorControlMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/reactor_control.png");
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 316;

    private static final int TITLE_Y = 10;
    private static final int TEMP_Y = 34;
    private static final int NEUTRON_Y = 58;
    private static final int FUEL_Y = 82;
    // Seven text lines of 10 px fit between the fuel gauge (ends at 106) and the readout window's edge (180).
    private static final int POWER_Y = 108;
    private static final int STATE_Y = 118;
    private static final int STRESS_Y = 128;
    private static final int CORE_Y = 138;
    private static final int PARTS_Y = 148;
    private static final int ROD_Y = 158;
    /** Top of the fuel slots and the buttons beside them. */
    private static final int SLOT_Y = 192;
    private static final int GAUGE_WIDTH = 180;
    private static final int GAUGE_HEIGHT = 12;

    private Button scramButton;
    private Button autoButton;
    private Button thresholdDown;
    private Button thresholdUp;

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
                .bounds(leftPos + 207, topPos + SLOT_Y - 3, 41, 19)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.create_nuclearindustry.reactor.scram.tooltip")))
                .build());

        // Automatic SCRAM threshold between the fuel slots: [-] [threshold / OFF] [+]. Shift steps by 500.
        thresholdDown = addRenderableWidget(Button.builder(Component.literal("-"),
                        button -> click(hasShiftDown() ? ReactorControlMenu.BUTTON_THRESHOLD_DOWN_BIG : ReactorControlMenu.BUTTON_THRESHOLD_DOWN))
                .bounds(leftPos + 95, topPos + SLOT_Y - 3, 13, 19)
                .tooltip(Tooltip.create(Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.step.tooltip")))
                .build());
        autoButton = addRenderableWidget(Button.builder(autoLabel(), button -> click(ReactorControlMenu.BUTTON_TOGGLE_AUTO))
                .bounds(leftPos + 108, topPos + SLOT_Y - 3, 41, 19)
                .tooltip(Tooltip.create(Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.tooltip")))
                .build());
        thresholdUp = addRenderableWidget(Button.builder(Component.literal("+"),
                        button -> click(hasShiftDown() ? ReactorControlMenu.BUTTON_THRESHOLD_UP_BIG : ReactorControlMenu.BUTTON_THRESHOLD_UP))
                .bounds(leftPos + 149, topPos + SLOT_Y - 3, 13, 19)
                .tooltip(Tooltip.create(Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.step.tooltip")))
                .build());
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private Component scramLabel() {
        return Component.translatable(menu.isScramRequested()
                ? "gui.create_nuclearindustry.reactor.reset"
                : "gui.create_nuclearindustry.reactor.scram");
    }

    private Component autoLabel() {
        return menu.isAutoScramEnabled()
                ? Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.threshold", menu.getAutoScramThreshold())
                : Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.off");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        scramButton.setMessage(scramLabel());
        scramButton.active = menu.isReactorFormed();
        autoButton.setMessage(autoLabel());
        thresholdDown.active = menu.isAutoScramEnabled();
        thresholdUp.active = menu.isAutoScramEnabled();
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
        graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.reactor.fuel_in"), 20, SLOT_Y - 10, 0xFFCCCCCC, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.reactor.spent"), 164, SLOT_Y - 10, 0xFFCCCCCC, false);
        graphics.drawCenteredString(font, Component.translatable("gui.create_nuclearindustry.reactor.auto_scram"), 128, SLOT_Y - 12, 0xFFCCCCCC);

        if (menu.getReactorPos() == null || !menu.isReactorFormed()) {
            String key = menu.getReactorPos() == null
                    ? "gui.create_nuclearindustry.reactor.missing"
                    : "gui.create_nuclearindustry.reactor.unformed";
            graphics.drawCenteredString(font, Component.translatable(key), GUI_WIDTH / 2, 90, 0xFFFF5555);
            return;
        }

        double temp = menu.getCoreTemperature();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.temperature", Math.round(temp),
                Math.round(menu.getCoolantTemperature())), TEMP_Y, tempColor(temp));
        gauge(graphics, TEMP_Y + 12, (float) (temp / 4000.0));

        // Power as a share of what this core's fuel is rated for, whatever its size.
        double power = menu.getPowerFraction();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.power_level",
                String.format("%.1f", power * 100), menu.getRatedPower()), NEUTRON_Y, power > 1.05 ? 0xFFFFAA00 : 0xFFFFFF55);
        gauge(graphics, NEUTRON_Y + 12, (float) power);

        double fuel = menu.getFuelRemaining();
        double capacity = menu.getFuelCapacity();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.fuel",
                String.format("%.1f", fuel), String.format("%.1f", capacity), String.format("%.1f", menu.getFuelEnrichment())),
                FUEL_Y, 0xFF55FF55);
        gauge(graphics, FUEL_Y + 12, capacity <= 0 ? 0 : (float) (fuel / capacity));

        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.power",
                Math.round(menu.getPowerOutput()), String.format("%.1f", menu.getSteamGenerationRate())), POWER_Y, 0xFF55BBFF);
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.state", menu.getSyncedReactorStateName()),
                STATE_Y, stateColor(menu.getSyncedReactorStateName()));

        double damage = menu.getCoreDamage();
        Component stress = Component.translatable("gui.create_nuclearindustry.reactor.stress", Math.round(menu.getThermalStress()),
                Math.round(damage * 100));
        if (menu.isThermalExcursionActive()) {
            stress = stress.copy().append(" ").append(Component.translatable("gui.create_nuclearindustry.reactor.excursion"));
        }
        text(graphics, stress, STRESS_Y, damage > 0 ? 0xFFFF3333 : stressColor(menu.getThermalStress()));

        double xenon = menu.getXenonPenalty();
        int reactivity = menu.getNetReactivity();
        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.core",
                (reactivity > 0 ? "+" : "") + reactivity, Math.round(xenon * 100), Math.round(menu.getDecayHeatPower())),
                CORE_Y, xenon >= 0.15 ? 0xFFFFAA00 : 0xFFBBBBFF);

        Object[] parts = {signed(menu.getFuelReactivityPcm()), signed(menu.getRodReactivityPcm()),
                signed(menu.getTemperatureReactivityPcm()), signed(menu.getVoidReactivityPcm())};
        Component partsLine = Component.translatable("gui.create_nuclearindustry.reactor.reactivity_parts", parts);
        if (font.width(partsLine) > GAUGE_WIDTH + 43) {
            // five-digit terms do not fit the readout window with their words
            partsLine = Component.translatable("gui.create_nuclearindustry.reactor.reactivity_parts_short", parts);
        }
        text(graphics, partsLine, PARTS_Y, 0xFF8888AA);

        text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.rods",
                menu.getInsertedControlRodSegments(), menu.getExpectedControlRodSegments(),
                Math.round(menu.getControlRodInsertionRatio() * 100)), ROD_Y, 0xFFFFAA00);
        if (menu.isAutoScramTripped()) {
            text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.auto_scram.tripped", menu.getAutoScramThreshold()),
                    ROD_Y + 10, 0xFFFF5555);
        } else if (menu.isScramRequested()) {
            text(graphics, Component.translatable("gui.create_nuclearindustry.reactor.scram_engaged"), ROD_Y + 10, 0xFFFF5555);
        }
    }

    private static String signed(int pcm) {
        return (pcm > 0 ? "+" : "") + pcm;
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
