package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/**
 * Bulk acid leaching: a fan blowing through sulfuric acid washes trace minerals out of the items
 * in its air current, and contamination off them. One input, up to twelve chance outputs, like
 * Create's splashing.
 */
public class AcidLeachingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    public AcidLeachingRecipe(ProcessingRecipeParams params) {
        super(AllNuclearRecipeTypes.ACID_LEACHING, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && ingredients.get(0).test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 12;
    }
}
