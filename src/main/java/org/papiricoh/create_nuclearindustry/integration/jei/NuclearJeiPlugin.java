package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;

import java.util.List;

@JeiPlugin
public class NuclearJeiPlugin implements IModPlugin {
    public static final RecipeType<CentrifugingDisplay> CENTRIFUGING =
            RecipeType.create(Create_NuclearIndustry.MODID, "centrifuging", CentrifugingDisplay.class);
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
        registration.addRecipes(CENTRIFUGING, List.of(
                new CentrifugingDisplay(
                        new ItemStack(AllNuclearItems.RAW_URANIUM.get()),
                        uranium(UraniumItem.NATURAL_ENRICHMENT),
                        Component.translatable("jei.create_nuclearindustry.centrifuging.raw")),
                new CentrifugingDisplay(
                        uranium(UraniumItem.NATURAL_ENRICHMENT),
                        uranium(UraniumItem.REACTOR_FUEL_ENRICHMENT),
                        Component.translatable("jei.create_nuclearindustry.centrifuging.required")),
                new CentrifugingDisplay(
                        uranium(UraniumItem.REACTOR_FUEL_ENRICHMENT),
                        uranium(WarheadStats.REQUIRED_ENRICHMENT),
                        Component.translatable("jei.create_nuclearindustry.centrifuging.weapons"))
        ));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AllNuclearBlocks.CENTRIFUGE.get(), CENTRIFUGING);
    }

    private static ItemStack uranium(float enrichment) {
        ItemStack stack = new ItemStack(AllNuclearItems.URANIUM.get());
        UraniumItem.setEnrichment(stack, enrichment);
        return stack;
    }
}
