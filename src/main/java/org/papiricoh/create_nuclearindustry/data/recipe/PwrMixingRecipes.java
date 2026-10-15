package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

/**
 * Primary coolant: water with boron dissolved in it, the soluble poison a pressurized reactor trims its
 * power with; and boric acid, the concentrated boron a shut-down core is held with.
 */
@SuppressWarnings("unused")
public class PwrMixingRecipes extends MixingRecipeGen {

    GeneratedRecipe PRIMARY_COOLANT = create("primary_coolant", b -> b
            .require(Fluids.WATER, 1000)
            .require(AllNuclearItems.BORON.get())
            .output(AllNuclearFluids.PRIMARY_COOLANT.get(), 1000));

    /** Boric acid: borax leached with a splash of sulfuric acid, the soluble boron a shut-down core is held with. */
    GeneratedRecipe BORIC_ACID = create("boric_acid", b -> b
            .require(Fluids.WATER, 1000)
            .require(AllNuclearItems.BORAX_SALT.get())
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 100)
            .output(AllNuclearFluids.BORIC_ACID.get(), 1000));

    public PwrMixingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
