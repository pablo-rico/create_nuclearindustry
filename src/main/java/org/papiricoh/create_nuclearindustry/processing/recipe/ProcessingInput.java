package org.papiricoh.create_nuclearindustry.processing.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a single-slot, single-tank kinetic machine offers to its recipes. */
public record ProcessingInput(ItemStack item, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty() && fluid.isEmpty();
    }
}
