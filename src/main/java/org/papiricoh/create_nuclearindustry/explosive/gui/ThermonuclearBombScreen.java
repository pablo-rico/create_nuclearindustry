package org.papiricoh.create_nuclearindustry.explosive.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.ThermonuclearBombBlockEntity;

/**
 * The thermonuclear bomb's screen, drawn like the nuclear bomb's: a readout of what it would go off with
 * over the three parts' wells (the primary's in red, the secondary's and the tamper's), then the player's
 * inventory. The wells sit where {@link ThermonuclearBombMenu} puts its slots.
 */
public class ThermonuclearBombScreen extends AbstractContainerScreen<ThermonuclearBombMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 226;
    private static final int READOUT_TOP = 22;
    private static final int READOUT_BOTTOM = 98;
    private static final String KEY = "gui.create_nuclearindustry.thermonuclear_bomb.";

    public ThermonuclearBombScreen(ThermonuclearBombMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 8, 0xFFE066);
        ThermonuclearBombBlockEntity bomb = menu.getBomb();
        if (bomb != null) {
            ThermonuclearStats.Result stats = bomb.stats();
            int x = leftPos + 13;
            int y = topPos + READOUT_TOP + 4;
            ThermonuclearStats.Primary primary = bomb.primary();
            if (primary == ThermonuclearStats.Primary.NONE) {
                graphics.drawString(font, Component.translatable(KEY + "no_primary"), x, y, 0xFF5555);
            } else {
                graphics.drawString(font, Component.translatable(KEY + "primary",
                        Component.translatable(KEY + "primary." + primary.name().toLowerCase(java.util.Locale.ROOT))), x, y, 0xDDDDDD);
            }
            graphics.drawString(font, Component.translatable(KEY + "secondary", bomb.secondary(), ThermonuclearStats.SECONDARY_FULL),
                    x, y + 10, bomb.secondary() > 0 ? 0xDDDDDD : 0xFFAA55);
            graphics.drawString(font, Component.translatable(KEY + "tamper", bomb.tamper(), ThermonuclearStats.TAMPER_FULL), x, y + 20, 0xDDDDDD);
            if (stats.live()) {
                if (stats.fizzle()) {
                    graphics.drawString(font, Component.translatable(KEY + "fizzle"), x, y + 30, 0xFFAA55);
                } else {
                    graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.power",
                            String.format("%.0f", stats.power())), x, y + 30, 0xFF7777);
                }
                graphics.drawString(font, Component.translatable("gui.create_nuclearindustry.nuclear_bomb.radius",
                        stats.radius(), bomb.getEstimatedCraterRadius()), x, y + 40, 0xFFAA77);
                graphics.drawString(font, Component.translatable(KEY + "fallout", stats.falloutRadius()), x, y + 50, 0xC8E070);
            }
            NuclearBombScreen.drawState(graphics, bomb, x, topPos + READOUT_BOTTOM - 11);
            label(graphics, KEY + "slot.primary", ThermonuclearBombMenu.PRIMARY_X + 8);
            label(graphics, KEY + "slot.secondary", ThermonuclearBombMenu.SECONDARY_X + 36);
            label(graphics, KEY + "slot.tamper", ThermonuclearBombMenu.TAMPER_X + 17);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** A part's name under its wells, centred on {@code centreX}. */
    private void label(GuiGraphics graphics, String key, int centreX) {
        Component text = Component.translatable(key);
        graphics.drawString(font, text, leftPos + centreX - font.width(text) / 2, topPos + ThermonuclearBombMenu.SLOT_Y + 21, 0x9A9A9A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int l = leftPos;
        int t = topPos;
        graphics.fill(l, t, l + imageWidth, t + imageHeight, 0xFF151618);
        graphics.fill(l - 1, t - 1, l + imageWidth + 1, t + 1, 0xFF7A7A7A);
        graphics.fill(l - 1, t - 1, l + 1, t + imageHeight + 1, 0xFF7A7A7A);
        graphics.fill(l - 1, t + imageHeight - 1, l + imageWidth + 1, t + imageHeight + 1, 0xFF7A7A7A);
        graphics.fill(l + imageWidth - 1, t - 1, l + imageWidth + 1, t + imageHeight + 1, 0xFF7A7A7A);
        // The yellow and black band of a live device under the title.
        for (int x = 8; x < imageWidth - 8; x += 8) {
            graphics.fill(l + x, t + 18, l + Math.min(x + 4, imageWidth - 8), t + 20, 0xFFE0B020);
            graphics.fill(l + x + 4, t + 18, l + Math.min(x + 8, imageWidth - 8), t + 20, 0xFF202020);
        }
        // The readout's recess.
        graphics.fill(l + 8, t + READOUT_TOP, l + imageWidth - 8, t + READOUT_BOTTOM, 0xFF3A3A3A);
        graphics.fill(l + 9, t + READOUT_TOP + 1, l + imageWidth - 9, t + READOUT_BOTTOM - 1, 0xFF0B0D0C);
        for (int slot = 0; slot < ThermonuclearBombBlockEntity.SLOTS; slot++) {
            int border = switch (ThermonuclearBombBlockEntity.part(slot)) {
                case PRIMARY -> 0xFFB03030;
                case SECONDARY -> 0xFF4A7AA0;
                case TAMPER -> 0xFF6A6A50;
            };
            well(graphics, l + ThermonuclearBombMenu.slotX(slot), t + ThermonuclearBombMenu.SLOT_Y, border);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                well(graphics, l + 8 + col * 18, t + ThermonuclearBombMenu.INVENTORY_Y + row * 18, 0xFF555555);
            }
        }
        for (int col = 0; col < 9; col++) {
            well(graphics, l + 8 + col * 18, t + ThermonuclearBombMenu.HOTBAR_Y, 0xFF555555);
        }
    }

    /** A slot's sunken well, outlined in {@code border}. */
    private static void well(GuiGraphics graphics, int x, int y, int border) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        graphics.fill(x, y, x + 16, y + 16, 0xFF2F2F2F);
        graphics.fill(x, y, x + 16, y + 1, 0xFF1A1A1A);
        graphics.fill(x, y, x + 1, y + 16, 0xFF1A1A1A);
        graphics.fill(x, y + 15, x + 16, y + 16, 0xFF5A5A5A);
        graphics.fill(x + 15, y, x + 16, y + 16, 0xFF5A5A5A);
    }
}
