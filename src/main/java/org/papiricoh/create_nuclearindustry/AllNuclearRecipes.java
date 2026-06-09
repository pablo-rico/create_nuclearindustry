package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.EnrichedUraniumBlendRecipe;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.WeaponsGradeUraniumCoreRecipe;

public class AllNuclearRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugingRecipe>> CENTRIFUGING_TYPE =
            TYPES.register("centrifuging", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Create_NuclearIndustry.MODID + ":centrifuging";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugingRecipe>> CENTRIFUGING_SERIALIZER =
            SERIALIZERS.register("centrifuging", CentrifugingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnrichedUraniumBlendRecipe>> ENRICHED_URANIUM_BLEND_SERIALIZER =
            SERIALIZERS.register("enriched_uranium_blend", () -> new SimpleCraftingRecipeSerializer<>(EnrichedUraniumBlendRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WeaponsGradeUraniumCoreRecipe>> WEAPONS_GRADE_URANIUM_CORE_SERIALIZER =
            SERIALIZERS.register("weapons_grade_uranium_core", () -> new SimpleCraftingRecipeSerializer<>(WeaponsGradeUraniumCoreRecipe::new));

    public static void init() {}
}
