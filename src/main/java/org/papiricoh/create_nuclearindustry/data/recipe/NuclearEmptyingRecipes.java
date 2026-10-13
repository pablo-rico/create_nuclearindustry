package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

/** Item drains discharge gas cells, and recover the tritium in old D-T pellets. */
@SuppressWarnings("unused")
public class NuclearEmptyingRecipes extends StandardProcessingRecipeGen<EmptyingRecipe> {

    GeneratedRecipe DEUTERIUM_CELL = create("deuterium_cell", b -> b
            .require(AllNuclearItems.DEUTERIUM_CELL.get())
            .output(AllNuclearItems.EMPTY_GAS_CELL.get())
            .output(AllNuclearFluids.DEUTERIUM.get(), 250));

    GeneratedRecipe TRITIUM_CELL = create("tritium_cell", b -> b
            .require(AllNuclearItems.TRITIUM_CELL.get())
            .output(AllNuclearItems.EMPTY_GAS_CELL.get())
            .output(AllNuclearFluids.TRITIUM.get(), 250));

    GeneratedRecipe LIQUID_HELIUM_CELL = create("liquid_helium_cell", b -> b
            .require(AllNuclearItems.LIQUID_HELIUM_CELL.get())
            .output(AllNuclearItems.EMPTY_GAS_CELL.get())
            .output(AllNuclearFluids.LIQUID_HELIUM.get(), 250));

    // Pellets from the old fusion ring are no longer burnt; their tritium seeds a tokamak.
    GeneratedRecipe DT_FUEL_PELLET = create("dt_fuel_pellet", b -> b
            .require(AllNuclearItems.DT_FUEL_PELLET.get())
            .output(AllNuclearItems.SPENT_DT_PELLET.get())
            .output(AllNuclearFluids.TRITIUM.get(), 250));

    public NuclearEmptyingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.EMPTYING;
    }
}
