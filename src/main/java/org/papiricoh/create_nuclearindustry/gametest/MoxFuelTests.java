package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.physics.SpentAssembly;

import java.util.List;

/** MOX fuel: plutonium in depleted uranium, burning like 12% uranium but hotter and harder to hold. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MoxFuelTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void moxIsFreshFuelAtTwelvePercent(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ItemStack mox = new ItemStack(AllNuclearItems.MOX_REACTOR_FUEL.get());
        helper.assertTrue(reactor.isFreshFuel(mox), "the reactor takes MOX");
        helper.assertTrue(ReactorBlockEntity.getFuelEnrichment(mox) == 12f, "MOX burns as 12% uranium");
        helper.assertTrue(ReactorBlockEntity.isSpentFuel(new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get())),
                "spent MOX goes to the spent slots");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void moxIsBetweenLeuAndHeuButHotter(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(4, 4);
        physics.loadFuelAssembly(FuelKind.MOX, FuelKind.MOX_EQUIVALENT_ENRICHMENT);
        double reactivity = physics.getFuelReactivity();
        helper.assertTrue(reactivity > ReactorPhysicsSimulator.reactivityFor(5f) && reactivity < ReactorPhysicsSimulator.reactivityFor(20f),
                "MOX reactivity sits between LEU and HEU, got " + reactivity);
        helper.assertTrue(Math.abs(physics.getFuelHeatFactor() - 1.15) < 1e-9, "MOX makes 15% more heat");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void moxNeedsMoreControlRods(GameTestHelper helper) {
        // 7 control rods for 10 fuel channels: enough for uranium, not quite for MOX.
        ReactorPhysicsSimulator uranium = new ReactorPhysicsSimulator(10, 7);
        uranium.loadFuelAssembly(5f);
        ReactorPhysicsSimulator mox = new ReactorPhysicsSimulator(10, 7);
        mox.loadFuelAssembly(FuelKind.MOX, FuelKind.MOX_EQUIVALENT_ENRICHMENT);
        // The absorption is no longer capped at one bank: more rods than needed are worth more.
        helper.assertTrue(uranium.getMaxAbsorption() >= 1.0, "uranium is fully held, got " + uranium.getMaxAbsorption());
        helper.assertTrue(mox.getMaxAbsorption() < 1.0, "MOX slips past the same rods, got " + mox.getMaxAbsorption());
        ReactorPhysicsSimulator wellControlled = new ReactorPhysicsSimulator(10, 10);
        wellControlled.loadFuelAssembly(FuelKind.MOX, FuelKind.MOX_EQUIVALENT_ENRICHMENT);
        helper.assertTrue(wellControlled.getMaxAbsorption() >= 1.0, "with enough rods a SCRAM still stops MOX");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spentMoxRemembersItIsMox(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(2, 0);
        physics.loadFuelAssembly(FuelKind.MOX, FuelKind.MOX_EQUIVALENT_ENRICHMENT);
        physics.loadFuelAssembly(5f);
        double burnRate = FissionConfig.fuelBurnPerRod;
        try {
            FissionConfig.fuelBurnPerRod = 1.0;
            for (int i = 0; i < 400 && physics.hasFuel(); i++) {
                physics.tick();
                physics.burnFuel(20.0);
            }
        } finally {
            FissionConfig.fuelBurnPerRod = burnRate;
        }
        List<SpentAssembly> spent = physics.drainSpent();
        helper.assertTrue(spent.equals(List.of(new SpentAssembly(FuelKind.MOX, 12f), SpentAssembly.uranium(5f))),
                "assemblies burn out in order and keep their kind, got " + spent);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spentMoxLeavesAsSpentMoxItems(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = reactor.saveWithoutMetadata(registries);
            ListTag pending = new ListTag();
            CompoundTag entry = new CompoundTag();
            entry.putString("type", "mox");
            entry.putFloat("enrichment", 12f);
            pending.add(entry);
            tag.put("pendingSpent", pending);
            reactor.loadWithComponents(tag, registries);
        });
        helper.succeedWhen(() -> {
            ItemStack out = reactor.getFuelInventory().getStackInSlot(ReactorBlockEntity.FUEL_INPUT_SLOTS);
            helper.assertTrue(out.is(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get()), "spent MOX should come out as such, got " + out);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacySavesLoadAsUranium(GameTestHelper helper) {
        CompoundTag physicsTag = new CompoundTag();
        physicsTag.putInt("uraniumRodCount", 2);
        ListTag assemblies = new ListTag();
        CompoundTag assembly = new CompoundTag();
        assembly.putFloat("enrichment", 20f);
        assembly.putDouble("units", 50);
        assembly.putDouble("initialUnits", 100);
        assemblies.add(assembly);
        physicsTag.put("assemblies", assemblies);
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(0);
        physics.deserializeNBT(physicsTag);
        helper.assertTrue(physics.getFuelHeatFactor() == 1.0, "an assembly saved without a type is uranium");

        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = reactor.saveWithoutMetadata(registries);
            tag.remove("pendingSpent");
            ListTag legacy = new ListTag();
            legacy.add(FloatTag.valueOf(20f));
            tag.put("pendingDepleted", legacy);
            reactor.loadWithComponents(tag, registries);
        });
        helper.succeedWhen(() -> {
            ItemStack out = reactor.getFuelInventory().getStackInSlot(ReactorBlockEntity.FUEL_INPUT_SLOTS);
            helper.assertTrue(out.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()) && UraniumItem.getEnrichment(out) == 20f,
                    "an old pending float becomes 20% spent uranium fuel, got " + out);
        });
    }

    /** Recycling MOX loses plutonium: a MOX assembly takes 0.5 PuO2 and gives back less. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void moxRecyclingLosesPlutonium(GameTestHelper helper) {
        ProcessingRecipe<?, ?> pellets = processing(helper, "compacting/mox_fuel_pellet");
        int pelletsPerOxide = pellets.getRollableResultsAsItemStacks().get(0).getCount();
        double oxideIn = 2.0 / pelletsPerOxide;
        int solution = processing(helper, "mixing/dissolve_depleted_spent_fuel").getFluidResults().stream()
                .filter(f -> f.is(AllNuclearFluids.SPENT_FUEL_SOLUTION.get())).mapToInt(f -> f.getAmount()).sum();
        ProcessingRecipe<?, ?> centrifuge = processing(helper, "centrifuging/plutonium_nitrate");
        int nitrate = centrifuge.getFluidResults().stream().filter(f -> f.is(AllNuclearFluids.PLUTONIUM_NITRATE.get()))
                .mapToInt(f -> f.getAmount()).sum();
        int nitratePerOxide = processing(helper, "mixing/plutonium_dioxide").getFluidIngredients().get(0).amount();
        double oxideOut = solution * (double) nitrate / centrifuge.getFluidIngredients().get(0).amount() / nitratePerOxide;
        helper.assertTrue(oxideOut < oxideIn, "MOX gives back " + oxideOut + " PuO2 for " + oxideIn + " in");
        helper.succeed();
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String path) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path)).orElseThrow().value();
    }
}
