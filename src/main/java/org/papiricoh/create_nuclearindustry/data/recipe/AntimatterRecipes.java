package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes for antimatter containment and use: glass cylinders between copper-wound magnet
 * heads, each trap built round the one below it, with superconductor for the bigger fields; the
 * Annihilation Generator and the Antiproton Injector are built round a small trap. The Creative RF
 * Generator has none on purpose.
 */
public class AntimatterRecipes extends RecipeProvider {

    public AntimatterRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // The vial: a glass ampoule between two tiny copper heads.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.ANTIMATTER_VIAL.get(), 2)
                .pattern("N")
                .pattern("B")
                .pattern("N")
                .define('N', CreateItems.get("copper_nugget"))
                .define('B', Items.GLASS_BOTTLE)
                .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                .save(output, id("crafting/antimatter_vial"));
        // Small trap: a glass cylinder between two magnet heads, its field kept by an electron tube.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())
                .pattern("SCS")
                .pattern("GEG")
                .pattern("SCS")
                .define('S', CreateItems.get("iron_sheet"))
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/small_penning_trap"));
        // Medium trap: a small one stretched between superconducting heads and a brass clamp.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.PENNING_TRAP_ITEM.get())
                .pattern("UGU")
                .pattern("BTB")
                .pattern("UGU")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('G', Items.GLASS)
                .define('B', CreateItems.get("brass_sheet"))
                .define('T', AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())
                .unlockedBy("has_small_penning_trap", has(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/penning_trap"));
        // Large trap: two medium traps' coils on massive finned heads.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.LARGE_PENNING_TRAP_ITEM.get())
                .pattern("UCU")
                .pattern("TPT")
                .pattern("UCU")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('C', CreateItems.get("brass_casing"))
                .define('T', AntimatterRegistry.PENNING_TRAP_ITEM.get())
                .define('P', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_penning_trap", has(AntimatterRegistry.PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/large_penning_trap"));
        // Conduit: a glass tube ringed with copper magnets.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.ANTIMATTER_CONDUIT_ITEM.get(), 4)
                .pattern(" C ")
                .pattern("GEG")
                .pattern(" C ")
                .define('C', CreateItems.get("copper_sheet"))
                .define('G', Items.GLASS_PANE)
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_small_penning_trap", has(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/antimatter_conduit"));
        // Generator: a small trap for a chamber in a brass casing, superconducting field coils and a shaft out.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.ANNIHILATION_GENERATOR_ITEM.get())
                .pattern("UTU")
                .pattern("EBE")
                .pattern("PSP")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('T', AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())
                .define('E', CreateItems.get("electron_tube"))
                .define('B', CreateItems.get("brass_casing"))
                .define('P', CreateItems.get("precision_mechanism"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_small_penning_trap", has(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/annihilation_generator"));
        // Injector: a small trap on a neutral beam injector's nozzle, with superconducting kicker coils.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AntimatterRegistry.ANTIPROTON_INJECTOR_ITEM.get())
                .pattern(" T ")
                .pattern("UNU")
                .pattern(" E ")
                .define('T', AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('N', PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR_ITEM.get())
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_small_penning_trap", has(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/antiproton_injector"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
