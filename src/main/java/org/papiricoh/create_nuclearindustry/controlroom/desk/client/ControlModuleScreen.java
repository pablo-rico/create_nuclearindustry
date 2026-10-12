package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.client.widget.OptionList;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ConfigureModulePayload;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ConsoleSummary;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleMenu;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleChoices;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Sets up one control module. The header names the module and the machine its console is linked to,
 * and warns in red when the chosen function does nothing there. The left list holds the module's
 * functions (those the machine cannot use greyed out, with what they need); the right side follows the
 * function: the alarms or readings that machine has, each with its live value, or a controller's mode
 * and a description. Below sit the label, the threshold or setpoint (scroll it), the three redstone
 * outputs and, beside the inventory, the link frequency and the module's live signal.
 *
 * <p>Edits a local copy and sends the whole setup to the server on every change; the server sanitises
 * it as before. Live values come from the console's sample, resent while the screen is open.
 */
public class ControlModuleScreen extends AbstractSimiContainerScreen<ControlModuleMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/control_module.png");
    // Coordinates match @gui("control_module") in tools/texturegen/gui.py.
    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;
    private static final int HEADER_LEFT = 9, HEADER_RIGHT = 246, HEADER_LINE_1 = 7, HEADER_LINE_2 = 17;
    private static final int CAPTION_Y = 30;
    private static final int LIST_Y = 40, LIST_HEIGHT = 84;
    private static final int FUNCTIONS_X = 7, FUNCTIONS_WIDTH = 98;
    private static final int OPTIONS_X = 110, OPTIONS_WIDTH = 139;
    private static final int CONTROLS_Y = 128;
    private static final int LABEL_BOX_X = 7, LABEL_BOX_WIDTH = 80;
    private static final int SETTING_X = 91, SETTING_Y = 129, SETTING_WIDTH = 96, SETTING_HEIGHT = 16;
    private static final int OUTPUT_X = 193, OUTPUT_STEP = 19;
    private static final int LINK_ICON_X = 14, LINK_ICON_Y = 156;
    private static final int FREQUENCY_X = 14, FREQUENCY_FIRST_Y = 178, FREQUENCY_LAST_Y = 200;
    /** The signal meter's inner trough: 15 steps of 4 px, filled from the bottom. */
    private static final int METER_LEFT = 231, METER_RIGHT = 239, METER_BOTTOM = 228, METER_STEP = 4;
    private static final int METER_AREA_LEFT = 221, METER_AREA_RIGHT = 249, METER_AREA_TOP = 151, METER_AREA_BOTTOM = 235;
    private static final int METER_TEXT_Y = 155;

    private static final int COLOR_TEXT = 0xFFE6EAE2;
    private static final int COLOR_CAPTION = 0xFFF2F4EE;
    private static final int COLOR_DIM = 0xFF9AA59C;
    private static final int COLOR_TITLE = 0xFFE8C26A;
    private static final int COLOR_GOOD = 0xFF7FD46A;
    private static final int COLOR_WARN = 0xFFF0C050;
    private static final int COLOR_BAD = 0xFFFF6B5B;
    private static final String KEY = "create_nuclearindustry.control_module.";
    private static final String SCREEN = KEY + "screen.";

    private ControlModule module;
    private ConsoleSummary summary = ConsoleSummary.NONE;
    private ItemStack linkIcon = ItemStack.EMPTY;

    private OptionList<ModuleFunction> functionList;
    private OptionList<AlarmMetric> metricList;
    private OptionList<GaugeReading> readingList;
    private IconButton modeButton;
    private EditBox labelBox;
    private SettingInput settingInput;
    private Label settingLabel;
    private final IconButton[] outputButtons = new IconButton[OutputMode.values().length];

    public ControlModuleScreen(ControlModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        super.init();
        ControlModule live = menu.module();
        if (live == null || minecraft == null || minecraft.level == null) {
            onClose();
            return;
        }
        if (module == null) {
            // Kept across a resize: the server may not have echoed the last change yet.
            module = ControlModule.read(live.write(minecraft.level.registryAccess()), minecraft.level.registryAccess());
        }
        summary = menu.consoleSummary();
        linkIcon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "redstone_link")));

        functionList = new OptionList<ModuleFunction>(leftPos + FUNCTIONS_X, topPos + LIST_Y, FUNCTIONS_WIDTH, LIST_HEIGHT)
                .onSelect(this::pickFunction);
        metricList = new OptionList<AlarmMetric>(leftPos + OPTIONS_X, topPos + LIST_Y, OPTIONS_WIDTH, LIST_HEIGHT)
                .onSelect(metric -> {
                    module.withMetric(metric);
                    changed();
                })
                .withEmptyText(Component.translatable(SCREEN + "no_metrics"));
        readingList = new OptionList<GaugeReading>(leftPos + OPTIONS_X, topPos + LIST_Y, OPTIONS_WIDTH, LIST_HEIGHT)
                .onSelect(reading -> {
                    module.withReading(reading);
                    changed();
                })
                .withEmptyText(Component.translatable(SCREEN + "no_readings"));

        modeButton = new IconButton(leftPos + OPTIONS_X + 3, topPos + LIST_Y + 3, AllIcons.I_PLAY);
        modeButton.withCallback(() -> {
            module.withEngaged(!module.engaged());
            changed();
        });

        labelBox = new EditBox(font, leftPos + LABEL_BOX_X, topPos + CONTROLS_Y, LABEL_BOX_WIDTH, 18, Component.translatable(KEY + "label"));
        labelBox.setMaxLength(ControlModule.LABEL_MAX);
        labelBox.setValue(module.label());
        labelBox.setHint(Component.translatable(KEY + "label.hint"));
        labelBox.setResponder(text -> {
            if (!text.equals(module.label())) {
                module.withLabel(text);
                changed();
            }
        });

        settingLabel = new Label(leftPos + SETTING_X, topPos + SETTING_Y + 4, CommonComponents.EMPTY).colored(0xFFFFFF).withShadow();
        settingLabel.text = CommonComponents.EMPTY;
        settingInput = new SettingInput(leftPos + SETTING_X, topPos + SETTING_Y, SETTING_WIDTH, SETTING_HEIGHT);
        settingInput.withShiftStep(1)
                .withStepFunction(context -> ModuleChoices.coarseSetting(module.function()) && !context.shift ? 5 : 1)
                .calling(value -> {
                    module.withSetting(value);
                    changed();
                });

        OutputMode[] modes = OutputMode.values();
        for (int i = 0; i < modes.length; i++) {
            OutputMode mode = modes[i];
            IconButton button = new IconButton(leftPos + OUTPUT_X + i * OUTPUT_STEP, topPos + CONTROLS_Y, outputIcon(mode));
            button.withCallback(() -> {
                if (module.output() != mode) {
                    module.withOutput(mode);
                    changed();
                }
            });
            button.getToolTip().add(Component.translatable(KEY + "output", Component.translatable(mode.translationKey()))
                    .withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));
            button.getToolTip().add(Component.translatable(SCREEN + "output_hint." + mode.name().toLowerCase(Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
            outputButtons[i] = button;
        }

        addRenderableWidgets(functionList, metricList, readingList);
        addRenderableWidget(modeButton);
        addRenderableWidget(labelBox);
        addRenderableWidgets(settingInput, settingLabel);
        addRenderableWidgets(outputButtons);
        rebuildOptions(true);
        refresh();
    }

    // ------------------------------------------------------------------ choices

    private void pickFunction(ModuleFunction function) {
        module.withFunction(function);
        // Coming to an alarm or a gauge whose metric or reading does not suit the machine: start from
        // the first one that does, rather than a dark module.
        if (summary.linked() && function == ModuleFunction.LAMP_ALARM && !module.metric().watches(summary.kind())) {
            metricsFor(summary.kind()).stream().findFirst().ifPresent(module::withMetric);
        }
        if (summary.linked() && function == ModuleFunction.READING && module.reading().kind() != summary.kind()) {
            GaugeReading.forKind(summary.kind()).stream().findFirst().ifPresent(module::withReading);
        }
        changed();
        rebuildOptions(true);
    }

    /**
     * Fills the three lists for the console's machine, keeping the module's own choices visible;
     * {@code resetScroll} sends the alarm and reading lists back to the top.
     */
    private void rebuildOptions(boolean resetScroll) {
        TargetKind kind = summary.kind();
        boolean linked = summary.linked();

        List<OptionList.Entry<ModuleFunction>> functions = new ArrayList<>();
        for (ModuleFunction function : module.type().functions()) {
            ModuleChoices.Fit fit = ModuleChoices.fit(function, kind, summary.pressurizer());
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(function.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
            Component needs = needsText(function, fit);
            if (needs != null) {
                tooltip.add(needs.copy().withStyle(ChatFormatting.RED));
            }
            functions.add(OptionList.Entry.of(function, Component.translatable(function.translationKey()))
                    .withIcon(functionIcon(function))
                    .withAvailable(fit == ModuleChoices.Fit.OK)
                    .withTooltip(tooltip));
        }
        // The function list keeps its place: the player is clicking in it.
        functionList.setEntries(functions);
        functionList.setSelected(module.function());

        List<OptionList.Entry<AlarmMetric>> metrics = new ArrayList<>();
        List<AlarmMetric> offered = new ArrayList<>(linked ? metricsFor(kind) : Arrays.asList(AlarmMetric.values()));
        if (!offered.contains(module.metric())) {
            offered.add(0, module.metric());
        }
        for (AlarmMetric metric : offered) {
            boolean available = !linked || metric.watches(kind);
            OptionList.Entry<AlarmMetric> entry = OptionList.Entry.of(metric, Component.translatable(metric.getTranslationKey()))
                    .withIcon(metric.getIcon())
                    .withValueText(() -> metricValue(metric))
                    .withAvailable(available);
            if (!available) {
                entry = entry.withTooltip(needsKind(metric.watches()).copy().withStyle(ChatFormatting.RED));
            }
            metrics.add(entry);
        }
        metricList.setEntries(metrics, resetScroll);
        metricList.setSelected(module.metric());

        List<OptionList.Entry<GaugeReading>> readings = new ArrayList<>();
        List<GaugeReading> shown = new ArrayList<>(linked ? GaugeReading.forKind(kind) : Arrays.asList(GaugeReading.values()));
        if (!shown.contains(module.reading())) {
            shown.add(0, module.reading());
        }
        for (GaugeReading reading : shown) {
            boolean available = !linked || reading.kind() == kind;
            OptionList.Entry<GaugeReading> entry = OptionList.Entry.of(reading, Component.translatable(reading.translationKey()))
                    .withValueText(() -> readingValue(reading))
                    .withAvailable(available);
            entry = available
                    ? entry.withTooltip(Component.translatable(reading.kind().translationKey()).withStyle(ChatFormatting.GRAY))
                    : entry.withTooltip(needsKind(reading.kind()).copy().withStyle(ChatFormatting.RED));
            readings.add(entry);
        }
        readingList.setEntries(readings, resetScroll);
        readingList.setSelected(module.reading());
    }

    /** Alarm metrics a console linked to {@code kind} can set off. */
    private static List<AlarmMetric> metricsFor(TargetKind kind) {
        return Arrays.stream(AlarmMetric.values()).filter(metric -> metric.watches(kind)).toList();
    }

    /** "Needs: Tokamak" for a function the machine cannot use, or null when it can. */
    @Nullable
    private static Component needsText(ModuleFunction function, ModuleChoices.Fit fit) {
        return switch (fit) {
            case OK -> null;
            case NO_PRESSURIZER -> Component.translatable(SCREEN + "needs_pressurizer");
            case WRONG_MACHINE -> {
                TargetKind needs = ModuleChoices.needs(function);
                yield needs != null ? needsKind(needs) : Component.translatable(SCREEN + "needs_machine");
            }
        };
    }

    private static Component needsKind(TargetKind kind) {
        return Component.translatable(SCREEN + "needs", Component.translatable(kind.translationKey()));
    }

    /** The live value of a metric on the console's machine, red when it would light the lamp as set. */
    @Nullable
    private Component metricValue(AlarmMetric metric) {
        if (!summary.linked()) {
            return null;
        }
        MonitorReading sample = summary.sample();
        float value = metric.liveValue(sample);
        if (Float.isNaN(value)) {
            return Component.literal("-");
        }
        MutableComponent text = metric.isFlag()
                ? Component.translatable(SCREEN + (value > 0 ? "flag_on" : "flag_off"))
                : Component.literal(metric.formatValue(value));
        if (metric == module.metric() && metric.tripped(sample, module.setting())) {
            text = text.withStyle(ChatFormatting.RED);
        }
        return text;
    }

    /** Where a gauge's needle stands now on the console's machine, as a share of full scale. */
    @Nullable
    private Component readingValue(GaugeReading reading) {
        if (!summary.linked() || summary.sample().kind() != reading.kind()) {
            return null;
        }
        return Component.literal(Math.round(reading.fraction(summary.sample()) * 100) + "%");
    }

    // ------------------------------------------------------------------ changes

    private void changed() {
        refresh();
        if (minecraft != null && minecraft.level != null) {
            PacketDistributor.sendToServer(new ConfigureModulePayload(menu.deskPos(), menu.slot(),
                    module.writeConfig(minecraft.level.registryAccess())));
        }
    }

    /** Shows the widgets the function uses and brings them in line with the local copy. */
    private void refresh() {
        ModuleFunction function = module.function();
        boolean alarm = function == ModuleFunction.LAMP_ALARM;
        metricList.visible = alarm;
        readingList.visible = function == ModuleFunction.READING;
        modeButton.visible = module.isController();
        modeButton.setIcon(module.engaged() ? AllIcons.I_PLAY : AllIcons.I_PAUSE);
        modeButton.green = module.engaged();
        modeButton.setToolTip(Component.translatable(KEY + (module.engaged() ? "mode.auto" : "mode.standby"))
                .withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));
        modeButton.getToolTip().add(Component.translatable(SCREEN + "mode_hint").withStyle(ChatFormatting.GRAY));
        metricList.setSelected(module.metric());
        readingList.setSelected(module.reading());

        boolean setting = ModuleChoices.hasSetting(function);
        settingInput.visible = setting;
        settingInput.active = setting && !(alarm && module.metric().isFlag());
        settingLabel.visible = setting;
        settingInput.withRange(0, module.maxSetting() + 1);
        settingInput.setState(module.setting());
        settingLabel.text = Component.literal(settingText());
        settingLabel.setX(leftPos + SETTING_X + SETTING_WIDTH - 4 - font.width(settingLabel.text));

        OutputMode[] modes = OutputMode.values();
        for (int i = 0; i < modes.length; i++) {
            outputButtons[i].green = module.output() == modes[i];
        }
    }

    private String settingText() {
        ModuleFunction function = module.function();
        if (function == ModuleFunction.LAMP_ALARM) {
            return module.metric().isFlag() ? "-" : module.metric().format(module.setting());
        }
        if (function == ModuleFunction.SIGNAL_LEVEL) {
            return Integer.toString(module.setting());
        }
        return module.isController() ? module.formatSetpoint() : "";
    }

    private Component settingCaption() {
        ModuleFunction function = module.function();
        return Component.translatable(SCREEN + (function == ModuleFunction.LAMP_ALARM ? "threshold"
                : function == ModuleFunction.SIGNAL_LEVEL ? "level" : "setpoint"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (module == null) {
            return;
        }
        ConsoleSummary now = menu.consoleSummary();
        boolean machineChanged = now.kind() != summary.kind() || now.pressurizer() != summary.pressurizer();
        summary = now;
        if (machineChanged) {
            rebuildOptions(false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (module != null && module.output() == OutputMode.LINK) {
            boolean first = inBox(mouseX, mouseY, FREQUENCY_X, FREQUENCY_FIRST_Y, 16, 16);
            boolean last = inBox(mouseX, mouseY, FREQUENCY_X, FREQUENCY_LAST_Y, 16, 16);
            if (first || last) {
                ItemStack carried = menu.getCarried();
                module.withFrequencies(first ? carried : module.frequencyFirst(), last ? carried : module.frequencyLast());
                changed();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inBox(double mouseX, double mouseY, int x, int y, int width, int height) {
        double localX = mouseX - leftPos - x;
        double localY = mouseY - topPos - y;
        return localX >= 0 && localX < width && localY >= 0 && localY < height;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        if (module == null) {
            return;
        }
        boolean link = module.output() == OutputMode.LINK;
        graphics.renderItem(linkIcon, leftPos + LINK_ICON_X, topPos + LINK_ICON_Y);
        if (link) {
            graphics.renderItem(module.frequencyFirst(), leftPos + FREQUENCY_X, topPos + FREQUENCY_FIRST_Y);
            graphics.renderItem(module.frequencyLast(), leftPos + FREQUENCY_X, topPos + FREQUENCY_LAST_Y);
        } else {
            // Dim the link column: it only matters with the Redstone Link output.
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(leftPos + 9, topPos + 151, leftPos + 36, topPos + 235, 0xA0101614);
            graphics.pose().popPose();
        }
        ControlModule live = menu.module();
        int signal = live != null ? live.signal() : 0;
        if (signal > 0) {
            int top = METER_BOTTOM - signal * METER_STEP;
            graphics.fillGradient(leftPos + METER_LEFT, topPos + top, leftPos + METER_RIGHT, topPos + METER_BOTTOM, 0xFFFF4A3A, 0xFF8E1A12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (module == null) {
            return;
        }
        renderHeader(graphics);
        graphics.drawString(font, Component.translatable(SCREEN + "functions"), FUNCTIONS_X + 1, CAPTION_Y, COLOR_CAPTION, true);
        graphics.drawString(font, optionsCaption(), OPTIONS_X + 1, CAPTION_Y, COLOR_CAPTION, true);
        if (!metricList.visible && !readingList.visible) {
            renderAbout(graphics);
        }
        if (settingInput.visible) {
            Component caption = settingCaption();
            int room = SETTING_WIDTH - 8 - font.width(settingLabel.text);
            if (font.width(caption) <= room) {
                graphics.drawString(font, caption, SETTING_X + 3, SETTING_Y + 4, settingInput.active ? COLOR_DIM : 0xFF5E6660, false);
            }
        } else {
            graphics.drawCenteredString(font, Component.translatable(SCREEN + "no_setting"), SETTING_X + SETTING_WIDTH / 2, SETTING_Y + 4, 0xFF5E6660);
        }
        ControlModule live = menu.module();
        String signal = Integer.toString(live != null ? live.signal() : 0);
        graphics.drawCenteredString(font, signal, (METER_AREA_LEFT + METER_AREA_RIGHT + 1) / 2, METER_TEXT_Y, 0xFFFF6B5B);
    }

    /** Module type and console on top; under them what is wrong (or right) and the machine's status. */
    private void renderHeader(GuiGraphics graphics) {
        int room = HEADER_RIGHT - HEADER_LEFT;
        Component console = Component.translatable(SCREEN + "console", Component.translatable(summary.kind().translationKey()));
        int consoleWidth = Math.min(font.width(console), room / 2);
        graphics.drawString(font, fit(console, consoleWidth), HEADER_RIGHT - consoleWidth, HEADER_LINE_1, COLOR_TEXT, false);
        graphics.drawString(font, fit(Component.translatable(module.type().translationKey()), room - consoleWidth - 6),
                HEADER_LEFT, HEADER_LINE_1, COLOR_TITLE, false);

        Component status = statusText();
        int statusWidth = status == null ? 0 : Math.min(font.width(status), room / 2);
        if (status != null) {
            graphics.drawString(font, fit(status, statusWidth), HEADER_RIGHT - statusWidth, HEADER_LINE_2, statusColor(), false);
        }
        Component warning = warning();
        Component verdict = warning != null ? warning
                : Component.translatable(SCREEN + (summary.linked() ? "works" : "unlinked"));
        int verdictColor = warning != null ? COLOR_BAD : summary.linked() ? COLOR_GOOD : COLOR_DIM;
        graphics.drawString(font, fit(verdict, room - statusWidth - 6), HEADER_LEFT, HEADER_LINE_2, verdictColor, false);
    }

    /** Why the module as set up does nothing on this console, or null when it works (or nothing is linked). */
    @Nullable
    private Component warning() {
        if (!summary.linked()) {
            return null;
        }
        ModuleFunction function = module.function();
        ModuleChoices.Fit fit = ModuleChoices.fit(function, summary.kind(), summary.pressurizer());
        if (fit == ModuleChoices.Fit.NO_PRESSURIZER) {
            return Component.translatable(SCREEN + "warning.pressurizer");
        }
        TargetKind needs = fit == ModuleChoices.Fit.WRONG_MACHINE ? ModuleChoices.needs(function)
                : function == ModuleFunction.LAMP_ALARM && !module.metric().watches(summary.kind()) ? module.metric().watches()
                : function == ModuleFunction.READING && module.reading().kind() != summary.kind() ? module.reading().kind()
                : null;
        if (fit == ModuleChoices.Fit.WRONG_MACHINE && needs == null) {
            return Component.translatable(SCREEN + "warning.running");
        }
        return needs == null ? null
                : Component.translatable(SCREEN + "warning.machine", Component.translatable(needs.translationKey()));
    }

    /** The machine's status, as its monitors word it, or null while unlinked. */
    @Nullable
    private Component statusText() {
        String status = summary.status();
        return switch (summary.kind()) {
            case NONE -> null;
            case NO_SIGNAL -> Component.translatable("create_nuclearindustry.monitor.no_signal." + status);
            default -> status.startsWith(Create_NuclearIndustry.MODID + ".") ? Component.translatable(status) : Component.literal(status);
        };
    }

    /** Status colours as on the Control Room Monitor: red for trouble, green for running, amber otherwise. */
    private int statusColor() {
        String status = summary.status();
        if (summary.kind() == TargetKind.NO_SIGNAL || status.contains("SCRAM") || status.equals("MELTING") || status.equals("Critical")
                || status.endsWith(".radiation") || status.endsWith(".tripped")) {
            return COLOR_BAD;
        }
        if (status.equals("Running") || status.endsWith(".running") || status.endsWith(".clear") || status.endsWith(".burning")) {
            return COLOR_GOOD;
        }
        return COLOR_WARN;
    }

    private Component optionsCaption() {
        ModuleFunction function = module.function();
        return Component.translatable(SCREEN + (function == ModuleFunction.LAMP_ALARM ? "metrics"
                : function == ModuleFunction.READING ? "readings"
                : module.isController() ? "controller" : "about"));
    }

    /**
     * The right panel when the function has nothing to pick: a controller's mode, what the function does
     * and, for a controller, its setpoint and what it is asking for now.
     */
    private void renderAbout(GuiGraphics graphics) {
        int x = OPTIONS_X + 3;
        int y = LIST_Y + 4;
        int width = OPTIONS_WIDTH - 6;
        int bottom = LIST_Y + LIST_HEIGHT - 2;
        if (module.isController()) {
            Component mode = Component.translatable(KEY + (module.engaged() ? "mode.auto" : "mode.standby"));
            graphics.drawString(font, fit(mode, width - 22), x + 22, y + 5, module.engaged() ? COLOR_GOOD : COLOR_WARN, false);
            y += 24;
            bottom -= 12;
        }
        Component description = Component.translatable(module.function().translationKey() + ".desc");
        for (FormattedCharSequence line : font.split(description, width)) {
            if (y + font.lineHeight > bottom) {
                break;
            }
            graphics.drawString(font, line, x, y, COLOR_DIM, false);
            y += font.lineHeight + 1;
        }
        if (module.isController()) {
            ControlModule live = menu.module();
            Component state = live != null && module.engaged() && live.function() == module.function()
                    ? live.describeDemand()
                    : Component.translatable(KEY + "standby");
            Component line = Component.translatable(KEY + "setpoint", module.formatSetpoint(), state);
            graphics.drawString(font, fit(line, width), x, LIST_Y + LIST_HEIGHT - 11, COLOR_TEXT, false);
        }
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
        if (module == null || !menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> tooltip = hoverTooltip(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    /** Tooltips of the parts drawn on the background: the console line, the link column and the signal meter. */
    private List<Component> hoverTooltip(int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (inBox(mouseX, mouseY, HEADER_RIGHT - 100, HEADER_LINE_1 - 1, 100, 10) && summary.target() != null) {
            var pos = summary.target().pos();
            lines.add(Component.translatable("item.create_nuclearindustry.control_room_linker.target", pos.getX(), pos.getY(), pos.getZ()));
        } else if (inBox(mouseX, mouseY, LINK_ICON_X, LINK_ICON_Y, 16, 16)
                || inBox(mouseX, mouseY, FREQUENCY_X, FREQUENCY_FIRST_Y, 16, 16)
                || inBox(mouseX, mouseY, FREQUENCY_X, FREQUENCY_LAST_Y, 16, 16)) {
            lines.add(Component.translatable(KEY + "frequency").withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));
            lines.add(Component.translatable(SCREEN + (module.output() == OutputMode.LINK ? "frequency_hint" : "frequency_off"))
                    .withStyle(ChatFormatting.GRAY));
        } else if (inBox(mouseX, mouseY, METER_AREA_LEFT, METER_AREA_TOP, METER_AREA_RIGHT - METER_AREA_LEFT, METER_AREA_BOTTOM - METER_AREA_TOP)) {
            ControlModule live = menu.module();
            lines.add(Component.translatable(SCREEN + "signal", live != null ? live.signal() : 0)
                    .withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));
            lines.add(Component.translatable(SCREEN + "signal_hint").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /** {@code text} cut to {@code maxWidth} with "...", as one line. */
    private FormattedCharSequence fit(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text.getVisualOrderText();
        }
        String dots = "...";
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width(dots)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(dots, text.getStyle())));
    }

    // ------------------------------------------------------------------ icons

    private static AllIcons functionIcon(ModuleFunction function) {
        return switch (function) {
            case SCRAM, LAMP_SCRAM, TOKAMAK_SHUTDOWN -> AllIcons.I_STOP;
            case PULSE -> AllIcons.I_SEND_ONLY;
            case AUTO_SCRAM -> AllIcons.I_CONFIG_LOCKED;
            case TOKAMAK_FUEL, TOKAMAK_START, LAMP_RUNNING -> AllIcons.I_PLAY;
            case TOGGLE -> AllIcons.I_ACTIVE;
            case ROD_TARGET, READING -> AllIcons.I_MOVE_GAUGE;
            case SCRAM_THRESHOLD -> AllIcons.I_PRIORITY_HIGH;
            case SIGNAL_LEVEL -> AllIcons.I_SCROLL;
            case LAMP_NO_SIGNAL -> AllIcons.I_NONE;
            case LAMP_ALARM -> AllIcons.I_PRIORITY_VERY_HIGH;
            case AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER -> AllIcons.I_REFRESH;
            case PZR_HEATERS, TOKAMAK_HEATING -> AllIcons.I_FX_SURFACE_ON;
            case PZR_SPRAY -> AllIcons.I_FX_BLEND;
            case LAMP_RELIEF_OPEN -> AllIcons.I_SEND_AND_RECEIVE;
        };
    }

    private static AllIcons outputIcon(OutputMode mode) {
        return switch (mode) {
            case NONE -> AllIcons.I_DISABLE;
            case BELOW -> AllIcons.I_PRIORITY_VERY_LOW;
            case LINK -> AllIcons.I_SEND_ONLY;
        };
    }

    /**
     * Create's scroll input with this screen's wording: a threshold or setpoint moves 5 a notch and 1 with
     * shift (Create's own hint says shift goes faster, the other way round).
     */
    private final class SettingInput extends ScrollInput {
        SettingInput(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void updateTooltip() {
            toolTip.clear();
            if (module == null) {
                return;
            }
            toolTip.add(settingCaption().copy().withStyle(s -> s.withColor(HEADER_RGB.getRGB())));
            toolTip.add(Component.translatable(SCREEN + "scroll").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
            if (ModuleChoices.coarseSetting(module.function())) {
                toolTip.add(Component.translatable(SCREEN + "scroll_fine").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
            }
        }

        @Override
        public void onChanged() {
            super.onChanged();
            settingLabel.text = Component.literal(settingText());
            settingLabel.setX(leftPos + SETTING_X + SETTING_WIDTH - 4 - font.width(settingLabel.text));
        }
    }
}
