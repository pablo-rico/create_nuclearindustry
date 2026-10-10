package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sets up the windows of an annunciator panel, one tab each: the label, the alarm metric that suits
 * what the window is linked to and its threshold. Windows are linked with the Control Room Linker;
 * here they can only be unlinked. Every change goes to the server at once.
 */
public class AnnunciatorScreen extends AbstractContainerScreen<DeviceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/annunciator_panel.png");
    private static final int WIDTH = 200;
    private static final int HEIGHT = 142;
    private static final int TAB_Y = 20;
    private static final int LABEL_Y = 44;
    private static final int METRIC_Y = 62;
    private static final int INFO_Y = 88;
    private static final int UNLINK_Y = 114;
    private static final String KEY = "create_nuclearindustry.annunciator.";

    private int selected;
    private final Button[] tabs = new Button[AnnunciatorPanelBlockEntity.WINDOWS];
    private EditBox labelBox;
    private Button metricButton;
    private Button unlinkButton;
    private AlarmMetric metric = AlarmMetric.TEMPERATURE;
    private int setting;

    public AnnunciatorScreen(DeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        selected = Math.max(0, Math.min(AnnunciatorPanelBlockEntity.WINDOWS - 1, menu.part()));
    }

    private AnnunciatorPanelBlockEntity panel() {
        return menu.blockEntity() instanceof AnnunciatorPanelBlockEntity panel ? panel : null;
    }

    @Override
    protected void init() {
        super.init();
        if (panel() == null) {
            onClose();
            return;
        }
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            tabs[i] = addRenderableWidget(Button.builder(Component.literal(Integer.toString(i + 1)), b -> select(index))
                    .bounds(leftPos + 10 + i * 30, topPos + TAB_Y, 28, 18).build());
        }
        labelBox = new EditBox(font, leftPos + 11, topPos + LABEL_Y, 178, 14, Component.translatable(KEY + "label"));
        labelBox.setMaxLength(ControlModule.LABEL_MAX);
        labelBox.setHint(Component.translatable(KEY + "label.hint"));
        labelBox.setResponder(text -> {
            if (!text.equals(panel().window(selected).label())) {
                CompoundTag config = new CompoundTag();
                config.putString("Label", text);
                send(config);
            }
        });
        addRenderableWidget(labelBox);
        metricButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            List<AlarmMetric> options = metricsFor(panel().window(selected).kind());
            int at = options.indexOf(metric);
            metric = options.get(Math.floorMod(at + (hasShiftDown() ? -1 : 1), options.size()));
            CompoundTag config = new CompoundTag();
            config.putString("Metric", metric.name());
            send(config);
        }).bounds(leftPos + 10, topPos + METRIC_Y, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> step(-1)).bounds(leftPos + 134, topPos + METRIC_Y, 18, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> step(1)).bounds(leftPos + 172, topPos + METRIC_Y, 18, 20).build());
        unlinkButton = addRenderableWidget(Button.builder(Component.translatable(KEY + "unlink"), b -> {
            CompoundTag config = new CompoundTag();
            config.putBoolean("Unlink", true);
            send(config);
        }).bounds(leftPos + 10, topPos + UNLINK_Y, 90, 20).build());
        select(selected);
    }

    /** Metrics that work on a window linked to {@code kind}; all of them while it is unlinked. */
    private static List<AlarmMetric> metricsFor(TargetKind kind) {
        List<AlarmMetric> options = new ArrayList<>();
        for (AlarmMetric candidate : AlarmMetric.values()) {
            if (kind == TargetKind.NONE || candidate.watches(kind)) {
                options.add(candidate);
            }
        }
        return options.isEmpty() ? List.of(AlarmMetric.values()) : options;
    }

    private void select(int window) {
        selected = window;
        AnnunciatorPanelBlockEntity.Window setup = panel().window(window);
        metric = setup.metric();
        setting = setup.setting();
        labelBox.setValue(setup.label());
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].active = i != selected;
        }
        refresh();
    }

    private void step(int direction) {
        setting = Math.max(0, Math.min(100, setting + direction * (hasShiftDown() ? 1 : 5)));
        CompoundTag config = new CompoundTag();
        config.putInt("Setting", setting);
        send(config);
    }

    private void send(CompoundTag config) {
        refresh();
        PacketDistributor.sendToServer(new ConfigureDevicePayload(menu.pos(), selected, config));
    }

    private void refresh() {
        metricButton.setMessage(Component.translatable(KEY + "short." + metric.name().toLowerCase(Locale.ROOT)));
        unlinkButton.visible = panel() != null && panel().window(selected).target() != null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (panel() != null) {
            unlinkButton.visible = panel().window(selected).target() != null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (labelBox != null && labelBox.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            return labelBox.keyPressed(keyCode, scanCode, modifiers) || labelBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (metricButton.isHovered()) {
            graphics.renderTooltip(font, Component.translatable(metric.getTranslationKey()), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 7, 0xFFE8C26A, false);
        AnnunciatorPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
        graphics.drawCenteredString(font, metric.format(setting), 162, METRIC_Y + 6, 0xFFFFFFFF);
        AnnunciatorPanelBlockEntity.Window window = panel.window(selected);
        GlobalPos target = window.target();
        Component link;
        if (target == null) {
            link = Component.translatable(KEY + "screen.unlinked");
        } else {
            BlockPos pos = target.pos();
            link = Component.translatable(KEY + "screen.linked", Component.translatable(window.kind().translationKey()),
                    pos.getX(), pos.getY(), pos.getZ());
        }
        graphics.drawString(font, link, 10, INFO_Y, target == null ? 0xFFE8A93A : 0xFFB8F0C4, false);
        AnnunciatorLogic.State state = panel.logic().state(selected);
        Component stateText = Component.translatable(KEY + "state." + state.name().toLowerCase(Locale.ROOT));
        if (panel.logic().firstOut() == selected) {
            stateText = Component.translatable(KEY + "screen.first_out", stateText);
        }
        graphics.drawString(font, Component.translatable(KEY + "screen.state", stateText), 10, INFO_Y + 12,
                state == AnnunciatorLogic.State.NORMAL ? 0xFFB0B8B0 : 0xFFFF7A5A, false);
    }
}
