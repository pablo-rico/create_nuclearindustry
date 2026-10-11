package org.papiricoh.create_nuclearindustry.accelerator.gui;

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
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.IonSourceBlockEntity;

import java.util.Locale;

/**
 * Accelerator control panel: phase and why it waits or tripped, Start and Stop, the energy and
 * current setpoints, and the beam, losses, vacuum, magnets, radio-frequency and hydrogen readouts.
 * Coordinates match {@code accelerator_control()} in tools/texturegen/tex_accelerator.py.
 */
public class AcceleratorScreen extends AbstractContainerScreen<AcceleratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/accelerator_control.png");
    private static final String GUI = "gui.create_nuclearindustry.accelerator.";
    static final int WIDTH = 256;
    static final int HEIGHT = 196;
    static final int PHASE_X = 13;
    static final int PHASE_Y = 23;
    static final int ROW_X = 13;
    static final int ROW_Y = 47;
    static final int ROW_STEP = 11;
    static final int GAUGE_X = 172;
    static final int GAUGE_WIDTH = 70;
    static final int GAUGE_HEIGHT = 5;
    static final int BUTTON_Y = 140;
    static final int SETPOINT_LABEL_Y = 160;
    static final int SETPOINT_BUTTON_Y = 171;
    static final int SETPOINT_X0 = 12;
    static final int SETPOINT_STEP = 124;

    private Button startButton;
    private Button stopButton;

    public AcceleratorScreen(AcceleratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        startButton = addRenderableWidget(Button.builder(Component.translatable(GUI + "start"), b -> send(AcceleratorControlPayload.START, 0))
                .bounds(leftPos + 12, topPos + BUTTON_Y, 112, 14)
                .tooltip(Tooltip.create(Component.translatable(GUI + "start.tooltip"))).build());
        stopButton = addRenderableWidget(Button.builder(Component.translatable(GUI + "stop"), b -> send(AcceleratorControlPayload.STOP, 0))
                .bounds(leftPos + 132, topPos + BUTTON_Y, 112, 14)
                .tooltip(Tooltip.create(Component.translatable(GUI + "stop.tooltip"))).build());
        for (int i = 0; i < 2; i++) {
            int index = i;
            int x = leftPos + SETPOINT_X0 + i * SETPOINT_STEP;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> step(index, -1))
                    .bounds(x, topPos + SETPOINT_BUTTON_Y, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> step(index, 1))
                    .bounds(x + 98, topPos + SETPOINT_BUTTON_Y, 14, 14).build());
        }
    }

    private BeamReadings readings() {
        AcceleratorControllerBlockEntity controller = minecraft == null || minecraft.level == null ? null : menu.controller(minecraft.level);
        return controller == null ? BeamReadings.NONE : controller.readings();
    }

    private void send(int action, double value) {
        PacketDistributor.sendToServer(new AcceleratorControlPayload(menu.pos(), action, value));
    }

    /** One click on a setpoint's - or + button: 5% steps, 1% with shift. */
    private void step(int setpoint, int direction) {
        BeamReadings r = readings();
        double step = (Screen.hasShiftDown() ? 0.01 : 0.05) * direction;
        if (setpoint == 0) {
            send(AcceleratorControlPayload.ENERGY, clamp(r.energySetpoint() + step));
        } else {
            send(AcceleratorControlPayload.CURRENT, clamp(r.currentSetpoint() + step));
        }
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, Math.round(value * 100) / 100.0));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BeamReadings r = readings();
        startButton.active = r.formed() && (r.phase() == BeamPhase.OFFLINE || r.phase() == BeamPhase.RAMP_DOWN);
        stopButton.active = r.formed() && r.phase() != BeamPhase.OFFLINE && r.phase() != BeamPhase.RAMP_DOWN
                && r.phase() != BeamPhase.BEAM_DUMP;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        BeamReadings r = readings();
        int x = leftPos + GAUGE_X;
        gauge(graphics, x, topPos + ROW_Y, r.maxEnergy() <= 0 ? 0 : r.energy() / r.maxEnergy(), 0xFF6B92CF);
        gauge(graphics, x, topPos + ROW_Y + ROW_STEP, r.current() / Math.max(1, AcceleratorConfig.sourceCurrent), 0xFF8AC2B7);
        gauge(graphics, x, topPos + ROW_Y + 3 * ROW_STEP, r.lossFraction(), r.lossFraction() > 0.25 ? 0xFFD88168 : 0xFFE5C567);
        gauge(graphics, x, topPos + ROW_Y + 4 * ROW_STEP, (5 - r.vacuum()) / 12.0, 0xFF8AC2B7);
        gauge(graphics, x, topPos + ROW_Y + 5 * ROW_STEP, r.magnetPower(), 0xFF97B6E3);
        gauge(graphics, x, topPos + ROW_Y + 6 * ROW_STEP, r.rfDrive(), 0xFFEDB65E);
        gauge(graphics, x, topPos + ROW_Y + 7 * ROW_STEP, r.hydrogen() / (double) IonSourceBlockEntity.CAPACITY, 0xFFDBF2DF);
    }

    private static void gauge(GuiGraphics graphics, int x, int y, double share, int color) {
        int width = (int) Math.round(Math.max(0, Math.min(1, share)) * GAUGE_WIDTH);
        if (width > 0) {
            graphics.fill(x, y, x + width, y + GAUGE_HEIGHT, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BeamReadings r = readings();
        graphics.drawString(font, title, 8, 7, 0xFF3A3F3C, false);
        int text = 0xFFDBF2DF;
        int dim = 0xFF8C9E90;
        if (!r.formed()) {
            AcceleratorControllerBlockEntity controller = minecraft == null || minecraft.level == null ? null : menu.controller(minecraft.level);
            Component why = controller == null ? Component.empty() : controller.validationText();
            graphics.drawString(font, Component.translatable(GUI + "not_formed"), PHASE_X, PHASE_Y, 0xFFD88168, false);
            graphics.drawWordWrap(font, why, ROW_X, ROW_Y, 226, dim);
            return;
        }
        Component phase = Component.translatable(r.phase().langKey());
        int phaseColor = r.phase() == BeamPhase.BEAM_ON ? 0xFFEDB65E : 0xFF8AC2B7;
        graphics.drawString(font, Component.translatable(GUI + "phase", phase), PHASE_X, PHASE_Y, phaseColor, false);
        if (r.tripCause() != BeamTripCause.NONE) {
            graphics.drawString(font, Component.translatable(r.tripCause().langKey()), PHASE_X, PHASE_Y + 10, 0xFFD88168, false);
        } else if (r.suspended()) {
            graphics.drawString(font, Component.translatable(GUI + "suspended"), PHASE_X, PHASE_Y + 10, 0xFFE5C567, false);
        }
        String[] values = {
                AcceleratorControllerBlockEntity.energyText(r.energy()),
                String.format(Locale.ROOT, "%.1f / %.1f uA", r.current(), r.sourceCurrent()),
                AcceleratorControllerBlockEntity.powerText(r.power()),
                String.format(Locale.ROOT, "%.0f%%  %s", r.lossFraction() * 100, AcceleratorControllerBlockEntity.powerText(r.lossPower())),
                AcceleratorControllerBlockEntity.vacuumText(r.vacuum()),
                String.format(Locale.ROOT, "%.0f%%  B %.0f%%", r.magnetPower() * 100, r.field() * 100),
                String.format(Locale.ROOT, "%.1f / %.1f MW", r.rfPower(), r.rfDemand()),
                r.hydrogen() + " mB"
        };
        String[] labels = {"energy", "current", "power", "losses", "vacuum", "magnets", "rf", "hydrogen"};
        for (int i = 0; i < labels.length; i++) {
            int y = ROW_Y + i * ROW_STEP - 1;
            graphics.drawString(font, Component.translatable(GUI + labels[i]), ROW_X, y, dim, false);
            graphics.drawString(font, values[i], ROW_X + 53, y, text, false);
        }
        for (int i = 0; i < 2; i++) {
            int x = SETPOINT_X0 + i * SETPOINT_STEP;
            double value = i == 0 ? r.energySetpoint() : r.currentSetpoint();
            graphics.drawString(font, Component.translatable(GUI + (i == 0 ? "energy_setpoint" : "current_setpoint")), x, SETPOINT_LABEL_Y,
                    0xFF3A3F3C, false);
            String shown = String.format(Locale.ROOT, "%.0f%%", value * 100);
            graphics.drawString(font, shown, x + 56 - font.width(shown) / 2, SETPOINT_BUTTON_Y + 3, text, false);
        }
    }
}
