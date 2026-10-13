package org.papiricoh.create_nuclearindustry.panel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What the panel on the right of the screen shows about the block under the crosshair (see
 * {@link PanelSource}): a header (kicker, title, tag and a status line with its light), a list of
 * elements and a footer. Built afresh every frame from what the client has, and drawn by
 * {@code client.panel.PanelHud}; the accent colours the frame. No client classes, so gametests can
 * build cards and check their text.
 */
public final class PanelCard {
    /** Panel colours: cyan for what is live, amber for setups and warnings, green, red, and two greys. */
    public static final int CYAN = 0xFF46E8FF, AMBER = 0xFFFFB347, GREEN = 0xFF6CFF9A, RED = 0xFFFF5A4A,
            TEXT = 0xFFE8F4F8, DIM = 0xFF7F98A3;

    public final Component kicker;
    public final Component title;
    @Nullable
    public final Component tag;
    public final int accent;
    /** Tells cards of one block apart (a desk's slots), so changing between them replays the title. */
    public int key;
    @Nullable
    public Component status;
    public int statusColor = GREEN;
    public boolean statusBlinks;
    public final List<Element> elements = new ArrayList<>();
    @Nullable
    public Component footer;

    public PanelCard(Component kicker, Component title, @Nullable Component tag, int accent) {
        this.kicker = kicker;
        this.title = title;
        this.tag = tag;
        this.accent = accent;
    }

    public PanelCard key(int key) {
        this.key = key;
        return this;
    }

    public PanelCard status(Component text, int color, boolean blinks) {
        this.status = text;
        this.statusColor = color;
        this.statusBlinks = blinks;
        return this;
    }

    public PanelCard add(Element element) {
        elements.add(element);
        return this;
    }

    public PanelCard footer(Component text) {
        this.footer = text;
        return this;
    }

    /** Every text the card shows that a lang file should cover: headings, labels, values and notes. */
    public List<Component> texts() {
        List<Component> texts = new ArrayList<>();
        texts.add(kicker);
        texts.add(title);
        if (tag != null) {
            texts.add(tag);
        }
        if (status != null) {
            texts.add(status);
        }
        for (Element element : elements) {
            switch (element) {
                case Section section -> texts.add(section.title());
                case Field field -> {
                    texts.add(field.label());
                    texts.add(field.value());
                }
                case BigValue big -> texts.add(big.value());
                case Bar bar -> {
                    texts.add(bar.label());
                    texts.add(bar.right());
                }
                case Signal signal -> texts.add(signal.label());
                case Code code -> texts.add(code.label());
                case Items items -> texts.add(items.label());
                case Note note -> texts.add(note.text());
                case Alert alert -> texts.add(alert.text());
                case Grid grid -> grid.cells().forEach(cell -> {
                    texts.add(cell.label());
                    texts.add(cell.value());
                });
            }
        }
        if (footer != null) {
            texts.add(footer);
        }
        return texts;
    }

    /** One block of the panel's body, laid out top to bottom. */
    public sealed interface Element {
        /** Height in GUI pixels. */
        int height();
    }

    /** A small heading with a rule after it. */
    public record Section(Component title) implements Element {
        @Override
        public int height() {
            return 11;
        }
    }

    /** A label on the left and its value on the right. */
    public record Field(Component label, Component value, int color) implements Element {
        @Override
        public int height() {
            return 10;
        }
    }

    /** A big number with its unit, a symbol such as °C or MW. */
    public record BigValue(Component value, String unit, int color) implements Element {
        @Override
        public int height() {
            return 24;
        }
    }

    /** A segmented gauge, 0-1, with a label and a reading over it. */
    public record Bar(Component label, float fraction, Component right, int color) implements Element {
        @Override
        public int height() {
            return 16;
        }
    }

    /** A redstone signal 0-15 as a rising row of bars. */
    public record Signal(Component label, int level) implements Element {
        @Override
        public int height() {
            return 15;
        }
    }

    /** An Advanced Redstone Link code, one chip per character. */
    public record Code(Component label, String code, int color) implements Element {
        @Override
        public int height() {
            return 21;
        }
    }

    /** A Redstone Link frequency: its two items. */
    public record Items(Component label, List<ItemStack> stacks) implements Element {
        @Override
        public int height() {
            return 20;
        }
    }

    /** A dim line of advice. */
    public record Note(Component text) implements Element {
        @Override
        public int height() {
            return 9;
        }
    }

    /** Short label-value pairs two to a row, for counts and small readings. */
    public record Grid(List<Field> cells) implements Element {
        @Override
        public int height() {
            return (cells.size() + 1) / 2 * 10;
        }
    }

    /** Something wrong: a flashing stripe with its text. */
    public record Alert(Component text, int color) implements Element {
        @Override
        public int height() {
            return 12;
        }
    }
}
