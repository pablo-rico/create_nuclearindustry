package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.integration.sable.SableCompat;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEventTrigger;
import org.papiricoh.create_nuclearindustry.data.NuclearAdvancementProvider;

import java.util.EnumSet;
import java.util.Set;

/**
 * The generated advancement tree and the nuclear event trigger. Earning is not exercised here:
 * NeoForge never awards advancements to fake players, and mock server players trip other mods'
 * packets in the test server.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AdvancementTriggerTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyAdvancementIsLoaded(GameTestHelper helper) {
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            AdvancementHolder holder = advancement(helper, entry.id());
            helper.assertTrue(holder != null, "advancement " + entry.id() + " should be generated");
            helper.assertTrue(entry.parent() == null || holder.value().parent()
                            .map(parent -> parent.getPath().equals(entry.parent())).orElse(false),
                    entry.id() + " should hang from " + entry.parent());
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyEventHasAnAdvancementListeningForIt(GameTestHelper helper) {
        Set<NuclearEvent> heard = EnumSet.noneOf(NuclearEvent.class);
        // The generated tree, and the engines' two, written by hand to load only with Sable.
        java.util.List<AdvancementHolder> advancements = new java.util.ArrayList<>();
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            advancements.add(advancement(helper, entry.id()));
        }
        for (String id : java.util.List.of("nuclear_flight", "antimatter_thrust")) {
            AdvancementHolder holder = advancement(helper, id);
            helper.assertTrue((holder != null) == SableCompat.isLoaded(), id + " should load exactly when Sable is installed");
            if (holder != null) {
                advancements.add(holder);
            }
        }
        for (AdvancementHolder holder : advancements) {
            for (Criterion<?> criterion : holder.value().criteria().values()) {
                if (criterion.triggerInstance() instanceof NuclearEventTrigger.TriggerInstance instance) {
                    helper.assertTrue(criterion.trigger() == NuclearAdvancements.TRIGGER.get(), "criteria use the registered trigger");
                    for (NuclearEvent event : NuclearEvent.values()) {
                        if (instance.matches(event)) {
                            heard.add(event);
                        }
                    }
                }
            }
        }
        Set<NuclearEvent> missing = EnumSet.allOf(NuclearEvent.class);
        missing.removeAll(heard);
        if (!SableCompat.isLoaded()) {
            // Without ships there is nothing for the engines to push.
            missing.removeAll(EnumSet.of(NuclearEvent.NUCLEAR_FLIGHT, NuclearEvent.ANTIMATTER_THRUST));
        }
        helper.assertTrue(missing.isEmpty(), "every event earns something; missing " + missing);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anEventOnlyMatchesItsOwnCriterion(GameTestHelper helper) {
        NuclearEventTrigger.TriggerInstance formed = (NuclearEventTrigger.TriggerInstance)
                NuclearEventTrigger.TriggerInstance.of(NuclearEvent.REACTOR_FORMED).triggerInstance();
        helper.assertTrue(formed.matches(NuclearEvent.REACTOR_FORMED), "the reactor_formed event matches");
        helper.assertFalse(formed.matches(NuclearEvent.REACTOR_RUNNING), "another event does not");
        helper.succeed();
    }

    /** The antimatter branch hangs from the first nanogram; the quench is a hidden surprise. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theAntimatterAdvancementsAreLoaded(GameTestHelper helper) {
        String[][] expected = {{"first_nanogram", "gev_beam"}, {"quench", "first_nanogram"},
                {"annihilation_power", "first_nanogram"}, {"annihilation_ignition", "first_nanogram"}};
        for (String[] pair : expected) {
            AdvancementHolder holder = advancement(helper, pair[0]);
            helper.assertTrue(holder != null, pair[0] + " should be loaded");
            helper.assertTrue(holder.value().parent().map(parent -> parent.getPath().equals(pair[1])).orElse(false),
                    pair[0] + " should hang from " + pair[1]);
        }
        AdvancementHolder quench = advancement(helper, "quench");
        helper.assertTrue(quench.value().display().map(display -> display.isHidden()).orElse(false), "the quench is secret");
        NuclearEventTrigger.TriggerInstance ignition = (NuclearEventTrigger.TriggerInstance)
                NuclearEventTrigger.TriggerInstance.of(NuclearEvent.ANNIHILATION_IGNITION).triggerInstance();
        helper.assertTrue(ignition.matches(NuclearEvent.ANNIHILATION_IGNITION) && !ignition.matches(NuclearEvent.TOKAMAK_IGNITION),
                "an antiproton ignition is its own event, not the tokamak's");
        helper.succeed();
    }

    private static AdvancementHolder advancement(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id));
    }
}
