package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.CentrifugingRecipe;

import java.util.List;

@JeiPlugin
public class NuclearJeiPlugin implements IModPlugin {
    public static final RecipeType<CentrifugingRecipe> CENTRIFUGING =
            RecipeType.create(Create_NuclearIndustry.MODID, "centrifuging", CentrifugingRecipe.class);
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(AllNuclearItems.URANIUM.get(),
                (stack, context) -> String.format("enrichment=%.2f", UraniumItem.getEnrichment(stack)));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CentrifugingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        List<CentrifugingRecipe> recipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(AllNuclearRecipes.CENTRIFUGING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(CENTRIFUGING, recipes);
        registration.addItemStackInfo(AllNuclearItems.ENRICHED_URANIUM_BLEND.get().getDefaultInstance(),
                Component.translatable("jei.create_nuclearindustry.enriched_blend.info"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AllNuclearBlocks.CENTRIFUGE.get(), CENTRIFUGING);
    }
}
