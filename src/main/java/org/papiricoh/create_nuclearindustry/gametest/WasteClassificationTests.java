package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.scrubber.RadiationScrubberBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.WasteForm;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;
import java.util.function.Supplier;

/**
 * Waste comes in three classes: low level (scrubbers, contaminated water), intermediate level
 * (the old Radioactive Waste) and high level (PUREX fission products), each with its treatment.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class WasteClassificationTests {
    private static final BlockPos SCRUBBER = new BlockPos(6, 2, 6);

    private static ProcessingRecipe<?, ?> recipe(GameTestHelper helper, String path) {
        var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
        helper.assertTrue(holder.isPresent(), "missing recipe " + path);
        helper.assertTrue(holder.get().value() instanceof ProcessingRecipe<?, ?>, path + " should be a processing recipe");
        return (ProcessingRecipe<?, ?>) holder.get().value();
    }

    private static boolean outputs(ProcessingRecipe<?, ?> recipe, Item item) {
        return recipe.getRollableResultsAsItemStacks().stream().anyMatch(stack -> stack.is(item));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void purexGivesHighLevelWaste(GameTestHelper helper) {
        ProcessingRecipe<?, ?> purex = recipe(helper, "centrifuging/plutonium_nitrate");
        helper.assertTrue(outputs(purex, WasteRegistry.HIGH_LEVEL_WASTE.get()), "PUREX fission products are high-level waste");
        helper.assertFalse(outputs(purex, AllNuclearItems.RADIOACTIVE_WASTE.get()), "PUREX no longer gives intermediate-level waste");
        ProcessingRecipe<?, ?> water = recipe(helper, "centrifuging/contaminated_water");
        helper.assertTrue(outputs(water, WasteRegistry.LOW_LEVEL_WASTE.get()), "contaminated water gives low-level waste");
        helper.assertFalse(outputs(water, AllNuclearItems.RADIOACTIVE_WASTE.get()), "and no intermediate-level waste");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wasteClassesRankByActivity(GameTestHelper helper) {
        double low = RadiationHandler.stackRate(new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()));
        double intermediate = RadiationHandler.stackRate(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        double high = RadiationHandler.stackRate(new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get()));
        helper.assertTrue(low > 0 && low < intermediate && intermediate < high,
                "low < intermediate < high expected, got " + low + ", " + intermediate + ", " + high);
        helper.assertTrue(intermediate == 6.0, "intermediate-level waste keeps its old activity, got " + intermediate);
        Radioactivity hot = WasteRegistry.HIGH_LEVEL_WASTE.get().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        helper.assertTrue(hot != null && hot.heatPerSecond() > 0, "high-level waste is warm");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyPackageHasItsForm(GameTestHelper helper) {
        List<Supplier<? extends Block>> blocks = List.of(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL, WasteRegistry.COMPACTED_WASTE_DRUM,
                WasteRegistry.CEMENTED_WASTE_DRUM, WasteRegistry.VITRIFIED_WASTE_CANISTER);
        List<WasteForm> forms = List.of(WasteForm.RAW, WasteForm.COMPACTED, WasteForm.CEMENTED, WasteForm.VITRIFIED);
        float lastLeak = Float.MAX_VALUE;
        for (int i = 0; i < blocks.size(); i++) {
            WastePackage pack = WastePackage.of(blocks.get(i).get().defaultBlockState());
            helper.assertTrue(pack != null && pack.form() == forms.get(i), blocks.get(i).get() + " should be a " + forms.get(i) + " package");
            helper.assertTrue(pack.leakFactor() <= lastLeak, "better forms should leak less");
            lastLeak = pack.leakFactor();
        }
        helper.assertTrue(WastePackage.of(WasteRegistry.BENTONITE.get().defaultBlockState()) == null, "bentonite holds no waste");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldWasteRecipesKeepWorking(GameTestHelper helper) {
        ProcessingRecipe<?, ?> barrel = recipe(helper, "compacting/radioactive_waste_barrel");
        Ingredient waste = barrel.getIngredients().get(0);
        helper.assertTrue(waste.test(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get())), "barrels still take intermediate-level waste");
        helper.assertTrue(waste.test(new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get())), "and high-level waste, for older PUREX lines");
        helper.assertFalse(waste.test(new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get())), "low-level waste has its own drum");
        ProcessingRecipe<?, ?> pellet = recipe(helper, "compacting/waste_rtg_pellet");
        helper.assertTrue(pellet.getIngredients().get(0).test(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get())),
                "the waste RTG pellet stays intermediate-level");
        helper.assertTrue(outputs(recipe(helper, "compacting/compacted_waste_drum"), WasteRegistry.COMPACTED_WASTE_DRUM_ITEM.get()),
                "low-level waste compacts into a drum");
        helper.assertTrue(outputs(recipe(helper, "mixing/cemented_waste_drum"), WasteRegistry.CEMENTED_WASTE_DRUM_ITEM.get()),
                "intermediate-level waste is cemented");
        helper.assertTrue(outputs(recipe(helper, "mixing/bentonite"), WasteRegistry.BENTONITE_ITEM.get()), "bentonite is mixed");
        ProcessingRecipe<?, ?> glass = recipe(helper, "vitrification/vitrified_waste_canister");
        helper.assertTrue(outputs(glass, WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM.get()), "high-level waste is vitrified");
        helper.assertTrue(glass.getRequiredHeat() != com.simibubi.create.content.processing.recipe.HeatCondition.NONE,
                "vitrification needs heat");
        helper.succeed();
    }

    /** A scrubber driven from below at full speed, checked once the network has spun up. */
    private static void withScrubber(GameTestHelper helper, java.util.function.Consumer<RadiationScrubberBlockEntity> check) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(SCRUBBER.below(), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(SCRUBBER.below());
        motorEntity.generatedSpeed.setValue(256);
        helper.setBlock(SCRUBBER, AllNuclearBlocks.RADIATION_SCRUBBER.get().defaultBlockState());
        helper.runAfterDelay(10, () -> {
            double per = Config.scrubberWastePer;
            try {
                Config.scrubberWastePer = Config.scrubberRate;
                Contamination.set(helper.getLevel().getChunk(new ChunkPos(helper.absolutePos(SCRUBBER)).x,
                        new ChunkPos(helper.absolutePos(SCRUBBER)).z), 10);
                check.accept(helper.getBlockEntity(SCRUBBER));
            } finally {
                Config.scrubberWastePer = per;
                ContaminationTests.clean(helper, 1);
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_scrubberGivesLowLevelWaste", timeoutTicks = 100)
    public static void scrubberGivesLowLevelWaste(GameTestHelper helper) {
        withScrubber(helper, scrubber -> {
            scrubber.scrub();
            ItemStack out = scrubber.getOutput().getStackInSlot(0);
            helper.assertTrue(out.is(WasteRegistry.LOW_LEVEL_WASTE.get()) && out.getCount() == 1,
                    "what the scrubber absorbs is low-level waste, got " + out);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_scrubberWithOldWasteWaits", timeoutTicks = 100)
    public static void scrubberWithOldWasteWaitsToBeEmptied(GameTestHelper helper) {
        withScrubber(helper, scrubber -> {
            scrubber.getOutput().setStackInSlot(0, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 3));
            helper.assertTrue(scrubber.isFull(), "old waste in the slot leaves no room for the new kind");
            helper.assertTrue(scrubber.scrub() == 0, "so the scrubber waits");
            helper.assertTrue(scrubber.getAutomationHandler().extractItem(0, 64, false).getCount() == 3, "a hopper takes the old waste");
            helper.assertFalse(scrubber.isFull(), "then it has room again");
            helper.assertTrue(scrubber.scrub() > 0, "and scrubs on");
        });
    }
}
