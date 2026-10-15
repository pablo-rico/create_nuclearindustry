package org.papiricoh.create_nuclearindustry.creativetab.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTab;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection.FRAMES;
import static org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection.HEIGHT;
import static org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection.WIDTH;

/**
 * The section banners of the mod's creative tab, drawn over the empty row that opens each
 * section. A banner rests on its first frame and plays while the mouse is over it. They sit above
 * the slot highlight but below a carried item and the tooltip.
 */
public final class NuclearTabBanners {
    /** The tab row at the top of the grid, kept by {@code ItemPickerMenuMixin}. */
    public static volatile int topRow;

    private static final int VISIBLE_ROWS = 5;
    /** The grid's top-left slot frame, from the screen's corner. */
    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    /** Widest a title is drawn before it is scaled down, leaving the banner's picture in view. */
    private static final int TITLE_WIDTH = 108;
    private static final float Z = 200;
    private static final Map<NuclearTabSection, ResourceLocation> TEXTURES = new EnumMap<>(NuclearTabSection.class);

    static {
        for (NuclearTabSection section : NuclearTabSection.values()) {
            TEXTURES.put(section, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID,
                    "textures/gui/creative_section/" + section.id() + ".png"));
        }
    }

    private NuclearTabBanners() {}

    public static void render(CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (NuclearTab.headerRows().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        long ticks = Util.getMillis() / 50;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z);
        RenderSystem.enableDepthTest();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            NuclearTabSection section = NuclearTab.bannerAt(topRow + row);
            if (section == null) {
                continue;
            }
            int x = screen.getGuiLeft() + GRID_X;
            int y = screen.getGuiTop() + GRID_Y + row * HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
            int frame = hovered ? (int) (ticks / section.frametime % FRAMES) : 0;
            graphics.blit(TEXTURES.get(section), x, y, 0, frame * HEIGHT, WIDTH, HEIGHT, WIDTH, HEIGHT * FRAMES);
            title(graphics, font, section, x, y);
        }
        graphics.pose().popPose();
    }

    private static void title(GuiGraphics graphics, Font font, NuclearTabSection section, int x, int y) {
        Component title = Component.translatable(section.langKey());
        int width = font.width(title);
        float scale = Math.min(1f, TITLE_WIDTH / (float) width);
        int boxRight = x + 8 + Math.round(width * scale);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);
        graphics.fill(x + 2, y + 3, boxRight, y + 15, section.background);
        graphics.pose().translate(x + 5, y + 5 + (1 - scale) * 4, 1);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, title, 1, 1, section.shadow, false);
        graphics.drawString(font, title, 0, 0, section.text, false);
        graphics.pose().popPose();
    }

    /** Names the section of one of the mod's items under its name. */
    public static void addSectionLine(ItemStack stack, List<Component> tooltip) {
        NuclearTabSection section = NuclearTab.bannersEnabled ? NuclearTab.sectionOf(stack.getItem()) : null;
        if (section == null || tooltip.isEmpty()) {
            return;
        }
        try {
            tooltip.add(1, Component.translatable(section.langKey()).withColor(section.text & 0xFFFFFF));
        } catch (UnsupportedOperationException ignored) {
            // A tooltip some other mod handed back as an immutable list.
        }
    }
}
