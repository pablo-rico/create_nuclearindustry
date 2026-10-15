package org.papiricoh.create_nuclearindustry.creativetab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lays a creative tab out in sections: each section opens on a row of its own (the banner row,
 * {@link #COLUMNS} empty slots) and its last row is padded out, so every section starts at the
 * left edge. Generic so it can be tested without the game.
 */
public final class TabLayout {
    /** Slots in a creative tab row. */
    public static final int COLUMNS = 9;

    private TabLayout() {}

    /**
     * @param slots      the tab's slots, {@code empty} on banner rows and padding
     * @param headerRows the banner row of each section shown, by row index
     */
    public record Layout<S, T>(List<T> slots, Map<Integer, S> headerRows) {}

    /**
     * Groups {@code items} by section in {@code order}, keeping their order within a section.
     * Items whose section is {@code null} or not in {@code order} go to {@code fallback}; sections
     * with no items are left out.
     */
    public static <S, T> Layout<S, T> build(Collection<T> items, Function<T, S> sectionOf, List<S> order, S fallback, T empty) {
        Map<S, List<T>> grouped = new LinkedHashMap<>();
        for (S section : order) {
            grouped.put(section, new ArrayList<>());
        }
        grouped.putIfAbsent(fallback, new ArrayList<>());
        for (T item : items) {
            S section = sectionOf.apply(item);
            grouped.getOrDefault(section, grouped.get(fallback)).add(item);
        }
        List<T> slots = new ArrayList<>();
        Map<Integer, S> headers = new LinkedHashMap<>();
        grouped.forEach((section, members) -> {
            if (members.isEmpty()) {
                return;
            }
            headers.put(slots.size() / COLUMNS, section);
            slots.addAll(Collections.nCopies(COLUMNS, empty));
            slots.addAll(members);
            int partial = members.size() % COLUMNS;
            if (partial != 0) {
                slots.addAll(Collections.nCopies(COLUMNS - partial, empty));
            }
        });
        return new Layout<>(slots, headers);
    }
}
