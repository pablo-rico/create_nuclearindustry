package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

/** The same warheads take plutonium: each piece counts as two units of 90% uranium. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FissileWeaponsTests {
    private static final BlockPos BOMB = new BlockPos(2, 1, 2);
    private static final BlockPos PAD = new BlockPos(6, 1, 6);

    private static ItemStack uranium(float enrichment, int count) {
        return UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get(), count), enrichment);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void bombAndPadAcceptPlutonium(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        helper.setBlock(PAD, AllNuclearBlocks.LAUNCH_PAD.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        ItemStack plutonium = new ItemStack(AllNuclearItems.PLUTONIUM.get());
        helper.assertTrue(bomb.isValidFissile(plutonium) && pad.isValidFissile(plutonium), "plutonium arms both");
        helper.assertTrue(bomb.isValidUranium(uranium(90f, 1)), "90% uranium still arms the bomb");
        helper.assertFalse(bomb.isValidFissile(uranium(20f, 1)) || pad.isValidFissile(uranium(20f, 1)), "20% uranium does not");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plutoniumCountsDouble(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        bomb.getInventory().setStackInSlot(0, new ItemStack(AllNuclearItems.PLUTONIUM.get(), 8));
        helper.assertTrue(bomb.getValidUraniumCount() == 16, "8 plutonium are 16 units, got " + bomb.getValidUraniumCount());
        helper.assertTrue(bomb.getEstimatedPower() == WarheadStats.power(16, 90f), "power as 16 units of 90% uranium");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fullPlutoniumBombMatchesFullUraniumBomb(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        for (int slot = 0; slot < NuclearBombBlockEntity.SLOT_COUNT; slot++) {
            bomb.getInventory().setStackInSlot(slot, new ItemStack(AllNuclearItems.PLUTONIUM.get(), 8));
        }
        int plutoniumUnits = bomb.getValidUraniumCount();
        float plutoniumPower = bomb.getEstimatedPower();
        for (int slot = 0; slot < NuclearBombBlockEntity.SLOT_COUNT; slot++) {
            bomb.getInventory().setStackInSlot(slot, uranium(90f, 16));
        }
        helper.assertTrue(plutoniumUnits == 48 && bomb.getValidUraniumCount() == 48, "both fill the bomb's 48 units");
        helper.assertTrue(plutoniumPower == bomb.getEstimatedPower(), "and give the same blast");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plutoniumCoreNeedsFourPieces(GameTestHelper helper) {
        ShapedRecipe recipe = (ShapedRecipe) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/weapons_grade_plutonium_core"))
                .orElseThrow().value();
        long plutonium = recipe.getIngredients().stream().filter(i -> i.test(new ItemStack(AllNuclearItems.PLUTONIUM.get()))).count();
        helper.assertTrue(plutonium == 4, "a plutonium core takes 4 pieces, got " + plutonium);
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()),
                "it makes a plutonium core");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void bothCoresAreWarheadCores(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()).is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES)
                        && new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()).is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES),
                "both cores are warhead cores");
        if (CBCNuclearIntegration.isLoaded()) {
            ItemStack plutonium = new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
            helper.assertTrue(CannonShellRecipes.load(helper, plutonium) != null, "the nuclear cannon shell takes a plutonium core too");
        }
        helper.succeed();
    }

    /** A cannon shell goes off with the cores it was made with: one core is a smaller warhead than four. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aCannonShellCarriesItsCores(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        ItemStack core = new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get());
        WarheadLoad one = CannonShellRecipes.load(helper, core);
        WarheadLoad four = CannonShellRecipes.load(helper, core, core, core, core);
        helper.assertTrue(one != null && four != null, "one to four cores make a shell");
        helper.assertTrue(one.cores() == 1 && four.cores() == 4, "and the shell counts them: " + one + ", " + four);
        helper.assertTrue(WarheadStats.horizontalRadius(one.units(), one.enrichment()) < WarheadStats.horizontalRadius(four.units(), four.enrichment()),
                "one core reaches less far than four");
        helper.assertTrue(four.units() == WarheadStats.MAX_URANIUM_UNITS, "four cores are a full warhead, as the shell used to be");
        helper.assertTrue(CannonShellRecipes.load(helper, core, core, core, core, core) == null, "five cores do not fit a shell");
        helper.succeed();
    }
}
