package org.papiricoh.create_nuclearindustry.integration.jei;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;
import org.papiricoh.create_nuclearindustry.processing.recipe.NuclearProcessingRecipe;

import java.util.Arrays;
import java.util.List;

/**
 * JEI page for a single-slot kinetic machine: item and feed tank on the left, two item
 * outputs and the product and tails tanks on the right, and the machine turning above the arrow.
 */
public class ProcessingJeiCategory<R extends NuclearProcessingRecipe> implements IRecipeCategory<R> {
    private static final int WIDTH = 170;
    private static final int HEIGHT = 76;
    private static final int ARROW_Y = 38;
    private static final int TANK_HEIGHT = 40;

    private final RecipeType<R> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawableAnimated animatedArrow;
    private final int minimumRpm;
    private final AnimatedProcessingMachine machine;

    public ProcessingJeiCategory(IGuiHelper guiHelper, RecipeType<R> type, Component title, ItemLike catalyst, int minimumRpm,
                                 AnimatedProcessingMachine machine) {
        this.type = type;
        this.machine = machine;
        this.title = title;
        this.icon = guiHelper.createDrawableItemLike(catalyst);
        this.arrow = guiHelper.getRecipeArrow();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(40);
        this.minimumRpm = minimumRpm;
    }

    @Override
    public RecipeType<R> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, R recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (!ingredients.isEmpty()) {
            int count = ingredients.size();
            builder.addSlot(RecipeIngredientRole.INPUT, 4, ARROW_Y)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(ingredients.get(0).getItems()).map(stack -> stack.copyWithCount(count)).toList());
        }
        if (recipe.fluidIngredient() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 26, 6)
                    .setFluidRenderer(KineticProcessingBlockEntity.TANK_CAPACITY, false, 16, TANK_HEIGHT)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(recipe.fluidIngredient().getFluids()));
        }

        List<ProcessingOutput> outputs = recipe.getRollableResults();
        for (int i = 0; i < outputs.size(); i++) {
            ProcessingOutput output = outputs.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 82, ARROW_Y - 12 + i * 20)
                    .setOutputSlotBackground()
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (output.getChance() < 1) {
                            tooltip.add(Component.translatable("jei.create_nuclearindustry.chance",
                                    Math.round(output.getChance() * 100)).withStyle(ChatFormatting.GOLD));
                        }
                    });
        }
        addOutputTank(builder, recipe.productFluid(), 110, "jei.create_nuclearindustry.product");
        addOutputTank(builder, recipe.tailsFluid(), 134, "jei.create_nuclearindustry.tails");
    }

    private static void addOutputTank(IRecipeLayoutBuilder builder, FluidStack fluid, int x, String role) {
        if (fluid.isEmpty()) {
            return;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, x, 6)
                .setFluidRenderer(KineticProcessingBlockEntity.TANK_CAPACITY, false, 16, TANK_HEIGHT)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluid)
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable(role).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(R recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        machine.draw(graphics, 52, 24);
        arrow.draw(graphics, 50, ARROW_Y);
        animatedArrow.draw(graphics, 50, ARROW_Y);
        int seconds = Math.max(1, Math.round(recipe.getProcessingDuration() / 20.0f));
        Component info = Component.translatable("jei.create_nuclearindustry.processing_time", seconds, minimumRpm);
        graphics.drawString(Minecraft.getInstance().font, info, 4, HEIGHT - 10, 0xFF808080, false);
    }
}
