package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.crafting.Ingredient;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

@SuppressWarnings("unused")
public class NuclearSequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {

    {
        // An iron cladding sheet is loaded with two pellets and pressed shut.
        for (EnrichmentTier tier : new EnrichmentTier[]{EnrichmentTier.LOW, EnrichmentTier.HIGH}) {
            Ingredient pellet = MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM_FUEL_PELLET.get(), tier).toVanilla();
            create(tier.id() + "_uranium_reactor_fuel", b -> b
                    .require(CreateItems.get("iron_sheet"))
                    .transitionTo(AllNuclearItems.INCOMPLETE_URANIUM_REACTOR_FUEL.get())
                    .addOutput(enriched(AllNuclearItems.URANIUM_REACTOR_FUEL.get(), tier), 1)
                    .loops(1)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(pellet))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(pellet))
                    .addStep(PressingRecipe::new, step -> step));
        }
    }

    public NuclearSequencedAssemblyRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
