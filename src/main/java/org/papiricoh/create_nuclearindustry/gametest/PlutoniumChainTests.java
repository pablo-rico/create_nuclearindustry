package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;

/** Spent fuel reprocessing (PUREX) with Create machines, down to plutonium metal. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlutoniumChainTests {
    private static final List<String> CHAIN = List.of(
            "mixing/nitric_acid",
            "mixing/nitric_acid_from_saltpeter",
            "crushing/low_spent_fuel_fragments",
            "crushing/high_spent_fuel_fragments",
            "mixing/dissolve_low_spent_fuel",
            "mixing/dissolve_high_spent_fuel",
            "centrifuging/plutonium_nitrate",
            "mixing/plutonium_dioxide",
            "mixing/plutonium",
            "mixing/natural_uranium_dioxide_from_uranyl_nitrate",
            "mixing/low_uranium_dioxide_from_uranyl_nitrate");

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyReprocessingStepIsLoaded(GameTestHelper helper) {
        for (String path : CHAIN) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id(path)).isPresent(), "missing recipe " + path);
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("mixing/reprocess_low_enriched_fuel")).isEmpty(),
                "the old sulfuric acid reprocessing is replaced");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fragmentsKeepTheFuelTier(GameTestHelper helper) {
        ProcessingRecipe<?, ?> crushing = processing(helper, "crushing/high_spent_fuel_fragments");
        helper.assertTrue(crushing.getIngredients().get(0).test(
                        UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()), 20f)),
                "HEU spent fuel should be accepted");
        ItemStack fragments = crushing.getRollableResultsAsItemStacks().get(0);
        helper.assertTrue(fragments.is(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get()) && UraniumItem.getEnrichment(fragments) == 20f,
                "fragments keep the 20% tier, got " + UraniumItem.getEnrichment(fragments));
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void recoveredUraniumIsOneTierLower(GameTestHelper helper) {
        ItemStack fromLow = processing(helper, "mixing/dissolve_low_spent_fuel").getRollableResultsAsItemStacks().get(0);
        ItemStack fromHigh = processing(helper, "mixing/dissolve_high_spent_fuel").getRollableResultsAsItemStacks().get(0);
        helper.assertTrue(fromLow.is(AllNuclearItems.URANYL_NITRATE.get()) && UraniumItem.getEnrichment(fromLow) == 0.7f,
                "LEU fuel gives back natural uranium, got " + UraniumItem.getEnrichment(fromLow));
        helper.assertTrue(UraniumItem.getEnrichment(fromHigh) == 5f, "HEU fuel gives back LEU, got " + UraniumItem.getEnrichment(fromHigh));
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void leanerFuelBreedsMorePlutonium(GameTestHelper helper) {
        int low = fluidOut(processing(helper, "mixing/dissolve_low_spent_fuel"), AllNuclearFluids.SPENT_FUEL_SOLUTION.get());
        int high = fluidOut(processing(helper, "mixing/dissolve_high_spent_fuel"), AllNuclearFluids.SPENT_FUEL_SOLUTION.get());
        helper.assertTrue(low > high, "LEU spent fuel should carry more plutonium than HEU, " + low + " vs " + high);
        helper.succeed();
    }

    /** Following one fragment through the chain, the nitric acid recovered never pays for what dissolving took. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void nitricAcidLoopIsNetNegative(GameTestHelper helper) {
        ProcessingRecipe<?, ?> centrifuge = processing(helper, "centrifuging/plutonium_nitrate");
        int feed = centrifuge.getFluidIngredients().get(0).amount();
        int puOut = fluidOut(centrifuge, AllNuclearFluids.PLUTONIUM_NITRATE.get());
        int acidBack = fluidOut(centrifuge, AllNuclearFluids.NITRIC_ACID.get());
        ProcessingRecipe<?, ?> oxide = processing(helper, "mixing/plutonium_dioxide");
        int nitratePerOxide = oxide.getFluidIngredients().get(0).amount();
        int acidPerOxide = fluidOut(oxide, AllNuclearFluids.NITRIC_ACID.get());
        int acidFromUranyl = fluidOut(processing(helper, "mixing/natural_uranium_dioxide_from_uranyl_nitrate"), AllNuclearFluids.NITRIC_ACID.get());
        for (String fuel : List.of("low", "high")) {
            ProcessingRecipe<?, ?> dissolve = processing(helper, "mixing/dissolve_" + fuel + "_spent_fuel");
            double acidIn = dissolve.getFluidIngredients().get(0).amount();
            double solution = fluidOut(dissolve, AllNuclearFluids.SPENT_FUEL_SOLUTION.get());
            double nitrate = solution * puOut / feed;
            double back = solution * acidBack / feed + nitrate / nitratePerOxide * acidPerOxide + acidFromUranyl;
            helper.assertTrue(back < acidIn, fuel + " fuel: " + back + " mB of acid back for " + acidIn + " in");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void centrifugeSeparatesPlutonium(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        KineticTestHelper.placePoweredMachine(helper, pos, AllNuclearBlocks.CENTRIFUGE.get(), 256);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(pos);
        helper.runAfterDelay(2, () -> {
            int filled = centrifuge.getFluidHandler(Direction.UP)
                    .fill(new FluidStack(AllNuclearFluids.SPENT_FUEL_SOLUTION.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "the centrifuge should take dissolved spent fuel, took " + filled);
        });
        helper.succeedWhen(() -> {
            FluidStack product = centrifuge.getProductFluid();
            FluidStack tails = centrifuge.getTailsFluid();
            helper.assertTrue(product.is(AllNuclearFluids.PLUTONIUM_NITRATE.get()) && product.getAmount() == 250,
                    "expected 250 mB plutonium nitrate, got " + product.getAmount() + " " + product.getHoverName().getString());
            helper.assertTrue(tails.is(AllNuclearFluids.NITRIC_ACID.get()) && tails.getAmount() == 250,
                    "expected 250 mB recovered acid, got " + tails.getAmount());
            ItemStack waste = centrifuge.getItem(KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT);
            helper.assertTrue(waste.is(WasteRegistry.HIGH_LEVEL_WASTE.get()) && waste.getCount() >= 2,
                    "fission products settle out as high-level waste, got " + waste);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plutoniumIsMuchHotterThanUranium(GameTestHelper helper) {
        double plutonium = RadiationHandler.stackRate(new ItemStack(AllNuclearItems.PLUTONIUM.get()));
        double uranium = RadiationHandler.stackRate(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 90f));
        helper.assertTrue(plutonium > 4 * uranium, "plutonium should be far more radioactive, " + plutonium + " vs " + uranium);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plutoniumBurnsUnprotectedPlayers(GameTestHelper helper) {
        Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
        bare.getInventory().add(new ItemStack(AllNuclearItems.PLUTONIUM.get(), 4));
        float before = bare.getHealth();
        RadiationHandler.update(bare);
        helper.assertTrue(bare.getHealth() < before, "plutonium should burn a bare hand, health " + before + " -> " + bare.getHealth());

        Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
        suited.getInventory().add(new ItemStack(AllNuclearItems.PLUTONIUM.get(), 4));
        suited.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllNuclearItems.HAZMAT_HELMET.get()));
        suited.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllNuclearItems.HAZMAT_CHESTPLATE.get()));
        suited.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AllNuclearItems.HAZMAT_LEGGINGS.get()));
        suited.setItemSlot(EquipmentSlot.FEET, new ItemStack(AllNuclearItems.HAZMAT_BOOTS.get()));
        float suitedBefore = suited.getHealth();
        RadiationHandler.update(suited);
        helper.assertTrue(suited.getHealth() == suitedBefore, "a hazmat suit keeps the heat off");
        helper.succeed();
    }

    private static int fluidOut(ProcessingRecipe<?, ?> recipe, Fluid fluid) {
        return recipe.getFluidResults().stream().filter(stack -> stack.is(fluid)).mapToInt(FluidStack::getAmount).sum();
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String path) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow().value();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
