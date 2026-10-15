package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.SterileFood;
import org.papiricoh.create_nuclearindustry.isotope.physics.GammaIrradiation;
import org.papiricoh.create_nuclearindustry.isotope.physics.NeutronActivation;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.TargetIrradiationRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * JEI pages that turn one item into another under radiation: a Target Station's beam below the
 * antiproton threshold, the reactor's irradiation channel, and the Gamma Irradiator's
 * sterilisation. One input, one output and a line on what it takes.
 */
public class IsotopeJeiCategory implements IRecipeCategory<IsotopeJeiCategory.Conversion> {
    public static final RecipeType<Conversion> TARGET = RecipeType.create(Create_NuclearIndustry.MODID, "target_irradiation", Conversion.class);
    public static final RecipeType<Conversion> REACTOR = RecipeType.create(Create_NuclearIndustry.MODID, "reactor_irradiation", Conversion.class);
    public static final RecipeType<Conversion> STERILISATION = RecipeType.create(Create_NuclearIndustry.MODID, "gamma_sterilisation", Conversion.class);
    private static final int WIDTH = 176;
    private static final int HEIGHT = 44;

    /** One page: what goes in, what comes out, and the line under them. */
    public record Conversion(List<ItemStack> inputs, ItemStack output, Component note) {}

    private final RecipeType<Conversion> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawableAnimated animatedArrow;

    public IsotopeJeiCategory(IGuiHelper gui, RecipeType<Conversion> type, String titleKey, ItemLike icon) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = gui.createDrawableItemLike(icon);
        this.arrow = gui.getRecipeArrow();
        this.animatedArrow = gui.createAnimatedRecipeArrow(60);
    }

    /** The Target Station's irradiation recipes. */
    public static List<Conversion> targets(RecipeManager recipes) {
        double max = AcceleratorConfig.antimatter().thresholdGev();
        return recipes.getAllRecipesFor(AllNuclearRecipeTypes.TARGET_IRRADIATION.<SingleRecipeInput, TargetIrradiationRecipe>getType())
                .stream().map(RecipeHolder::value)
                .filter(recipe -> !recipe.getIngredients().isEmpty() && !recipe.getRollableResults().isEmpty())
                .map(recipe -> new Conversion(Arrays.asList(recipe.getIngredients().get(0).getItems()),
                        recipe.getRollableResults().get(0).getStack(),
                        Component.translatable("jei.create_nuclearindustry.target_irradiation.energy", Math.round(recipe.nominalKj() / 1000),
                                fmt(IsotopeConfig.targetMinEnergyGev), fmt(max))))
                .toList();
    }

    /** Cobalt into cobalt-60 in the reactor's channel. */
    public static List<Conversion> reactor() {
        double seconds = NeutronActivation.requiredFluence(IsotopeConfig.channel()) / 1000.0 / 20.0;
        return List.of(new Conversion(List.of(new ItemStack(IsotopeRegistry.COBALT_INGOT.get())), new ItemStack(IsotopeRegistry.COBALT_60.get()),
                Component.translatable("jei.create_nuclearindustry.reactor_irradiation.time", Math.round(seconds))));
    }

    /** Every food with a harmful effect, and its sterile self. */
    public static List<Conversion> sterilisation() {
        int ticks = GammaIrradiation.processingTicks(IsotopeConfig.irradiator(), 1.0);
        Component note = Component.translatable("jei.create_nuclearindustry.gamma_sterilisation.time", fmt(ticks / 20.0));
        return BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(stack -> stack.has(DataComponents.FOOD) && SterileFood.sterilisable(stack))
                .map(stack -> new Conversion(List.of(stack), SterileFood.sterilise(stack), note))
                .toList();
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, value == Math.rint(value) ? "%.0f" : "%.1f", value);
    }

    @Override
    public RecipeType<Conversion> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Conversion recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 42, 6).setStandardSlotBackground().addItemStacks(recipe.inputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 6).setOutputSlotBackground().addItemStack(recipe.output());
    }

    @Override
    public void draw(Conversion recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 75, 6);
        animatedArrow.draw(graphics, 75, 6);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.note(), (WIDTH - font.width(recipe.note())) / 2, HEIGHT - 12, 0xFF808080, false);
    }
}
