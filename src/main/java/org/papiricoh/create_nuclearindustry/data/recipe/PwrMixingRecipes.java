package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

/** Primary coolant: water with boron dissolved in it, the soluble poison a pressurized reactor trims its power with. */
@SuppressWarnings("unused")
public class PwrMixingRecipes extends MixingRecipeGen {

    GeneratedRecipe PRIMARY_COOLANT = create("primary_coolant", b -> b
            .require(Fluids.WATER, 1000)
            .require(AllNuclearItems.BORON.get())
            .output(AllNuclearFluids.PRIMARY_COOLANT.get(), 1000));

    public PwrMixingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
