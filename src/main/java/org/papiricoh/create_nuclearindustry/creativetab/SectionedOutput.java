package org.papiricoh.create_nuclearindustry.creativetab;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The creative tab's output, filled section by section: {@link #section} picks where the
 * following {@code accept}s go. Nothing reaches the tab until {@link #finish}, which hands the
 * items over in section order, so the tab, its search and JEI all see them grouped. An item
 * accepted with no section set is a mistake and throws.
 */
public final class SectionedOutput implements CreativeModeTab.Output {
    private final CreativeModeTab.Output delegate;
    private final Map<NuclearTabSection, List<Entry>> entries = new EnumMap<>(NuclearTabSection.class);
    @Nullable
    private NuclearTabSection current;

    private record Entry(ItemStack stack, CreativeModeTab.TabVisibility visibility) {}

    public SectionedOutput(CreativeModeTab.Output delegate) {
        this.delegate = delegate;
    }

    /** The section the next items go to. */
    public SectionedOutput section(NuclearTabSection section) {
        current = section;
        return this;
    }

    /** Forgets the current section, so an area that forgets to pick its own fails loudly. */
    public void clearSection() {
        current = null;
    }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        if (current == null) {
            throw new IllegalStateException("No creative tab section picked for " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        entries.computeIfAbsent(current, section -> new ArrayList<>()).add(new Entry(stack, visibility));
    }

    /** Passes everything on to the tab in section order. Returns each item's section. */
    public Map<Item, NuclearTabSection> finish() {
        Map<Item, NuclearTabSection> sections = new HashMap<>();
        entries.forEach((section, list) -> list.forEach(entry -> {
            delegate.accept(entry.stack(), entry.visibility());
            sections.putIfAbsent(entry.stack().getItem(), section);
        }));
        entries.clear();
        return sections;
    }
}
