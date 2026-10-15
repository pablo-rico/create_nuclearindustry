package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/**
 * A target bombarded in an accelerator's Target Station with a beam below the antiproton threshold:
 * the beam's photons and neutrons transmute it, uranium into molybdenum-99 for instance. One input,
 * one output. The processing duration holds the beam energy one item needs, in megajoules.
 */
public class TargetIrradiationRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    public TargetIrradiationRecipe(ProcessingRecipeParams params) {
        super(AllNuclearRecipeTypes.TARGET_IRRADIATION, params);
    }

    /** Beam energy deposited in the target per item made, kJ. */
    public double nominalKj() {
        return Math.max(1, getProcessingDuration()) * 1000.0;
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
        return 1;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }
}
