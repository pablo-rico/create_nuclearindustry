package org.papiricoh.create_nuclearindustry.dyson.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmGeometry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmSyncPayload;
import org.papiricoh.create_nuclearindustry.dyson.terminal.ConfigureSwarmOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.terminal.SwarmTerminalMenu;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.dyson.gui.TerminalLayout.*;

/**
 * A swarm terminal's screen. The sky round the sun on the left shows the four orbits as they look from the ground, with a few of
 * their sails going round; the picked orbit is bright, closed ones dotted. On the right, the picked orbit: open or closed, and its
 * radius, inclination and node as sliders, sent to the server when let go; its sails and how many wear out within a day. Along
 * the bottom, the whole swarm and what it beams down.
 */
public class SwarmTerminalScreen extends AbstractContainerScreen<SwarmTerminalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/swarm_terminal_screen.png");
    private static final String KEY = "create_nuclearindustry.swarm.";
    private static final int TEXT = 0xFFFFE6A0, FAINT = 0xFFB89A5A, SUN = 0xFFFFF2B0;
    private static final int[] ORBIT_COLOURS = {0xFFFFC857, 0xFF7FD8FF, 0xFFFF8FB8, 0xFFA8FF8A};
    /** Ticks a value set here is shown before the server's copy takes over again. */
    private static final int PENDING_TICKS = 40;
    /** Sails drawn going round each orbit in the plot. */
    private static final int PLOT_SAILS = 40;

    private int picked;
    /** The slider being dragged: 0 radius, 1 inclination, 2 node; -1 none. */
    private int dragging = -1;
    @Nullable
    private OrbitShape pending;
    private int pendingOrbit = -1;
    private long pendingUntil;

    public SwarmTerminalScreen(SwarmTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = -1000;
    }

    private long now() {
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
    }

    /** The orbit as shown: what was just set here, or else what the server last said. */
    private OrbitShape shape(int orbit) {
        if (pending != null && pendingOrbit == orbit && (dragging >= 0 || now() < pendingUntil)) return pending;
        return ClientSwarm.get().shapes()[orbit];
    }

    private boolean over(LayoutRect r, double mx, double my) {
        return r.contains(mx - leftPos, my - topPos);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (int i = 0; i < TABS.size(); i++) {
            if (over(TABS.get(i), mx, my)) {
                picked = i;
                return true;
            }
        }
        if (over(ENABLE, mx, my)) {
            OrbitShape s = shape(picked);
            send(new OrbitShape(!s.enabled(), s.radius(), s.inclination(), s.node()));
            return true;
        }
        LayoutRect[] sliders = {RADIUS, INCLINATION, NODE};
        for (int i = 0; i < sliders.length; i++) {
            if (over(sliders[i], mx, my)) {
                dragging = i;
                drag(mx);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            drag(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0) {
            drag(mx);
            dragging = -1;
            if (pending != null) send(pending);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    /** Moves the dragged slider to the mouse, in whole degrees. */
    private void drag(double mx) {
        LayoutRect track = dragging == 0 ? RADIUS : dragging == 1 ? INCLINATION : NODE;
        float t = Mth.clamp((float) (mx - leftPos - track.x() - 3) / (track.w() - 6), 0, 1);
        OrbitShape s = shape(picked);
        float radius = s.radius(), inclination = s.inclination(), node = s.node();
        if (dragging == 0) radius = Math.round(OrbitShape.MIN_RADIUS + t * (OrbitShape.MAX_RADIUS - OrbitShape.MIN_RADIUS));
        else if (dragging == 1) inclination = Math.round(t * OrbitShape.MAX_INCLINATION);
        else node = Math.round(t * 359);
        pending = new OrbitShape(s.enabled(), radius, inclination, node);
        pendingOrbit = picked;
    }

    private void send(OrbitShape shape) {
        pending = shape;
        pendingOrbit = picked;
        pendingUntil = now() + PENDING_TICKS;
        PacketDistributor.sendToServer(new ConfigureSwarmOrbitPayload(menu.pos, picked, shape.enabled(), shape.radius(),
                shape.inclination(), shape.node()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mx, int my) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, SHEET, SHEET);
        for (int i = 0; i < TABS.size(); i++) {
            LayoutRect r = TABS.get(i);
            graphics.blit(TEXTURE, leftPos + r.x(), topPos + r.y(), SPRITE_TAB.x(), SPRITE_TAB.y() + (i == picked ? r.h() : 0), r.w(), r.h(), SHEET, SHEET);
        }
        OrbitShape s = shape(picked);
        graphics.blit(TEXTURE, leftPos + ENABLE.x(), topPos + ENABLE.y(), SPRITE_TOGGLE.x(), SPRITE_TOGGLE.y() + (s.enabled() ? ENABLE.h() : 0),
                ENABLE.w(), ENABLE.h(), SHEET, SHEET);
        slider(graphics, RADIUS, (s.radius() - OrbitShape.MIN_RADIUS) / (OrbitShape.MAX_RADIUS - OrbitShape.MIN_RADIUS));
        slider(graphics, INCLINATION, s.inclination() / OrbitShape.MAX_INCLINATION);
        slider(graphics, NODE, s.node() / 359f);
        plot(graphics, partial);
    }

    private void slider(GuiGraphics graphics, LayoutRect track, float t) {
        graphics.blit(TEXTURE, leftPos + track.x(), topPos + track.y(), SPRITE_SLIDER.x(), SPRITE_SLIDER.y(), track.w(), track.h(), SHEET, SHEET);
        int knob = Math.round(Mth.clamp(t, 0, 1) * (track.w() - SPRITE_KNOB.w()));
        graphics.blit(TEXTURE, leftPos + track.x() + knob, topPos + track.y(), SPRITE_KNOB.x(), SPRITE_KNOB.y(), SPRITE_KNOB.w(), SPRITE_KNOB.h(), SHEET, SHEET);
    }

    /** The orbits round the sun, as seen from the ground: each ring and a few of its sails going round. */
    private void plot(GuiGraphics graphics, float partial) {
        int cx = leftPos + SKY_CX, cy = topPos + SKY_CY;
        float ppd = OrbitShapeScale.PIXELS_PER_DEGREE;
        double time = now() + partial;
        SwarmSyncPayload swarm = ClientSwarm.get();
        for (int pass = 0; pass < 2; pass++) {
            for (int o = 0; o < OrbitShape.COUNT; o++) {
                if ((o == picked) != (pass == 1)) continue;
                OrbitShape s = shape(o);
                int colour = o == picked ? ORBIT_COLOURS[o] : (ORBIT_COLOURS[o] & 0x00FFFFFF) | 0x70000000;
                for (int k = 0; k < 96; k++) {
                    if (!s.enabled() && k % 3 != 0) continue;
                    point(graphics, s, k / 96.0 * Math.PI * 2, cx, cy, ppd, colour, 0);
                }
                long sails = Math.min(PLOT_SAILS, swarm.sails()[o]);
                for (int i = 0; i < sails; i++) {
                    double phase = Math.PI * 2 * (frac(i * 0.6180339887498949) + time / SwarmGeometry.period(s.radius()));
                    point(graphics, s, phase, cx, cy, ppd, 0xFFFFFFFF, 1);
                }
            }
        }
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, SUN);
        graphics.fill(cx - 4, cy - 2, cx + 5, cy + 3, SUN);
        graphics.fill(cx - 2, cy - 4, cx + 3, cy + 5, SUN);
    }

    /** Draws the point of orbit {@code s} at {@code phase}: a pixel, or a 2×2 sail with {@code size} 1. */
    private static void point(GuiGraphics graphics, OrbitShape s, double phase, int cx, int cy, float ppd, int colour, int size) {
        double node = Math.toRadians(s.node()), tilt = Math.toRadians(s.inclination());
        double cn = Math.cos(node), sn = Math.sin(node), ci = Math.cos(tilt);
        double c = Math.cos(phase), sp = Math.sin(phase);
        double across = s.radius() * (c * sn + sp * ci * cn), down = s.radius() * (c * cn - sp * ci * sn);
        int x = cx + (int) Math.round(across * ppd), y = cy + (int) Math.round(down * ppd);
        graphics.fill(x, y, x + 1 + size, y + 1 + size, colour);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        graphics.drawString(font, title, TITLE.x() + 2, TITLE.y() + 2, TEXT, false);
        for (int i = 0; i < TABS.size(); i++) {
            LayoutRect r = TABS.get(i);
            String label = String.valueOf(i + 1);
            graphics.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + 3, i == picked ? TEXT : FAINT, false);
        }
        OrbitShape s = shape(picked);
        centred(graphics, Component.translatable(KEY + (s.enabled() ? "open" : "closed")), ENABLE, TEXT);
        centred(graphics, Component.translatable(KEY + "radius", degrees(s.radius())), RADIUS, TEXT);
        centred(graphics, Component.translatable(KEY + "inclination", degrees(s.inclination())), INCLINATION, TEXT);
        centred(graphics, Component.translatable(KEY + "node", degrees(s.node())), NODE, TEXT);

        SwarmSyncPayload swarm = ClientSwarm.get();
        graphics.drawString(font, Component.translatable(KEY + "orbit_sails", swarm.sails()[picked]), ORBIT_STATS.x() + 2, ORBIT_STATS.y() + 3, TEXT, false);
        graphics.drawString(font, Component.translatable(KEY + "expiring", swarm.expiringToday()[picked]), ORBIT_STATS.x() + 2, ORBIT_STATS.y() + 15, FAINT, false);

        graphics.drawString(font, Component.translatable(KEY + "summary", swarm.totalSails(), Math.round(swarm.sunFactor() * 100),
                Math.round(swarm.pool())), SWARM.x() + 4, SWARM.y() + 5, TEXT, false);
        graphics.drawString(font, Component.translatable(KEY + "beamed", swarm.receivers(), swarm.delivered()),
                SWARM.x() + 4, SWARM.y() + 18, FAINT, false);
    }

    private void centred(GuiGraphics graphics, Component text, LayoutRect r, int colour) {
        graphics.drawString(font, text, r.x() + (r.w() - font.width(text)) / 2, r.y() + 3, colour, true);
    }

    private static String degrees(float value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partial) {
        super.render(graphics, mx, my, partial);
        renderTooltip(graphics, mx, my);
    }
}
