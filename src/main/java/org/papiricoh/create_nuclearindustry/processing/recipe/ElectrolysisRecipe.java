package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/** Electrolytic cell: heavy water enrichment, deuterium release, lithium reduction. */
public class ElectrolysisRecipe extends NuclearProcessingRecipe {

    public ElectrolysisRecipe(ProcessingRecipeParams params) {
        super(AllNuclearRecipeTypes.ELECTROLYSIS, params);
    }
}
