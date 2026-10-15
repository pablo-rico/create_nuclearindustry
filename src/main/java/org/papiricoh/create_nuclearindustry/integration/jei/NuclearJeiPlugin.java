package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
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
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.fuelcycle.jei.FuelCycleJei;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ElectrolysisRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ProcessingInput;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationInput;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationRecipe;
import org.papiricoh.create_nuclearindustry.waste.vitrification.WasteRecipeTypes;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@JeiPlugin
public class NuclearJeiPlugin implements IModPlugin {
    public static final RecipeType<CentrifugingRecipe> CENTRIFUGING =
            RecipeType.create(Create_NuclearIndustry.MODID, "centrifuging", CentrifugingRecipe.class);
    public static final RecipeType<ElectrolysisRecipe> ELECTROLYSIS =
            RecipeType.create(Create_NuclearIndustry.MODID, "electrolysis", ElectrolysisRecipe.class);
    public static final RecipeType<VitrificationRecipe> VITRIFICATION =
            RecipeType.create(Create_NuclearIndustry.MODID, "vitrification", VitrificationRecipe.class);
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "jei_plugin");

    /** Items whose enrichment component makes them distinct entries. */
    private static final List<Supplier<? extends Item>> ENRICHED_ITEMS = List.of(
            AllNuclearItems.URANIUM,
            AllNuclearItems.URANIUM_DIOXIDE,
            AllNuclearItems.URANIUM_FUEL_PELLET,
            AllNuclearItems.URANIUM_REACTOR_FUEL,
            AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL,
            AllNuclearItems.SPENT_FUEL_FRAGMENTS,
            AllNuclearItems.URANYL_NITRATE);

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
                        Component.translatable("jei.create_nuclearindustry.category.electrolysis"), AllNuclearBlocks.ELECTROLYZER.get(), 32),
                new VitrificationJeiCategory(gui, VITRIFICATION),
                new TargetStationJeiCategory(gui),
                new IsotopeJeiCategory(gui, IsotopeJeiCategory.TARGET, "jei.create_nuclearindustry.category.target_irradiation",
                        AcceleratorRegistry.TARGET_STATION_ITEM.get()),
                new IsotopeJeiCategory(gui, IsotopeJeiCategory.REACTOR, "jei.create_nuclearindustry.category.reactor_irradiation",
                        IsotopeRegistry.IRRADIATION_CHANNEL_ITEM.get()),
                new IsotopeJeiCategory(gui, IsotopeJeiCategory.STERILISATION, "jei.create_nuclearindustry.category.gamma_sterilisation",
                        IsotopeRegistry.GAMMA_IRRADIATOR_ITEM.get()));
        FuelCycleJei.registerCategories(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipes = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();
        registration.addRecipes(CENTRIFUGING, recipes.getAllRecipesFor(AllNuclearRecipeTypes.CENTRIFUGING.<ProcessingInput, CentrifugingRecipe>getType())
                .stream().map(RecipeHolder::value).toList());
        registration.addRecipes(ELECTROLYSIS, recipes.getAllRecipesFor(AllNuclearRecipeTypes.ELECTROLYSIS.<ProcessingInput, ElectrolysisRecipe>getType())
                .stream().map(RecipeHolder::value).toList());
        registration.addRecipes(VITRIFICATION, recipes.getAllRecipesFor(WasteRecipeTypes.VITRIFICATION.<VitrificationInput, VitrificationRecipe>getType())
                .stream().map(RecipeHolder::value).toList());
        FuelCycleJei.registerRecipes(registration);
        registration.addRecipes(TargetStationJeiCategory.TYPE, List.of(TargetStationJeiCategory.Yield.current()));
        registration.addRecipes(IsotopeJeiCategory.TARGET, IsotopeJeiCategory.targets(recipes));
        registration.addRecipes(IsotopeJeiCategory.REACTOR, IsotopeJeiCategory.reactor());
        registration.addRecipes(IsotopeJeiCategory.STERILISATION, IsotopeJeiCategory.sterilisation());
        addInfo(registration);
        addPlantInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AllNuclearBlocks.CENTRIFUGE.get(), CENTRIFUGING);
        registration.addRecipeCatalyst(AllNuclearBlocks.ELECTROLYZER.get(), ELECTROLYSIS);
        registration.addRecipeCatalyst(WasteRegistry.VITRIFICATION_MELTER.get(), VITRIFICATION);
        registration.addRecipeCatalyst(VitrificationJeiCategory.createStack("blaze_burner"), VITRIFICATION);
        FuelCycleJei.registerRecipeCatalysts(registration);
        registration.addRecipeCatalyst(AcceleratorRegistry.TARGET_STATION_ITEM.get(), TargetStationJeiCategory.TYPE);
        registration.addRecipeCatalyst(AcceleratorRegistry.TARGET_STATION_ITEM.get(), IsotopeJeiCategory.TARGET);
        registration.addRecipeCatalyst(IsotopeRegistry.IRRADIATION_CHANNEL_ITEM.get(), IsotopeJeiCategory.REACTOR);
        registration.addRecipeCatalyst(IsotopeRegistry.GAMMA_IRRADIATOR_ITEM.get(), IsotopeJeiCategory.STERILISATION);
    }

    /** The pages of the power plant areas, listed in {@link PlantJeiInfo}. */
    private static void addPlantInfo(IRecipeRegistration registration) {
        for (PlantJeiInfo.Page page : PlantJeiInfo.pages()) {
            Component text = Component.translatable(page.key());
            if (!page.items().isEmpty()) {
                registration.addIngredientInfo(page.items(), VanillaTypes.ITEM_STACK, text);
            }
            if (!page.fluids().isEmpty()) {
                registration.addIngredientInfo(page.fluids(), NeoForgeTypes.FLUID_STACK, text);
            }
        }
    }

    private static void addInfo(IRecipeRegistration registration) {
        registration.addIngredientInfo(AllNuclearItems.URANIUM_REACTOR_FUEL.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_fuel"));
        registration.addIngredientInfo(AllNuclearItems.URANIUM_ROD.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_fuel"));
        registration.addIngredientInfo(AllNuclearItems.REACTOR_CONTROLLER.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_poisons"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_CONTROLLER.get(),
                Component.translatable("jei.create_nuclearindustry.info.tokamak"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_CENTRAL_SOLENOID.get(),
                Component.translatable("jei.create_nuclearindustry.info.tokamak"));
        registration.addIngredientInfo(AllNuclearItems.REACTOR_REDSTONE_PORT.get(),
                Component.translatable("jei.create_nuclearindustry.info.reactor_redstone_port"));
        registration.addIngredientInfo(AllNuclearItems.COOLING_TOWER_CONTROLLER.get(),
                Component.translatable("jei.create_nuclearindustry.info.cooling_tower"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get()),
                        new ItemStack(AllNuclearItems.DEPLETED_URANIUM_HELMET.get()), new ItemStack(AllNuclearItems.DEPLETED_URANIUM_CHESTPLATE.get()),
                        new ItemStack(AllNuclearItems.DEPLETED_URANIUM_LEGGINGS.get()), new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BOOTS.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.depleted_uranium"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()),
                        new ItemStack(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get()), new ItemStack(AllNuclearItems.PLUTONIUM_DIOXIDE.get()),
                        new ItemStack(AllNuclearItems.PLUTONIUM.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.reprocessing"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.MOX_FUEL_PELLET.get()),
                        new ItemStack(AllNuclearItems.MOX_REACTOR_FUEL.get()), new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.mox"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.RTG.get()),
                        new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get()), new ItemStack(AllNuclearItems.WASTE_RTG_PELLET.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.rtg"));
        registration.addIngredientInfo(AllNuclearItems.TURBINE_EXHAUST_PORT.get(),
                Component.translatable("jei.create_nuclearindustry.info.turbine_exhaust_port"));
        registration.addIngredientInfo(AllNuclearItems.TURBINE_OUTPUT.get(),
                Component.translatable("jei.create_nuclearindustry.info.turbine"));
        registration.addIngredientInfo(AllNuclearItems.FUSION_PLASMA_TURBINE.get(),
                Component.translatable("jei.create_nuclearindustry.info.turbine"));
        registration.addIngredientInfo(AllNuclearItems.DIRTY_BOMB.get(),
                Component.translatable("jei.create_nuclearindustry.info.dirty_bomb"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.POTASSIUM_IODIDE.get()),
                        new ItemStack(AllNuclearItems.PRUSSIAN_BLUE.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.medicine"));
        registration.addIngredientInfo(AllNuclearItems.GEIGER_COUNTER.get(),
                Component.translatable("jei.create_nuclearindustry.info.radiation"));
        registration.addIngredientInfo(AllNuclearItems.RADIOACTIVE_WASTE.get(),
                Component.translatable("jei.create_nuclearindustry.info.radiation"));
        registration.addIngredientInfo(AllNuclearItems.RADIATION_SCRUBBER.get(),
                Component.translatable("jei.create_nuclearindustry.info.radiation_scrubber"));
        registration.addIngredientInfo(org.papiricoh.create_nuclearindustry.AllNuclearFluids.CONTAMINATED_WATER_BUCKET.get(),
                Component.translatable("jei.create_nuclearindustry.info.contaminated_water"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get()),
                        new ItemStack(AllNuclearItems.CONTROL_MONITOR.get()), new ItemStack(AllNuclearItems.NUCLEAR_ALARM.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.control_room"));
        registration.addIngredientInfo(new ItemStack(AllNuclearItems.CONTROL_MONITOR.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.create_nuclearindustry.info.control_monitor"));
        List<ItemStack> desk = new java.util.ArrayList<>(List.of(new ItemStack(AllNuclearItems.REACTOR_CONTROL_PANEL.get())));
        AllNuclearItems.CONTROL_MODULES.values().forEach(module -> desk.add(new ItemStack(module.get())));
        registration.addIngredientInfo(desk, VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.control_desk"));
        registration.addIngredientInfo(List.of(new ItemStack(AllNuclearItems.GRAPHITE.get()),
                        new ItemStack(AllNuclearItems.GRAPHITE_MODERATOR.get()), new ItemStack(AllNuclearItems.BERYLLIUM_REFLECTOR.get())),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.create_nuclearindustry.info.moderation"));
    }
}
