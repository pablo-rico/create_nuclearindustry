package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorLayout;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.client.widget.OptionList;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Wrench setup of a control room screen: its title, its layout and style, what the graph's lines
 * plot (two always, a third when one is chosen), and which readings it lists in which order. Opened on the clicked monitor; the setup belongs to
 * the screen's origin, which this reads, and every change goes to the server at once as a
 * {@link ConfigureDevicePayload}, where it is checked again.
 */
public class MonitorConfigScreen extends AbstractSimiContainerScreen<DeviceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/control_monitor.png");
    private static final String KEY = "create_nuclearindustry.monitor.setup.";
    // Keep in sync with control_monitor in tools/texturegen/tex_controlroom2.py.
    static final int WIDTH = 220;
    static final int HEIGHT = 234;
    static final int LABEL_X = 10;
    static final int FIELD_X = 62;
    static final int FIELD_WIDTH = 148;
    static final int FIELD_HEIGHT = 14;
    static final int TITLE_Y = 21;
    static final int LAYOUT_Y = 38;
    static final int THEME_Y = 55;
    static final int SERIES_A_Y = 72;
    static final int SERIES_B_Y = 89;
    static final int SERIES_C_Y = 106;
    static final int READINGS_Y = 126;
    static final int LIST_X = 10;
    static final int LIST_Y = 138;
    static final int LIST_WIDTH = 178;
    static final int LIST_HEIGHT = 84;
    static final int BUTTON_X = 192;
    static final int BUTTON_STEP = 22;
    private static final int TITLE_COLOR = 0xFFE8C26A;
    private static final int LABEL_COLOR = 0xFFB0B8B0;
    private static final int FIELD_TEXT = 0xFFDDE4D8;

    @Nullable
    private MonitorConfig config;
    private TargetKind kind = TargetKind.NONE;
    private List<GaugeReading> seriesOptions = List.of();
    private EditBox titleBox;
    private OptionList<MonitorRow> rowList;
    private IconButton showButton;
    private IconButton upButton;
    private IconButton downButton;
    private IconButton resetButton;

    public MonitorConfigScreen(DeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The screen's origin, which holds the setup and the reading. */
    @Nullable
    private ControlMonitorBlockEntity screen() {
        return menu.blockEntity() instanceof ControlMonitorBlockEntity monitor ? monitor.origin().orElse(null) : null;
    }

    @Override
    protected void init() {
        setWindowSize(WIDTH, HEIGHT);
        super.init();
        ControlMonitorBlockEntity screen = screen();
        if (screen == null) {
            onClose();
            return;
        }
        kind = screen.getReading().kind();
        if (config == null) {
            config = screen.getConfig();
        }
        seriesOptions = GaugeReading.forKind(kind);

        // Unbordered: the field is drawn by the background, the text at the same place as the selectors'.
        titleBox = new EditBox(font, leftPos + FIELD_X + 4, topPos + TITLE_Y + 3, FIELD_WIDTH - 8, FIELD_HEIGHT - 4,
                Component.translatable(KEY + "title"));
        titleBox.setBordered(false);
        titleBox.setMaxLength(MonitorConfig.TITLE_MAX);
        titleBox.setTextColor(FIELD_TEXT);
        titleBox.setValue(config.title());
        titleBox.setHint(Component.translatable(kind.translationKey()));
        titleBox.setResponder(text -> {
            String clean = MonitorConfig.sanitizeTitle(text);
            if (!clean.equals(config.title())) {
                update(config.withTitle(clean));
            }
        });
        addRenderableWidget(titleBox);

        List<Component> layouts = new ArrayList<>();
        for (MonitorLayout layout : MonitorLayout.values()) {
            layouts.add(Component.translatable(layout.translationKey()));
        }
        selector(LAYOUT_Y, "layout", layouts, config.layout().ordinal(),
                state -> update(config.withLayout(MonitorLayout.values()[state])));
        List<Component> themes = new ArrayList<>();
        for (MonitorTheme theme : MonitorTheme.values()) {
            themes.add(Component.translatable(theme.translationKey()));
        }
        selector(THEME_Y, "theme", themes, config.theme().ordinal(),
                state -> update(config.withTheme(MonitorTheme.values()[state])));
        List<Component> series = new ArrayList<>();
        series.add(Component.translatable(KEY + "series.default"));
        for (GaugeReading reading : seriesOptions) {
            series.add(Component.translatable(reading.translationKey()));
        }
        selector(SERIES_A_Y, "series_a", series, seriesIndex(config.seriesA()),
                state -> update(config.withSeries(seriesAt(state), config.seriesB())));
        selector(SERIES_B_Y, "series_b", series, seriesIndex(config.seriesB()),
                state -> update(config.withSeries(config.seriesA(), seriesAt(state))));
        // The third line has no usual reading: its first option is none at all.
        List<Component> third = new ArrayList<>(series);
        third.set(0, Component.translatable(KEY + "series.none"));
        selector(SERIES_C_Y, "series_c", third, seriesIndex(config.seriesC()),
                state -> update(config.withSeriesC(seriesAt(state))));

        rowList = new OptionList<MonitorRow>(leftPos + LIST_X, topPos + LIST_Y, LIST_WIDTH, LIST_HEIGHT)
                .allowUnavailableSelection(true)
                .withEmptyText(Component.translatable(KEY + "rows.unlinked"))
                .onSelect(row -> refreshButtons());
        addRenderableWidget(rowList);
        int buttonY = topPos + LIST_Y;
        showButton = button(buttonY, AllIcons.I_CONFIRM, this::toggleShown);
        upButton = button(buttonY + BUTTON_STEP, AllIcons.I_PRIORITY_HIGH, () -> move(-1));
        downButton = button(buttonY + 2 * BUTTON_STEP, AllIcons.I_PRIORITY_LOW, () -> move(1));
        resetButton = button(buttonY + 3 * BUTTON_STEP, AllIcons.I_CONFIG_RESET,
                () -> update(config.withRows(List.of(), Set.of())));
        upButton.setToolTip(Component.translatable(KEY + "rows.up"));
        downButton.setToolTip(Component.translatable(KEY + "rows.down"));
        resetButton.setToolTip(Component.translatable(KEY + "rows.default"));
        refreshRows();
    }

    /**
     * A field of options: Create's scroll input, which here also steps forward on a click (back with
     * shift), and the label that shows the choice.
     */
    private void selector(int y, String key, List<Component> options, int state, IntConsumer onChange) {
        Label label = new Label(leftPos + FIELD_X + 4, topPos + y + 3, Component.empty()).colored(FIELD_TEXT);
        // The label only shows the choice: clicks go to the input under it.
        label.active = false;
        int room = FIELD_WIDTH - 8;
        ScrollInput input = new CyclingInput(leftPos + FIELD_X, topPos + y, FIELD_WIDTH, FIELD_HEIGHT)
                .forOptions(options)
                .format(index -> fit(options.get(index), room))
                .titled(Component.translatable(KEY + key))
                .writingTo(label)
                .setState(state)
                .calling(onChange::accept);
        addRenderableWidget(input);
        addRenderableWidget(label);
    }

    private IconButton button(int y, AllIcons icon, Runnable action) {
        IconButton button = new IconButton(leftPos + BUTTON_X, y, icon);
        button.withCallback(action);
        return addRenderableWidget(button);
    }

    private Component fit(Component text, int room) {
        if (font.width(text) <= room) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(text.getString(), room - font.width("...")) + "...");
    }

    private int seriesIndex(@Nullable GaugeReading reading) {
        int index = reading == null ? -1 : seriesOptions.indexOf(reading);
        return index + 1;
    }

    @Nullable
    private GaugeReading seriesAt(int state) {
        return state <= 0 || state > seriesOptions.size() ? null : seriesOptions.get(state - 1);
    }

    // ------------------------------------------------------------------ rows

    private List<MonitorRow> order() {
        return new ArrayList<>(MonitorRow.order(kind, config));
    }

    private void toggleShown() {
        MonitorRow row = rowList.selected();
        if (row == null) {
            return;
        }
        EnumSet<MonitorRow> hidden = config.hidden().isEmpty() ? EnumSet.noneOf(MonitorRow.class) : EnumSet.copyOf(config.hidden());
        if (!hidden.remove(row)) {
            hidden.add(row);
        }
        update(config.withRows(order(), hidden));
    }

    private void move(int step) {
        MonitorRow row = rowList.selected();
        List<MonitorRow> order = order();
        int from = row == null ? -1 : order.indexOf(row);
        int to = from + step;
        if (from < 0 || to < 0 || to >= order.size()) {
            return;
        }
        order.set(from, order.get(to));
        order.set(to, row);
        update(config.withRows(order, config.hidden()));
        rowList.ensureVisible(row);
    }

    /** The rows of what the screen watches, in its order, each with its live value; hidden ones greyed out. */
    private void refreshRows() {
        List<OptionList.Entry<MonitorRow>> entries = new ArrayList<>();
        for (MonitorRow row : MonitorRow.order(kind, config)) {
            boolean shown = !config.hidden().contains(row);
            entries.add(OptionList.Entry.of(row, Component.translatable(row.labelKey()))
                    .withIcon(shown ? AllIcons.I_CONFIRM : AllIcons.I_DISABLE)
                    .withAvailable(shown)
                    .withValueText(() -> liveValue(row))
                    .withTooltip(Component.translatable(KEY + (shown ? "rows.shown" : "rows.hidden"))));
        }
        rowList.setEntries(entries);
        refreshButtons();
    }

    private Component liveValue(MonitorRow row) {
        ControlMonitorBlockEntity screen = screen();
        MonitorReading reading = screen == null ? MonitorReading.NONE : screen.getReading();
        MonitorRow.Line line = row.line(reading);
        if (line == null) {
            return Component.empty();
        }
        return line.translated() ? Component.translatable(line.value()) : Component.literal(line.value());
    }

    private void refreshButtons() {
        MonitorRow row = rowList.selected();
        List<MonitorRow> order = MonitorRow.order(kind, config);
        int index = row == null ? -1 : order.indexOf(row);
        boolean hidden = row != null && config.hidden().contains(row);
        showButton.active = index >= 0;
        showButton.setIcon(hidden ? AllIcons.I_DISABLE : AllIcons.I_CONFIRM);
        showButton.setToolTip(Component.translatable(KEY + (hidden ? "rows.show" : "rows.hide")));
        upButton.active = index > 0;
        downButton.active = index >= 0 && index < order.size() - 1;
        resetButton.active = config.customRows();
    }

    // ------------------------------------------------------------------ sending

    private void update(MonitorConfig next) {
        config = next;
        PacketDistributor.sendToServer(new ConfigureDevicePayload(menu.pos(), 0, next.write()));
        refreshRows();
    }

    /** Linked to something else while open: start again from what the screen now shows. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ControlMonitorBlockEntity screen = screen();
        if (screen == null) {
            return;
        }
        TargetKind now = screen.getReading().kind();
        if (now != kind && now != TargetKind.NO_SIGNAL) {
            config = screen.getConfig();
            rebuildWidgets();
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 7, TITLE_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "title"), LABEL_X, TITLE_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "layout"), LABEL_X, LAYOUT_Y + 3, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(KEY + "theme"), LABEL_X, THEME_Y + 3, LABEL_COLOR, false);
        // The lines' names in their colours in the chosen style: a preview of the style as it is cycled.
        int[] lines = MonitorPalette.of(config == null ? MonitorTheme.STANDARD : config.theme()).guiSeries();
        graphics.drawString(font, Component.translatable(KEY + "series_a"), LABEL_X, SERIES_A_Y + 3, lines[0], false);
        graphics.drawString(font, Component.translatable(KEY + "series_b"), LABEL_X, SERIES_B_Y + 3, lines[1], false);
        graphics.drawString(font, Component.translatable(KEY + "series_c"), LABEL_X, SERIES_C_Y + 3, lines[2], false);
        graphics.drawString(font, Component.translatable(KEY + "rows"), LABEL_X, READINGS_Y, LABEL_COLOR, false);
        Component watching = Component.translatable(kind.translationKey());
        graphics.drawString(font, watching, WIDTH - 10 - font.width(watching), READINGS_Y, 0xFF6C7771, false);
    }

    /** Create's scroll input, which also steps through its options when clicked: forward, or back with shift. */
    static final class CyclingInput extends SelectionScrollInput {
        CyclingInput(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (max <= min) {
                return;
            }
            int span = max - min;
            int next = min + Math.floorMod(state - min + (Screen.hasShiftDown() ? -1 : 1), span);
            if (next != state) {
                setState(next);
                onChanged();
            }
        }
    }
}
