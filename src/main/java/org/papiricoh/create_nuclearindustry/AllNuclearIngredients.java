package org.papiricoh.create_nuclearindustry;

import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;

public class AllNuclearIngredients {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<MinEnrichmentIngredient>> MIN_ENRICHMENT =
            INGREDIENT_TYPES.register("min_enrichment", () -> new IngredientType<>(MinEnrichmentIngredient.CODEC));

    public static void init() {}
}
