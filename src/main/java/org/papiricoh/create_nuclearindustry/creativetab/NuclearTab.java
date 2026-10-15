package org.papiricoh.create_nuclearindustry.creativetab;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What the creative tab knows about its sections between building its contents and drawing
 * them: each item's section (from {@link SectionedOutput#finish}) and the row each banner sits on.
 * {@code CreativeModeTabMixin} lays the tab out with {@link #withBanners} once it is built.
 */
public final class NuclearTab {
    /** {@code client.creativeTabSections}; off, the tab is one flat list, still in section order. */
    public static volatile boolean bannersEnabled = true;
    private static volatile Map<Item, NuclearTabSection> sections = Map.of();
    private static volatile Map<Integer, NuclearTabSection> headerRows = Map.of();

    private NuclearTab() {}

    public static void setSections(Map<Item, NuclearTabSection> bySection) {
        sections = Map.copyOf(bySection);
    }

    @Nullable
    public static NuclearTabSection sectionOf(Item item) {
        return sections.get(item);
    }

    /** The section whose banner is on {@code row} of the tab, or null. */
    @Nullable
    public static NuclearTabSection bannerAt(int row) {
        return headerRows.get(row);
    }

    public static Map<Integer, NuclearTabSection> headerRows() {
        return headerRows;
    }

    /** The tab's contents with a banner row before each section, or as they are with banners off. */
    public static Collection<ItemStack> withBanners(Collection<ItemStack> contents) {
        if (!bannersEnabled) {
            headerRows = Map.of();
            return contents;
        }
        TabLayout.Layout<NuclearTabSection, ItemStack> layout = TabLayout.build(contents, stack -> sectionOf(stack.getItem()),
                List.of(NuclearTabSection.values()), NuclearTabSection.OTHER, ItemStack.EMPTY);
        headerRows = Map.copyOf(layout.headerRows());
        return layout.slots();
    }
}
