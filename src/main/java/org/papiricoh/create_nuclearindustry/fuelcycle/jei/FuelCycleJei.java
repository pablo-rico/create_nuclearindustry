package org.papiricoh.create_nuclearindustry.fuelcycle.jei;

import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.AcidLeachingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.IrradiationRecipe;

import java.util.List;
import java.util.function.Supplier;

/**
 * JEI for the renewable fuel cycle: the acid leaching and irradiation fan treatments' categories,
 * recipes and catalysts, as Create shows its own fan treatments. Called by
 * {@link org.papiricoh.create_nuclearindustry.integration.jei.NuclearJeiPlugin}; only loaded when JEI is.
 */
public final class FuelCycleJei {
    public static final RecipeType<RecipeHolder<AcidLeachingRecipe>> ACID_LEACHING =
            RecipeType.createRecipeHolderType(id("fan_acid_leaching"));
    public static final RecipeType<RecipeHolder<IrradiationRecipe>> IRRADIATION =
            RecipeType.createRecipeHolderType(id("fan_irradiation"));

    private static FanTreatmentCategories.AcidLeaching acidLeaching;
    private static FanTreatmentCategories.Irradiation irradiation;

    private FuelCycleJei() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        acidLeaching = new FanTreatmentCategories.AcidLeaching(new CreateRecipeCategory.Info<>(ACID_LEACHING,
                Component.translatable("jei.create_nuclearindustry.category.fan_acid_leaching"), new EmptyBackground(178, 72),
                new DoubleItemIcon(FuelCycleJei::propeller, () -> new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get())),
                () -> recipes(AllNuclearRecipeTypes.ACID_LEACHING), List.of(fan("fan_acid_leaching"))));
        irradiation = new FanTreatmentCategories.Irradiation(new CreateRecipeCategory.Info<>(IRRADIATION,
                Component.translatable("jei.create_nuclearindustry.category.fan_irradiation"), new EmptyBackground(178, 72),
                new DoubleItemIcon(FuelCycleJei::propeller, () -> new ItemStack(FuelCycleRegistry.IRRADIATION_SOURCE_ITEM.get())),
                () -> recipes(AllNuclearRecipeTypes.IRRADIATION), List.of(fan("fan_irradiation"))));
        registration.addRecipeCategories(acidLeaching, irradiation);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        if (acidLeaching != null) {
            acidLeaching.registerRecipes(registration);
        }
        if (irradiation != null) {
            irradiation.registerRecipes(registration);
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (acidLeaching != null) {
            acidLeaching.registerCatalysts(registration);
        }
        if (irradiation != null) {
            irradiation.registerCatalysts(registration);
        }
        // What has to stand in front of the fan.
        registration.addRecipeCatalyst(new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get()), ACID_LEACHING);
        registration.addRecipeCatalyst(new ItemStack(FuelCycleRegistry.IRRADIATION_SOURCE_ITEM.get()), IRRADIATION);
    }

    /** Every loaded recipe of a fan treatment, or none before a world is open. */
    private static <R extends Recipe<SingleRecipeInput>> List<RecipeHolder<R>> recipes(AllNuclearRecipeTypes type) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        net.minecraft.world.item.crafting.RecipeType<R> recipeType = type.getType();
        return level.getRecipeManager().getAllRecipesFor(recipeType);
    }

    private static ItemStack propeller() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "propeller")));
    }

    /** An Encased Fan named after the treatment, as Create names its fans in JEI. */
    private static Supplier<ItemStack> fan(String name) {
        return () -> {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "encased_fan")));
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("create_nuclearindustry.recipe." + name + ".fan")
                    .withStyle(style -> style.withItalic(false)));
            return stack;
        };
    }
}
