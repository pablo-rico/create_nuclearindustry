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

    /**
     * Events added ahead of the advancements that will listen for them (the guide's new tabs). The
     * guide work empties this set; until then they are the only events allowed to earn nothing.
     */
    private static final Set<NuclearEvent> AWAITING_ADVANCEMENTS = EnumSet.range(NuclearEvent.REACTOR_FULL_POWER,
            NuclearEvent.NUCLEAR_SHELL_FIRED);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyEventHasAnAdvancementListeningForIt(GameTestHelper helper) {
        Set<NuclearEvent> heard = EnumSet.noneOf(NuclearEvent.class);
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            for (Criterion<?> criterion : advancement(helper, entry.id()).value().criteria().values()) {
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
        missing.removeAll(AWAITING_ADVANCEMENTS);
        helper.assertTrue(heard.stream().noneMatch(AWAITING_ADVANCEMENTS::contains),
                "events that earn something leave AWAITING_ADVANCEMENTS");
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

    private static AdvancementHolder advancement(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id));
    }
}
