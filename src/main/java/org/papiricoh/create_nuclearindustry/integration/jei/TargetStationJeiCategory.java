package org.papiricoh.create_nuclearindustry.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AntimatterModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TargetStationModel;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;

import java.util.List;
import java.util.Locale;

/**
 * JEI page for the Target Station's antiproton production: the beam energy it needs, the curve of
 * antiprotons per minute and megawatt of beam above that threshold, and where they go (a powered
 * trap or conduit beside the station). Built from the current config, so it shows the server's
 * numbers on a single player world and the defaults' shape elsewhere.
 */
public class TargetStationJeiCategory implements IRecipeCategory<TargetStationJeiCategory.Yield> {
    public static final RecipeType<Yield> TYPE = RecipeType.create(Create_NuclearIndustry.MODID, "target_station", Yield.class);
    private static final String KEY = "jei.create_nuclearindustry.target_station.";
    private static final int WIDTH = 177;
    private static final int HEIGHT = 92;
    /** The plot, in the page's pixels. */
    private static final int PLOT_LEFT = 26, PLOT_RIGHT = 150, PLOT_TOP = 14, PLOT_BOTTOM = 62;
    private static final int AXIS = 0xFF8B8B8B, CURVE = 0xFF6BFF8A, THRESHOLD = 0xFFFFB23F, GRID = 0x40FFFFFF;

    /**
     * One page.
     *
     * @param thresholdGev  least beam energy that makes antiprotons
     * @param saturationGev energy from which the yield stops rising
     * @param curve         {@code {GeV, ng/min per MW}} from the threshold up
     * @param referenceGev  the energy the page quotes a yield at
     * @param referenceYield ng/min per MW at {@code referenceGev}
     */
    public record Yield(double thresholdGev, double saturationGev, double[][] curve, double referenceGev, double referenceYield) {
        /** The page for the current config. */
        public static Yield current() {
            TargetStationModel.Params p = AcceleratorConfig.targetStation();
            AntimatterModel.Params am = AcceleratorConfig.antimatter();
            double saturation = TargetStationModel.saturationGev(am);
            return new Yield(am.thresholdGev(), saturation, TargetStationModel.yieldCurve(p, am, 48), saturation,
                    TargetStationModel.perMinutePerMw(p, am, saturation));
        }
    }

    private final IDrawable icon;

    public TargetStationJeiCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(AcceleratorRegistry.TARGET_STATION.get());
    }

    @Override
    public RecipeType<Yield> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.create_nuclearindustry.category.target_station");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Yield recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 4, 30)
                .addItemStack(new ItemStack(AcceleratorRegistry.TARGET_STATION_ITEM.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 157, 30)
                .addItemStacks(List.of(new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()),
                        new ItemStack(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(KEY + "output")
                        .withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public void draw(Yield recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        double[][] curve = recipe.curve();
        double maxGev = curve.length == 0 ? 1 : curve[curve.length - 1][0];
        double maxYield = 0;
        for (double[] point : curve) {
            maxYield = Math.max(maxYield, point[1]);
        }
        maxYield = Math.max(maxYield, 1.0e-9);

        // Axes and the saturation level.
        graphics.fill(PLOT_LEFT, PLOT_TOP, PLOT_LEFT + 1, PLOT_BOTTOM + 1, AXIS);
        graphics.fill(PLOT_LEFT, PLOT_BOTTOM, PLOT_RIGHT + 1, PLOT_BOTTOM + 1, AXIS);
        int top = y(recipe.referenceYield(), maxYield);
        graphics.fill(PLOT_LEFT + 1, top, PLOT_RIGHT + 1, top + 1, GRID);
        // No antiprotons below the threshold.
        int threshold = x(recipe.thresholdGev(), maxGev);
        for (int yy = PLOT_TOP; yy < PLOT_BOTTOM; yy += 3) {
            graphics.fill(threshold, yy, threshold + 1, yy + 2, THRESHOLD);
        }
        // The curve, as a filled area under a bright edge.
        for (int i = 1; i < curve.length; i++) {
            int x0 = x(curve[i - 1][0], maxGev);
            int x1 = x(curve[i][0], maxGev);
            int y1 = y(curve[i][1], maxYield);
            graphics.fill(x0, y1 + 1, Math.max(x0 + 1, x1), PLOT_BOTTOM, 0x306BFF8A);
            graphics.fill(x0, y1, Math.max(x0 + 1, x1), y1 + 1, CURVE);
        }
        graphics.drawString(font, "0", PLOT_LEFT - 2, PLOT_BOTTOM + 2, AXIS, false);
        String gevText = fmt(maxGev, 0) + " GeV";
        graphics.drawString(font, gevText, PLOT_RIGHT - font.width(gevText), PLOT_BOTTOM + 2, AXIS, false);
        graphics.drawString(font, Component.translatable(KEY + "yield", fmt(recipe.referenceYield(), 2), fmt(recipe.referenceGev(), 0)),
                PLOT_LEFT, 2, 0xFF6BFF8A, false);
        graphics.drawString(font, Component.translatable(KEY + "threshold", fmt(recipe.thresholdGev(), 0)), 4, 72, THRESHOLD, false);
        graphics.drawString(font, Component.translatable(KEY + "saturation", fmt(recipe.saturationGev(), 0)), 4, 82, 0xFF808080, false);
    }

    /** Over the plot: what it shows. */
    @Override
    public void getTooltip(ITooltipBuilder tooltip, Yield recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= PLOT_LEFT && mouseX <= PLOT_RIGHT && mouseY >= PLOT_TOP && mouseY <= PLOT_BOTTOM) {
            tooltip.add(Component.translatable(KEY + "curve"));
        }
    }

    private static int x(double gev, double maxGev) {
        double share = maxGev <= 0 ? 0 : Math.max(0, Math.min(1, gev / maxGev));
        return PLOT_LEFT + 1 + (int) Math.round(share * (PLOT_RIGHT - PLOT_LEFT - 1));
    }

    private static int y(double yield, double maxYield) {
        double share = Math.max(0, Math.min(1, yield / maxYield));
        return PLOT_BOTTOM - 1 - (int) Math.round(share * (PLOT_BOTTOM - PLOT_TOP - 2));
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
