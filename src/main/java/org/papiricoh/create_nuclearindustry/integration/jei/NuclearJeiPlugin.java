package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;

import java.util.ArrayList;
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
                        rawUraniumVariants(),
                        uranium(UraniumItem.NATURAL_ENRICHMENT),
                        Component.translatable("jei.create_nuclearindustry.centrifuging.raw")),
                new CentrifugingDisplay(
                        uraniumIngotVariants(),
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

    /** Every raw uranium accepted by the centrifuge, this mod's and any other's. */
    private static List<ItemStack> rawUraniumVariants() {
        List<ItemStack> stacks = new ArrayList<>();
        BuiltInRegistries.ITEM.getTag(AllNuclearTags.Items.RAW_URANIUM)
                .ifPresent(holders -> holders.forEach(holder -> stacks.add(new ItemStack(holder))));
        if (stacks.isEmpty()) {
            stacks.add(new ItemStack(AllNuclearItems.RAW_URANIUM.get()));
        }
        return stacks;
    }

    private static List<ItemStack> uraniumIngotVariants() {
        List<ItemStack> stacks = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = new ItemStack(item);
            if (org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity.isUraniumIngot(stack)) {
                stacks.add(stack);
            }
        });
        return stacks;
    }

    private static ItemStack uranium(float enrichment) {
        ItemStack stack = new ItemStack(AllNuclearItems.URANIUM.get());
        UraniumItem.setEnrichment(stack, enrichment);
        return stack;
    }
}
