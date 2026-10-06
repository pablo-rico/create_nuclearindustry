package org.papiricoh.create_nuclearindustry.enrichment.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.papiricoh.create_nuclearindustry.AllNuclearIngredients;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Matches items whose enrichment lies in {@code [min_enrichment, max_enrichment)}.
 * {@code max_enrichment} is optional, so recipes written before it existed still load.
 */
public record MinEnrichmentIngredient(Ingredient base, float minEnrichment, float maxEnrichment) implements ICustomIngredient {
    private static final float UNBOUNDED = 1000.0f;

    public static final MapCodec<MinEnrichmentIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(MinEnrichmentIngredient::base),
            Codec.FLOAT.fieldOf("min_enrichment").forGetter(MinEnrichmentIngredient::minEnrichment),
            Codec.FLOAT.optionalFieldOf("max_enrichment", UNBOUNDED).forGetter(MinEnrichmentIngredient::maxEnrichment)
    ).apply(instance, MinEnrichmentIngredient::new));

    public MinEnrichmentIngredient(Ingredient base, float minEnrichment) {
        this(base, minEnrichment, UNBOUNDED);
    }

    /** Any of {@code item} enriched within {@code tier}. */
    public static MinEnrichmentIngredient tier(ItemLike item, EnrichmentTier tier) {
        return tier(Ingredient.of(item), tier);
    }

    public static MinEnrichmentIngredient tier(Ingredient base, EnrichmentTier tier) {
        return new MinEnrichmentIngredient(base, tier.min(), tier == EnrichmentTier.WEAPONS ? UNBOUNDED : tier.max());
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!base.test(stack)) {
            return false;
        }
        float enrichment = UraniumItem.getEnrichment(stack);
        return enrichment >= minEnrichment && enrichment < maxEnrichment;
    }

    @Override
    public Stream<ItemStack> getItems() {
        float shown = Math.max(minEnrichment, EnrichmentTier.of(minEnrichment).nominal());
        return Arrays.stream(base.getItems()).map(stack -> UraniumItem.withEnrichment(stack, shown));
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
