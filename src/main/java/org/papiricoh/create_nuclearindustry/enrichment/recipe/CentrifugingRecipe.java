package org.papiricoh.create_nuclearindustry.enrichment.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.Optional;

public class CentrifugingRecipe implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CentrifugingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CentrifugingRecipe::ingredient),
            com.mojang.serialization.Codec.FLOAT.optionalFieldOf("increment", 0.25f).forGetter(CentrifugingRecipe::increment),
            com.mojang.serialization.Codec.FLOAT.optionalFieldOf("max_enrichment", 98.0f).forGetter(CentrifugingRecipe::maxEnrichment),
            com.mojang.serialization.Codec.FLOAT.optionalFieldOf("result_enrichment").forGetter(CentrifugingRecipe::resultEnrichment),
            com.mojang.serialization.Codec.INT.optionalFieldOf("processing_time", 2000).forGetter(CentrifugingRecipe::processingTime)
    ).apply(instance, CentrifugingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    private final Ingredient ingredient;
    private final float increment;
    private final float maxEnrichment;
    private final Optional<Float> resultEnrichment;
    private final int processingTime;

    public CentrifugingRecipe(Ingredient ingredient, float increment, float maxEnrichment, Optional<Float> resultEnrichment, int processingTime) {
        this.ingredient = ingredient;
        this.increment = increment;
        this.maxEnrichment = maxEnrichment;
        this.resultEnrichment = resultEnrichment;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        if (!ingredient.test(stack)) {
            return false;
        }
        return resultEnrichment.isPresent() || UraniumItem.getEnrichment(stack) < maxEnrichment;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
        ItemStack result = new ItemStack(AllNuclearItems.URANIUM.get());
        float enrichment = resultEnrichment.orElseGet(() ->
                Math.min(maxEnrichment, UraniumItem.getEnrichment(input.item()) + increment));
        UraniumItem.setEnrichment(result, enrichment);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        ItemStack result = new ItemStack(AllNuclearItems.URANIUM.get());
        UraniumItem.setEnrichment(result, maxEnrichment);
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllNuclearRecipes.CENTRIFUGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AllNuclearRecipes.CENTRIFUGING_TYPE.get();
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public float increment() {
        return increment;
    }

    public float maxEnrichment() {
        return maxEnrichment;
    }

    public Optional<Float> resultEnrichment() {
        return resultEnrichment;
    }

    public int processingTime() {
        return processingTime;
    }

    public static class Serializer implements RecipeSerializer<CentrifugingRecipe> {
        @Override
        public MapCodec<CentrifugingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
