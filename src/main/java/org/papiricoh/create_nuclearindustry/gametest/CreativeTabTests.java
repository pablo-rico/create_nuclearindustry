package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllCreativeTabs;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTab;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.TabLayout;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The creative tab in sections: every item has one, each section opens on an empty banner row,
 * every section but "other" has something in it, and the search tab gets no gaps.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CreativeTabTests {
    private static final String BATCH = "creative_tab";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theTabIsLaidOutInSections(GameTestHelper helper) {
        CreativeModeTab tab = AllCreativeTabs.NUCLEAR_TAB.get();
        String failure = null;
        try {
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                    helper.getLevel().registryAccess()));
        } catch (RuntimeException e) {
            failure = e.toString();
        }
        helper.assertTrue(failure == null, "building the tab failed: " + failure);
        helper.assertTrue(NuclearTab.bannersEnabled, "sections are on by default");

        List<ItemStack> slots = new ArrayList<>(tab.getDisplayItems());
        Map<Integer, NuclearTabSection> banners = NuclearTab.headerRows();
        helper.assertTrue(slots.size() % TabLayout.COLUMNS == 0, "the tab ends on a full row");
        Set<NuclearTabSection> shown = EnumSet.noneOf(NuclearTabSection.class);
        int items = 0;
        for (int row = 0; row < slots.size() / TabLayout.COLUMNS; row++) {
            List<ItemStack> cells = slots.subList(row * TabLayout.COLUMNS, (row + 1) * TabLayout.COLUMNS);
            NuclearTabSection banner = banners.get(row);
            if (banner != null) {
                helper.assertTrue(cells.stream().allMatch(ItemStack::isEmpty), "the " + banner.id() + " banner row holds items");
                helper.assertTrue(shown.add(banner), banner.id() + " shows twice");
                helper.assertTrue(row + 1 < slots.size() / TabLayout.COLUMNS && !slots.get((row + 1) * TabLayout.COLUMNS).isEmpty(),
                        "the " + banner.id() + " section is empty");
                continue;
            }
            helper.assertFalse(cells.getFirst().isEmpty(), "row " + row + " starts with a gap");
            for (ItemStack stack : cells) {
                if (stack.isEmpty()) {
                    continue;
                }
                items++;
                helper.assertTrue(NuclearTab.sectionOf(stack.getItem()) != null,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()) + " has no section");
            }
        }
        helper.assertTrue(banners.containsKey(0), "the tab opens on a banner");
        for (NuclearTabSection section : NuclearTabSection.values()) {
            // Other mods' items and, without Create Aeronautics, the engines are the only sections that may be empty.
            boolean expected = section == NuclearTabSection.OTHER ? false
                    : section != NuclearTabSection.PROPULSION || org.papiricoh.create_nuclearindustry.integration.sable.SableCompat.isLoaded();
            helper.assertTrue(shown.contains(section) == expected, "section " + section.id() + (expected ? " is missing" : " shows with nothing in it"));
            helper.assertTrue(Language.getInstance().has(section.langKey()), "no title for " + section.id());
        }
        helper.assertTrue(tab.getSearchTabDisplayItems().stream().noneMatch(ItemStack::isEmpty), "the search tab has gaps");
        helper.assertTrue(items == tab.getSearchTabDisplayItems().size(),
                "the tab shows " + items + " items but searches " + tab.getSearchTabDisplayItems().size());
        helper.succeed();
    }
}
