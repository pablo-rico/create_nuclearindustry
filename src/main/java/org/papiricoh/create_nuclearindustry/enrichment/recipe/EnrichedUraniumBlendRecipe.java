package org.papiricoh.create_nuclearindustry.enrichment.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

public class EnrichedUraniumBlendRecipe extends CustomRecipe {
    public static final float REQUIRED_ENRICHMENT = 20.0f;
    private static final int REQUIRED_URANIUM = 4;
    private static final int REQUIRED_BORON = 1;

    public EnrichedUraniumBlendRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int enrichedUranium = 0;
        int boron = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(stack) >= REQUIRED_ENRICHMENT) {
                enrichedUranium++;
            } else if (stack.is(AllNuclearItems.BORON.get())) {
                boron++;
            } else {
                return false;
            }
        }

        return enrichedUranium == REQUIRED_URANIUM && boron == REQUIRED_BORON;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        return new ItemStack(AllNuclearItems.ENRICHED_URANIUM_BLEND.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= REQUIRED_URANIUM + REQUIRED_BORON;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(AllNuclearItems.ENRICHED_URANIUM_BLEND.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllNuclearRecipes.ENRICHED_URANIUM_BLEND_SERIALIZER.get();
    }
}
