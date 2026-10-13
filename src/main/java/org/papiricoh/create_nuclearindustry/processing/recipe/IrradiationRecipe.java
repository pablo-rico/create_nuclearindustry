package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/**
 * Bulk irradiation: a fan blowing past a strongly radioactive block bathes the items in its air
 * current in neutrons, breeding or activating them. One input, up to twelve chance outputs, like
 * Create's splashing.
 */
public class IrradiationRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    public IrradiationRecipe(ProcessingRecipeParams params) {
        super(AllNuclearRecipeTypes.IRRADIATION, params);
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
