package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.WeaponsGradeUraniumCoreRecipe;

public class AllNuclearRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WeaponsGradeUraniumCoreRecipe>> WEAPONS_GRADE_URANIUM_CORE_SERIALIZER =
            SERIALIZERS.register("weapons_grade_uranium_core", () -> new SimpleCraftingRecipeSerializer<>(WeaponsGradeUraniumCoreRecipe::new));

    public static void init() {}
}
