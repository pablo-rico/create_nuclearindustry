package org.papiricoh.create_nuclearindustry.client.panel;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.panel.PanelSource;

import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.panel.PanelCard.*;

/**
 * The panel: a readout that slides in on the right of the screen while the crosshair is on a block
 * whose entity is a {@link PanelSource} (control modules and consoles, Advanced Redstone Links, the
 * fission reactor, the tokamak), and slides out when it leaves. No goggles needed; sneaking asks the
 * card for its details. Scaled down when it is taller than the screen. A chamfered dark glass frame edged in the card's accent colour, with scan lines, a
 * sweep, corner brackets and a title that decodes itself when the card changes.
 * <p>
 * Drawn in GUI pixels; colours carry the panel's fade in their alpha.
 */
public final class PanelHud implements LayeredDraw.Layer {
    public static final PanelHud INSTANCE = new PanelHud();
    private static final int WIDTH = 164, MARGIN = 10, CHAMFER = 7, PAD = 11, HEADER = 42, GAP = 3;
    private static final int BG_TOP = 0xE00C1A21, BG_BOTTOM = 0xE8050C10, DARK_TEXT = 0xFF061015;
    private static final float OPEN_SECONDS = 0.18f, CLOSE_SECONDS = 0.14f;
    private static final long DECODE_MS = 280, SWEEP_MS = 3200;
    private static final String GLYPHS = "#%&*+=?@0123456789ABCDEFX<>/\\";

    @Nullable
    private Object key;
    @Nullable
    private PanelCard card;
    private float open;
    private long lastFrame = Util.getMillis();
    private long changedAt;

