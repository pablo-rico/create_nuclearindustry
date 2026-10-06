package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearWashingRecipes extends StandardProcessingRecipeGen<SplashingRecipe> {

    GeneratedRecipe URANIUM_CONCENTRATE = create("uranium_concentrate", b -> b
            .require(AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(AllNuclearItems.URANIUM_CONCENTRATE.get())
            .output(0.3f, AllNuclearItems.URANIUM_CONCENTRATE.get()));

    // Beryl crystals wash out of the same pegmatite.
    GeneratedRecipe SPODUMENE_CONCENTRATE = create("spodumene_concentrate", b -> b
            .require(AllNuclearItems.CRUSHED_SPODUMENE.get())
            .output(AllNuclearItems.SPODUMENE_CONCENTRATE.get())
            .output(0.12f, AllNuclearItems.BERYLLIUM.get()));

    GeneratedRecipe BORON = create("boron", b -> b
            .require(AllNuclearItems.BORAX_SALT.get())
            .output(0.5f, AllNuclearItems.BORON.get()));

    public NuclearWashingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.SPLASHING;
    }
}
