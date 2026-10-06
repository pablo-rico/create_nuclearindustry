package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

import java.util.List;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FissionFuelTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void onlyAssembledFuelIsAccepted(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.assertFalse(reactor.isFreshFuel(enriched(AllNuclearItems.URANIUM.get(), 20f)),
                "bare uranium metal must not be accepted as fuel");
        helper.assertFalse(reactor.isFreshFuel(enriched(AllNuclearItems.URANIUM_REACTOR_FUEL.get(), 0.7f)),
                "natural-uranium fuel is below the minimum enrichment");
        helper.assertTrue(reactor.isFreshFuel(enriched(AllNuclearItems.URANIUM_REACTOR_FUEL.get(), 5f)),
                "LEU fuel should be accepted");
        helper.assertTrue(reactor.isFreshFuel(new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get())),
                "fuel made before enrichment tiers (no component) should still be accepted");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void richerFuelLastsLongerAndRunsHotter(GameTestHelper helper) {
        double leuUnits = ReactorPhysicsSimulator.fuelUnitsFor(5f);
        double heuUnits = ReactorPhysicsSimulator.fuelUnitsFor(20f);
        helper.assertTrue(Math.abs(leuUnits - Config.fuelUnitsPerAssembly) < 1e-6, "LEU is the reference assembly, got " + leuUnits);
        helper.assertTrue(heuUnits > 2 * leuUnits, "HEU should hold well over twice the units, got " + heuUnits);
        helper.assertTrue(ReactorPhysicsSimulator.reactivityFor(20f) > ReactorPhysicsSimulator.reactivityFor(5f),
                "HEU should be more reactive than LEU");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spentFuelRemembersItsEnrichment(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(2, 0);
        physics.loadFuelAssembly(20f);
        physics.loadFuelAssembly(5f);
        double burnRate = Config.uraniumBurnPerNeutron;
        try {
            Config.uraniumBurnPerNeutron = 1.0;
            for (int i = 0; i < 200 && physics.hasFuel(); i++) {
                physics.tick();
            }
        } finally {
            Config.uraniumBurnPerNeutron = burnRate;
        }
        List<Float> spent = physics.drainSpentAssemblies();
        helper.assertTrue(spent.equals(List.of(20f, 5f)), "assemblies should burn out in load order with their enrichment, got " + spent);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyFuelCounterBecomesAssemblies(GameTestHelper helper) {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("uraniumRodCount", 4);
        legacy.putDouble("fuelRemaining", 150.0);
        legacy.putDouble("coreTemperature", 300.0);
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(0);
        physics.deserializeNBT(legacy);
        helper.assertTrue(physics.getLoadedAssemblyCount() == 2, "150 legacy units should become two assemblies");
        helper.assertTrue(Math.abs(physics.getFuelRemaining() - 150.0) < 1e-6, "no fuel may be lost, got " + physics.getFuelRemaining());
        helper.assertTrue(Math.abs(physics.getFuelReactivity() - 1.0) < 1e-6, "legacy fuel keeps its old reactivity");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formedReactorLoadsFuelFromItsSlots(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.getFuelInventory().insertItem(0, enriched(AllNuclearItems.URANIUM_REACTOR_FUEL.get(), 20f), false);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.getPhysicsSimulator().getLoadedAssemblyCount() == 1, "one assembly should be loaded");
            helper.assertTrue(Math.abs(reactor.getAverageFuelEnrichment() - 20f) < 1e-3,
                    "loaded fuel should be 20%, got " + reactor.getAverageFuelEnrichment());
            helper.succeed();
        });
    }

    private static ItemStack enriched(net.minecraft.world.item.Item item, float enrichment) {
        ItemStack stack = new ItemStack(item);
        UraniumItem.setEnrichment(stack, enrichment);
        return stack;
    }
}
