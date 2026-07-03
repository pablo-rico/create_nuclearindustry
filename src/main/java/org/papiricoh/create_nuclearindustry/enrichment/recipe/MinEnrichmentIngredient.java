package org.papiricoh.create_nuclearindustry.enrichment.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.papiricoh.create_nuclearindustry.AllNuclearIngredients;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.Arrays;
import java.util.stream.Stream;

public record MinEnrichmentIngredient(Ingredient base, float minEnrichment) implements ICustomIngredient {
    public static final MapCodec<MinEnrichmentIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(MinEnrichmentIngredient::base),
            Codec.FLOAT.fieldOf("min_enrichment").forGetter(MinEnrichmentIngredient::minEnrichment)
    ).apply(instance, MinEnrichmentIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack) && UraniumItem.getEnrichment(stack) >= minEnrichment;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Arrays.stream(base.getItems())
                .map(stack -> UraniumItem.withEnrichment(stack, minEnrichment));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return AllNuclearIngredients.MIN_ENRICHMENT.get();
    }
}
