package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.CentrifugingRecipe;

import java.util.Arrays;
import java.util.List;

public class CentrifugingJeiCategory implements IRecipeCategory<CentrifugingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public CentrifugingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(120, 44);
        this.icon = guiHelper.createDrawableItemLike(AllNuclearBlocks.CENTRIFUGE.get());
    }

    @Override
    public RecipeType<CentrifugingRecipe> getRecipeType() {
        return NuclearJeiPlugin.CENTRIFUGING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.create_nuclearindustry.category.centrifuging");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .toList();
        ItemStack output = makeOutput(recipe, inputs);

        builder.addSlot(RecipeIngredientRole.INPUT, 18, 14)
                .setStandardSlotBackground()
                .addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 14)
                .setOutputSlotBackground()
                .addItemStack(output)
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.create_nuclearindustry.centrifuging.enrichment",
                        String.format("%.2f", UraniumItem.getEnrichment(output))
                ).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public List<Component> getTooltipStrings(CentrifugingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return List.of(Component.translatable("jei.create_nuclearindustry.centrifuging.required").withStyle(ChatFormatting.GRAY));
    }

    private static ItemStack makeOutput(CentrifugingRecipe recipe, List<ItemStack> inputs) {
        ItemStack input = inputs.isEmpty() ? new ItemStack(AllNuclearItems.URANIUM.get()) : inputs.get(0).copy();
        ItemStack output = new ItemStack(AllNuclearItems.URANIUM.get());
        float enrichment = recipe.resultEnrichment()
                .orElseGet(() -> Math.min(recipe.maxEnrichment(), UraniumItem.getEnrichment(input) + recipe.increment()));
        UraniumItem.setEnrichment(output, enrichment);
        return output;
    }
}