    private PanelHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "panel"), INSTANCE);
    }

    // ------------------------------------------------------------------ what to show

    private record Target(Object key, PanelCard card) {}

    private record Key(BlockPos pos, int slot) {}

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        float step = Math.min(0.1f, Math.max(0, (now - lastFrame) / 1000f));
        lastFrame = now;
        boolean visible = ClientConfig.panelHud && minecraft.player != null && minecraft.level != null
                && !minecraft.options.hideGui && minecraft.screen == null && !minecraft.getDebugOverlay().showDebugScreen();
        Target target = visible ? target(minecraft) : null;
        if (target != null) {
            if (!target.key().equals(key)) {
                key = target.key();
                changedAt = now;
            }
            card = target.card();
            open = Math.min(1, open + step / OPEN_SECONDS);
        } else {
            open = Math.max(0, open - step / CLOSE_SECONDS);
            if (open == 0) {
                key = null;
                card = null;
            }
        }
        if (card != null && open > 0.02f && visible) {
            draw(graphics, minecraft.font, card, easeOut(open), now);
        }
    }

    @Nullable
    private static Target target(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        if (!(minecraft.level.getBlockEntity(pos) instanceof PanelSource source)) {
            return null;
        }
        PanelCard card = source.panelCard(blockHit.getLocation(), minecraft.player.isShiftKeyDown());
        return card == null ? null : new Target(new Key(pos.immutable(), card.key), card);
    }

    private static float easeOut(float t) {
        float inverse = 1 - t;
        return 1 - inverse * inverse * inverse;
    }

    // ------------------------------------------------------------------ frame

    private void draw(GuiGraphics g, Font font, PanelCard card, float t, long now) {
        int body = 0;
        for (Element element : card.elements) {
            body += element.height() + GAP;
        }
        int footer = card.footer != null ? 15 : 6;
        int height = HEADER + body + footer;
        // A card taller than the screen is drawn smaller, anchored to the right edge.
        float scale = Math.min(1, (g.guiHeight() - 12f) / height);
        int screenWidth = Math.round(g.guiWidth() / scale);
        int screenHeight = Math.round(g.guiHeight() / scale);
        int slide = Math.round((1 - t) * (WIDTH + MARGIN + 6));
        int x0 = screenWidth - WIDTH - MARGIN + slide;
        int y0 = Math.max(6, (screenHeight - height) / 2 - 8);
        int x1 = x0 + WIDTH;
        int y1 = y0 + height;
        int accent = card.accent;
        g.pose().pushPose();
        g.pose().translate(0, 0, 50);
        g.pose().scale(scale, scale, 1);

        frame(g, x0, y0, x1, y1, accent, t, now);
        header(g, font, card, x0, y0, x1, t, now);

        int y = y0 + HEADER;
        int inner = WIDTH - 2 * PAD;
        for (Element element : card.elements) {
            element(g, font, element, x0 + PAD, y, inner, t, now);
            y += element.height() + GAP;
        }
        if (card.footer != null) {
            int fy = y1 - 12;
            g.fill(x0 + PAD, fy, x1 - PAD, fy + 1, fade(accent, 0.22f * t));
            text(g, font, card.footer.getString(), x0 + PAD, fy + 4, 0.55f, fade(DIM, t), false, inner - 16);
            // Activity lights: three dots blinking in turn.
            for (int i = 0; i < 3; i++) {
                boolean lit = (now / 220 + 2 - i) % 3 == 0;
                g.fill(x1 - PAD - 2 - i * 4, fy + 5, x1 - PAD - i * 4, fy + 7, fade(accent, (lit ? 0.9f : 0.25f) * t));
            }
        }
        g.pose().popPose();
    }

    /** The chamfered glass, its edge, the accent rail, scan lines, the sweep and the corner brackets. */
    private static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int accent, float t, long now) {
        int height = y1 - y0;
        int top = fade(BG_TOP, t);
        int bottom = fade(BG_BOTTOM, t);
        for (int row = 0; row < CHAMFER; row++) {
            g.fill(x0 + CHAMFER - row, y0 + row, x1, y0 + row + 1, top);
            g.fill(x0, y1 - CHAMFER + row, x1 - row - 1, y1 - CHAMFER + row + 1, bottom);
        }
        g.fillGradient(x0, y0 + CHAMFER, x1, y1 - CHAMFER, top, bottom);

        // Scan lines and a slow sweep down the glass.
        int scan = fade(0x0CFFFFFF, t);
        for (int y = y0 + CHAMFER; y < y1 - CHAMFER; y += 3) {
            g.fill(x0 + 1, y, x1 - 1, y + 1, scan);
        }
        int sweep = y0 + CHAMFER + (int) ((now % SWEEP_MS) * (height - 2 * CHAMFER) / SWEEP_MS);
        g.fill(x0 + 1, sweep, x1 - 1, sweep + 1, fade(accent, 0.10f * t));
        g.fill(x0 + 1, sweep - 1, x1 - 1, sweep, fade(accent, 0.04f * t));

        // Edge, cut at two corners.
        int edge = fade(accent, 0.55f * t);
        g.fill(x0 + CHAMFER, y0, x1, y0 + 1, edge);
        g.fill(x1 - 1, y0, x1, y1 - CHAMFER, edge);
        g.fill(x0, y1 - 1, x1 - CHAMFER, y1, edge);
        g.fill(x0, y0 + CHAMFER, x0 + 1, y1, edge);
        for (int i = 0; i < CHAMFER; i++) {
            g.fill(x0 + CHAMFER - 1 - i, y0 + i, x0 + CHAMFER - i, y0 + i + 1, edge);
            g.fill(x1 - CHAMFER + i, y1 - 1 - i, x1 - CHAMFER + i + 1, y1 - i, edge);
        }

        // The accent rail down the left of the header, with its glow.
        g.fill(x0 + 2, y0 + CHAMFER + 2, x0 + 6, y0 + HEADER - 6, fade(accent, 0.18f * t));
        g.fill(x0 + 3, y0 + CHAMFER + 2, x0 + 5, y0 + HEADER - 6, fade(accent, t));
        g.fill(x0 + 3, y0 + HEADER - 2, x0 + 5, y1 - CHAMFER - 2, fade(accent, 0.18f * t));

        // Ticks down the right edge.
        for (int y = y0 + 12; y < y1 - CHAMFER - 4; y += 6) {
            g.fill(x1 - 4, y, x1 - 2, y + 1, fade(accent, 0.35f * t));
        }

        // Brackets outside two corners.
        int bracket = fade(accent, 0.8f * t);
        g.fill(x1 - 9, y0 - 3, x1 + 3, y0 - 2, bracket);
        g.fill(x1 + 2, y0 - 3, x1 + 3, y0 + 9, bracket);
        g.fill(x0 - 3, y1 + 2, x0 + 9, y1 + 3, bracket);
        g.fill(x0 - 3, y1 - 9, x0 - 2, y1 + 3, bracket);
    }

    /** Kicker, decoding title, type tag, status light and the rule under them. */
    private void header(GuiGraphics g, Font font, PanelCard card, int x0, int y0, int x1, float t, long now) {
        int accent = card.accent;
        int tagWidth = 0;
        if (card.tag != null) {
            String tag = card.tag.getString().toUpperCase(Locale.ROOT);
            tagWidth = Math.round(spacedWidth(font, tag, 0.6f, 1)) + 7;
            int tx = x1 - 7 - tagWidth;
            g.fill(tx, y0 + 5, tx + tagWidth, y0 + 14, fade(accent, 0.9f * t));
            g.fill(tx - 2, y0 + 5, tx - 1, y0 + 14, fade(accent, 0.5f * t));
            spaced(g, font, tag, tx + 4, y0 + 7, 0.6f, fade(DARK_TEXT, t), 1);
        }
        String kicker = card.kicker.getString().toUpperCase(Locale.ROOT);
        spaced(g, font, fitSpaced(font, kicker, WIDTH - 2 * PAD - tagWidth - 6, 0.55f, 1), x0 + PAD, y0 + 7, 0.55f,
                fade(mix(accent, DIM, 0.45f), t), 1);

        String title = decode(card.title.getString(), now);
        text(g, font, title, x0 + PAD, y0 + 16, 1.0f, fade(TEXT, t), true, WIDTH - 2 * PAD);

        if (card.status != null) {
            int sy = y0 + 29;
            float pulse = card.statusBlinks ? 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(now / 160.0)) : 1;
            g.fill(x0 + PAD - 1, sy - 1, x0 + PAD + 5, sy + 5, fade(card.statusColor, 0.22f * pulse * t));
            g.fill(x0 + PAD, sy, x0 + PAD + 4, sy + 4, fade(card.statusColor, pulse * t));
            spaced(g, font, card.status.getString().toUpperCase(Locale.ROOT), x0 + PAD + 8, sy, 0.65f,
                    fade(card.statusColor, t), 1);
        }
        int rule = y0 + HEADER - 4;
        g.fill(x0 + PAD, rule - 1, x0 + PAD + 14, rule + 1, fade(accent, t));
        for (int x = x0 + PAD + 17; x < x1 - PAD; x += 4) {
            g.fill(x, rule, x + 2, rule + 1, fade(accent, 0.35f * t));
        }
    }

    /** The title, its unrevealed letters cycling through glyphs for a moment after the card changes. */
    private String decode(String title, long now) {
        long elapsed = now - changedAt;
        if (elapsed >= DECODE_MS || title.isEmpty()) {
            return title;
        }
        int revealed = (int) (title.length() * elapsed / DECODE_MS);
        StringBuilder text = new StringBuilder(title.length());
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (i < revealed || c == ' ') {
                text.append(c);
            } else {
                long frame = now / 45;
                text.append(GLYPHS.charAt((int) Math.floorMod(frame * 31 + i * 17 + frame * i * 7, (long) GLYPHS.length())));
            }
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ elements

    private static void element(GuiGraphics g, Font font, Element element, int x, int y, int width, float t, long now) {
        switch (element) {
            case Section section -> {
                String title = section.title().getString().toUpperCase(Locale.ROOT);
                float end = spaced(g, font, title, x, y + 3, 0.55f, fade(DIM, t), 1);
                g.fill(Math.round(end) + 4, y + 5, x + width, y + 6, fade(DIM, 0.35f * t));
                g.fill(x + width - 3, y + 4, x + width, y + 7, fade(DIM, 0.6f * t));
            }
            case Field field -> field(g, font, field, x, y, width, t);
            case Grid grid -> {
                int half = (width - 8) / 2;
                for (int i = 0; i < grid.cells().size(); i++) {
                    int cx = x + (i % 2) * (half + 8);
                    int cy = y + (i / 2) * 10;
                    field(g, font, grid.cells().get(i), cx, cy, half, t);
                }
                // A faint divider down the middle.
                g.fill(x + half + 3, y + 1, x + half + 4, y + grid.height() - 2, fade(DIM, 0.18f * t));
            }
            case BigValue big -> {
                String unit = big.unit();
                float unitWidth = unit.isEmpty() ? 0 : font.width(unit) + 3;
                String value = big.value().getString();
                float scale = font.width(value) * 2 + unitWidth <= width ? 2 : 1.5f;
                value = fit(font, value, (width - unitWidth) / scale);
                // A faint echo behind the number reads as a glow.
                text(g, font, value, x + 1, y + 4, scale, fade(big.color(), 0.25f * t), false, width);
                text(g, font, value, x, y + 3, scale, fade(big.color(), t), true, width);
                if (!unit.isEmpty()) {
                    text(g, font, unit, x + font.width(value) * scale + 3, y + 3 + 9 * scale - 9, 1, fade(mix(big.color(), TEXT, 0.4f), t),
                            false, width);
                }
            }
            case Bar bar -> {
                spaced(g, font, bar.label().getString().toUpperCase(Locale.ROOT), x, y + 1, 0.55f, fade(DIM, t), 1);
                String right = bar.right().getString();
                text(g, font, right, x + width - font.width(right) * 0.6f, y, 0.6f, fade(bar.color(), t), false, width);
                segments(g, x, y + 8, width, 5, 26, bar.fraction(), bar.color(), t);
            }
            case Signal signal -> {
                spaced(g, font, signal.label().getString().toUpperCase(Locale.ROOT), x, y + 4, 0.55f, fade(DIM, t), 1);
                int level = Math.max(0, Math.min(15, signal.level()));
                int barsLeft = x + width - 15 * 4 + 1;
                String number = Integer.toString(level);
                text(g, font, number, barsLeft - 5 - font.width(number), y + 3, 1, fade(level > 0 ? RED : DIM, t), true, width);
                for (int i = 0; i < 15; i++) {
                    int barHeight = 3 + i * 7 / 14;
                    int bx = barsLeft + i * 4;
                    boolean lit = i < level;
                    int color = lit ? mix(0xFF9A1C12, RED, i / 14f) : 0xFFFFFFFF;
                    g.fill(bx, y + 12 - barHeight, bx + 3, y + 12, fade(color, (lit ? 1 : 0.12f) * t));
                }
            }
            case Code code -> {
                spaced(g, font, code.label().getString().toUpperCase(Locale.ROOT), x, y + 1, 0.55f, fade(DIM, t), 1);
                String chars = LinkCode.sanitize(code.code());
                for (int i = 0; i < LinkCode.LENGTH; i++) {
                    int cx = x + i * 18;
                    int cy = y + 7;
                    g.fill(cx, cy, cx + 15, cy + 13, fade(0xFF000000, 0.45f * t));
                    outline(g, cx, cy, 15, 13, fade(code.color(), 0.75f * t));
                    g.fill(cx + 2, cy + 11, cx + 13, cy + 12, fade(code.color(), 0.25f * t));
                    if (i < chars.length()) {
                        String c = String.valueOf(chars.charAt(i));
                        text(g, font, c, cx + 8 - font.width(c) / 2f, cy + 3, 1, fade(code.color(), t), true, 15);
                    } else if (i == chars.length() && (now / 400) % 2 == 0) {
                        g.fill(cx + 5, cy + 9, cx + 10, cy + 10, fade(code.color(), t));
                    }
                }
            }
            case Items items -> {
                spaced(g, font, items.label().getString().toUpperCase(Locale.ROOT), x, y + 6, 0.55f, fade(DIM, t), 1);
                for (int i = 0; i < items.stacks().size(); i++) {
                    int ix = x + width - (items.stacks().size() - i) * 20;
                    g.fill(ix, y, ix + 18, y + 18, fade(0xFF000000, 0.45f * t));
                    outline(g, ix, y, 18, 18, fade(CYAN, 0.45f * t));
                    ItemStack stack = items.stacks().get(i);
                    if (!stack.isEmpty() && t > 0.9f) {
                        g.renderItem(stack, ix + 1, y + 1);
                    }
                }
            }
            case Note note -> text(g, font, "› " + note.text().getString(), x, y + 1, 0.6f, fade(DIM, t), false, width);
            case Alert alert -> {
                float flash = 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.sin(now / 140.0));
                g.fill(x, y, x + width, y + 11, fade(alert.color(), 0.16f * flash * t));
                g.fill(x, y, x + 2, y + 11, fade(alert.color(), flash * t));
                // Hazard notches at the right end.
                for (int i = 0; i < 3; i++) {
                    int nx = x + width - 4 - i * 5;
                    g.fill(nx, y + 2, nx + 2, y + 9, fade(alert.color(), 0.5f * flash * t));
                }
                text(g, font, alert.text().getString().toUpperCase(Locale.ROOT), x + 6, y + 2, 0.75f, fade(alert.color(), t), true,
                        width - 22);
            }
        }
    }

    /**
     * A label on the left and its value on the right, in {@code width} GUI pixels. The value is laid
     * out first, up to three quarters of the row: a label can lose letters, a reading should not.
     */
    private static void field(GuiGraphics g, Font font, Field field, int x, int y, int width, float t) {
        String value = fit(font, field.value().getString(), width * 0.75f / 0.75f);
        float valueWidth = font.width(value) * 0.75f;
        String label = fitSpaced(font, field.label().getString().toUpperCase(Locale.ROOT), width - valueWidth - 4, 0.55f, 1);
        spaced(g, font, label, x, y + 2, 0.55f, fade(DIM, t), 1);
        text(g, font, value, x + width - valueWidth, y + 1, 0.75f, fade(field.color(), t), false, width);
    }

    /** A segmented gauge: lit segments brighten towards the end, the rest dark, with a tick every quarter. */
    private static void segments(GuiGraphics g, int x, int y, int width, int height, int count, float fraction, int color, float t) {
        int lit = Math.round(Math.max(0, Math.min(1, fraction)) * count);
        for (int i = 0; i < count; i++) {
            int sx = x + i * width / count;
            int ex = x + (i + 1) * width / count - 1;
            boolean on = i < lit;
            int c = on ? mix(mix(color, 0xFF000000, 0.35f), color, i / (float) Math.max(1, count - 1)) : color;
            g.fill(sx, y, ex, y + height, fade(c, (on ? 1 : 0.13f) * t));
            if (i % (count / 4) == 0) {
                g.fill(sx, y + height + 1, sx + 1, y + height + 3, fade(DIM, 0.6f * t));
            }
        }
        g.fill(x + width - 1, y + height + 1, x + width, y + height + 3, fade(DIM, 0.6f * t));
    }

    private static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    // ------------------------------------------------------------------ text and colour

    /** Text at a scale, cut to {@code maxWidth} GUI pixels. */
    private static void text(GuiGraphics g, Font font, String text, float x, float y, float scale, int color, boolean shadow, float maxWidth) {
        if (alpha(color) < 0x08) {
            return;
        }
        String shown = fit(font, text, maxWidth / scale);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, shown, 0, 0, color, shadow);
        g.pose().popPose();
    }

    /** Capitals spread out a little, the panel's label style; returns where the text ends. */
    private static float spaced(GuiGraphics g, Font font, String text, float x, float y, float scale, int color, int spacing) {
        float cursor = x;
        boolean draw = alpha(color) >= 0x08;
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            if (draw) {
                g.pose().pushPose();
                g.pose().translate(cursor, y, 0);
                g.pose().scale(scale, scale, 1);
                g.drawString(font, c, 0, 0, color, false);
                g.pose().popPose();
            }
            cursor += (font.width(c) + spacing) * scale;
        }
        return cursor;
    }

    private static float spacedWidth(Font font, String text, float scale, int spacing) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += (font.width(String.valueOf(text.charAt(i))) + spacing) * scale;
        }
        return width;
    }

    /** Spaced capitals cut with "..." to fit {@code maxWidth} GUI pixels. */
    private static String fitSpaced(Font font, String text, float maxWidth, float scale, int spacing) {
        if (spacedWidth(font, text, scale, spacing) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && spacedWidth(font, cut + "...", scale, spacing) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /** {@code text} cut with "..." to fit {@code maxWidth} unscaled font pixels. */
    private static String fit(Font font, String text, float maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, (int) maxWidth - font.width("..."))) + "...";
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }

    /** The colour with its alpha multiplied by {@code factor}. */
    private static int fade(int argb, float factor) {
        int alpha = Math.round(alpha(argb) * Math.max(0, Math.min(1, factor)));
        return alpha << 24 | argb & 0xFFFFFF;
    }

    private static int mix(int from, int to, float amount) {
        float a = Math.max(0, Math.min(1, amount));
        int r = Math.round(((from >> 16) & 0xFF) * (1 - a) + ((to >> 16) & 0xFF) * a);
        int gr = Math.round(((from >> 8) & 0xFF) * (1 - a) + ((to >> 8) & 0xFF) * a);
        int b = Math.round((from & 0xFF) * (1 - a) + (to & 0xFF) * a);
        int al = Math.round(alpha(from) * (1 - a) + alpha(to) * a);
        return al << 24 | r << 16 | gr << 8 | b;
    }
}
