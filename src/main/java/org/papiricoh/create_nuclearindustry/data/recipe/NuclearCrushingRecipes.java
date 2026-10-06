package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearCrushingRecipes extends StandardProcessingRecipeGen<CrushingRecipe> {

    GeneratedRecipe URANIUM_ORE = create("uranium_ore", b -> b
            .require(AllNuclearBlocks.URANIUM_ORE.get())
            .output(AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.75f, AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.6f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .output(0.125f, Items.COBBLED_DEEPSLATE)
            .duration(350));

    // Uraninite veins are hydrothermal: fluorite gangue comes up with the ore.
    GeneratedRecipe RAW_URANIUM = create("raw_uranium", b -> b
            .require(AllNuclearTags.Items.RAW_URANIUM)
            .output(AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.6f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .duration(400));

    GeneratedRecipe BORAX_ORE = create("borax_ore", b -> b
            .require(AllNuclearBlocks.BORAX_ORE.get())
            .output(AllNuclearItems.BORAX_SALT.get(), 3)
            .output(0.5f, AllNuclearItems.BORAX_SALT.get())
            .output(0.125f, Items.COBBLESTONE)
            .duration(250));

    public NuclearCrushingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.CRUSHING;
    }
}
