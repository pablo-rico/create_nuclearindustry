package org.papiricoh.create_nuclearindustry.processing;

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
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ElectrolysisRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.NuclearProcessingRecipe;

import java.util.List;
import java.util.Locale;

/** The mod's own Create-style processing recipe types. */
public enum AllNuclearRecipeTypes implements IRecipeTypeInfo {
    CENTRIFUGING(CentrifugingRecipe::new),
    ELECTROLYSIS(ElectrolysisRecipe::new);

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializer;
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> type;

    <R extends StandardProcessingRecipe<?>> AllNuclearRecipeTypes(StandardProcessingRecipe.Factory<R> factory) {
        String name = name().toLowerCase(Locale.ROOT);
        id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name);
        serializer = Registers.SERIALIZERS.register(name, () -> new StandardProcessingRecipe.Serializer<>(factory));
        type = Registers.TYPES.register(name, () -> RecipeType.simple(id));
    }

    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZERS.register(modEventBus);
        Registers.TYPES.register(modEventBus);
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

    /** Every loaded recipe of this type. */
    public <R extends NuclearProcessingRecipe> List<RecipeHolder<R>> all(Level level) {
        RecipeType<R> recipeType = getType();
        return level.getRecipeManager().getAllRecipesFor(recipeType);
    }

    private static final class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
                DeferredRegister.create(Registries.RECIPE_SERIALIZER, Create_NuclearIndustry.MODID);
        private static final DeferredRegister<RecipeType<?>> TYPES =
                DeferredRegister.create(Registries.RECIPE_TYPE, Create_NuclearIndustry.MODID);
    }
}
