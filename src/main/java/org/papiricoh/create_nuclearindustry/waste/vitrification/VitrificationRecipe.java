package org.papiricoh.create_nuclearindustry.waste.vitrification;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Melting waste into glass in a {@link VitrificationMelterBlockEntity}. Ingredients are
 * shapeless and counted by repetition (four waste ingredients take four waste items), drawn
 * from any of the melter's input slots. The heat condition is checked against the Blaze Burner
 * under the melter.
 */
public class VitrificationRecipe extends StandardProcessingRecipe<VitrificationInput> {

    public VitrificationRecipe(ProcessingRecipeParams params) {
        super(WasteRecipeTypes.VITRIFICATION, params);
    }

    @Override
    public boolean matches(VitrificationInput input, Level level) {
        return consumption(input.items()) != null;
    }

    /**
     * How many items each slot would give up for one run, or {@code null} if the slots cannot
     * pay for it. Each ingredient takes one item from the first slot that still has a match.
     */
    @Nullable
    public int[] consumption(List<ItemStack> slots) {
        int[] taken = new int[slots.size()];
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int slot = 0; slot < slots.size(); slot++) {
                ItemStack stack = slots.get(slot);
                if (!stack.isEmpty() && stack.getCount() > taken[slot] && ingredient.test(stack)) {
                    taken[slot]++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return taken;
    }

    /** True if {@code stack} is one of this recipe's ingredients. */
    public boolean uses(ItemStack stack) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected boolean canRequireHeat() {
        return true;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }
}
