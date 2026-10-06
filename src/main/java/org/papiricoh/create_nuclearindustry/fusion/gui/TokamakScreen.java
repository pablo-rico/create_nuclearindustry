package org.papiricoh.create_nuclearindustry.fusion.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;

import static org.papiricoh.create_nuclearindustry.fusion.gui.TokamakMenu.*;

public class TokamakScreen extends AbstractContainerScreen<TokamakMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/tokamak_control.png");
    private static final int WIDTH = 248;
    private static final int HEIGHT = 196;
    private static final int GAUGE_X = 14;
    private static final int GAUGE_WIDTH = 120;

    private Button fuelButton;

    public TokamakScreen(TokamakMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        fuelButton = addRenderableWidget(Button.builder(fuelLabel(), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BUTTON_TOGGLE_FUEL);
                    }
                })
                .bounds(leftPos + 162, topPos + 168, 78, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.create_nuclearindustry.tokamak.fuel.tooltip")))
                .build());
    }

    private Component fuelLabel() {
        return Component.translatable(menu.get(FUEL_ENABLED) != 0
                ? "gui.create_nuclearindustry.tokamak.fuel_on"
                : "gui.create_nuclearindustry.tokamak.fuel_off");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        fuelButton.setMessage(fuelLabel());
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("gui.create_nuclearindustry.tokamak.title"), WIDTH / 2, 8, 0xFF8AD4F0);
        FusionReactorBlockEntity.Status status = FusionReactorBlockEntity.Status.values()[
                Math.max(0, Math.min(FusionReactorBlockEntity.Status.values().length - 1, menu.get(STATUS)))];
        if (status == FusionReactorBlockEntity.Status.UNFORMED) {
            graphics.drawCenteredString(font, Component.translatable("gui.create_nuclearindustry.tokamak.unformed"), WIDTH / 2, 90, 0xFFFF5555);
            return;
        }

        int temperature = menu.get(TEMPERATURE);
        line(graphics, 26, Component.translatable("gui.create_nuclearindustry.tokamak.temperature", temperature), temperatureColor(temperature));
        gauge(graphics, 37, temperature / TokamakPhysicsSimulator.MAX_TEMP, temperature >= TokamakPhysicsSimulator.DANGER_TEMP);

        double confinement = menu.get(CONFINEMENT_PERMILLE) / 1000.0;
        line(graphics, 48, Component.translatable("gui.create_nuclearindustry.tokamak.confinement",
                pct(confinement), pct(menu.get(COVERAGE_PERMILLE) / 1000.0), pct(menu.get(DRIVE_PERMILLE) / 1000.0)),
                confinement >= TokamakPhysicsSimulator.MIN_BURN_CONFINEMENT ? 0xFF55FF55 : 0xFFFFAA00);
        gauge(graphics, 59, confinement, false);

        int instability = menu.get(INSTABILITY);
        line(graphics, 70, Component.translatable("gui.create_nuclearindustry.tokamak.instability", instability),
                instability > 50 ? 0xFFFF5555 : 0xFFCCCCCC);
        gauge(graphics, 81, instability / 100.0, instability > 50);

        TokamakPhysicsSimulator.Mode mode = TokamakPhysicsSimulator.Mode.values()[
                Math.max(0, Math.min(2, menu.get(MODE)))];
        line(graphics, 94, Component.translatable("gui.create_nuclearindustry.tokamak.power", menu.get(POWER_MW),
                Component.translatable("gui.create_nuclearindustry.tokamak.mode." + mode.name().toLowerCase())), 0xFFFFD27C);
        line(graphics, 106, Component.translatable("gui.create_nuclearindustry.tokamak.burn",
                String.format("%.3f", menu.get(BURN_X10000) / 10000.0), String.format("%.2f", menu.get(TBR_X100) / 100.0)), 0xFFCCCCCC);
        line(graphics, 118, Component.translatable("gui.create_nuclearindustry.tokamak.structure",
                menu.get(RADIUS), menu.get(COILS), menu.get(BLANKETS)), 0xFFAAAAAA);
        if (status == FusionReactorBlockEntity.Status.SUSPENDED) {
            line(graphics, 130, Component.translatable("gui.create_nuclearindustry.tokamak.suspended"), 0xFFFFAA00);
        }

        tank(graphics, 168, menu.get(DEUTERIUM), FusionReactorBlockEntity.FUEL_TANK_CAPACITY, 0xFF5CD6FF, "D");
        tank(graphics, 186, menu.get(TRITIUM), FusionReactorBlockEntity.FUEL_TANK_CAPACITY, 0xFFFF66AA, "T");
        tank(graphics, 204, menu.get(COOLANT), FusionReactorBlockEntity.TANK_CAPACITY, 0xFF3F67C9, "C");
        tank(graphics, 222, menu.get(STEAM), FusionReactorBlockEntity.TANK_CAPACITY, 0xFFFFD27C, "S");
    }

    private void line(GuiGraphics graphics, int y, Component text, int color) {
        graphics.drawString(font, text, GAUGE_X, y, color, false);
    }

    private void gauge(GuiGraphics graphics, int y, double fraction, boolean alarm) {
        int width = (int) Math.round(GAUGE_WIDTH * Math.max(0, Math.min(1, fraction)));
        graphics.fill(GAUGE_X, y, GAUGE_X + width, y + 5, alarm ? 0xFFE04A3A : 0xFF4FB8D8);
    }

    /** Vertical tank gauge in the right-hand column. */
    private void tank(GuiGraphics graphics, int x, int amount, int capacity, int color, String label) {
        int top = 28;
        int height = 120;
        int filled = (int) Math.round(height * Math.min(1.0, amount / (double) capacity));
        graphics.fill(x, top + height - filled, x + 12, top + height, color);
        graphics.drawCenteredString(font, label, x + 6, top + height + 3, 0xFFCCCCCC);
    }

    private static String pct(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    private static int temperatureColor(int temperature) {
        if (temperature >= TokamakPhysicsSimulator.DANGER_TEMP) return 0xFFFF5555;
        if (temperature >= TokamakPhysicsSimulator.IGNITION_TEMP) return 0xFFFFAA00;
        return 0xFF8AD4F0;
    }
}
