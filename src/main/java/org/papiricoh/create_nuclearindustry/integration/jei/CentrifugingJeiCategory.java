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
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.List;

public class CentrifugingJeiCategory implements IRecipeCategory<CentrifugingDisplay> {
    private final IDrawable background;
    private final IDrawable icon;

    public CentrifugingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(120, 44);
        this.icon = guiHelper.createDrawableItemLike(AllNuclearBlocks.CENTRIFUGE.get());
    }

    @Override
    public RecipeType<CentrifugingDisplay> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugingDisplay display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 14)
                .setStandardSlotBackground()
                .addItemStack(display.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 14)
                .setOutputSlotBackground()
                .addItemStack(display.output())
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.create_nuclearindustry.centrifuging.enrichment",
                        String.format("%.2f", UraniumItem.getEnrichment(display.output()))
                ).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public List<Component> getTooltipStrings(CentrifugingDisplay display, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return List.of(display.description().copy().withStyle(ChatFormatting.GRAY));
    }
}
