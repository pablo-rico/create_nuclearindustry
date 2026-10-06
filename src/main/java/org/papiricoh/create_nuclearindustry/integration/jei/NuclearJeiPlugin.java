package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ElectrolysisRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ProcessingInput;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@JeiPlugin
public class NuclearJeiPlugin implements IModPlugin {
    public static final RecipeType<CentrifugingRecipe> CENTRIFUGING =
            RecipeType.create(Create_NuclearIndustry.MODID, "centrifuging", CentrifugingRecipe.class);
    public static final RecipeType<ElectrolysisRecipe> ELECTROLYSIS =
            RecipeType.create(Create_NuclearIndustry.MODID, "electrolysis", ElectrolysisRecipe.class);
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "jei_plugin");

    /** Items whose enrichment component makes them distinct entries. */
    private static final List<Supplier<? extends Item>> ENRICHED_ITEMS = List.of(
            AllNuclearItems.URANIUM,
            AllNuclearItems.URANIUM_DIOXIDE,
            AllNuclearItems.URANIUM_FUEL_PELLET,
            AllNuclearItems.URANIUM_REACTOR_FUEL,
            AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> enrichment = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return UraniumItem.getEnrichment(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return String.format("enrichment=%.2f", UraniumItem.getEnrichment(stack));
            }
        };
        for (Supplier<? extends Item> item : ENRICHED_ITEMS) {
            registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.get(), enrichment);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ProcessingJeiCategory<>(gui, CENTRIFUGING,
                        Component.translatable("jei.create_nuclearindustry.category.centrifuging"), AllNuclearBlocks.CENTRIFUGE.get(), 32),
                new ProcessingJeiCategory<>(gui, ELECTROLYSIS,
                        Component.translatable("jei.create_nuclearindustry.category.electrolysis"), AllNuclearBlocks.ELECTROLYZER.get(), 32));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipes = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();
        registration.addRecipes(CENTRIFUGING, recipes.getAllRecipesFor(AllNuclearRecipeTypes.CENTRIFUGING.<ProcessingInput, CentrifugingRecipe>getType())
                .stream().map(RecipeHolder::value).toList());
        registration.addRecipes(ELECTROLYSIS, recipes.getAllRecipesFor(AllNuclearRecipeTypes.ELECTROLYSIS.<ProcessingInput, ElectrolysisRecipe>getType())
                .stream().map(RecipeHolder::value).toList());
        addInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AllNuclearBlocks.CENTRIFUGE.get(), CENTRIFUGING);
        registration.addRecipeCatalyst(AllNuclearBlocks.ELECTROLYZER.get(), ELECTROLYSIS);
    }

    private static void addInfo(IRecipeRegistration registration) {
        registration.addIngredientInfo(AllNuclearItems.URANIUM_REACTOR_FUEL.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_fuel"));
        registration.addIngredientInfo(AllNuclearItems.URANIUM_ROD.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_fuel"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_CONTROLLER.get(),
                Component.translatable("jei.create_nuclearindustry.info.tokamak"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_CENTRAL_SOLENOID.get(),
                Component.translatable("jei.create_nuclearindustry.info.tokamak"));
        registration.addIngredientInfo(AllNuclearItems.REACTOR_REDSTONE_PORT.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_redstone_port"));
        registration.addIngredientInfo(AllNuclearItems.TURBINE_OUTPUT.get(),
                Component.translatable("jei.create_nuclearindustry.info.turbine"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_PLASMA_TURBINE.get(),
                Component.translatable("jei.create_nuclearindustry.info.turbine"));
        registration.addIngredientInfo(AllNuclearItems.GEIGER_COUNTER.get(),
                Component.translatable("jei.create_nuclearindustry.info.radiation"));
        registration.addIngredientInfo(AllNuclearItems.RADIOACTIVE_WASTE.get(),
                Component.translatable("jei.create_nuclearindustry.info.radiation"));
    }
}
