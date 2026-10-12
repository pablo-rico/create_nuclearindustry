package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

/** Spouts charge gas cells, the portable form of fusion fuel. */
@SuppressWarnings("unused")
public class NuclearFillingRecipes extends StandardProcessingRecipeGen<FillingRecipe> {

    GeneratedRecipe DEUTERIUM_CELL = create("deuterium_cell", b -> b
            .require(AllNuclearItems.EMPTY_GAS_CELL.get())
            .require(AllNuclearFluids.DEUTERIUM.get(), 250)
            .output(AllNuclearItems.DEUTERIUM_CELL.get()));

    GeneratedRecipe TRITIUM_CELL = create("tritium_cell", b -> b
            .require(AllNuclearItems.EMPTY_GAS_CELL.get())
            .require(AllNuclearFluids.TRITIUM.get(), 250)
            .output(AllNuclearItems.TRITIUM_CELL.get()));

    GeneratedRecipe LIQUID_HELIUM_CELL = create("liquid_helium_cell", b -> b
            .require(AllNuclearItems.EMPTY_GAS_CELL.get())
            .require(AllNuclearFluids.LIQUID_HELIUM.get(), 250)
            .output(AllNuclearItems.LIQUID_HELIUM_CELL.get()));

    public NuclearFillingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.FILLING;
    }
}
