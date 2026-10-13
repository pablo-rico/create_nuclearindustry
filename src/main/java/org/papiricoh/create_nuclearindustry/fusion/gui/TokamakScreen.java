package org.papiricoh.create_nuclearindustry.fusion.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.MagnetModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

import java.util.List;

/**
 * Tokamak control panel: phase and why it waits or tripped, Start and Shutdown, the fuelling, D:T and
 * heating setpoints, plasma, magnet and vacuum readouts, and the six tanks. Coordinates match
 * {@code tokamak_control()} in tools/texturegen/gui.py.
 */
public class TokamakScreen extends AbstractContainerScreen<TokamakMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/tokamak_control.png");
    private static final String GUI = "gui.create_nuclearindustry.tokamak.";
    static final int WIDTH = 256;
    static final int HEIGHT = 222;
    // Phase panel.
    private static final int PHASE_X = 13;
    private static final int PHASE_Y = 25;
    private static final int LAMP_X0 = 236, LAMP_Y0 = 27, LAMP_X1 = 244, LAMP_Y1 = 39;
    // Telemetry rows and gauges.
    private static final int ROW_X = 13;
    private static final int ROW_Y = 51;
    private static final int ROW_STEP = 11;
    private static final int GAUGE_X = 118;
    private static final int GAUGE_WIDTH = 48;
    private static final int GAUGE_HEIGHT = 5;
    // Tanks.
    private static final int TANK_X = 178;
    private static final int TANK_STEP = 12;
    private static final int TANK_WIDTH = 8;
    private static final int TANK_TOP = 51;
    private static final int TANK_HEIGHT = 96;
    private static final int TANK_LABEL_Y = 151;
    // Controls.
    private static final int BUTTON_Y = 170;
    private static final int SETPOINT_X0 = 12;
    private static final int SETPOINT_STEP = 78;
    private static final int SETPOINT_LABEL_Y = 189;
    private static final int SETPOINT_BUTTON_Y = 199;
    private static final int SETPOINT_VALUE_Y = 202;

    private Button startButton;
    private Button shutdownButton;
    private Button heatingButton;

    public TokamakScreen(TokamakMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        startButton = addRenderableWidget(Button.builder(Component.translatable(GUI + "start"), b -> send(TokamakControlPayload.START, 0))
                .bounds(leftPos + 12, topPos + BUTTON_Y, 56, 14)
                .tooltip(Tooltip.create(Component.translatable(GUI + "start.tooltip"))).build());
        shutdownButton = addRenderableWidget(Button.builder(Component.translatable(GUI + "shutdown"), b -> send(TokamakControlPayload.SHUTDOWN, 0))
                .bounds(leftPos + 72, topPos + BUTTON_Y, 56, 14)
                .tooltip(Tooltip.create(Component.translatable(GUI + "shutdown.tooltip"))).build());
        heatingButton = addRenderableWidget(Button.builder(heatingLabel(readings()), b -> send(TokamakControlPayload.HEATING_TOGGLE, 0))
                .bounds(leftPos + 132, topPos + BUTTON_Y, 112, 14)
                .tooltip(Tooltip.create(Component.translatable(GUI + "heating.tooltip"))).build());
        for (int i = 0; i < 3; i++) {
            int index = i;
            int x = leftPos + SETPOINT_X0 + i * SETPOINT_STEP;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> step(index, -1))
                    .bounds(x, topPos + SETPOINT_BUTTON_Y, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> step(index, 1))
                    .bounds(x + 62, topPos + SETPOINT_BUTTON_Y, 14, 14).build());
        }
    }

    private TokamakReadings readings() {
        FusionReactorBlockEntity tokamak = minecraft == null || minecraft.level == null ? null : menu.tokamak(minecraft.level);
        return tokamak == null ? TokamakReadings.NONE : tokamak.readings();
    }

    private void send(int action, double value) {
        PacketDistributor.sendToServer(new TokamakControlPayload(menu.pos(), action, value));
    }

    /** One click on a setpoint's - or + button; shift makes the step five times finer. */
    private void step(int setpoint, int direction) {
        TokamakReadings r = readings();
        double fine = Screen.hasShiftDown() ? 0.2 : 1.0;
        switch (setpoint) {
            case 0 -> send(TokamakControlPayload.FUEL_RATE, Math.max(0, r.fuelRate() + direction * fine * fuelStep(r)));
            case 1 -> send(TokamakControlPayload.DT_RATIO, clamp(r.dtRatio() + direction * fine * 0.05));
            default -> send(TokamakControlPayload.HEATING_LEVEL, clamp(r.heatingLevel() + direction * fine * 0.1));
        }
    }

    /** A twentieth of the fuelling that holds the density limit. */
    private static double fuelStep(TokamakReadings r) {
        int volume = r.majorRadius() <= 0 ? 0
                : org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry.of(Math.max(4, r.majorRadius())).plasmaCount();
        return Math.max(0.01, PlasmaModel.fuelRateFor(FusionConfig.plasmaParams(), volume, 1.0) / 20.0);
    }

    private static Component heatingLabel(TokamakReadings r) {
        return Component.translatable(r.heatingEnabled() ? GUI + "heating_on" : GUI + "heating_off");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TokamakReadings r = readings();
        heatingButton.setMessage(heatingLabel(r));
        startButton.active = r.formed() && (r.phase() == TokamakPhase.OFFLINE || r.phase() == TokamakPhase.RAMP_DOWN);
        shutdownButton.active = r.formed() && r.phase() != TokamakPhase.OFFLINE && r.phase() != TokamakPhase.RAMP_DOWN;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTankTooltip(graphics, r, mouseX - leftPos, mouseY - topPos, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, WIDTH / 2, 8, 0xFF8AD4F0);
        FusionReactorBlockEntity tokamak = minecraft == null || minecraft.level == null ? null : menu.tokamak(minecraft.level);
        TokamakReadings r = readings();
        if (tokamak == null || !r.formed()) {
            graphics.drawString(font, Component.translatable(GUI + "unformed"), PHASE_X, PHASE_Y, 0xFFFF5555, false);
            if (tokamak != null) {
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(tokamak.getValidationText(), 150);
                for (int i = 0; i < lines.size() && i < 9; i++) {
                    graphics.drawString(font, lines.get(i), ROW_X, ROW_Y + i * ROW_STEP, 0xFFCCCCCC, false);
                }
            }
            lamp(graphics, 0xFF552222);
            labelsForSetpoints(graphics, r);
            return;
        }

        // Phase and why it waits or tripped.
        int phaseColor = phaseColor(r);
        graphics.drawString(font, Component.translatable(GUI + "phase", Component.translatable(r.phase().langKey())),
                PHASE_X, PHASE_Y, phaseColor, false);
        graphics.drawString(font, phaseDetail(r), PHASE_X, PHASE_Y + 10, r.tripCause() != TripCause.NONE ? 0xFFFF7766 : 0xFFAAAAAA, false);
        lamp(graphics, phaseColor);

        // Plasma.
        row(graphics, 0, Component.translatable(GUI + "temperature", Math.round(r.temperature())), temperatureColor(r.temperature()));
        gauge(graphics, 0, r.temperature() / PlasmaModel.MAX_TEMP, r.temperature() >= PlasmaModel.DANGER_TEMP);
        row(graphics, 1, Component.translatable(GUI + "density", pct(r.density())),
                r.density() > PlasmaModel.DENSITY_LIMIT ? 0xFFFF5555 : r.density() > PlasmaModel.DENSITY_WARNING ? 0xFFFFAA00 : 0xFFCCCCCC);
        gauge(graphics, 1, r.density() / 1.2, r.density() > PlasmaModel.DENSITY_LIMIT);
        row(graphics, 2, Component.translatable(GUI + "instability", Math.round(r.instability())),
                r.instability() > PlasmaModel.INSTABILITY_WARNING ? 0xFFFF5555 : 0xFFCCCCCC);
        gauge(graphics, 2, r.instability() / PlasmaModel.DISRUPTION_INSTABILITY, r.instability() > PlasmaModel.INSTABILITY_WARNING);
        row(graphics, 3, Component.translatable(GUI + "ash_gain", pct(r.ashFraction()), FusionReactorBlockEntity.gainText(r.gain())),
                r.ignited() ? 0xFFFFD27C : 0xFFCCCCCC);
        row(graphics, 4, Component.translatable(GUI + "fusion", Math.round(r.fusionPower()),
                Component.translatable(r.fusionPower() <= 0 ? GUI + "mode.cold" : r.dtMode() ? GUI + "mode.dt" : GUI + "mode.dd")), 0xFFFFD27C);
        row(graphics, 5, Component.translatable(GUI + "heating", Math.round(r.heatingPower())), 0xFFCCCCCC);
        row(graphics, 6, Component.translatable(GUI + "field", pct(r.field()), pct(r.fieldTarget())),
                r.fieldTarget() < FusionConfig.minimumField ? 0xFFFFAA00 : 0xFFCCCCCC);
        row(graphics, 7, Component.translatable(GUI + "magnets", String.format("%.1f", r.magnetTemperature())),
                r.magnetTemperature() <= MagnetModel.READY_TEMPERATURE ? 0xFF8AD4F0 : 0xFFCCCCCC);
        row(graphics, 8, Component.translatable(GUI + "vacuum", FusionReactorBlockEntity.vacuumText(r.vacuum())),
                r.vacuum() <= FusionConfig.vacuumLimit ? 0xFF8AD4F0 : 0xFFCCCCCC);
        if (r.tritiumOverflow()) {
            row(graphics, 9, Component.translatable(GUI + "tritium_overflow"), 0xFFFFAA00);
        } else {
            row(graphics, 9, Component.translatable(GUI + "burn", String.format("%.3f", r.burnRate()),
                    String.format("%.2f", r.tritiumBreeding())), 0xFFAAAAAA);
        }

        // Tanks.
        tank(graphics, 0, r.deuterium(), FusionReactorBlockEntity.FUEL_TANK_CAPACITY, 0xFF5CD6FF, "D");
        tank(graphics, 1, r.tritium(), FusionReactorBlockEntity.FUEL_TANK_CAPACITY, r.tritiumOverflow() ? 0xFFFFAA00 : 0xFFFF66AA, "T");
        tank(graphics, 2, r.coolant(), FusionReactorBlockEntity.TANK_CAPACITY, 0xFF3F67C9, "W");
        tank(graphics, 3, r.plasmaSteam(), FusionReactorBlockEntity.TANK_CAPACITY, 0xFFFFD27C, "S");
        tank(graphics, 4, r.liquidHelium(), FusionReactorBlockEntity.HELIUM_TANK_CAPACITY, 0xFFB8F0FF, "L");
        tank(graphics, 5, r.warmHelium(), FusionReactorBlockEntity.HELIUM_TANK_CAPACITY, 0xFFE0D0A0, "G");

        labelsForSetpoints(graphics, r);
    }

    private void labelsForSetpoints(GuiGraphics graphics, TokamakReadings r) {
        String[] labels = {"fuel_rate", "dt_ratio", "heating_level"};
        String[] values = {
                String.format("%.2f", r.fuelRate()),
                Math.round(r.dtRatio() * 100) + ":" + Math.round((1 - r.dtRatio()) * 100),
                pct(r.heatingLevel())};
        for (int i = 0; i < 3; i++) {
            int center = SETPOINT_X0 + i * SETPOINT_STEP + 38;
            graphics.drawCenteredString(font, Component.translatable(GUI + labels[i]), center, SETPOINT_LABEL_Y, 0xFFAAAAAA);
            graphics.drawCenteredString(font, values[i], center, SETPOINT_VALUE_Y, 0xFFFFFFFF);
        }
    }

    private Component phaseDetail(TokamakReadings r) {
        if (r.suspended()) {
            return Component.translatable(GUI + "suspended");
        }
        if ((r.phase() == TokamakPhase.OFFLINE || r.phase() == TokamakPhase.RAMP_DOWN) && r.tripCause() != TripCause.NONE) {
            return Component.translatable(GUI + "trip", Component.translatable(r.tripCause().langKey()));
        }
        return switch (r.phase()) {
            case OFFLINE -> Component.translatable(GUI + "detail.offline");
            case PUMP_DOWN -> Component.translatable(GUI + "detail.pump_down", FusionReactorBlockEntity.vacuumText(r.vacuum()),
                    FusionReactorBlockEntity.vacuumText(FusionConfig.vacuumLimit));
            case COOL_DOWN -> Component.translatable(GUI + "detail.cool_down", String.format("%.1f", r.magnetTemperature()),
                    String.format("%.0f", MagnetModel.READY_TEMPERATURE));
            case ENERGIZE -> Component.translatable(GUI + "detail.energize", pct(r.field()), pct(r.fieldTarget()));
            case BREAKDOWN -> Component.translatable(GUI + "detail.breakdown");
            case HEATING -> Component.translatable(GUI + "detail.heating", Math.round(PlasmaModel.IGNITION_TEMP));
            case BURN -> Component.translatable(r.ignited() ? GUI + "detail.ignited" : GUI + "detail.burn");
            case RAMP_DOWN -> Component.translatable(GUI + "detail.ramp_down");
        };
    }

    private static int phaseColor(TokamakReadings r) {
        if (r.tripCause() != TripCause.NONE && r.tripCause() != TripCause.OPERATOR
                && (r.phase() == TokamakPhase.OFFLINE || r.phase() == TokamakPhase.RAMP_DOWN)) {
            return 0xFFFF5555;
        }
        return switch (r.phase()) {
            case OFFLINE -> 0xFF999999;
            case PUMP_DOWN, COOL_DOWN, ENERGIZE -> 0xFF8AD4F0;
            case BREAKDOWN -> 0xFFE070FF;
            case HEATING -> 0xFFFFAA00;
            case BURN -> 0xFFFFE070;
            case RAMP_DOWN -> 0xFFFFFF55;
        };
    }

    private void lamp(GuiGraphics graphics, int color) {
        graphics.fill(LAMP_X0, LAMP_Y0, LAMP_X1, LAMP_Y1, color);
    }

    private void row(GuiGraphics graphics, int index, Component text, int color) {
        graphics.drawString(font, text, ROW_X, ROW_Y + index * ROW_STEP, color, false);
    }

    private void gauge(GuiGraphics graphics, int index, double fraction, boolean alarm) {
        int width = (int) Math.round(GAUGE_WIDTH * Math.max(0, Math.min(1, fraction)));
        int y = ROW_Y + index * ROW_STEP + 1;
        graphics.fill(GAUGE_X, y, GAUGE_X + width, y + GAUGE_HEIGHT, alarm ? 0xFFE04A3A : 0xFF4FB8D8);
    }

    private void tank(GuiGraphics graphics, int index, int amount, int capacity, int color, String label) {
        int x = TANK_X + index * TANK_STEP;
        int filled = (int) Math.round(TANK_HEIGHT * Math.min(1.0, amount / (double) capacity));
        graphics.fill(x, TANK_TOP + TANK_HEIGHT - filled, x + TANK_WIDTH, TANK_TOP + TANK_HEIGHT, color);
        graphics.drawCenteredString(font, label, x + TANK_WIDTH / 2, TANK_LABEL_Y, 0xFFCCCCCC);
    }

    private void renderTankTooltip(GuiGraphics graphics, TokamakReadings r, int x, int y, int mouseX, int mouseY) {
        if (!r.formed() || y < TANK_TOP || y >= TANK_LABEL_Y + 9) {
            return;
        }
        int index = (x - TANK_X) / TANK_STEP;
        if (x < TANK_X || index > 5 || (x - TANK_X) % TANK_STEP >= TANK_WIDTH) {
            return;
        }
        String[] names = {"deuterium", "tritium", "coolant", "plasma_steam", "liquid_helium", "warm_helium"};
        int[] amounts = {r.deuterium(), r.tritium(), r.coolant(), r.plasmaSteam(), r.liquidHelium(), r.warmHelium()};
        int[] capacities = {FusionReactorBlockEntity.FUEL_TANK_CAPACITY, FusionReactorBlockEntity.FUEL_TANK_CAPACITY,
                FusionReactorBlockEntity.TANK_CAPACITY, FusionReactorBlockEntity.TANK_CAPACITY,
                FusionReactorBlockEntity.HELIUM_TANK_CAPACITY, FusionReactorBlockEntity.HELIUM_TANK_CAPACITY};
        graphics.renderComponentTooltip(font, List.of(Component.translatable(GUI + "tank." + names[index]),
                Component.translatable(GUI + "tank.amount", amounts[index], capacities[index])), mouseX, mouseY);
    }

    private static String pct(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static int temperatureColor(double temperature) {
        if (temperature >= PlasmaModel.DANGER_TEMP) return 0xFFFF5555;
        if (temperature >= PlasmaModel.IGNITION_TEMP) return 0xFFFFAA00;
        return 0xFF8AD4F0;
    }
}
