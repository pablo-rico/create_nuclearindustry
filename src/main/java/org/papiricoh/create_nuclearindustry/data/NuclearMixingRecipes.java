package org.papiricoh.create_nuclearindustry.data;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearMixingRecipes extends MixingRecipeGen {

    // Gunpowder is the only sulfur source in vanilla; burnt with water it gives oleum-strength acid.
    GeneratedRecipe SULFURIC_ACID = create("sulfuric_acid", b -> b
            .require(Fluids.WATER, 500)
            .require(Items.GUNPOWDER)
            .output(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .requiresHeat(HeatCondition.HEATED));

    // CaF2 + H2SO4 -> 2 HF + CaSO4
    GeneratedRecipe HYDROFLUORIC_ACID = create("hydrofluoric_acid", b -> b
            .require(AllNuclearItems.FLUORITE.get())
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .output(AllNuclearFluids.HYDROFLUORIC_ACID.get(), 500)
            .requiresHeat(HeatCondition.HEATED));

    public NuclearMixingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
