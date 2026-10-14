package org.papiricoh.create_nuclearindustry.enrichment.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;

public class WeaponsGradeUraniumCoreRecipe extends CustomRecipe {
    public static final int REQUIRED_URANIUM = 8;

    public WeaponsGradeUraniumCoreRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int validUranium = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(AllNuclearItems.URANIUM.get())
                    || UraniumItem.getEnrichment(stack) < WarheadStats.REQUIRED_ENRICHMENT) {
                return false;
            }
            validUranium++;
        }
        return validUranium == REQUIRED_URANIUM;
    }

    /** The core keeps the leanest enrichment of the metal cast into it. */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        float leanest = Float.MAX_VALUE;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                leanest = Math.min(leanest, UraniumItem.getEnrichment(stack));
            }
        }
        ItemStack core = new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get());
        if (leanest != Float.MAX_VALUE) {
            core.set(AllNuclearDataComponents.ENRICHMENT.get(), leanest);
        }
        return core;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= REQUIRED_URANIUM;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllNuclearRecipes.WEAPONS_GRADE_URANIUM_CORE_SERIALIZER.get();
    }
}
