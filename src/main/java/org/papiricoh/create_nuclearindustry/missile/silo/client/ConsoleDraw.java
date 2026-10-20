package org.papiricoh.create_nuclearindustry.missile.silo.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Text sizes are fixed so neighbouring columns stay readable at every GUI scale. */
public final class ConsoleDraw {
    public static final float SMALL = 0.75f, DENSE = 2f / 3f;

    private ConsoleDraw() {}

    public static void line(GuiGraphics g, Font font, Component text, int x, int y, int width, int colour, float scale) {
        String value = ConsoleText.trim(text.getString(), (int) Math.floor(width / scale), font::width);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, Component.literal(value), 0, 0, colour, false);
        g.pose().popPose();
    }

    public static void wrap(GuiGraphics g, Font font, Component text, int x, int y, int width, int colour, float scale, int maxLines) {
        var lines = font.split(text, (int) Math.floor(width / scale));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            g.drawString(font, lines.get(i), 0, i * font.lineHeight, colour, false);
        g.pose().popPose();
    }
}
