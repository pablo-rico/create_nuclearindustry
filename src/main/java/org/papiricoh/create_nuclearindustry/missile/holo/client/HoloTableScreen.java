package org.papiricoh.create_nuclearindustry.missile.holo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloDesignatePayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableMenu;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloView;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;
import org.papiricoh.create_nuclearindustry.missile.radar.client.ScopeDraw;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.client.ClientTerrain;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The command table's screen: its map from above with the land, the tracks (flown, still to fly, where they come down), the radars
 * and the silo's reach; the list of tracks; the silo's state. In designate mode a click on the map sends the silo there, as a
 * ground or air burst; the server checks the table, the silo and its reach before it takes.
 */
public class HoloTableScreen extends AbstractContainerScreen<HoloTableMenu> {
    private static final int W = 320, H = 222, MAP = 200, MX = 8, MY = 16;
    private static final int PX = 216, PW = 96;
    private static final int CYAN = 0xFF6FF4FF, DIM = 0xFF2C7C88, FAINT = 0xFF1A4850, RED = 0xFFFF4A3A, AMBER = 0xFFFFB347, GREEN = 0xFF7DFF9A,
            WHITE = 0xFFE8FFFF;
    private static final String KEY = "holo_table.create_nuclearindustry.screen.";

    private int view = HoloView.VIEWS[0];
    private boolean auto = true, designate, airburst, dragging;
    private double cx, cz;
    private UUID selected;

    public HoloTableScreen(HoloTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
    }

    private HoloTracksPayload picture() {
        return ClientHolo.picture(menu.centre);
    }

    private double now(float partial) {
        return minecraft.level == null ? 0 : minecraft.level.getGameTime() + partial;
    }

    // ------------------------------------------------------------------ geometry (offsets from the table's centre)

    private float sx(double dx) {
        return (float) (leftPos + MX + MAP / 2.0 + (dx - cx) / view * MAP);
    }

    private float sy(double dz) {
        return (float) (topPos + MY + MAP / 2.0 + (dz - cz) / view * MAP);
    }

    private double dx(double screenX) {
        return cx + (screenX - leftPos - MX - MAP / 2.0) / MAP * view;
    }

    private double dz(double screenY) {
        return cz + (screenY - topPos - MY - MAP / 2.0) / MAP * view;
    }

    private boolean overMap(double mx, double my) {
        return mx >= leftPos + MX && mx < leftPos + MX + MAP && my >= topPos + MY && my < topPos + MY + MAP;
    }

