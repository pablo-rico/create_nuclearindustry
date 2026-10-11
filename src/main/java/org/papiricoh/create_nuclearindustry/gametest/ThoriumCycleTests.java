package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.physics.SpentAssembly;
import org.papiricoh.create_nuclearindustry.reactor.physics.ThoriumBreeding;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelData;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

import java.util.List;
import java.util.Optional;

/** Thorium fuel: it loads, starts weak, grows more reactive as it breeds uranium-233, and leaves as spent thorium fuel. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ThoriumCycleTests {

    private static final List<String> CHAIN = List.of(
            "acid_leaching/heavy_mineral_sand",
            "irradiation/thorium_dioxide",
            "mixing/irradiated_thorium",
            "compacting/thorium_fuel_pellet",
            "compacting/thorium_fuel_pellet_from_uranium_233",
            "sequenced_assembly/thorium_reactor_fuel",
            "crushing/depleted_thorium_reactor_fuel",
            "crushing/depleted_thorium_reactor_fuel_cooled",
            "crushing/depleted_thorium_reactor_fuel_aged",
            "mixing/dissolve_thorium_spent_fuel",
            "centrifuging/thorium_fuel_solution",
            "mixing/uranium_233_dioxide",
            "irradiation/depleted_uranium_dioxide",
            "irradiation/depleted_uranium",
            "mixing/irradiated_depleted_uranium");

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyThoriumRecipeIsLoaded(GameTestHelper helper) {
        for (String path : CHAIN) {
            helper.assertTrue(helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path)).isPresent(), "missing recipe " + path);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void thoriumFuelIsFreshFuel(GameTestHelper helper) {
        ItemStack fuel = new ItemStack(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get());
        helper.assertTrue(ReactorBlockEntity.fuelKindOf(fuel) == FuelKind.THORIUM, "thorium fuel is of the thorium kind");
        helper.assertTrue(ReactorBlockEntity.getFuelEnrichment(fuel) == FuelKind.THORIUM_EQUIVALENT_ENRICHMENT,
                "thorium is rated at its equivalent enrichment");
        ItemStack spent = new ItemStack(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get());
        helper.assertTrue(ReactorBlockEntity.isSpentFuel(spent), "spent thorium goes to the spent slots");
        helper.assertTrue(SpentFuel.isSpentFuel(spent), "spent thorium cools like the other spent fuels");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formedReactorLoadsThoriumFuel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            helper.assertTrue(reactor.isFreshFuel(new ItemStack(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get())), "the reactor takes thorium");
            reactor.getFuelInventory().insertItem(0, new ItemStack(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get()), false);
        });
        helper.runAfterDelay(10, () -> {
            ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
            helper.assertTrue(physics.getLoadedAssemblyCount() == 1, "one thorium assembly should be loaded");
            List<ReactorPhysicsSimulator.AssemblyView> views = physics.assemblyViews();
            helper.assertTrue(!views.isEmpty() && views.get(0).kind() == FuelKind.THORIUM, "the loaded assembly is thorium");
            helper.assertTrue(physics.getFuelReactivity() < ReactorPhysicsSimulator.reactivityFor(5f),
                    "fresh thorium is weaker than low-enriched uranium, got " + physics.getFuelReactivity());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void thoriumGrowsMoreReactiveAsItBreeds(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(1, 0);
        physics.loadFuelAssembly(FuelKind.THORIUM, FuelKind.THORIUM_EQUIVALENT_ENRICHMENT);
        double fresh = physics.getFuelReactivity();
        double atPeak = fresh;
        double burnAtPeak = 0.0;
        double burnRate = Config.uraniumBurnPerNeutron;
        try {
            Config.uraniumBurnPerNeutron = 0.0005;
            for (int i = 0; i < 20000 && physics.hasFuel(); i++) {
                physics.tick();
                physics.applyExternalCooling(1000.0, 0.0);
                if (physics.hasFuel() && physics.getFuelReactivity() > atPeak) {
                    atPeak = physics.getFuelReactivity();
                    burnAtPeak = physics.assemblyViews().get(0).burnFraction();
                }
            }
        } finally {
            Config.uraniumBurnPerNeutron = burnRate;
        }
        helper.assertTrue(Math.abs(fresh - ThoriumBreeding.SEED_WORTH) < 1e-6, "fresh thorium only has its seed, got " + fresh);
        helper.assertTrue(atPeak > fresh * 2.0, "breeding should more than double the reactivity, peaked at " + atPeak);
        helper.assertTrue(burnAtPeak > 0.3 && burnAtPeak < 0.8, "the peak comes midway through the burn, came at " + burnAtPeak);
        helper.assertFalse(physics.hasFuel(), "the assembly should burn out");
        List<SpentAssembly> spent = physics.drainSpent();
        helper.assertTrue(spent.equals(List.of(new SpentAssembly(FuelKind.THORIUM, FuelKind.THORIUM_EQUIVALENT_ENRICHMENT))),
                "spent thorium keeps its kind, got " + spent);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void thoriumOutlastsLowEnrichedUranium(GameTestHelper helper) {
        ReactorPhysicsSimulator uranium = new ReactorPhysicsSimulator(1, 0);
        uranium.loadFuelAssembly(5f);
        ReactorPhysicsSimulator thorium = new ReactorPhysicsSimulator(1, 0);
        thorium.loadFuelAssembly(FuelKind.THORIUM, FuelKind.THORIUM_EQUIVALENT_ENRICHMENT);
        helper.assertTrue(Math.abs(thorium.getFuelRemaining() - uranium.getFuelRemaining() * ThoriumBreeding.LIFE_FACTOR) < 1e-6,
                "a thorium assembly holds " + ThoriumBreeding.LIFE_FACTOR + " times the fuel, got " + thorium.getFuelRemaining());
        helper.assertTrue(thorium.getFuelHeatFactor() < 1.0, "thoria runs a little cooler");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void spentThoriumLeavesTheReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> reactor.getFuelInventory().insertItem(0, new ItemStack(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get()), false));
        helper.runAfterDelay(10, () -> {
            ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
            helper.assertTrue(physics.getLoadedAssemblyCount() == 1, "the thorium assembly should be loaded");
            double burnRate = Config.uraniumBurnPerNeutron;
            try {
                // Burn it out within this tick, keeping the core cool so the reactor stays whole.
                Config.uraniumBurnPerNeutron = 1.0;
                physics.setControlRodInsertion(0f);
                for (int i = 0; i < 2000 && physics.hasFuel(); i++) {
                    physics.tick();
                    physics.applyExternalCooling(1000.0, 0.0);
                }
            } finally {
                Config.uraniumBurnPerNeutron = burnRate;
                physics.forceScramForLoad();
                physics.applyExternalCooling(10000.0, 2000.0);
            }
            helper.assertFalse(physics.hasFuel(), "the thorium assembly should burn out");
        });
        helper.succeedWhen(() -> {
            ItemStack out = ItemStack.EMPTY;
            for (int slot = ReactorBlockEntity.FUEL_INPUT_SLOTS; slot < ReactorBlockEntity.FUEL_SLOT_COUNT; slot++) {
                ItemStack stack = reactor.getFuelInventory().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    out = stack;
                }
            }
            helper.assertTrue(out.is(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get()), "spent thorium should come out, got " + out);
            SpentFuelData data = out.get(SpentFuelRegistry.SPENT_FUEL.get());
            helper.assertTrue(data != null && data.grade() == CoolingGrade.HOT, "fresh out of the core it is hot, got " + data);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spentThoriumIsShearedByItsGrade(GameTestHelper helper) {
        for (CoolingGrade grade : CoolingGrade.values()) {
            ItemStack spent = new ItemStack(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get());
            spent.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(grade));
            Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe = AllRecipeTypes.CRUSHING.find(new SingleRecipeInput(spent), helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "spent thorium graded " + grade + " should crush");
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
                helper.assertTrue(result.is(FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS.get()),
                        "spent thorium graded " + grade + " should give thorium fragments, got " + result);
            }
        }
        helper.succeed();
    }
}
