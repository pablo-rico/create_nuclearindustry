package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CentrifugeTests {
    private static final BlockPos CENTRIFUGE = new BlockPos(2, 1, 2);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void naturalFeedSplitsIntoProductAndTails(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, CENTRIFUGE, AllNuclearBlocks.CENTRIFUGE.get(), 64);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(CENTRIFUGE);
        helper.runAfterDelay(2, () -> {
            IFluidHandler feed = centrifuge.getFluidHandler(Direction.UP);
            int filled = feed.fill(new FluidStack(AllNuclearFluids.URANIUM_HEXAFLUORIDE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "feed tank should accept natural UF6, accepted " + filled);
        });
        helper.succeedWhen(() -> {
            FluidStack product = centrifuge.getProductFluid();
            FluidStack tails = centrifuge.getTailsFluid();
            helper.assertTrue(product.is(AllNuclearFluids.LOW_ENRICHED_URANIUM_HEXAFLUORIDE.get()) && product.getAmount() == 250,
                    "expected 250 mB LEU, got " + product.getAmount() + " " + product.getHoverName().getString());
            helper.assertTrue(tails.is(AllNuclearFluids.DEPLETED_URANIUM_HEXAFLUORIDE.get()) && tails.getAmount() == 750,
                    "expected 750 mB depleted tails, got " + tails.getAmount());
            helper.assertTrue(centrifuge.getInputFluid().isEmpty(), "feed should be consumed");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void fullOutputStallsWithoutLosingInput(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, CENTRIFUGE, AllNuclearBlocks.CENTRIFUGE.get(), 128);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(CENTRIFUGE);
        helper.runAfterDelay(2, () -> {
            IItemHandler all = centrifuge.getItemHandler(null);
            all.insertItem(KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT, new ItemStack(Items.DIRT, 64), false);
            all.insertItem(KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT + 1, new ItemStack(Items.DIRT, 64), false);
            ItemStack lithium = new ItemStack(AllNuclearItems.LITHIUM.get(), 8);
            ItemStack rest = centrifuge.getItemHandler(Direction.UP).insertItem(0, lithium, false);
            helper.assertTrue(rest.isEmpty(), "centrifuge should accept lithium");
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(centrifuge.isOutputBlocked(), "centrifuge should report a full output");
            ItemStack input = centrifuge.getItem(KineticProcessingBlockEntity.INPUT_SLOT);
            helper.assertTrue(input.is(AllNuclearItems.LITHIUM.get()) && input.getCount() == 8,
                    "input must be kept while the output is full, got " + input);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyItemCentrifugeKeepsItsItems(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, CENTRIFUGE, AllNuclearBlocks.CENTRIFUGE.get(), 64);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(CENTRIFUGE);
        var registries = helper.getLevel().registryAccess();

        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.setStackInSlot(0, new ItemStack(AllNuclearItems.RAW_URANIUM.get(), 3));
        legacy.setStackInSlot(1, new ItemStack(AllNuclearItems.URANIUM.get(), 2));
        CompoundTag tag = centrifuge.saveWithoutMetadata(registries);
        tag.remove("Items");
        tag.put("inventory", legacy.serializeNBT(registries));
        tag.put("processing", new ItemStack(AllNuclearItems.URANIUM.get()).save(registries));
        centrifuge.loadCustomOnly(tag, registries);

        int raw = 0;
        int uranium = 0;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = centrifuge.getItem(slot);
            if (stack.is(AllNuclearItems.RAW_URANIUM.get())) raw += stack.getCount();
            if (stack.is(AllNuclearItems.URANIUM.get())) uranium += stack.getCount();
        }
        helper.assertTrue(raw == 3 && uranium == 3, "legacy items should be recovered, got raw=" + raw + " uranium=" + uranium);
        helper.succeed();
    }
}
