package org.papiricoh.create_nuclearindustry.processing.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/** Gas centrifuge stage: splits a feed into an enriched product and depleted tails. */
public class CentrifugingRecipe extends NuclearProcessingRecipe {

    public CentrifugingRecipe(ProcessingRecipeParams params) {
        super(AllNuclearRecipeTypes.CENTRIFUGING, params);
    }
}
