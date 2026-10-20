package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import java.util.concurrent.CompletableFuture;

/** Manufacture of the battery and its sealed interceptor rounds. */
public class DefenseRecipes extends RecipeProvider {
    public DefenseRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }
    @Override protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, DefenseRegistry.BATTERY_ITEM.get())
                .pattern("SPS").pattern("ECE").pattern("IAI")
                .define('S', CreateItems.get("sturdy_sheet")).define('P', CreateItems.get("precision_mechanism"))
                .define('E', CreateItems.get("electron_tube")).define('C', CreateItems.get("cogwheel"))
                .define('A', CreateItems.get("andesite_casing"))
                .define('I', CreateItems.get("iron_sheet"))
                .unlockedBy("has_precision_mechanism", has(CreateItems.get("precision_mechanism"))).save(output, id("crafting/interceptor_battery"));
    }
    private ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path); }
    public static net.minecraft.data.DataProvider assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new DefenseSequencedRecipes(output, registries);
    }
}

/** Create's sequenced-assembly generator has a different output shape from vanilla crafting. */
class DefenseSequencedRecipes extends SequencedAssemblyRecipeGen {
    {
        create("interceptor", b -> b.require(CreateItems.get("iron_sheet"))
                .transitionTo(DefenseRegistry.INCOMPLETE_INTERCEPTOR.get()).addOutput(DefenseRegistry.INTERCEPTOR.get(), 1).loops(1)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(Ingredient.of(SiloRegistry.PROPELLANT.get())))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("precision_mechanism")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("electron_tube")))
                .addStep(PressingRecipe::new, step -> step));
    }
    DefenseSequencedRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries, Create_NuclearIndustry.MODID); }
}
