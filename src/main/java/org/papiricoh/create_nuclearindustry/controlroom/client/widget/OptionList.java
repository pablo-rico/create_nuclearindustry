package org.papiricoh.create_nuclearindustry.controlroom.client.widget;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A scrollable list of options for the control room screens, drawn over a dark recess of the screen
 * background. Rows show an optional Create icon, a name and an optional value on the right; rows that
 * are not available are greyed out and, unless {@link #allowUnavailableSelection} is set, cannot be
 * picked. The host screen does not need to forward drags: the scrollbar follows the held mouse button
 * on its own, because vanilla container screens swallow {@code mouseDragged}.
 */
public class OptionList<T> extends AbstractSimiWidget {
    /** Height of one row, in GUI pixels. */
    public static final int ROW_HEIGHT = 12;
    /** Width of the scrollbar strip on the right edge. */
    public static final int SCROLLBAR_WIDTH = 4;
    private static final int ICON_SIZE = 10;

    /** Mod palette for lists, matching the control room screens. */
    public static final int COLOR_BACKGROUND = 0x90101614;
    public static final int COLOR_STRIPE = 0x0EFFFFFF;
    public static final int COLOR_HOVER = 0x22FFFFFF;
    public static final int COLOR_SELECTED = 0x386FBF4E;
    public static final int COLOR_SELECTED_EDGE = 0x806FBF4E;
    public static final int COLOR_ACCENT = 0xFF6FBF4E;
    public static final int COLOR_TEXT = 0xFFDDE4D8;
    public static final int COLOR_TEXT_SELECTED = 0xFFFFFFFF;
    public static final int COLOR_VALUE = 0xFFB8F0C4;
    public static final int COLOR_DIM = 0xFF6C7771;
    public static final int COLOR_TRACK = 0xFF202629;
    public static final int COLOR_THUMB = 0xFF849087;
    public static final int COLOR_THUMB_ACTIVE = 0xFFB5BFB0;

    /**
     * One row. {@code valueText} is re-read every frame so it can show live readings; {@code tooltip}
     * is shown on hover (a description, or what the option still needs when it is not available).
     */
    public record Entry<T>(T value, Component name, @Nullable AllIcons icon, @Nullable Supplier<Component> valueText,
                           boolean available, List<Component> tooltip) {
        public Entry {
            Objects.requireNonNull(name, "name");
            tooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
        }

        /** An available row with just a name. */
        public static <T> Entry<T> of(T value, Component name) {
            return new Entry<>(value, name, null, null, true, List.of());
        }

        public Entry<T> withIcon(@Nullable AllIcons icon) {
            return new Entry<>(value, name, icon, valueText, available, tooltip);
        }

        public Entry<T> withValueText(@Nullable Supplier<Component> valueText) {
            return new Entry<>(value, name, icon, valueText, available, tooltip);
        }

        public Entry<T> withAvailable(boolean available) {
            return new Entry<>(value, name, icon, valueText, available, tooltip);
        }

        public Entry<T> withTooltip(List<Component> tooltip) {
            return new Entry<>(value, name, icon, valueText, available, tooltip);
        }

        public Entry<T> withTooltip(Component... lines) {
            return withTooltip(Arrays.asList(lines));
        }
    }

    private List<Entry<T>> entries = List.of();
    @Nullable
    private T selected;
    private Consumer<T> onSelect = value -> {};
    private boolean allowUnavailableSelection;
    private Component emptyText = CommonComponents.EMPTY;
    private int scroll;
    private boolean draggingBar;
    private int dragGrabOffset;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;

    public OptionList(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /** Called with the new value whenever the player picks a different row (not on {@link #setSelected}). */
    public OptionList<T> onSelect(Consumer<T> callback) {
        this.onSelect = callback == null ? value -> {} : callback;
        return this;
    }

    /** Whether greyed-out rows can still be picked (for example to show what they need). Off by default. */
    public OptionList<T> allowUnavailableSelection(boolean allow) {
        this.allowUnavailableSelection = allow;
        return this;
    }

    /** Text shown dimmed in the middle of the list when it has no rows. */
    public OptionList<T> withEmptyText(Component text) {
        this.emptyText = text == null ? CommonComponents.EMPTY : text;
        return this;
    }

    /**
     * Replaces the rows. The scroll position is kept (clamped to the new length), so refreshing the same
     * options does not jump; the selected value stays selected if it is still in the list.
     */
    public void setEntries(List<Entry<T>> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    /** Replaces the rows and, if {@code resetScroll}, goes back to the top. */
    public void setEntries(List<Entry<T>> entries, boolean resetScroll) {
        setEntries(entries);
        if (resetScroll) {
            scroll = 0;
        }
    }

    public List<Entry<T>> entries() {
        return entries;
    }

    @Nullable
    public T selected() {
        return selected;
    }

    @Nullable
    public Entry<T> selectedEntry() {
        int index = indexOf(selected);
        return index < 0 ? null : entries.get(index);
    }

    /** Selects a value without calling the callback, and scrolls it into view. */
    public void setSelected(@Nullable T value) {
        selected = value;
        ensureVisible(value);
    }

    /** Scrolls the least needed for the row holding {@code value} to be fully shown. */
    public void ensureVisible(@Nullable T value) {
        int index = indexOf(value);
        if (index < 0) {
            return;
        }
        int top = index * ROW_HEIGHT;
        if (top < scroll) {
            scroll = top;
        } else if (top + ROW_HEIGHT > scroll + height) {
            scroll = top + ROW_HEIGHT - height;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    public void ensureSelectedVisible() {
        ensureVisible(selected);
    }

    /** Scroll position in pixels from the top. */
    public int scrollOffset() {
        return scroll;
    }

    public void setScrollOffset(int pixels) {
        scroll = Mth.clamp(pixels, 0, maxScroll());
    }

    /** How many rows fit without scrolling. */
    public int visibleRows() {
        return Math.max(1, height / ROW_HEIGHT);
    }

    /** The row under a point, or null over the scrollbar, empty space or outside the list. */
    @Nullable
    public Entry<T> entryAt(double mouseX, double mouseY) {
        int index = rowAt(mouseX, mouseY);
        return index < 0 ? null : entries.get(index);
    }

    private int indexOf(@Nullable T value) {
        if (value == null) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (Objects.equals(entries.get(i).value(), value)) {
                return i;
            }
        }
        return -1;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + contentWidth() || mouseY < getY() || mouseY >= getY() + height) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - getY() + scroll) / ROW_HEIGHT);
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private boolean selectable(Entry<T> entry) {
        return entry.available() || allowUnavailableSelection;
    }

    private int contentHeight() {
        return entries.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private boolean hasScrollbar() {
        return maxScroll() > 0;
    }

    private int contentWidth() {
        return hasScrollbar() ? width - SCROLLBAR_WIDTH - 1 : width;
    }

    private int thumbHeight() {
        return Mth.clamp(height * height / Math.max(1, contentHeight()), 8, height);
    }

    private int thumbY() {
        int max = maxScroll();
        return getY() + (max == 0 ? 0 : (height - thumbHeight()) * scroll / max);
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return hasScrollbar() && mouseX >= getX() + width - SCROLLBAR_WIDTH && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    private void dragTo(double mouseY) {
        int travel = height - thumbHeight();
        if (travel <= 0) {
            return;
        }
        double fraction = (mouseY - dragGrabOffset - getY()) / travel;
        scroll = Mth.clamp((int) Math.round(fraction * maxScroll()), 0, maxScroll());
    }

    private void pick(int index) {
        Entry<T> entry = entries.get(index);
        boolean changed = !Objects.equals(selected, entry.value());
        selected = entry.value();
        ensureVisible(selected);
        if (changed) {
            onSelect.accept(selected);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (overScrollbar(mouseX, mouseY)) {
            int thumbTop = thumbY();
            if (mouseY >= thumbTop && mouseY < thumbTop + thumbHeight()) {
                dragGrabOffset = (int) mouseY - thumbTop;
            } else {
                // Clicking the track jumps the thumb's middle there, then keeps dragging.
                dragGrabOffset = thumbHeight() / 2;
                dragTo(mouseY);
            }
            draggingBar = true;
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0 || !selectable(entries.get(index))) {
            return false;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        pick(index);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragTo(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingBar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingBar = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY) || !hasScrollbar() || scrollY == 0) {
            return false;
        }
        int before = scroll;
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * ROW_HEIGHT, 0, maxScroll());
        return scroll != before;
    }

    /** Up and down move the selection to the previous or next row that can be picked. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused() || entries.isEmpty()) {
            return false;
        }
        int step = keyCode == InputConstants.KEY_UP ? -1 : keyCode == InputConstants.KEY_DOWN ? 1 : 0;
        if (step == 0) {
            return false;
        }
        int index = indexOf(selected);
        if (index < 0) {
            index = step > 0 ? -1 : entries.size();
        }
        for (int i = index + step; i >= 0 && i < entries.size(); i += step) {
            if (selectable(entries.get(i))) {
                pick(i);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (draggingBar) {
            // Container screens never forward mouseDragged, so follow the held button here.
            long window = Minecraft.getInstance().getWindow().getWindow();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                dragTo(mouseY);
            } else {
                draggingBar = false;
            }
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());

        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int right = x + contentWidth();
        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);

        if (entries.isEmpty()) {
            if (!emptyText.getString().isEmpty()) {
                graphics.drawCenteredString(font, emptyText, x + width / 2, y + (height - font.lineHeight) / 2 + 1, COLOR_DIM);
            }
            return;
        }

        int hoveredRow = draggingBar ? -1 : rowAt(mouseX, mouseY);
        graphics.enableScissor(x, y, right, y + height);
        int first = scroll / ROW_HEIGHT;
        int last = Math.min(entries.size() - 1, (scroll + height - 1) / ROW_HEIGHT);
        for (int i = first; i <= last; i++) {
            renderRow(graphics, font, entries.get(i), i, x, y + i * ROW_HEIGHT - scroll, right, i == hoveredRow);
        }
        graphics.disableScissor();

        if (hasScrollbar()) {
            int barX = x + width - SCROLLBAR_WIDTH;
            graphics.fill(barX, y, x + width, y + height, COLOR_TRACK);
            int thumbTop = thumbY();
            boolean hot = draggingBar || overScrollbar(mouseX, mouseY);
            graphics.fill(barX, thumbTop, x + width, thumbTop + thumbHeight(), hot ? COLOR_THUMB_ACTIVE : COLOR_THUMB);
            graphics.fill(barX, thumbTop, barX + 1, thumbTop + thumbHeight(), 0x40FFFFFF);
        }
    }

    private void renderRow(GuiGraphics graphics, Font font, Entry<T> entry, int index, int x, int rowY, int right,
                           boolean hovered) {
        boolean isSelected = selected != null && Objects.equals(selected, entry.value());
        if (index % 2 == 1) {
            graphics.fill(x, rowY, right, rowY + ROW_HEIGHT, COLOR_STRIPE);
        }
        if (isSelected) {
            graphics.fill(x, rowY, right, rowY + ROW_HEIGHT, COLOR_SELECTED);
            graphics.fill(x, rowY, right, rowY + 1, COLOR_SELECTED_EDGE);
            graphics.fill(x, rowY + ROW_HEIGHT - 1, right, rowY + ROW_HEIGHT, COLOR_SELECTED_EDGE);
            graphics.fill(x, rowY, x + 1, rowY + ROW_HEIGHT, COLOR_ACCENT);
        } else if (hovered && selectable(entry)) {
            graphics.fill(x, rowY, right, rowY + ROW_HEIGHT, COLOR_HOVER);
        }

        int textX = x + 3;
        if (entry.icon() != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 2, rowY + (ROW_HEIGHT - ICON_SIZE) / 2f, 0);
            graphics.pose().scale(ICON_SIZE / 16f, ICON_SIZE / 16f, 1);
            if (!entry.available()) {
                graphics.setColor(0.45f, 0.45f, 0.45f, 1);
            }
            entry.icon().render(graphics, 0, 0);
            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
            textX = x + 2 + ICON_SIZE + 2;
        }

        int textY = rowY + (ROW_HEIGHT - font.lineHeight) / 2 + 1;
        int valueLeft = right - 2;
        Component value = valueText(entry);
        if (value != null) {
            int valueWidth = font.width(value);
            valueLeft = right - 2 - valueWidth;
            graphics.drawString(font, value, valueLeft, textY, entry.available() ? COLOR_VALUE : COLOR_DIM, false);
            valueLeft -= 4;
        }

        int nameColor = !entry.available() ? COLOR_DIM : isSelected ? COLOR_TEXT_SELECTED : COLOR_TEXT;
        int room = valueLeft - textX;
        if (font.width(entry.name()) <= room) {
            graphics.drawString(font, entry.name(), textX, textY, nameColor, false);
        } else if (room > 0) {
            graphics.drawString(font, ellipsize(font, entry.name(), room), textX, textY, nameColor, false);
        }
    }

    @Nullable
    private static Component valueText(Entry<?> entry) {
        if (entry.valueText() == null) {
            return null;
        }
        Component value = entry.valueText().get();
        return value == null || value.getString().isEmpty() ? null : value;
    }

    private static FormattedCharSequence ellipsize(Font font, Component text, int room) {
        String dots = "...";
        FormattedText cut = font.substrByWidth(text, Math.max(0, room - font.width(dots)));
        return Language.getInstance()
                .getVisualOrder(FormattedText.composite(cut, FormattedText.of(dots, text.getStyle())));
    }

    /** True when the row's name does not fit and is drawn cut short, so the tooltip repeats it. */
    private boolean truncated(Entry<T> entry) {
        Font font = Minecraft.getInstance().font;
        int textX = 3 + (entry.icon() != null ? ICON_SIZE + 1 : 0);
        Component value = valueText(entry);
        int valueRoom = value == null ? 2 : font.width(value) + 6;
        return font.width(entry.name()) > contentWidth() - textX - valueRoom;
    }

    /** The hovered row's tooltip (its full name first when it is cut short); empty over the scrollbar. */
    @Override
    public List<Component> getToolTip() {
        if (!visible || draggingBar || lastMouseX == Integer.MIN_VALUE) {
            return List.of();
        }
        Entry<T> entry = entryAt(lastMouseX, lastMouseY);
        if (entry == null) {
            return List.of();
        }
        if (!truncated(entry)) {
            return entry.tooltip();
        }
        List<Component> lines = new ArrayList<>(entry.tooltip().size() + 1);
        lines.add(entry.name());
        lines.addAll(entry.tooltip());
        return lines;
    }

    /**
     * Defers the tooltip to the end of the frame so widgets drawn after the list cannot cover it. Create's
     * container screens draw {@link #getToolTip} themselves, so nothing is queued there.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractSimiContainerScreen<?>) {
            return;
        }
        List<Component> tooltip = getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        if (screen != null) {
            screen.setTooltipForNextRenderPass(tooltip.stream().map(Component::getVisualOrderText).toList());
        } else {
            graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        Entry<T> entry = selectedEntry();
        output.add(NarratedElementType.TITLE, entry != null ? entry.name() : getMessage());
    }
}
