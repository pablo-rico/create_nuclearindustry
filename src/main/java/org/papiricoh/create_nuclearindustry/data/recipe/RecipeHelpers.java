package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

final class RecipeHelpers {

    private RecipeHelpers() {}

    /** {@code item} carrying the nominal enrichment of {@code tier}. */
    static ItemStack enriched(ItemLike item, EnrichmentTier tier) {
        return enriched(item, tier, 1);
    }

    static ItemStack enriched(ItemLike item, EnrichmentTier tier, int count) {
        ItemStack stack = new ItemStack(item, count);
        UraniumItem.setEnrichment(stack, tier.nominal());
        return stack;
    }
}
