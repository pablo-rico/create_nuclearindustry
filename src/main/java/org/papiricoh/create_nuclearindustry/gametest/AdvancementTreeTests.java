package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.data.NuclearAdvancementProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The shape of the generated advancement tree: six tabs, each a root with its own background, every
 * advancement in the tab it was written for (so a parent never pulls a child into another tab),
 * secret ones framed as goals or challenges, and the ids from before the tabs all still there.
 * Which events earn something is {@link AdvancementTriggerTests}' business.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AdvancementTreeTests {
    /** Their own batch: they build nothing. */
    private static final String BATCH = "advancement_tree";
    /** Ids players may already have earned: they must survive any reshuffle of the tree. */
    private static final List<String> LEGACY_IDS = List.of("root", "yellowcake", "reactor_fuel", "plutonium", "mox",
            "reactor_formed", "reactor_running", "moderated_core", "scram", "iodine_pit", "control_room", "turbine",
            "cooling_tower", "meltdown", "lithium_6", "deuterium", "tokamak_ignition", "geiger", "hazmat",
            "radiation_sickness", "radiation_purge", "weapons_core", "nuclear_detonation", "dirty_detonation");

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void sixTabsOfAboutFiftyAdvancements(GameTestHelper helper) {
        List<NuclearAdvancementProvider.Entry> entries = NuclearAdvancementProvider.ENTRIES;
        helper.assertTrue(entries.size() >= 40, "the guide has at least 40 advancements, found " + entries.size());
        List<NuclearAdvancementProvider.Entry> roots = NuclearAdvancementProvider.roots();
        helper.assertTrue(roots.size() == 7, "seven tabs, found " + roots.size() + " roots");
        Set<String> tabs = new HashSet<>();
        for (NuclearAdvancementProvider.Entry root : roots) {
            helper.assertTrue(root.tab().equals(root.id()), root.id() + " is the root of its own tab");
            tabs.add(root.tab());
            AdvancementHolder holder = advancement(helper, root.id());
            helper.assertTrue(holder != null, "root " + root.id() + " is loaded");
            Optional<DisplayInfo> display = holder == null ? Optional.empty() : holder.value().display();
            helper.assertTrue(display.isPresent() && display.get().getBackground().isPresent(),
                    "root " + root.id() + " has a background");
        }
        helper.assertTrue(tabs.contains(NuclearAdvancementProvider.FUEL_CYCLE), "the original root still roots the fuel cycle tab");
        helper.assertTrue(new HashSet<>(NuclearAdvancementProvider.BACKGROUNDS.keySet()).equals(tabs), "a background for every tab");
        helper.assertTrue(new HashSet<>(NuclearAdvancementProvider.BACKGROUNDS.values()).size() == 7, "every tab has its own background");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void everyAdvancementSitsInItsTab(GameTestHelper helper) {
        Set<String> ids = new HashSet<>();
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            helper.assertTrue(ids.add(entry.id()), "advancement " + entry.id() + " is listed twice");
            helper.assertTrue(!entry.tab().isEmpty(), entry.id() + " has a tab");
            String root = rootOf(helper, entry.id());
            helper.assertTrue(entry.tab().equals(root), entry.id() + " is meant for the " + entry.tab() + " tab but hangs under " + root);
            if (entry.parent() != null) {
                NuclearAdvancementProvider.Entry parent = find(entry.parent());
                helper.assertTrue(parent != null && parent.tab().equals(entry.tab()),
                        entry.id() + " and its parent " + entry.parent() + " share a tab");
            }
        }
        for (String legacy : LEGACY_IDS) {
            helper.assertTrue(ids.contains(legacy), "the old advancement " + legacy + " is kept, so players keep their progress");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void secretsAreGoalsOrChallenges(GameTestHelper helper) {
        int secrets = 0;
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            AdvancementHolder holder = advancement(helper, entry.id());
            helper.assertTrue(holder != null && holder.value().display().isPresent(), entry.id() + " is loaded with a display");
            if (holder == null || holder.value().display().isEmpty()) {
                continue;
            }
            DisplayInfo display = holder.value().display().get();
            helper.assertTrue(display.isHidden() == entry.hidden(), entry.id() + " hidden as written");
            helper.assertTrue(display.getType() == entry.type(), entry.id() + " framed as written");
            if (entry.hidden()) {
                secrets++;
                helper.assertTrue(entry.type() == AdvancementType.GOAL || entry.type() == AdvancementType.CHALLENGE,
                        "secret " + entry.id() + " should be a goal or a challenge");
                helper.assertTrue(!entry.isRoot(), "a root is never secret: " + entry.id());
            }
        }
        helper.assertTrue(secrets >= 6, "the guide keeps a handful of secret challenges, found " + secrets);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void everyAdvancementHasText(GameTestHelper helper) {
        Language language = Language.getInstance();
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            helper.assertTrue(language.has(entry.titleKey()), "no title for " + entry.id());
            helper.assertTrue(language.has(entry.descriptionKey()), "no description for " + entry.id());
            helper.assertTrue(!entry.titleEs().isBlank() && !entry.descriptionEs().isBlank(), entry.id() + " has Spanish text");
        }
        helper.succeed();
    }

    /** Follows the loaded tree's parents up to the root, or "?" when the chain breaks. */
    private static String rootOf(GameTestHelper helper, String id) {
        String current = id;
        for (int depth = 0; depth < 64; depth++) {
            AdvancementHolder holder = advancement(helper, current);
            if (holder == null) {
                return "?";
            }
            Optional<ResourceLocation> parent = holder.value().parent();
            if (parent.isEmpty()) {
                return current;
            }
            current = parent.get().getPath();
        }
        return "?";
    }

    private static NuclearAdvancementProvider.Entry find(String id) {
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static AdvancementHolder advancement(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id));
    }
}
