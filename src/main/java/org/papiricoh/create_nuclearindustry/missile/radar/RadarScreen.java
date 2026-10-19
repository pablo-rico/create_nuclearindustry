package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** A coordinate plot only: opening this screen never requests map chunks. */
public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private double span = 4096;
    private int page;
    private UUID selected;
    private static final int PAGE_SIZE = 6;
    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 340; imageHeight = 242;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("−"), b -> span = Math.min(60_000_000, span * 2)).bounds(leftPos + 8, topPos + 214, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> span = Math.max(64, span / 2)).bounds(leftPos + 36, topPos + 214, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1)).bounds(leftPos + 220, topPos + 214, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(Math.max(0, (menu.contacts.size() - 1) / PAGE_SIZE), page + 1)).bounds(leftPos + 306, topPos + 214, 24, 20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff17242c);
        g.fill(leftPos + 8, topPos + 40, leftPos + 206, topPos + 202, 0xff071b17);
        for (int x = 0; x < 5; x++) g.vLine(leftPos + 8 + x * 49, topPos + 40, topPos + 201, 0xff173e32);
        for (int z = 0; z < 5; z++) g.hLine(leftPos + 8, leftPos + 205, topPos + 40 + z * 40, 0xff173e32);
        RadarContacts.Contact focus = menu.contacts.stream().filter(c -> c.id().equals(selected)).findFirst().orElse(null);
        double cx = focus == null ? menu.pos.getX() : focus.position().x;
        double cz = focus == null ? menu.pos.getZ() : focus.position().z;
        g.enableScissor(leftPos + 9, topPos + 41, leftPos + 205, topPos + 201);
        for (var c : menu.contacts) {
            int x = px(c.position().x, cx), z = pz(c.position().z, cz);
            int ix = px(c.impact().x, cx), iz = pz(c.impact().z, cz);
            int color = c.id().equals(selected) ? 0xffffdd77 : 0xff62efab;
            if (c.origin() != null) line(g, px(c.origin().getX(), cx), pz(c.origin().getZ(), cz), x, z, 0xff315a4a);
            line(g, x, z, ix, iz, 0xff4d7866);
            g.fill(x - 2, z - 2, x + 3, z + 3, color);
            g.hLine(ix - 3, ix + 3, iz, 0xffff735d); g.vLine(ix, iz - 3, iz + 3, 0xffff735d);
        }
        g.disableScissor();
    }
    private int px(double x, double cx) { return leftPos + 107 + (int) Math.max(-10000, Math.min(10000, (x - cx) / span * 198)); }
    private int pz(double z, double cz) { return topPos + 121 + (int) Math.max(-10000, Math.min(10000, (z - cz) / span * 162)); }
    private void line(GuiGraphics g, int x, int y, int tx, int ty, int color) {
        // Clip before rasterization; distant contacts must not allocate work proportional to distance.
        double low = 0, high = 1;
        int dx = tx - x, dy = ty - y;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x - leftPos - 9, leftPos + 204 - x, y - topPos - 41, topPos + 200 - y};
        for (int edge = 0; edge < 4; edge++) {
            if (p[edge] == 0) { if (q[edge] < 0) return; }
            else if (p[edge] < 0) low = Math.max(low, q[edge] / p[edge]);
            else high = Math.min(high, q[edge] / p[edge]);
        }
        if (low > high) return;
        tx = (int) (x + high * dx); ty = (int) (y + high * dy);
        x = (int) (x + low * dx); y = (int) (y + low * dy);
        int steps = Math.max(Math.abs(tx - x), Math.abs(ty - y));
        for (int i = 0; i <= steps; i++) { double t = steps == 0 ? 0 : (double) i / steps;
            int a = (int) (x + (tx - x) * t), b = (int) (y + (ty - y) * t); g.fill(a, b, a + 1, b + 1, color); }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0xffd9eee5, false);
        g.drawString(font, Component.translatable("radar.create_nuclearindustry." + (menu.status == 2 ? "static_only" : menu.status == 1 ? "online" : "offline")), 8, 24, 0xff83cbaa, false);
        page = Math.min(page, Math.max(0, (menu.contacts.size() - 1) / PAGE_SIZE));
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < menu.contacts.size(); i++) {
            var c = menu.contacts.get(page * PAGE_SIZE + i); int y = 42 + i * 25;
            g.drawString(font, c.id().toString().substring(0, 8), 214, y, c.id().equals(selected) ? 0xffffdd77 : 0xffdddddd, false);
            g.drawString(font, Component.translatable("radar.create_nuclearindustry.impact", Math.max(0, (c.ticks() + 19) / 20)), 214, y + 11, 0xff88baaa, false);
        }
        g.drawString(font, (page + 1) + "/" + Math.max(1, (menu.contacts.size() + PAGE_SIZE - 1) / PAGE_SIZE), 257, 220, 0xffffffff, false);
        g.drawString(font, "X/Z · " + (int) span + " m", 66, 220, 0xff88baaa, false);
        g.drawString(font, Component.translatable("radar.create_nuclearindustry.contacts", menu.contacts.size()), 214, 198, 0xff88baaa, false);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= leftPos + 210 && x < leftPos + 333 && y >= topPos + 40 && y < topPos + 190) {
            int index = page * PAGE_SIZE + (int) (y - topPos - 40) / 25;
            if (index < menu.contacts.size()) { selected = menu.contacts.get(index).id(); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        if (x >= leftPos + 210 && x < leftPos + 333 && y >= topPos + 40 && y < topPos + 190) {
            int index = page * PAGE_SIZE + (y - topPos - 40) / 25;
            if (index < menu.contacts.size()) {
                var c = menu.contacts.get(index);
                g.renderComponentTooltip(font, java.util.List.of(Component.literal(c.id().toString()),
                        Component.translatable("radar.create_nuclearindustry.origin", c.origin() == null ? Component.translatable("radar.create_nuclearindustry.unknown") : Component.literal(c.origin().toShortString())),
                        Component.translatable("radar.create_nuclearindustry.position", coordinates(c.position())),
                        Component.translatable("radar.create_nuclearindustry.velocity", coordinates(c.velocity())),
                        Component.translatable("radar.create_nuclearindustry.target", coordinates(c.impact()))), x, y);
            }
        }
    }
    private static String coordinates(Vec3 v) { return String.format(java.util.Locale.ROOT, "%.0f / %.0f / %.0f", v.x, v.y, v.z); }
}
