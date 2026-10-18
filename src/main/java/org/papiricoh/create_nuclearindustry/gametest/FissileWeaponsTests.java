package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.WeaponsGradeUraniumCoreRecipe;
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Warheads take weapons-grade cores, uranium or plutonium, each worth the metal cast into it: eight
 * units, so six make a full warhead. Loose metal has to be cast into a core first.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FissileWeaponsTests {
    private static final BlockPos BOMB = new BlockPos(2, 1, 2);
    private static final BlockPos PAD = new BlockPos(6, 1, 6);

    private static ItemStack uranium(float enrichment, int count) {
        return UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get(), count), enrichment);
    }

    private static ItemStack uraniumCore(int count) {
        return new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get(), count);
    }

    private static ItemStack plutoniumCore(int count) {
        return new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get(), count);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void bombAndPadTakeCoresNotLooseMetal(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        int warhead = LaunchPadBlockEntity.FIRST_URANIUM_SLOT;
        for (ItemStack core : List.of(uraniumCore(1), plutoniumCore(1))) {
            helper.assertTrue(bomb.isValidCharge(core) && pad.isItemValid(warhead, core), "a core arms both: " + core);
        }
        for (ItemStack metal : List.of(new ItemStack(AllNuclearItems.PLUTONIUM.get()), uranium(90f, 1))) {
            helper.assertFalse(bomb.isValidCharge(metal) || pad.isItemValid(warhead, metal), "loose weapons metal arms neither: " + metal);
        }
        // Uranium short of weapons grade is no fission warhead, only a dirty missile's load.
        helper.assertFalse(bomb.isValidCharge(uranium(20f, 1)), "20% uranium does not arm the bomb");
        helper.assertTrue(pad.isItemValid(warhead, uranium(20f, 1)) && !pad.isValidFissile(uranium(20f, 1)),
                "the pad takes it as radiological material");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aCoreIsWorthItsMetal(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        bomb.getInventory().setStackInSlot(0, uraniumCore(1));
        helper.assertTrue(bomb.getValidUraniumCount() == WeaponsGradeUraniumCoreRecipe.REQUIRED_URANIUM,
                "a uranium core is its eight uranium, got " + bomb.getValidUraniumCount());
        bomb.getInventory().setStackInSlot(0, plutoniumCore(1));
        helper.assertTrue(bomb.getValidUraniumCount() == 4 * FissileMaterial.PLUTONIUM_UNIT_WEIGHT,
                "a plutonium core is its four plutonium, each worth two, got " + bomb.getValidUraniumCount());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void sixCoresAreAFullWarhead(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        ItemStack left = bomb.getInventory().insertItem(0, uraniumCore(3), false);
        helper.assertTrue(left.getCount() == 1, "a slot holds two cores, " + left.getCount() + " left over");
        bomb.getInventory().insertItem(1, plutoniumCore(2), false);
        bomb.getInventory().insertItem(2, uraniumCore(2), false);
        helper.assertTrue(bomb.getCoreCount() == NuclearBombBlockEntity.MAX_CORES && bomb.getValidUraniumCount() == 48,
                "six cores fill the bomb's 48 units, got " + bomb.getValidUraniumCount());
        helper.assertTrue(bomb.getEstimatedPower() == WarheadStats.power(48, 90f) && bomb.getEstimatedRadius() == 250,
                "and give a full warhead's blast");
        helper.succeed();
    }

    /** A uranium core keeps the leanest enrichment cast into it, so richer metal still makes a stronger core. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aUraniumCoreKeepsItsEnrichment(GameTestHelper helper) {
        CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/weapons_grade_uranium_core"))
                .orElseThrow().value();
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(7, uranium(96f, 1)));
        grid.add(uranium(93f, 1));
        grid.add(ItemStack.EMPTY);
        ItemStack core = recipe.assemble(CraftingInput.of(3, 3, grid), helper.getLevel().registryAccess());
        helper.assertTrue(Math.abs(FissileMaterial.effectiveEnrichment(core) - 93f) < 0.01f,
                "the core counts at 93%, got " + FissileMaterial.effectiveEnrichment(core));
        helper.assertTrue(FissileMaterial.effectiveEnrichment(uraniumCore(1)) == WarheadStats.REQUIRED_ENRICHMENT,
                "a core with no enrichment noted counts as weapons grade");
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
        helper.assertTrue(uraniumCore(1).is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES)
                        && plutoniumCore(1).is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES),
                "both cores are warhead cores");
        if (CBCNuclearIntegration.isLoaded()) {
            helper.assertTrue(CannonShellRecipes.load(helper, plutoniumCore(1)) != null, "the nuclear cannon shell takes a plutonium core too");
        }
        helper.succeed();
    }

    /** A cannon shell goes off with the cores it was made with: one core is a smaller warhead than six. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aCannonShellCarriesItsCores(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        ItemStack core = uraniumCore(1);
        WarheadLoad one = CannonShellRecipes.load(helper, core);
        WarheadLoad six = CannonShellRecipes.load(helper, core, core, core, core, core, core);
        helper.assertTrue(one != null && six != null, "one to six cores make a shell");
        helper.assertTrue(one.cores() == 1 && six.cores() == 6, "and the shell counts them: " + one + ", " + six);
        helper.assertTrue(WarheadStats.horizontalRadius(one.units(), one.enrichment()) < WarheadStats.horizontalRadius(six.units(), six.enrichment()),
                "one core reaches less far than six");
        helper.assertTrue(six.units() == WarheadStats.MAX_URANIUM_UNITS, "six cores are a full warhead");
        helper.assertTrue(CannonShellRecipes.load(helper, core, core, core, core, core, core, core) == null, "seven cores do not fit a shell");
        helper.succeed();
    }

    /** A bomb and a pad loaded with loose metal before warheads took cores drop it beside them; their cores stay. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyLooseMetalIsEjected(GameTestHelper helper) {
        helper.setBlock(BOMB, AllNuclearBlocks.NUCLEAR_BOMB.get());
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(BOMB);
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        bomb.getInventory().setStackInSlot(0, uranium(90f, 16));
        bomb.getInventory().setStackInSlot(1, uraniumCore(2));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, new ItemStack(AllNuclearItems.PLUTONIUM.get(), 8));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT + 1, plutoniumCore(4));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(bomb.getInventory().getStackInSlot(0).isEmpty(), "the bomb's loose uranium is out");
            helper.assertTrue(bomb.getInventory().getStackInSlot(1).getCount() == 2, "its cores stay");
            helper.assertTrue(pad.getInventory().getStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT).isEmpty(),
                    "the pad's loose plutonium is out");
            helper.assertTrue(pad.getInventory().getStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT + 1).getCount() == 2,
                    "and the cores past two to a slot");
            AABB around = new AABB(helper.absolutePos(BOMB)).inflate(3).minmax(new AABB(helper.absolutePos(PAD)).inflate(3));
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(dropped == 16 + 8 + 2, "nothing is lost: " + dropped + " items dropped");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
            helper.succeed();
        });
    }
}
