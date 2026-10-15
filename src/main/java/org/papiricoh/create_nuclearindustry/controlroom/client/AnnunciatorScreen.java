package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPreset;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorSeverity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Wrench setup of an annunciator panel. At the top the panel's six windows as it shows them, lit by
 * their state in the colour of their severity: click one to set it up. Under them the chosen window's
 * label, alarm metric (those that suit what it is linked to), threshold and severity, and its link;
 * then the panel's group nameplate, the whole board's style and a preset that sets all six windows
 * at once. Windows are linked with the Control Room Linker; here they can only be unlinked. Every
 * change goes to the server at once as a {@link ConfigureDevicePayload}, where it is checked again.
 */
public class AnnunciatorScreen extends AbstractSimiContainerScreen<DeviceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/annunciator_panel.png");
    private static final String KEY = "create_nuclearindustry.annunciator.";
    // Keep in sync with annunciator_panel in tools/texturegen/tex_controlroom2.py.
    static final int WIDTH = 220;
    static final int HEIGHT = 246;
    static final int LABEL_X = 10;
    static final int FIELD_X = 62;
    static final int FIELD_WIDTH = 148;
    static final int PRESET_WIDTH = 126;
    static final int FIELD_HEIGHT = 14;
    /** The preview of the six windows: two columns, three rows. */
    static final int PREVIEW_X = 14;
    static final int PREVIEW_Y = 24;
    static final int PREVIEW_W = 94;
    static final int PREVIEW_H = 18;
    static final int PREVIEW_STEP_X = 98;
    static final int PREVIEW_STEP_Y = 20;
    static final int NAME_Y = 90;
    static final int METRIC_Y = 107;
    static final int SETTING_Y = 124;
    static final int SEVERITY_Y = 141;
    static final int INFO_Y = 159;
    static final int NAMEPLATE_Y = 191;
    static final int THEME_Y = 208;
    static final int PRESET_Y = 225;
    static final int BUTTON_X = 192;
    private static final int TITLE_COLOR = 0xFFE8C26A;
    private static final int LABEL_COLOR = 0xFFB0B8B0;
    private static final int FIELD_TEXT = 0xFFDDE4D8;
    private static final int SELECTED = 0xFFE8C26A;
    private static final int OFF = 0xFF2E3431;
    private static final int OFF_TEXT = 0xFF8A938C;

    private int selected;
    private AnnunciatorPreset preset = AnnunciatorPreset.REACTOR_CORE;
    private AlarmMetric metric = AlarmMetric.TEMPERATURE;
    private EditBox labelBox;
    private EditBox nameplateBox;
    private IconButton unlinkButton;
    private IconButton applyButton;

    public AnnunciatorScreen(DeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        selected = Math.max(0, Math.min(AnnunciatorPanelBlockEntity.WINDOWS - 1, menu.part()));
    }

    @Nullable
    private AnnunciatorPanelBlockEntity panel() {
        return menu.blockEntity() instanceof AnnunciatorPanelBlockEntity panel ? panel : null;
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        super.init();
        AnnunciatorPanelBlockEntity panel = panel();
        if (panel == null) {
            onClose();
            return;
        }
        AnnunciatorPanelBlockEntity.Window window = panel.window(selected);
        metric = window.metric();

        labelBox = field(NAME_Y, KEY + "label", ControlModule.LABEL_MAX, window.label(), Component.translatable(KEY + "label.hint"),
                text -> {
                    AnnunciatorPanelBlockEntity current = panel();
                    if (current != null && !ControlModule.sanitizeLabel(text).equals(current.window(selected).label())) {
                        CompoundTag config = new CompoundTag();
                        config.putString("Label", text);
                        send(config);
                    }
                });

        List<AlarmMetric> metrics = metricsFor(window.kind());
        List<Component> metricNames = new ArrayList<>();
        for (AlarmMetric candidate : metrics) {
            metricNames.add(Component.translatable(candidate.getTranslationKey()));
        }
        selector(METRIC_Y, FIELD_WIDTH, "metric", metricNames, Math.max(0, metrics.indexOf(metric)), state -> {
            metric = metrics.get(state);
            CompoundTag config = new CompoundTag();
            config.putString("Metric", metric.name());
            send(config);
            rebuildWidgets();
        });

        Label settingLabel = new Label(leftPos + FIELD_X + 4, topPos + SETTING_Y + 3, Component.empty()).colored(FIELD_TEXT);
        settingLabel.active = false;
        ScrollInput setting = new ScrollInput(leftPos + FIELD_X, topPos + SETTING_Y, FIELD_WIDTH, FIELD_HEIGHT)
                .withRange(0, 101)
                .format(value -> Component.literal(metric.isFlag() ? "-" : metric.format(value)))
                .titled(Component.translatable(KEY + "setting"))
                .writingTo(settingLabel)
                .setState(window.setting())
                .calling(value -> {
                    CompoundTag config = new CompoundTag();
                    config.putInt("Setting", value);
                    send(config);
                });
        setting.active = !metric.isFlag();
        addRenderableWidget(setting);
        addRenderableWidget(settingLabel);

        List<Component> severities = new ArrayList<>();
        for (AnnunciatorSeverity severity : AnnunciatorSeverity.values()) {
            severities.add(Component.translatable(severity.translationKey()));
        }
        selector(SEVERITY_Y, FIELD_WIDTH, "severity", severities, window.severity().ordinal(), state -> {
            CompoundTag config = new CompoundTag();
            config.putString("Severity", AnnunciatorSeverity.values()[state].name());
            send(config);
        });

        unlinkButton = new IconButton(leftPos + BUTTON_X, topPos + INFO_Y - 1, AllIcons.I_DISABLE);
        unlinkButton.withCallback(() -> {
            CompoundTag config = new CompoundTag();
            config.putBoolean("Unlink", true);
            send(config);
        });
        unlinkButton.setToolTip(Component.translatable(KEY + "unlink"));
        addRenderableWidget(unlinkButton);

        nameplateBox = field(NAMEPLATE_Y, KEY + "nameplate", ControlModule.LABEL_MAX, panel.nameplate(),
                Component.translatable(KEY + "nameplate.hint"), text -> {
                    AnnunciatorPanelBlockEntity current = panel();
                    if (current != null && !ControlModule.sanitizeLabel(text).equals(current.nameplate())) {
                        CompoundTag config = new CompoundTag();
                        config.putString("Nameplate", text);
                        send(config);
                    }
                });

        List<Component> themes = new ArrayList<>();
        for (MonitorTheme theme : MonitorTheme.values()) {
            themes.add(Component.translatable(theme.translationKey()));
        }
        selector(THEME_Y, FIELD_WIDTH, "theme", themes, panel.theme().ordinal(), state -> {
            CompoundTag config = new CompoundTag();
            config.putString("Theme", MonitorTheme.values()[state].name());
            send(config);
        });

        List<Component> presets = new ArrayList<>();
        for (AnnunciatorPreset option : AnnunciatorPreset.values()) {
            presets.add(Component.translatable(option.translationKey()));
        }
        selector(PRESET_Y, PRESET_WIDTH, "preset", presets, preset.ordinal(), state -> preset = AnnunciatorPreset.values()[state]);
        applyButton = new IconButton(leftPos + BUTTON_X, topPos + PRESET_Y - 2, AllIcons.I_CONFIRM);
        applyButton.withCallback(this::applyPreset);
        addRenderableWidget(applyButton);
        refresh();
    }

    /** An unbordered text field on the background's well, its text where the selectors write theirs. */
    private EditBox field(int y, String key, int maxLength, String value, Component hint, java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(font, leftPos + FIELD_X + 4, topPos + y + 3, FIELD_WIDTH - 8, FIELD_HEIGHT - 4, Component.translatable(key));
        box.setBordered(false);
        box.setMaxLength(maxLength);
        box.setTextColor(FIELD_TEXT);
        box.setValue(value);
        box.setHint(hint);
        box.setResponder(responder);
        return addRenderableWidget(box);
    }

    /** A field of options, stepped by clicking (back with shift) or scrolling. */
    private void selector(int y, int width, String key, List<Component> options, int state, IntConsumer onChange) {
        Label label = new Label(leftPos + FIELD_X + 4, topPos + y + 3, Component.empty()).colored(FIELD_TEXT);
        label.active = false;
        int room = width - 8;
        ScrollInput input = new MonitorConfigScreen.CyclingInput(leftPos + FIELD_X, topPos + y, width, FIELD_HEIGHT)
                .forOptions(options)
                .format(index -> fit(options.get(index), room))
                .titled(Component.translatable(KEY + key))
                .writingTo(label)
                .setState(Math.max(0, Math.min(options.size() - 1, state)))
                .calling(onChange::accept);
        addRenderableWidget(input);
        addRenderableWidget(label);
    }

    private Component fit(Component text, int room) {
        if (font.width(text) <= room) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(text.getString(), room - font.width("...")) + "...");
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

    /**
     * Sends the chosen preset for all six windows with their labels in the player's language; the
     * server takes the metrics, thresholds and severities from its own table.
     */
    private void applyPreset() {
        CompoundTag config = new CompoundTag();
        config.putString("Preset", preset.name());
        ListTag labels = new ListTag();
        for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
            labels.add(StringTag.valueOf(Component.translatable(preset.labelKey(i)).getString()));
        }
        config.put("Labels", labels);
        PacketDistributor.sendToServer(new ConfigureDevicePayload(menu.pos(), selected, config));
        pendingRebuild = 4;
    }

    /** Ticks until the fields are read again from the panel, once the server's answer has come. */
    private int pendingRebuild;

    private void select(int window) {
        if (window == selected) {
            return;
        }
        selected = window;
        rebuildWidgets();
    }

    private void send(CompoundTag config) {
        PacketDistributor.sendToServer(new ConfigureDevicePayload(menu.pos(), selected, config));
        refresh();
    }

    private void refresh() {
        AnnunciatorPanelBlockEntity panel = panel();
        if (unlinkButton != null) {
            unlinkButton.visible = panel != null && panel.window(selected).target() != null;
        }
        if (applyButton != null) {
            applyButton.setToolTip(Component.translatable(KEY + "preset.hint", Component.translatable(preset.translationKey())));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (pendingRebuild > 0 && --pendingRebuild == 0) {
            rebuildWidgets();
            return;
        }
        refresh();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
            int x = leftPos + previewX(i);
            int y = topPos + previewY(i);
            if (mouseX >= x && mouseX < x + PREVIEW_W && mouseY >= y && mouseY < y + PREVIEW_H) {
                select(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : new EditBox[]{labelBox, nameplateBox}) {
            if (box != null && box.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
                return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int previewX(int window) {
        return PREVIEW_X + (window % 2) * PREVIEW_STEP_X;
    }

    private static int previewY(int window) {
        return PREVIEW_Y + (window / 2) * PREVIEW_STEP_Y;
    }

    private static int lit(AnnunciatorSeverity severity) {
        return switch (severity) {
            case ALARM -> 0xFFE8442E;
            case WARNING -> 0xFFF2B33D;
            case STATUS -> 0xFFEDEBDF;
        };
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        AnnunciatorPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
        // The windows as the panel shows them: lit (steady here) while not normal.
        for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
            AnnunciatorPanelBlockEntity.Window window = panel.window(i);
            int x = leftPos + previewX(i);
            int y = topPos + previewY(i);
            boolean lit = panel.logic().state(i) != AnnunciatorLogic.State.NORMAL;
            if (i == selected) {
                graphics.fill(x - 2, y - 2, x + PREVIEW_W + 2, y + PREVIEW_H + 2, SELECTED);
            }
            graphics.fill(x - 1, y - 1, x + PREVIEW_W + 1, y + PREVIEW_H + 1, 0xFF141719);
            graphics.fill(x, y, x + PREVIEW_W, y + PREVIEW_H, lit ? lit(window.severity()) : OFF);
            // The severity as a stripe down the left, also when dark.
            graphics.fill(x, y, x + 2, y + PREVIEW_H, lit(window.severity()));
            if (panel.isFirstOut(i)) {
                graphics.renderOutline(x - 1, y - 1, PREVIEW_W + 2, PREVIEW_H + 2, 0xFFFFFFFF);
            }
            String text = window.target() == null ? "-" : window.label().isEmpty()
                    ? Component.translatable(KEY + "short." + window.metric().name().toLowerCase(Locale.ROOT)).getString()
                    : window.label();
            Component shown = fit(Component.literal(text.toUpperCase(Locale.ROOT)), PREVIEW_W - 8);
            graphics.drawString(font, shown, x + 2 + (PREVIEW_W - 2 - font.width(shown)) / 2, y + (PREVIEW_H - 8) / 2,
                    lit ? 0xFF1E1E1E : OFF_TEXT, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 7, TITLE_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "label"), LABEL_X, NAME_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "metric"), LABEL_X, METRIC_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "setting"), LABEL_X, SETTING_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "severity"), LABEL_X, SEVERITY_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "nameplate"), LABEL_X, NAMEPLATE_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "theme"), LABEL_X, THEME_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "preset"), LABEL_X, PRESET_Y + 3, LABEL_COLOR, false);
        AnnunciatorPanelBlockEntity panel = panel();
        if (panel == null) {
            return;
        }
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
        graphics.drawString(font, fit(link, BUTTON_X - 14), LABEL_X, INFO_Y, target == null ? 0xFFE8A93A : 0xFFB8F0C4, false);
        AnnunciatorLogic.State state = panel.logic().state(selected);
        Component stateText = Component.translatable(KEY + "state." + state.name().toLowerCase(Locale.ROOT));
        if (panel.isFirstOut(selected)) {
            stateText = Component.translatable(KEY + "screen.first_out", stateText);
        }
        graphics.drawString(font, fit(Component.translatable(KEY + "screen.state", stateText), BUTTON_X - 14), LABEL_X, INFO_Y + 12,
                state == AnnunciatorLogic.State.NORMAL ? LABEL_COLOR : 0xFFFF7A5A, false);
    }
}
