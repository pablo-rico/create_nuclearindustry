package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

/** Crafting-table recipes for blocks and items added or changed by the rewrite. */
public class NuclearCraftingRecipes extends RecipeProvider {

    public NuclearCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    /** Depleted uranium metal (under 0.7%) as dense shielding blocks and heavy armour. */
    private void depletedUranium(RecipeOutput output) {
        Ingredient metal = MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED).toVanilla();
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.DEPLETED_URANIUM_BLOCK.get())
                .pattern("UUU")
                .pattern("UUU")
                .pattern("UUU")
                .define('U', metal)
                .unlockedBy("has_uranium", has(AllNuclearItems.URANIUM.get()))
                .save(output, id("crafting/depleted_uranium_block"));
        duArmor(output, metal, AllNuclearItems.DEPLETED_URANIUM_HELMET.get(), "UUU", "U U", null);
        duArmor(output, metal, AllNuclearItems.DEPLETED_URANIUM_CHESTPLATE.get(), "U U", "UUU", "UUU");
        duArmor(output, metal, AllNuclearItems.DEPLETED_URANIUM_LEGGINGS.get(), "UUU", "U U", "U U");
        duArmor(output, metal, AllNuclearItems.DEPLETED_URANIUM_BOOTS.get(), "U U", "U U", null);
    }

    private static void duArmor(RecipeOutput output, Ingredient metal, net.minecraft.world.item.Item result,
                                String top, String middle, String bottom) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result).pattern(top).pattern(middle);
        if (bottom != null) {
            builder.pattern(bottom);
        }
        builder.define('U', metal)
                .unlockedBy("has_uranium", has(AllNuclearItems.URANIUM.get()))
                .save(output, id("crafting/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result).getPath()));
    }

    /** Screens, the reactor panel, the alarm and the linker that ties them to machines. */
    private void controlRoom(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, AllNuclearItems.CONTROL_MONITOR.get(), 2)
                .pattern("PGP")
                .pattern("GEG")
                .pattern("PRP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('G', Items.GLASS_PANE)
                .define('E', CreateItems.get("electron_tube"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/control_monitor"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, AllNuclearItems.REACTOR_CONTROL_PANEL.get())
                .pattern("BBB")
                .pattern("ECE")
                .pattern("PRP")
                .define('B', Items.STONE_BUTTON)
                .define('E', CreateItems.get("electron_tube"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .define('P', CreateItems.get("brass_sheet"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/reactor_control_panel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, AllNuclearItems.NUCLEAR_ALARM.get())
                .pattern(" L ")
                .pattern("PNP")
                .pattern(" E ")
                .define('L', Items.REDSTONE_LAMP)
                .define('P', CreateItems.get("iron_sheet"))
                .define('N', Items.NOTE_BLOCK)
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/nuclear_alarm"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllNuclearItems.CONTROL_ROOM_LINKER.get())
                .pattern("  E")
                .pattern(" B ")
                .pattern("B  ")
                .define('E', CreateItems.get("electron_tube"))
                .define('B', CreateItems.get("brass_sheet"))
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/control_room_linker"));
    }

    /** Graphite moderator columns and beryllium reflector lining for the fission core. */
    private void moderation(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.GRAPHITE_MODERATOR.get())
                .pattern("GGG")
                .pattern("GGG")
                .pattern("GGG")
                .define('G', AllNuclearItems.GRAPHITE.get())
                .unlockedBy("has_graphite", has(AllNuclearItems.GRAPHITE.get()))
                .save(output, id("crafting/graphite_moderator"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.BERYLLIUM_REFLECTOR.get(), 2)
                .pattern("BPB")
                .pattern("PCP")
                .pattern("BPB")
                .define('B', AllNuclearItems.BERYLLIUM.get())
                .define('P', CreateItems.get("copper_sheet"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_beryllium", has(AllNuclearItems.BERYLLIUM.get()))
                .save(output, id("crafting/beryllium_reflector"));
    }

    /** Concrete and andesite for the shell; Create parts for the controller, ports and fans. */
    private void coolingTower(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.COOLING_TOWER_CASING.get(), 8)
                .pattern("SSS")
                .pattern("SAS")
                .pattern("SSS")
                .define('S', Items.SMOOTH_STONE)
                .define('A', CreateItems.get("andesite_alloy"))
                .unlockedBy("has_andesite_alloy", has(CreateItems.get("andesite_alloy")))
                .save(output, id("crafting/cooling_tower_casing"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.COOLING_TOWER_CONTROLLER.get())
                .pattern(" E ")
                .pattern("PCP")
                .pattern(" R ")
                .define('C', AllNuclearItems.COOLING_TOWER_CASING.get())
                .define('E', CreateItems.get("electron_tube"))
                .define('P', CreateItems.get("copper_sheet"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_cooling_tower_casing", has(AllNuclearItems.COOLING_TOWER_CASING.get()))
                .save(output, id("crafting/cooling_tower_controller"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.COOLING_TOWER_FLUID_PORT.get(), 2)
                .pattern(" P ")
                .pattern("PCP")
                .pattern(" P ")
                .define('C', AllNuclearItems.COOLING_TOWER_CASING.get())
                .define('P', CreateItems.get("fluid_pipe"))
                .unlockedBy("has_cooling_tower_casing", has(AllNuclearItems.COOLING_TOWER_CASING.get()))
                .save(output, id("crafting/cooling_tower_fluid_port"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.COOLING_TOWER_FAN.get())
                .pattern(" F ")
                .pattern(" C ")
                .pattern(" S ")
                .define('C', AllNuclearItems.COOLING_TOWER_CASING.get())
                .define('F', CreateItems.get("propeller"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_cooling_tower_casing", has(AllNuclearItems.COOLING_TOWER_CASING.get()))
                .save(output, id("crafting/cooling_tower_fan"));
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // The fuel channel no longer embeds fuel: assemblies are loaded through the fuel port.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.URANIUM_ROD.get(), 2)
                .pattern(" B ")
                .pattern("BCB")
                .pattern(" B ")
                .define('B', Items.IRON_BARS)
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/uranium_rod"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.ELECTROLYZER.get())
                .pattern(" P ")
                .pattern("ASA")
                .pattern(" C ")
                .define('P', CreateItems.get("fluid_pipe"))
                .define('A', CreateItems.get("copper_sheet"))
                .define('S', CreateItems.get("shaft"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/electrolyzer"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.EMPTY_GAS_CELL.get(), 2)
                .pattern(" C ")
                .pattern("CGC")
                .pattern(" C ")
                .define('C', CreateItems.get("copper_sheet"))
                .define('G', Items.GLASS)
                .unlockedBy("has_copper_sheet", has(CreateItems.get("copper_sheet")))
                .save(output, id("crafting/empty_gas_cell"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_CENTRAL_SOLENOID.get())
                .pattern(" S ")
                .pattern("ECE")
                .pattern(" S ")
                .define('S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('E', AllNuclearItems.FUSION_ELECTROMAGNET.get())
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .unlockedBy("has_superconductor", has(AllNuclearItems.SUPERCONDUCTOR_INGOT.get()))
                .save(output, id("crafting/fusion_central_solenoid"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_BREEDING_BLANKET.get())
                .requires(AllNuclearItems.LITHIUM_6_BREEDER_ASSEMBLY.get())
                .requires(AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .requires(AllNuclearItems.BERYLLIUM.get())
                .unlockedBy("has_breeder_assembly", has(AllNuclearItems.LITHIUM_6_BREEDER_ASSEMBLY.get()))
                .save(output, id("crafting/fusion_breeding_blanket"));

        // Old flat-ring corners become plain vessel segments.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_ACCELERATOR_SEGMENT.get())
                .requires(AllNuclearItems.FUSION_ACCELERATOR_CORNER.get())
                .unlockedBy("has_corner", has(AllNuclearItems.FUSION_ACCELERATOR_CORNER.get()))
                .save(output, id("crafting/fusion_accelerator_segment_from_corner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllNuclearItems.GEIGER_COUNTER.get())
                .pattern(" E ")
                .pattern("BTB")
                .pattern(" R ")
                .define('E', CreateItems.get("electron_tube"))
                .define('B', CreateItems.get("brass_sheet"))
                .define('T', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/geiger_counter"));

        // Lead-lined rubberised fabric, approximated with yellow wool and iron.
        hazmat(output, AllNuclearItems.HAZMAT_HELMET.get(), "WWW", "WGW", null);
        hazmat(output, AllNuclearItems.HAZMAT_CHESTPLATE.get(), "W W", "WIW", "WWW");
        hazmat(output, AllNuclearItems.HAZMAT_LEGGINGS.get(), "WWW", "W W", "W W");
        hazmat(output, AllNuclearItems.HAZMAT_BOOTS.get(), "W W", "I I", null);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.REINFORCED_TURBINE_GLASS.get(), 4)
                .pattern("PGP")
                .pattern("G G")
                .pattern("PGP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output, id("crafting/reinforced_turbine_glass"));

        turbines(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, AllNuclearItems.REACTOR_REDSTONE_PORT.get())
                .pattern(" R ")
                .pattern("QCQ")
                .pattern(" R ")
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .define('Q', Items.COMPARATOR)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/reactor_redstone_port"));

        coolingTower(output);
        depletedUranium(output);
        moderation(output);
        controlRoom(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.RTG.get())
                .pattern("PPP")
                .pattern("PCS")
                .pattern("PPP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/rtg"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.RADIATION_SCRUBBER.get())
                .pattern(" F ")
                .pattern("PCP")
                .pattern(" S ")
                .define('F', CreateItems.get("filter"))
                .define('P', CreateItems.get("iron_sheet"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/radiation_scrubber"));

        // Plutonium's smaller critical mass: four pieces make a core, against eight of uranium.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get())
                .pattern("PP")
                .pattern("PP")
                .define('P', AllNuclearItems.PLUTONIUM.get())
                .unlockedBy("has_plutonium", has(AllNuclearItems.PLUTONIUM.get()))
                .save(output, id("crafting/weapons_grade_plutonium_core"));

        // Legacy item from an unfinished recipe chain: turn any leftovers into usable oxide.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, enriched(AllNuclearItems.URANIUM_DIOXIDE.get(), EnrichmentTier.LOW))
                .requires(AllNuclearItems.ENRICHED_URANIUM_BLEND.get())
                .unlockedBy("has_blend", has(AllNuclearItems.ENRICHED_URANIUM_BLEND.get()))
                .save(output, id("crafting/uranium_dioxide_from_blend"));
    }

    /** Box turbines take a lot of casing, so casings come four at a time. */
    private static void turbines(RecipeOutput output) {
        shaped(output, AllNuclearItems.TURBINE_CASING.get(), 4, "turbine_casing",
                new String[]{"AAA", "ACA", "AAA"}, 'A', CreateItems.get("andesite_alloy"), 'C', AllNuclearItems.REACTOR_CASING.get());
        shaped(output, AllNuclearItems.TURBINE_ROTOR.get(), 1, "turbine_rotor",
                new String[]{" I ", "ISI", " I "}, 'I', Items.IRON_INGOT, 'S', CreateItems.get("shaft"));
        shaped(output, AllNuclearItems.TURBINE_FLUID_PORT.get(), 1, "turbine_fluid_port",
                new String[]{" C ", "CPC", " C "}, 'C', AllNuclearItems.TURBINE_CASING.get(), 'P', CreateItems.get("fluid_pipe"));
        shaped(output, AllNuclearItems.TURBINE_EXHAUST_PORT.get(), 1, "turbine_exhaust_port",
                new String[]{" V ", "CPC", " C "}, 'C', AllNuclearItems.TURBINE_CASING.get(), 'P', CreateItems.get("fluid_pipe"),
                'V', Items.IRON_BARS);
        shaped(output, AllNuclearItems.TURBINE_OUTPUT.get(), 1, "turbine_output",
                new String[]{" B ", "CRC", " S "}, 'B', CreateItems.get("brass_ingot"), 'C', AllNuclearItems.TURBINE_CASING.get(),
                'R', AllNuclearItems.TURBINE_ROTOR.get(), 'S', CreateItems.get("shaft"));
        shaped(output, AllNuclearItems.FUSION_TURBINE_CASING.get(), 4, "fusion_turbine_casing",
                new String[]{"CSC", "SAS", "CSC"}, 'C', CreateItems.get("brass_sheet"), 'S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get(),
                'A', AllNuclearItems.FUSION_CRYOSTAT_CASING.get());
        shaped(output, AllNuclearItems.FUSION_TURBINE_ROTOR.get(), 1, "fusion_turbine_rotor",
                new String[]{" S ", "RTR", " S "}, 'S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get(), 'R', AllNuclearItems.TURBINE_ROTOR.get(),
                'T', AllNuclearItems.TRITIUM_CELL.get());
        shaped(output, AllNuclearItems.FUSION_TURBINE_FLUID_PORT.get(), 1, "fusion_turbine_fluid_port",
                new String[]{" C ", "SPS", " C "}, 'C', AllNuclearItems.FUSION_TURBINE_CASING.get(), 'S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get(),
                'P', CreateItems.get("fluid_pipe"));
        shaped(output, AllNuclearItems.FUSION_PLASMA_TURBINE.get(), 1, "fusion_plasma_turbine",
                new String[]{"BTB", "TST", "BRB"}, 'B', CreateItems.get("brass_sheet"), 'T', AllNuclearItems.FUSION_TURBINE_ROTOR.get(),
                'S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get(), 'R', AllNuclearItems.FUSION_TURBINE_CASING.get());
    }

    /** {@code keys} alternates symbol and ingredient. */
    private static void shaped(RecipeOutput output, net.minecraft.world.level.ItemLike result, int count, String name,
                               String[] pattern, Object... keys) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, count);
        for (String row : pattern) {
            builder.pattern(row);
        }
        for (int i = 0; i < keys.length; i += 2) {
            builder.define((Character) keys[i], (net.minecraft.world.level.ItemLike) keys[i + 1]);
        }
        builder.unlockedBy("has_ingredient", has((net.minecraft.world.level.ItemLike) keys[1])).save(output, id("crafting/" + name));
    }

    private static void hazmat(RecipeOutput output, net.minecraft.world.item.Item result, String top, String middle, String bottom) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(top)
                .pattern(middle);
        if (bottom != null) {
            builder.pattern(bottom);
        }
        String all = top + middle + (bottom == null ? "" : bottom);
        builder.define('W', Items.YELLOW_WOOL);
        if (all.indexOf('G') >= 0) {
            builder.define('G', Items.GLASS_PANE);
        }
        if (all.indexOf('I') >= 0) {
            builder.define('I', Items.IRON_INGOT);
        }
        builder.unlockedBy("has_yellow_wool", has(Items.YELLOW_WOOL))
                .save(output, id("crafting/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result).getPath()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
