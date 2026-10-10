package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;

import java.util.concurrent.CompletableFuture;

/**
 * Where the tokamak's first helium comes from: spinning liquid air separates the light gas from the
 * nitrogen. Real air holds only a few parts per million of helium; the recipe is generous so a first
 * charge is within reach. The nitrogen goes back to the cryogenic plant as pre-cooling.
 */
@SuppressWarnings("unused")
public class CryoCentrifugingRecipes extends StandardProcessingRecipeGen<CentrifugingRecipe> {

    GeneratedRecipe LIQUID_AIR = create("liquid_air", b -> b
            .require(AllNuclearFluids.LIQUID_AIR.get(), 1000)
            .output(AllNuclearFluids.LIQUID_NITROGEN.get(), 990)
            .output(AllNuclearFluids.HELIUM.get(), 10)
            .duration(200));

    public CryoCentrifugingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllNuclearRecipeTypes.CENTRIFUGING;
    }
}
