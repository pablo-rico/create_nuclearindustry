package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A recipe for a kinetic machine with one item slot and one input tank. Repeated item
 * ingredients mean "this many of the same item": every ingredient must accept the stack
 * in the slot and the stack must hold at least that many items.
 *
 * <p>Fluid results go to the machine's product tank (first) and tails tank (second).
 */
public abstract class NuclearProcessingRecipe extends StandardProcessingRecipe<ProcessingInput> {

    protected NuclearProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
        super(typeInfo, params);
    }

    @Override
    public boolean matches(ProcessingInput input, Level level) {
        return matchesItems(input.item()) && matchesFluid(input.fluid());
    }

    public boolean matchesItems(ItemStack stack) {
        if (ingredients.isEmpty()) {
            return true;
        }
        if (stack.getCount() < ingredients.size()) {
            return false;
        }
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.test(stack)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesFluid(FluidStack fluid) {
        if (fluidIngredients.isEmpty()) {
            return true;
        }
        return fluidIngredients.get(0).test(fluid);
    }

    /** True if this recipe could start with {@code stack} once enough of it is supplied. */
    public boolean acceptsItem(ItemStack stack) {
        return !ingredients.isEmpty() && ingredients.get(0).test(stack);
    }

    public boolean acceptsFluid(FluidStack fluid) {
        return !fluidIngredients.isEmpty() && fluidIngredients.get(0).ingredient().test(fluid);
    }

    public int itemCost() {
        return ingredients.size();
    }

    public int fluidCost() {
        return fluidIngredients.isEmpty() ? 0 : fluidIngredients.get(0).amount();
    }

    public FluidStack productFluid() {
        return fluidResults.isEmpty() ? FluidStack.EMPTY : fluidResults.get(0);
    }

    public FluidStack tailsFluid() {
        return fluidResults.size() < 2 ? FluidStack.EMPTY : fluidResults.get(1);
    }

    public SizedFluidIngredient fluidIngredient() {
        return fluidIngredients.isEmpty() ? null : fluidIngredients.get(0);
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 2;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }
}
