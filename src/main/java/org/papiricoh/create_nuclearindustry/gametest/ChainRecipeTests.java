package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.List;
import java.util.Optional;

/** Every step of the fuel chain is loaded, and enriched outputs carry the right tier. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ChainRecipeTests {

    private static final List<String> CHAIN = List.of(
            "crushing/uranium_ore",
            "crushing/raw_uranium",
            "splashing/uranium_concentrate",
            "mixing/sulfuric_acid",
            "mixing/hydrofluoric_acid",
            "mixing/yellowcake",
            "mixing/uranium_hexafluoride",
            "centrifuging/low_uranium_hexafluoride",
            "centrifuging/high_uranium_hexafluoride",
            "centrifuging/weapons_uranium_hexafluoride",
            "mixing/low_uranium_dioxide",
            "mixing/weapons_uranium_metal",
            "pressing/low_uranium_fuel_pellet",
            "pressing/high_uranium_fuel_pellet",
            "sequenced_assembly/low_uranium_reactor_fuel",
            "sequenced_assembly/high_uranium_reactor_fuel",
            "crushing/raw_spodumene",
            "splashing/spodumene_concentrate",
            "mixing/lithium_carbonate",
            "electrolysis/lithium",
            "centrifuging/lithium_6",
            "electrolysis/heavy_water",
            "electrolysis/deuterium",
            "filling/deuterium_cell",
            "emptying/tritium_cell");

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyChainRecipeIsLoaded(GameTestHelper helper) {
        for (String path : CHAIN) {
            helper.assertTrue(recipe(helper, path).isPresent(), "missing recipe " + path);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void pelletPressingKeepsTheTier(GameTestHelper helper) {
        Recipe<?> pressing = recipe(helper, "pressing/high_uranium_fuel_pellet").orElseThrow().value();
        ItemStack result = pressing.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(AllNuclearItems.URANIUM_FUEL_PELLET.get()), "pressing should make pellets");
        helper.assertTrue(UraniumItem.getEnrichment(result) == EnrichmentTier.HIGH.nominal(),
                "HEU pellet should be 20%, got " + UraniumItem.getEnrichment(result));

        var ingredient = pressing.getIngredients().get(0);
        helper.assertTrue(ingredient.test(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM_DIOXIDE.get()), 20f)),
                "HEU oxide should match");
        helper.assertFalse(ingredient.test(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM_DIOXIDE.get()), 5f)),
                "LEU oxide must not match the HEU recipe");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void weaponsGradeMetalFeedsTheWarheads(GameTestHelper helper) {
        Recipe<?> reduction = recipe(helper, "mixing/weapons_uranium_metal").orElseThrow().value();
        ItemStack metal = reduction.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(metal.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(metal) >= 90f,
                "weapons-grade reduction should give 90% uranium, got " + metal);
        helper.succeed();
    }

    private static Optional<RecipeHolder<?>> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }
}
