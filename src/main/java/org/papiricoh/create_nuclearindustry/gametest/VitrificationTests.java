package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationMelterBlockEntity;

/** The vitrification melter, heated by a real Create Blaze Burner underneath. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class VitrificationTests {
    private static final BlockPos BURNER = new BlockPos(6, 1, 6);
    private static final BlockPos MELTER = BURNER.above();

    /** A Blaze Burner with its blaze, idle (smouldering) until fed. */
    private static void placeBurner(GameTestHelper helper) {
        Block burner = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"));
        helper.setBlock(BURNER, burner.defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
    }

    private static boolean feedCoal(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BURNER);
        BlockState state = helper.getLevel().getBlockState(pos);
        return BlazeBurnerBlock.tryInsert(state, helper.getLevel(), pos, new ItemStack(Items.COAL), false, false, false)
                .getResult().consumesAction();
    }

    private static VitrificationMelterBlockEntity placeMelter(GameTestHelper helper) {
        helper.setBlock(MELTER, WasteRegistry.VITRIFICATION_MELTER.get().defaultBlockState());
        return helper.getBlockEntity(MELTER);
    }

    /** Loads one batch through the automation handler, as a funnel would. */
    private static void loadBatch(GameTestHelper helper, IItemHandler handler) {
        ItemStack[] batch = {new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 4), new ItemStack(Items.SAND, 2),
                new ItemStack(AllNuclearItems.BORAX_SALT.get(), 1)};
        for (ItemStack stack : batch) {
            ItemStack rest = stack;
            for (int slot = 0; slot < VitrificationMelterBlockEntity.INPUT_SLOTS && !rest.isEmpty(); slot++) {
                rest = handler.insertItem(slot, rest, false);
            }
            helper.assertTrue(rest.isEmpty(), "the melter should take " + stack);
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void melterVitrifiesOnALitBlazeBurner(GameTestHelper helper) {
        placeBurner(helper);
        VitrificationMelterBlockEntity melter = placeMelter(helper);
        helper.assertTrue(feedCoal(helper), "the burner should take coal");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(melter.heatLevel().isAtLeast(BlazeBurnerBlock.HeatLevel.FADING),
                    "a fed burner should heat the melter, got " + melter.heatLevel());
            loadBatch(helper, melter.getAutomationHandler());
        });
        helper.succeedWhen(() -> {
            ItemStack out = melter.getInventory().getStackInSlot(VitrificationMelterBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(out.is(WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM.get()) && out.getCount() == 1,
                    "one canister expected, got " + out);
            for (int slot = 0; slot < VitrificationMelterBlockEntity.INPUT_SLOTS; slot++) {
                helper.assertTrue(melter.getInventory().getStackInSlot(slot).isEmpty(), "the whole batch should be used");
            }
            ItemStack taken = melter.getAutomationHandler().extractItem(VitrificationMelterBlockEntity.OUTPUT_SLOT, 1, true);
            helper.assertTrue(taken.getCount() == 1, "a funnel should take the canister out");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void melterNeedsAHeatedBurner(GameTestHelper helper) {
        placeBurner(helper);
        VitrificationMelterBlockEntity melter = placeMelter(helper);
        helper.runAfterDelay(2, () -> loadBatch(helper, melter.getAutomationHandler()));
        helper.runAfterDelay(260, () -> {
            helper.assertFalse(melter.heatLevel().isAtLeast(BlazeBurnerBlock.HeatLevel.FADING),
                    "an unfed burner only smoulders, got " + melter.heatLevel());
            helper.assertFalse(melter.isMelting(), "nothing should melt without heat");
            helper.assertTrue(melter.getInventory().getStackInSlot(VitrificationMelterBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "no canister without heat");
            helper.assertTrue(melter.radiationStrength() > 0, "the waste waiting inside should irradiate");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void melterKeepsEachIngredientInItsOwnSlot(GameTestHelper helper) {
        VitrificationMelterBlockEntity melter = placeMelter(helper);
        helper.runAfterDelay(1, () -> {
            IItemHandler handler = melter.getAutomationHandler();
            ItemStack dirt = new ItemStack(Items.DIRT);
            helper.assertTrue(handler.insertItem(0, dirt, false).getCount() == 1, "dirt is no ingredient");
            ItemStack waste = new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 4);
            helper.assertTrue(handler.insertItem(0, waste, false).isEmpty(), "high-level waste goes in");
            helper.assertTrue(handler.insertItem(1, waste.copy(), false).getCount() == 4, "but only into one slot");
            helper.assertTrue(handler.insertItem(1, new ItemStack(Items.SAND), false).isEmpty(), "sand takes the next slot");
            helper.assertTrue(handler.insertItem(VitrificationMelterBlockEntity.OUTPUT_SLOT, new ItemStack(Items.SAND), false).getCount() == 1,
                    "nothing goes into the output");
            helper.assertTrue(handler.extractItem(0, 1, false).isEmpty(), "inputs cannot be pulled out by automation");
            helper.succeed();
        });
    }
}
