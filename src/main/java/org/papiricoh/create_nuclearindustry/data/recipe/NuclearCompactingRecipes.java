package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearCompactingRecipes extends StandardProcessingRecipeGen<BasinRecipe> {

    // Waste is sealed into a shielded drum for storage.
    GeneratedRecipe WASTE_BARREL = create("radioactive_waste_barrel", b -> b
            .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
            .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
            .require(CreateItems.get("iron_sheet")).require(CreateItems.get("iron_sheet"))
            .require(CreateItems.get("iron_sheet")).require(CreateItems.get("iron_sheet"))
            .output(AllNuclearItems.RADIOACTIVE_WASTE_BARREL.get()));

    public NuclearCompactingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.COMPACTING;
    }
}
