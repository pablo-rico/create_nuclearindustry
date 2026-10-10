package org.papiricoh.create_nuclearindustry.waste.vitrification;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;
import java.util.Locale;

/** The waste area's own Create-style processing recipe types. */
public enum WasteRecipeTypes implements IRecipeTypeInfo {
    VITRIFICATION(VitrificationRecipe::new);

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializer;
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> type;

    <R extends StandardProcessingRecipe<?>> WasteRecipeTypes(StandardProcessingRecipe.Factory<R> factory) {
        String name = name().toLowerCase(Locale.ROOT);
        id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name);
        serializer = AllNuclearRecipes.SERIALIZERS.register(name, () -> new StandardProcessingRecipe.Serializer<>(factory));
        type = Types.TYPES.register(name, () -> RecipeType.simple(id));
    }

    /** Touches the enum so its entries are on the registers, and registers the recipe types. */
    public static void register(IEventBus modEventBus) {
        Types.TYPES.register(modEventBus);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializer.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    /** Every loaded vitrification recipe. */
    public static List<RecipeHolder<VitrificationRecipe>> vitrification(Level level) {
        RecipeType<VitrificationRecipe> recipeType = VITRIFICATION.getType();
        return level.getRecipeManager().getAllRecipesFor(recipeType);
    }

    private static final class Types {
        private static final DeferredRegister<RecipeType<?>> TYPES =
                DeferredRegister.create(Registries.RECIPE_TYPE, Create_NuclearIndustry.MODID);
    }
}
