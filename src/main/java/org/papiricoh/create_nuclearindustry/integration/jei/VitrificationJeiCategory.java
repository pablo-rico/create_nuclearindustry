package org.papiricoh.create_nuclearindustry.integration.jei;

import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.lang3.mutable.MutableInt;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI page for the Vitrification Melter, laid out like Create's heated mixing: the ingredients in a
 * basin-style grid, the melter over an animated Blaze Burner, the canister on the right and the heat
 * bar along the bottom. A lit burner is enough; a Blaze Cake (superheating) melts twice as fast.
 */
public class VitrificationJeiCategory implements IRecipeCategory<VitrificationRecipe> {
    private static final int WIDTH = 177;
    private static final int HEIGHT = 103;
    private static final String KEY = "jei.create_nuclearindustry.vitrification.";

    private final RecipeType<VitrificationRecipe> type;
    private final IDrawable icon;
    private final IDrawable slot;
    private final AnimatedMelter melter = new AnimatedMelter();
    private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

    public VitrificationJeiCategory(IGuiHelper guiHelper, RecipeType<VitrificationRecipe> type) {
        this.type = type;
        this.icon = guiHelper.createDrawableItemLike(WasteRegistry.VITRIFICATION_MELTER.get());
        this.slot = texture(AllGuiTextures.JEI_SLOT);
    }

    @Override
    public RecipeType<VitrificationRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.create_nuclearindustry.category.vitrification");
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
    public void setRecipe(IRecipeLayoutBuilder builder, VitrificationRecipe recipe, IFocusGroup focuses) {
        // Repeated ingredients count as one slot with a stack size, as in Create's basin categories.
        List<Pair<Ingredient, MutableInt>> inputs = ItemHelper.condenseIngredients(recipe.getIngredients());
        int xOffset = inputs.size() < 3 ? (3 - inputs.size()) * 19 / 2 : 0;
        for (int i = 0; i < inputs.size(); i++) {
            int count = inputs.get(i).getSecond().getValue();
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : inputs.get(i).getFirst().getItems()) {
                stacks.add(stack.copyWithCount(count));
            }
            builder.addSlot(RecipeIngredientRole.INPUT, 17 + xOffset + (i % 3) * 19, 51 - (i / 3) * 19)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(stacks);
        }

        List<ProcessingOutput> outputs = recipe.getRollableResults();
        for (int i = 0; i < outputs.size(); i++) {
            ProcessingOutput output = outputs.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 51 - i * 19)
                    .setBackground(slot, -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (output.getChance() < 1) {
                            tooltip.add(Component.translatable("jei.create_nuclearindustry.chance",
                                    Math.round(output.getChance() * 100)).withStyle(ChatFormatting.GOLD));
                        }
                    });
        }

        HeatCondition heat = recipe.getRequiredHeat();
        if (heat != HeatCondition.NONE) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81)
                    .addItemStack(createStack("blaze_burner"));
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 153, 81)
                .addItemStack(createStack("blaze_cake"))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        heat == HeatCondition.SUPERHEATED ? KEY + "cake_required" : KEY + "cake_faster")
                        .withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public void draw(VitrificationRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        HeatCondition heat = recipe.getRequiredHeat();
        boolean noHeat = heat == HeatCondition.NONE;

        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
        AllGuiTextures shadow = noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
        shadow.render(graphics, 81, 58 + (noHeat ? 10 : 30));
        AllGuiTextures bar = noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR;
        bar.render(graphics, 4, 80);
        graphics.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 9, 86,
                heat.getColor(), false);

        if (!noHeat) {
            heater.withHeat(heat.visualizeAsBlazeBurner()).draw(graphics, WIDTH / 2 + 3, 55);
        }
        melter.draw(graphics, WIDTH / 2 + 3, 34);

        // A superheated burner melts twice as fast, so a heated recipe shows both times.
        int ticks = recipe.getProcessingDuration();
        Component time = heat == HeatCondition.SUPERHEATED
                ? Component.translatable(KEY + "time", seconds(ticks / 2))
                : Component.translatable(KEY + "time_superheated", seconds(ticks), seconds(ticks / 2));
        graphics.drawString(Minecraft.getInstance().font, time, 4, 4, 0xFF808080, false);
    }

    /** A stack of one of Create's items, looked up by id: Registrate's entry classes are not on our classpath. */
    static ItemStack createStack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", id)));
    }

    private static String seconds(int ticks) {
        float seconds = Math.max(1, ticks) / 20.0f;
        return seconds == Math.round(seconds) ? Integer.toString(Math.round(seconds)) : String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private static IDrawable texture(AllGuiTextures texture) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return texture.getWidth();
            }

            @Override
            public int getHeight() {
                return texture.getHeight();
            }

            @Override
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                texture.render(graphics, xOffset, yOffset);
            }
        };
    }
}
