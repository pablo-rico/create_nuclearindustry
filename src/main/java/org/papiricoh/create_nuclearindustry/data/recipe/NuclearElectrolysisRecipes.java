package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.ElectrolysisRecipe;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearElectrolysisRecipes extends StandardProcessingRecipeGen<ElectrolysisRecipe> {

    // Light water splits faster than heavy water, so the electrolyte left behind
    // concentrates deuterium: the historical Norsk Hydro route. Slow by design.
    GeneratedRecipe HEAVY_WATER = create("heavy_water", b -> b
            .require(Fluids.WATER, 1000)
            .output(AllNuclearFluids.HEAVY_WATER.get(), 10)
            .duration(100));

    // D2O -> D2 + 1/2 O2
    GeneratedRecipe DEUTERIUM = create("deuterium", b -> b
            .require(AllNuclearFluids.HEAVY_WATER.get(), 100)
            .output(AllNuclearFluids.DEUTERIUM.get(), 100)
            .duration(80));

    // Molten-salt electrolysis of the carbonate (via the chloride) yields lithium metal.
    GeneratedRecipe LITHIUM = create("lithium", b -> b
            .require(AllNuclearItems.LITHIUM_CARBONATE.get())
            .output(AllNuclearItems.LITHIUM.get())
            .duration(120));

    public NuclearElectrolysisRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllNuclearRecipeTypes.ELECTROLYSIS;
    }
}
