package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * How a control room screen is set up with a wrench: its title, its {@link MonitorLayout}, the order
 * of its rows and which are hidden, and the readings its graph plots. The default (empty title,
 * {@link MonitorLayout#AUTO}, no rows, no series) is what every screen showed before it could be set up.
 *
 * @param title  shown on the title line instead of the kind of target; empty for the kind
 * @param rows   the rows in the order the player put them; empty for the usual order. Rows left out
 *               go last (see {@link MonitorRow#order})
 * @param hidden rows not drawn
 * @param seriesA what the graph's first line plots, or null for the usual reading of the target
 * @param seriesB the same for the second line
 */
public record MonitorConfig(String title, MonitorLayout layout, List<MonitorRow> rows, Set<MonitorRow> hidden,
                            @Nullable GaugeReading seriesA, @Nullable GaugeReading seriesB) {
    /** Longest title: what fits the title line of a two-block screen with its status. */
    public static final int TITLE_MAX = 24;
    public static final MonitorConfig DEFAULT = new MonitorConfig("", MonitorLayout.AUTO, List.of(), Set.of(), null, null);

    public MonitorConfig {
        title = title == null ? "" : title;
        layout = layout == null ? MonitorLayout.AUTO : layout;
        rows = rows == null ? List.of() : List.copyOf(rows);
        hidden = hidden == null || hidden.isEmpty() ? Set.of() : Set.copyOf(hidden);
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    /** Whether the rows were arranged or hidden: a single block then pages through all it shows. */
    public boolean customRows() {
        return !rows.isEmpty() || !hidden.isEmpty();
    }

    public MonitorConfig withTitle(String newTitle) {
        return new MonitorConfig(newTitle, layout, rows, hidden, seriesA, seriesB);
    }

    public MonitorConfig withLayout(MonitorLayout newLayout) {
        return new MonitorConfig(title, newLayout, rows, hidden, seriesA, seriesB);
    }

    public MonitorConfig withRows(List<MonitorRow> newRows, Set<MonitorRow> newHidden) {
        return new MonitorConfig(title, layout, newRows, newHidden, seriesA, seriesB);
    }

    public MonitorConfig withSeries(@Nullable GaugeReading newA, @Nullable GaugeReading newB) {
        return new MonitorConfig(title, layout, rows, hidden, newA, newB);
    }

    /**
     * This setup made safe for a screen watching a target of {@code kind}: a printable title of at most
     * {@link #TITLE_MAX} characters, only rows and series of that kind (none for an unlinked screen),
     * and the rows back to "usual order" when they are in it with none hidden.
     */
    public MonitorConfig sanitize(TargetKind kind) {
        String cleanTitle = sanitizeTitle(title);
        boolean known = kind != TargetKind.NONE && kind != TargetKind.NO_SIGNAL;
        if (!known) {
            // No signal or no link: the type is unknown, so the rows and series are kept as sent rather than
            // dropped (the renderer only draws the target's own rows); only the title is cleaned.
            List<MonitorRow> kept = new ArrayList<>();
            for (MonitorRow row : rows) {
                if (row != null && !kept.contains(row)) {
                    kept.add(row);
                }
            }
            EnumSet<MonitorRow> keptHidden = EnumSet.noneOf(MonitorRow.class);
            for (MonitorRow row : hidden) {
                if (row != null) {
                    keptHidden.add(row);
                }
            }
            return new MonitorConfig(cleanTitle, layout, List.copyOf(kept), keptHidden, seriesA, seriesB);
        }
        List<MonitorRow> order = List.of();
        Set<MonitorRow> cleanHidden = Set.of();
        if (known) {
            EnumSet<MonitorRow> hiddenOfKind = EnumSet.noneOf(MonitorRow.class);
            for (MonitorRow row : hidden) {
                if (row != null && row.kind() == kind) {
                    hiddenOfKind.add(row);
                }
            }
            cleanHidden = hiddenOfKind;
            List<MonitorRow> full = MonitorRow.order(kind, this);
            order = hiddenOfKind.isEmpty() && full.equals(MonitorRow.forKind(kind)) ? List.of() : full;
        }
        GaugeReading cleanA = known && seriesA != null && seriesA.kind() == kind ? seriesA : null;
        GaugeReading cleanB = known && seriesB != null && seriesB.kind() == kind ? seriesB : null;
        return new MonitorConfig(cleanTitle, layout, order, cleanHidden, cleanA, cleanB);
    }

    /**
     * Printable text only, as chat allows it (no formatting codes, control characters or line
     * breaks), trimmed, at most {@link #TITLE_MAX} characters without splitting a character in two.
     */
    public static String sanitizeTitle(@Nullable String text) {
        if (text == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                // A formatting code goes whole: the sign and the letter after it.
                i++;
            } else if (c >= ' ' && c != 127) {
                clean.append(c);
            }
        }
        String stripped = clean.toString().strip();
        if (stripped.length() <= TITLE_MAX) {
            return stripped;
        }
        int end = TITLE_MAX;
        if (Character.isHighSurrogate(stripped.charAt(end - 1))) {
            end--;
        }
        return stripped.substring(0, end).strip();
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Title", title);
        tag.putString("Layout", layout.name());
        tag.put("Rows", names(rows));
        List<MonitorRow> sortedHidden = new ArrayList<>(hidden);
        sortedHidden.sort(Comparator.naturalOrder());
        tag.put("Hidden", names(sortedHidden));
        tag.putString("SeriesA", seriesA == null ? "" : seriesA.name());
        tag.putString("SeriesB", seriesB == null ? "" : seriesB.name());
        return tag;
    }

    /**
     * The setup in {@code tag}, taking only what it understands: unknown rows and readings are
     * dropped, a missing value is the default. Not sanitised for a kind of target: see {@link #sanitize}.
     */
    public static MonitorConfig read(CompoundTag tag) {
        String title = tag.contains("Title", Tag.TAG_STRING) ? tag.getString("Title") : "";
        MonitorLayout layout = MonitorLayout.byName(tag.getString("Layout"));
        List<MonitorRow> rows = rows(tag.getList("Rows", Tag.TAG_STRING));
        List<MonitorRow> hidden = rows(tag.getList("Hidden", Tag.TAG_STRING));
        GaugeReading seriesA = GaugeReading.parse(tag.getString("SeriesA"));
        GaugeReading seriesB = GaugeReading.parse(tag.getString("SeriesB"));
        return new MonitorConfig(sanitizeTitle(title), layout, rows, hidden.isEmpty() ? Set.of() : EnumSet.copyOf(hidden),
                seriesA, seriesB);
    }

    private static ListTag names(List<MonitorRow> rows) {
        ListTag list = new ListTag();
        for (MonitorRow row : rows) {
            list.add(StringTag.valueOf(row.name()));
        }
        return list;
    }

    /** Known rows, once each; no more than there are rows, however long the list sent. */
    private static List<MonitorRow> rows(ListTag list) {
        List<MonitorRow> rows = new ArrayList<>();
        int limit = Math.min(list.size(), MonitorRow.values().length * 2);
        for (int i = 0; i < limit; i++) {
            MonitorRow row = MonitorRow.parse(list.getString(i));
            if (row != null && !rows.contains(row)) {
                rows.add(row);
            }
        }
        return rows;
    }
}