    private boolean overButton(int index, double mx, double my) {
        int y = topPos + MY + MAP - 3 * 16 + index * 16;
        return mx >= leftPos + PX && mx < leftPos + PX + PW && my >= y && my < y + 14;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        HoloTracksPayload picture = picture();
        if (auto && picture != null) {
            view = ClientHolo.view(picture);
            cx = picture.board().ox();
            cz = picture.board().oz();
        }
        g.fill(leftPos, topPos, leftPos + W, topPos + H, 0xF0081418);
        g.renderOutline(leftPos, topPos, W, H, DIM);
        g.fill(leftPos + MX - 1, topPos + MY - 1, leftPos + MX + MAP + 1, topPos + MY + MAP + 1, FAINT);
        g.fill(leftPos + MX, topPos + MY, leftPos + MX + MAP, topPos + MY + MAP, 0xFF030C10);
        g.enableScissor(leftPos + MX, topPos + MY, leftPos + MX + MAP, topPos + MY + MAP);
        terrain(g);
        double now = now(partial);
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            for (int i = 1; i < 8; i++) {
                float t = leftPos + MX + MAP * i / 8f, u = topPos + MY + MAP * i / 8f;
                d.line(t, topPos + MY, t, topPos + MY + MAP, 0.5f, 0x2240E0FF);
                d.line(leftPos + MX, u, leftPos + MX + MAP, u, 0.5f, 0x2240E0FF);
            }
            d.circle(sx(0), sy(0), 2.5f, 1, WHITE, false);
            if (picture != null) draw(d, picture, now);
            if (designate && overMap(mouseX, mouseY)) {
                boolean ok = inReach(picture, dx(mouseX), dz(mouseY));
                int c = ok ? GREEN : RED;
                d.line(mouseX - 6, mouseY, mouseX + 6, mouseY, 1, c);
                d.line(mouseX, mouseY - 6, mouseX, mouseY + 6, 1, c);
                d.circle(mouseX, mouseY, 4, 1, c, false);
            }
        }
        g.disableScissor();
    }

    private void terrain(GuiGraphics g) {
        int lod = HoloView.lod(view);
        double half = view / 2.0;
        int bx = menu.centre.getX(), bz = menu.centre.getZ();
        long[] keys = TerrainTile.covering(lod, (int) Math.floor(bx + cx - half), (int) Math.floor(bz + cz - half), (int) Math.ceil(bx + cx + half),
                (int) Math.ceil(bz + cz + half));
        if (keys.length > 36) return;
        ClientTerrain.want(menu.centre, keys);
        int span = TerrainTile.span(lod);
        for (long key : keys) {
            ResourceLocation texture = ClientTerrain.texture(key);
            if (texture == null) continue;
            double ox = TerrainTile.origin(lod, TerrainTile.tx(key)) - bx - 0.5, oz = TerrainTile.origin(lod, TerrainTile.tz(key)) - bz - 0.5;
            ScopeDraw.texture(g, texture, sx(ox), sy(oz), sx(ox + span), sy(oz + span), 0, 0, 1, 1, 0xB4FFFFFF);
        }
    }

    private void draw(ScopeDraw d, HoloTracksPayload picture, double now) {
        float scale = (float) MAP / view;
        for (HoloTracksPayload.Radar radar : picture.radars()) {
            float x = sx(radar.dx() + 0.5), y = sy(radar.dz() + 0.5);
            int c = radar.online() ? GREEN : 0xFF607060;
            d.rect(x - 1.5f, y - 1.5f, x + 1.5f, y + 1.5f, c);
            if (radar.reach() > 0) d.circle(x, y, radar.reach() * scale, 0.7f, ScopeDraw.alpha(c, 0.45f), true);
        }
        HoloTracksPayload.Silo silo = picture.silo();
        if (silo != null) {
            float x = sx(silo.dx() + 0.5), y = sy(silo.dz() + 0.5);
            d.circle(x, y, 3, 1.2f, AMBER, false);
            if (silo.reach() > 0) d.circle(x, y, silo.reach() * scale, 0.9f, ScopeDraw.alpha(AMBER, 0.6f), false);
            if (silo.targeted()) {
                float tx = sx(silo.tx() + 0.5), ty = sy(silo.tz() + 0.5);
                d.line(x, y, tx, ty, 0.7f, ScopeDraw.alpha(RED, 0.6f));
                d.line(tx - 4, ty - 4, tx + 4, ty + 4, 1.2f, RED);
                d.line(tx - 4, ty + 4, tx + 4, ty - 4, 1.2f, RED);
                if (silo.cep() > 0) d.circle(tx, ty, Math.max(1.5f, silo.cep() * scale), 0.7f, ScopeDraw.alpha(RED, 0.7f), true);
            }
        }
        double since = Math.max(0, Math.min(20, now - picture.sample()));
        float g = Math.max(1e-5f, picture.gravity());
        for (HoloTracksPayload.Track t : picture.tracks()) {
            int c = t.id().equals(selected) ? WHITE : t.threat() ? RED : CYAN;
            float[] h = t.history();
            for (int i = 3; i + 2 < h.length; i += 3) d.line(sx(h[i - 3]), sy(h[i - 1]), sx(h[i]), sy(h[i + 2]), 1, ScopeDraw.alpha(c, 0.3f + 0.6f * i / h.length));
            double px = t.x() + t.vx() * since, pz = t.z() + t.vz() * since, py = t.y() + t.vy() * since - 0.5 * g * since * since;
            double vy = t.vy() - g * since;
            double fall = RadarModel.timeToGround(new org.papiricoh.create_nuclearindustry.missile.physics.V3(px, py, pz),
                    new org.papiricoh.create_nuclearindustry.missile.physics.V3(t.vx(), vy, t.vz()), g, Math.min(RadarModel.SEA_LEVEL, t.fy()));
            if (!Double.isFinite(fall)) fall = 0;
            float ix = sx(px + t.vx() * fall), iz = sy(pz + t.vz() * fall);
            int dashes = 16;
            for (int i = 0; i < dashes; i += 2) {
                float a = (float) i / dashes, b = (float) (i + 1) / dashes;
                d.line(sx(px) + (ix - sx(px)) * a, sy(pz) + (iz - sy(pz)) * a, sx(px) + (ix - sx(px)) * b, sy(pz) + (iz - sy(pz)) * b, 0.8f,
                        ScopeDraw.alpha(t.threat() ? RED : AMBER, 0.75f));
            }
            int ring = t.threat() ? RED : AMBER;
            d.ellipse(sx(t.ix()), sy(t.iz()), Math.max(2, t.along() * scale), Math.max(1.5f, t.across() * scale), t.heading(), 0.8f,
                    ScopeDraw.alpha(ring, 0.85f), true, ScopeDraw.alpha(ring, 0.12f));
            d.circle(sx(t.fx()), sy(t.fz()), 2.5f, 0.8f, ScopeDraw.alpha(GREEN, 0.7f), false);
            d.disc(sx(px), sy(pz), 5, ScopeDraw.alpha(c, 0.35f), ScopeDraw.alpha(c, 0));
            d.disc(sx(px), sy(pz), 2, c, ScopeDraw.alpha(c, 0.7f));
        }
        for (ClientHolo.Flash flash : ClientHolo.flashes()) {
            float age = (net.minecraft.Util.getMillis() - flash.at()) / (float) ClientHolo.FLASH_MILLIS;
            float x = sx(flash.x() - menu.centre.getX() - 0.5), y = sy(flash.z() - menu.centre.getZ() - 0.5);
            for (int k = 0; k < 3; k++) {
                float wave = (age * 3 + k * 0.33f) % 1;
                d.circle(x, y, Math.max(3, flash.radius() * scale) * (0.3f + wave * 2.2f), 1.2f, ScopeDraw.alpha(0xFFFFF0C0, (1 - wave) * (1 - age)), false);
            }
        }
    }

    private boolean inReach(HoloTracksPayload picture, double dx, double dz) {
        if (picture == null || picture.silo() == null || picture.silo().reach() <= 0) return false;
        HoloTracksPayload.Silo s = picture.silo();
        return Math.hypot(dx - s.dx(), dz - s.dz()) <= s.reach();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        HoloTracksPayload picture = picture();
        g.drawString(font, title, MX, 5, CYAN, false);
        String scale = view >= 1000 ? (view / 1000) + " km" : view + " m";
        String corner = scale + (auto ? " · " + Component.translatable(KEY + "auto").getString() : "");
        g.drawString(font, corner, MX + MAP - font.width(corner), 5, DIM, false);
        int y = MY;
        g.drawString(font, Component.translatable(KEY + "tracks"), PX, y, CYAN, false);
        y += 11;
        double now = now(0);
        if (picture == null || picture.tracks().isEmpty()) {
            g.drawString(font, Component.translatable(KEY + "none"), PX, y, FAINT, false);
            y += 10;
        } else {
            List<HoloTracksPayload.Track> tracks = picture.tracks();
            for (int i = 0; i < Math.min(7, tracks.size()); i++) {
                HoloTracksPayload.Track t = tracks.get(i);
                int c = t.id().equals(selected) ? WHITE : t.threat() ? RED : CYAN;
                double eta = Math.max(0, (t.eta() - (now - picture.sample())) / 20);
                g.drawString(font, t.designation(), PX, y, c, false);
                g.drawString(font, String.format(Locale.ROOT, "%d:%02d", (int) eta / 60, (int) eta % 60), PX + 62, y, c, false);
                y += 10;
            }
        }
        y = MY + 110;
        g.drawString(font, Component.translatable(KEY + "silo"), PX, y, AMBER, false);
        HoloTracksPayload.Silo silo = picture == null ? null : picture.silo();
        if (silo == null) g.drawString(font, Component.translatable(KEY + "no_silo"), PX, y + 11, FAINT, false);
        else {
            SiloControllerEntity.State[] states = SiloControllerEntity.State.values();
            String state = states[Math.max(0, Math.min(states.length - 1, silo.state()))].name().toLowerCase(Locale.ROOT);
            g.drawString(font, Component.translatable("silo.create_nuclearindustry.state." + state), PX, y + 11, WHITE, false);
            if (silo.reach() > 0) g.drawString(font, Component.translatable(KEY + "reach", Math.round(silo.reach())), PX, y + 21, AMBER, false);
            if (silo.reach() > 0) g.drawString(font, Component.translatable(KEY + "cep", Math.round(silo.cep())), PX, y + 31, AMBER, false);
        }
        String[] buttons = {designate ? "designate_on" : "designate", airburst ? "airburst" : "groundburst", "auto_view"};
        for (int i = 0; i < 3; i++) {
            int by = MY + MAP - 3 * 16 + i * 16;
            boolean lit = i == 0 && designate || i == 1 && airburst || i == 2 && auto;
            g.fill(PX, by, PX + PW, by + 14, lit ? 0x6040E0FF : 0x30204850);
            g.renderOutline(PX, by, PW, 14, overButton(i, mouseX, mouseY) ? WHITE : DIM);
            g.drawCenteredString(font, Component.translatable(KEY + buttons[i]), PX + PW / 2, by + 3, lit ? WHITE : CYAN);
        }
        if (designate && overMap(mouseX, mouseY)) {
            int bx = menu.centre.getX(), bz = menu.centre.getZ();
            String at = (int) Math.floor(bx + 0.5 + dx(mouseX)) + ", " + (int) Math.floor(bz + 0.5 + dz(mouseY));
            g.drawString(font, at, mouseX - leftPos + 8, mouseY - topPos - 10, inReach(picture, dx(mouseX), dz(mouseY)) ? GREEN : RED, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (overButton(0, mx, my)) {
                designate = !designate;
                return true;
            }
            if (overButton(1, mx, my)) {
                airburst = !airburst;
                return true;
            }
            if (overButton(2, mx, my)) {
                auto = true;
                return true;
            }
            if (overMap(mx, my)) {
                if (designate) {
                    int x = (int) Math.floor(menu.centre.getX() + 0.5 + dx(mx)), z = (int) Math.floor(menu.centre.getZ() + 0.5 + dz(my));
                    PacketDistributor.sendToServer(new HoloDesignatePayload(menu.centre, x, z, airburst));
                    designate = false;
                    return true;
                }
                HoloTracksPayload picture = picture();
                if (picture != null) {
                    double best = 8 * 8;
                    for (HoloTracksPayload.Track t : picture.tracks()) {
                        double d2 = Math.pow(sx(t.x()) - mx, 2) + Math.pow(sy(t.z()) - my, 2);
                        if (d2 < best) {
                            best = d2;
                            selected = t.id();
                        }
                    }
                }
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0 && !designate) {
            auto = false;
            cx -= dx / MAP * view;
            cz -= dy / MAP * view;
            double limit = HoloView.VIEWS[HoloView.VIEWS.length - 1] / 2.0;
            cx = Mth.clamp(cx, -limit, limit);
            cz = Mth.clamp(cz, -limit, limit);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (overMap(mx, my)) {
            auto = false;
            int index = 0;
            for (int i = 0; i < HoloView.VIEWS.length; i++) if (HoloView.VIEWS[i] == view) index = i;
            int next = Mth.clamp(index + (scrollY > 0 ? -1 : 1), 0, HoloView.VIEWS.length - 1);
            double wx = dx(mx), wz = dz(my);
            view = HoloView.VIEWS[next];
            cx = wx - (mx - leftPos - MX - MAP / 2.0) / MAP * view;
            cz = wz - (my - topPos - MY - MAP / 2.0) / MAP * view;
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
