package org.papiricoh.create_nuclearindustry.missile.holo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.gui.HoloLayout;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloCommandPayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableMenu;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloView;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;
import org.papiricoh.create_nuclearindustry.missile.radar.client.ScopeDraw;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ConsoleDraw;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.client.ClientTerrain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.papiricoh.create_nuclearindustry.missile.gui.HoloLayout.*;

/**
 * The shared tactical map and tabbed device lists. Commands refer to linked controllers, never a launch trigger. The satellites tab
 * lists the table's own satellites, the foreign ones its radars have catalogued and its satellite launchers; a launch is aimed on
 * the map (an anchor, and for a low orbit a second click towards its heading) and sent with the launcher's orbit choice.
 */
public class HoloTableScreen extends AbstractContainerScreen<HoloTableMenu> {
    private static final int W = WIDTH, H = HEIGHT, MAP_SIZE = HoloLayout.MAP.w(), MX = HoloLayout.MAP.x(), MY = HoloLayout.MAP.y();
    private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/holo_table_screen.png");
    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/holo_table_widgets.png");
    private static final int CYAN = 0xFF6FF4FF, DIM = 0xFF2C7C88, FAINT = 0xFF1A4850, RED = 0xFFFF4A3A, AMBER = 0xFFFFB347, GREEN = 0xFF7DFF9A,
            WHITE = 0xFFE8FFFF;
    private static final String KEY = "holo_table.create_nuclearindustry.screen.";

    private int view = HoloView.VIEWS[0];
    private boolean auto = true, designate, airburst, dragging;
    private double cx, cz;
    private UUID selected;
    private int tab, scroll, radarRow = -1, shooterRow = -1, spacing;
    private boolean sequence;
    private final Set<BlockPos> salvo = new LinkedHashSet<>();
    /** Satellites tab: the satellite picked, or the launcher (its silo link), the orbit asked for and the launch's aim. */
    private UUID satellite;
    private int launcher = -1;
    private boolean geo;
    /** 0 no anchor, 1 anchored and waiting for a low orbit's heading, 2 ready. Anchor in world blocks, heading in degrees (-1 none). */
    private int anchorStep, anchorX, anchorZ, heading = -1;
    private static final int TAB_SAT = 4, HEADING_STEP = 15;

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
        return (float) (leftPos + MX + MAP_SIZE / 2.0 + (dx - cx) / view * MAP_SIZE);
    }

    private float sy(double dz) {
        return (float) (topPos + MY + MAP_SIZE / 2.0 + (dz - cz) / view * MAP_SIZE);
    }

    private double dx(double screenX) {
        return cx + (screenX - leftPos - MX - MAP_SIZE / 2.0) / MAP_SIZE * view;
    }

    private double dz(double screenY) {
        return cz + (screenY - topPos - MY - MAP_SIZE / 2.0) / MAP_SIZE * view;
    }

    private boolean overMap(double mx, double my) {
        return mx >= leftPos + MX && mx < leftPos + MX + MAP_SIZE && my >= topPos + MY && my < topPos + MY + MAP_SIZE;
    }

    private boolean over(LayoutRect rect, double mx, double my) {
        return rect.contains(mx - leftPos, my - topPos);
    }

    private boolean hasTexture(ResourceLocation texture) {
        return minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private void sprite(GuiGraphics g, LayoutRect source, int offsetX, int offsetY, int x, int y, int w, int h, int sourceW, int sourceH) {
        if (hasTexture(WIDGETS)) g.blit(WIDGETS, x, y, w, h, source.x() + offsetX, source.y() + offsetY, sourceW, sourceH, WIDGETS_W, WIDGETS_H);
    }

    private BlockPos controller(HoloTracksPayload.Silo silo) {
        if (silo == null || minecraft.level == null) return null;
        if (minecraft.level.getBlockEntity(menu.centre) instanceof HoloTableBlockEntity table && silo.index() >= 0 && silo.index() < table.silos().size()) {
            var link = table.silos().get(silo.index());
            if (link.dimension().equals(minecraft.level.dimension())) return link.pos();
        }
        return null;
    }

    private void command(byte action, BlockPos pos, int x, int z, int arg, UUID flight, List<BlockPos> silos) {
        PacketDistributor.sendToServer(new HoloCommandPayload(menu.centre, action, pos == null ? BlockPos.ZERO : pos, x, z, arg, flight, silos));
    }

    private HoloTracksPayload.Track selectedTrack() {
        HoloTracksPayload p = picture();
        return p == null ? null : p.tracks().stream().filter(t -> t.id().equals(selected)).findFirst().orElse(null);
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
        if (hasTexture(SCREEN)) g.blit(SCREEN, leftPos, topPos, 0, 0, W, H, W, H);
        else for (LayoutRect rect : HoloLayout.ALL) {
            g.fill(leftPos + rect.x(), topPos + rect.y(), leftPos + rect.right(), topPos + rect.bottom(), 0xFF102830);
            g.renderOutline(leftPos + rect.x(), topPos + rect.y(), rect.w(), rect.h(), DIM);
        }
        g.fill(leftPos + MX - 1, topPos + MY - 1, leftPos + MX + MAP_SIZE + 1, topPos + MY + MAP_SIZE + 1, FAINT);
        g.fill(leftPos + MX, topPos + MY, leftPos + MX + MAP_SIZE, topPos + MY + MAP_SIZE, 0xFF030C10);
        g.enableScissor(leftPos + MX, topPos + MY, leftPos + MX + MAP_SIZE, topPos + MY + MAP_SIZE);
        terrain(g);
        double now = now(partial);
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            for (int i = 1; i < 8; i++) {
                float t = leftPos + MX + MAP_SIZE * i / 8f, u = topPos + MY + MAP_SIZE * i / 8f;
                d.line(t, topPos + MY, t, topPos + MY + MAP_SIZE, 0.5f, 0x2240E0FF);
                d.line(leftPos + MX, u, leftPos + MX + MAP_SIZE, u, 0.5f, 0x2240E0FF);
            }
            d.circle(sx(0), sy(0), 2.5f, 1, WHITE, false);
            if (picture != null) draw(d, picture, now);
            if (designate && overMap(mouseX, mouseY)) {
                boolean ok = tab == TAB_SAT || inReach(picture, dx(mouseX), dz(mouseY));
                int c = tab == TAB_SAT ? CYAN : ok ? GREEN : RED;
                if (tab == TAB_SAT && anchorStep == 1 && !geo)
                    d.line(sx(anchorX - menu.centre.getX() - 0.5), sy(anchorZ - menu.centre.getZ() - 0.5), mouseX, mouseY, 0.8f, ScopeDraw.alpha(CYAN, 0.7f));
                d.line(mouseX - 6, mouseY, mouseX + 6, mouseY, 1, c);
                d.line(mouseX, mouseY - 6, mouseX, mouseY + 6, 1, c);
                d.circle(mouseX, mouseY, 4, 1, c, false);
            }
        }
        if (picture != null) satelliteIcons(g, picture, now);
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
        float scale = (float) MAP_SIZE / view;
        for (HoloTracksPayload.Radar radar : picture.radars()) {
            float x = sx(radar.dx() + 0.5), y = sy(radar.dz() + 0.5);
            int c = radar.online() ? GREEN : 0xFF607060;
            d.rect(x - 1.5f, y - 1.5f, x + 1.5f, y + 1.5f, c);
            if (radar.reach() > 0) d.circle(x, y, radar.reach() * scale, 0.7f, ScopeDraw.alpha(c, 0.45f), true);
        }
        for (HoloTracksPayload.Silo silo : picture.silos()) {
            if (!silo.loaded()) continue;
            float x = sx(silo.dx() + 0.5), y = sy(silo.dz() + 0.5);
            d.circle(x, y, 3, 1.2f, AMBER, false);
            if (silo.index() == picture.board().selectedSilo() && silo.reach() > 0) d.circle(x, y, silo.reach() * scale, 0.9f, ScopeDraw.alpha(AMBER, 0.6f), false);
            if (silo.targeted()) {
                float tx = sx(silo.tx() + 0.5), ty = sy(silo.tz() + 0.5);
                d.line(x, y, tx, ty, 0.7f, ScopeDraw.alpha(RED, 0.6f));
                d.line(tx - 4, ty - 4, tx + 4, ty + 4, 1.2f, RED);
                d.line(tx - 4, ty + 4, tx + 4, ty - 4, 1.2f, RED);
                if (silo.cep() > 0) d.circle(tx, ty, Math.max(1.5f, silo.cep() * scale), 0.7f, ScopeDraw.alpha(RED, 0.7f), true);
            }
        }
        for (var shooter : picture.shooters()) {
            if (shooter.type() != 0) continue;
            float x = sx(shooter.dx() + 0.5), y = sy(shooter.dz() + 0.5);
            d.rect(x - 3, y - 2, x + 3, y + 2, shooter.loaded() ? GREEN : DIM);
            d.circle(x, y, shooter.range() * scale, .7f, ScopeDraw.alpha(GREEN, .25f), true);
        }
        if (tab == 3) d.circle(sx(picture.defense().zoneDx()), sy(picture.defense().zoneDz()),
                (picture.defense().zoneRadius() == 0 ? 2000 : picture.defense().zoneRadius()) * scale, .8f, ScopeDraw.alpha(CYAN, .6f), true);
        double since = Math.max(0, Math.min(20, now - picture.sample()));
        for (var intercept : picture.intercepts()) {
            float x = sx(intercept.x() + intercept.vx() * since), y = sy(intercept.z() + intercept.vz() * since);
            float px = sx(intercept.px()), py = sy(intercept.pz());
            int colour = intercept.interceptor().equals(selected) ? WHITE : GREEN;
            d.disc(x, y, 2.5f, colour, colour);
            for (int i = 0; i < 16; i += 2) d.line(x + (px - x) * i / 16, y + (py - y) * i / 16,
                    x + (px - x) * (i + 1) / 16, y + (py - y) * (i + 1) / 16, .8f, colour);
            d.line(px - 3, py - 3, px + 3, py + 3, 1, AMBER);
            d.line(px - 3, py + 3, px + 3, py - 3, 1, AMBER);
        }
        float g = Math.max(1e-5f, picture.gravity());
        for (HoloTracksPayload.Track t : picture.tracks()) {
            int c = t.id().equals(selected) ? WHITE : picture.intercepts().stream().anyMatch(i -> i.target().equals(t.id())) ? AMBER : t.own() ? GREEN : t.threat() ? RED : CYAN;
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
        satellites(d, picture, now);
    }

    // ------------------------------------------------------------------ satellites on the map

    private double since(HoloTracksPayload picture, double now) {
        return Math.max(0, Math.min(20, now - picture.sample()));
    }

    /**
     * Footprints and dashed ground tracks of the satellites (all on the satellites tab, the picked one elsewhere), a ring round the
     * one picked and round any an interceptor is after, and on the satellites tab the launch being aimed.
     */
    private void satellites(ScopeDraw d, HoloTracksPayload picture, double now) {
        float scale = (float) MAP_SIZE / view;
        double since = since(picture, now);
        for (HoloTracksPayload.Sat sat : picture.orbital().satellites()) {
            int c = sat.own() ? CYAN : AMBER;
            boolean chosen = sat.id().equals(satellite);
            double[] at = SatelliteViews.offset(sat, menu.centre, now, since);
            float x = sx(at[0]), y = sy(at[1]);
            if (tab == TAB_SAT || chosen) {
                // A footprint many maps across would only show as a straight edge, if at all.
                if (SatelliteViews.hasFootprint(sat) && sat.footprint() * scale < MAP_SIZE * 8)
                    d.circle(x, y, sat.footprint() * scale, chosen ? 1 : 0.7f, ScopeDraw.alpha(c, chosen ? 0.75f : 0.35f), true);
                for (double[] seg : SatelliteViews.track(sat, menu.centre, now, since, 48))
                    d.line(sx(seg[0]), sy(seg[1]), sx(seg[2]), sy(seg[3]), chosen ? 1 : 0.7f, ScopeDraw.alpha(c, chosen ? 0.85f : 0.45f));
            }
            if (chosen) d.circle(x, y, 7, 1, WHITE, false);
            if (sat.pk() >= 0) d.circle(x, y, 9, 0.8f, ScopeDraw.alpha(RED, 0.85f), true);
        }
        if (tab != TAB_SAT || launcher < 0 || anchorStep == 0) return;
        float ax = sx(anchorX - menu.centre.getX() - 0.5), ay = sy(anchorZ - menu.centre.getZ() - 0.5);
        d.circle(ax, ay, 4, 1.2f, GREEN, false);
        d.line(ax - 6, ay, ax - 2, ay, 1, GREEN);
        d.line(ax + 2, ay, ax + 6, ay, 1, GREEN);
        d.line(ax, ay - 6, ax, ay - 2, 1, GREEN);
        d.line(ax, ay + 2, ax, ay + 6, 1, GREEN);
        if (geo || heading < 0) return;
        // The low orbit's first stretch: a dashed line from the anchor along its heading, an arrow head at its end.
        double r = Math.toRadians(heading);
        float ux = (float) Math.sin(r), uy = (float) Math.cos(r), len = MAP_SIZE * 0.45f;
        for (int i = 0; i < 16; i += 2)
            d.line(ax + ux * len * i / 16, ay + uy * len * i / 16, ax + ux * len * (i + 1) / 16, ay + uy * len * (i + 1) / 16, 1, ScopeDraw.alpha(GREEN, 0.85f));
        float ex = ax + ux * len, ey = ay + uy * len;
        d.line(ex, ey, ex - ux * 6 - uy * 4, ey - uy * 6 + ux * 4, 1.2f, GREEN);
        d.line(ex, ey, ex - ux * 6 + uy * 4, ey - uy * 6 - ux * 4, 1.2f, GREEN);
    }

    /** The ground stations and satellites as icons, over the vector layer; the picked satellite's name beside it. */
    private void satelliteIcons(GuiGraphics g, HoloTracksPayload picture, double now) {
        double since = since(picture, now);
        for (HoloTracksPayload.Station station : picture.orbital().stations())
            icon(g, SPRITE_STATIONS, station.online() ? 0 : 8, 0, sx(station.dx() + 0.5), sy(station.dz() + 0.5), station.online() ? GREEN : DIM);
        for (HoloTracksPayload.Sat sat : picture.orbital().satellites()) {
            double[] at = SatelliteViews.offset(sat, menu.centre, now, since);
            float x = sx(at[0]), y = sy(at[1]);
            icon(g, SPRITE_SAT_KINDS, kindIndex(sat.kind()) * 8, sat.own() ? 0 : 8, x, y, sat.own() ? CYAN : AMBER);
            if (sat.id().equals(satellite)) small(g, Component.literal(sat.designation()), Math.round(x) + 9, Math.round(y) - 3, 48, WHITE);
        }
    }

    /** An 8 × 8 icon from the widget sheet centred on ({@code x, y}); a dot without the sheet. */
    private void icon(GuiGraphics g, LayoutRect sheet, int u, int v, float x, float y, int fallback) {
        int ix = Math.round(x) - 4, iy = Math.round(y) - 4;
        if (hasTexture(WIDGETS)) sprite(g, sheet, u, v, ix, iy, 8, 8, 8, 8);
        else g.fill(ix + 2, iy + 2, ix + 6, iy + 6, fallback);
    }

    private static int kindIndex(byte kind) {
        return kind == 1 ? 1 : 0;
    }

    private boolean inReach(HoloTracksPayload picture, double dx, double dz) {
        if (picture == null || picture.silo() == null || picture.silo().reach() <= 0) return false;
        HoloTracksPayload.Silo s = picture.silo();
        return Math.hypot(dx - s.dx(), dz - s.dz()) <= s.reach();
    }

    private Component text(String suffix) { return Component.translatable(KEY + suffix); }

    private Component state(HoloTracksPayload.Silo silo) {
        if (!silo.loaded()) return text("unloaded");
        var values = SiloControllerEntity.State.values();
        return Component.translatable("silo.create_nuclearindustry.state." + values[Mth.clamp(silo.state(), 0, values.length - 1)].name().toLowerCase(Locale.ROOT));
    }

    private Component error(HoloTracksPayload.Silo silo) {
        return !silo.loaded() ? text("unloaded") : silo.error().isEmpty() ? state(silo)
                : Component.translatable("silo.create_nuclearindustry.error." + silo.error(), "?", "?", "?");
    }

    private void fit(GuiGraphics g, Component text, int x, int y, int width, int colour) {
        float scale = Math.min(1, width / (float) Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    private void small(GuiGraphics g, Component text, int x, int y, int width, int colour) {
        ConsoleDraw.line(g, font, text, x, y, width, colour, ConsoleDraw.DENSE);
    }

    private Component rowInfo(HoloTracksPayload.Silo silo) {
        if (!silo.loaded()) return Component.literal("—");
        if (!silo.error().isEmpty()) return error(silo);
        if (silo.state() == SiloControllerEntity.State.COUNTDOWN.ordinal())
            return Component.translatable(KEY + "row.countdown", String.format(Locale.ROOT, "%.1f", Math.max(0, silo.countdown()) / 20.0));
        if (silo.targeted() && (silo.state() == SiloControllerEntity.State.READY.ordinal() || silo.state() == SiloControllerEntity.State.ARMED.ordinal())) {
            double distance = Math.hypot(silo.tx() - silo.dx(), silo.tz() - silo.dz());
            String metres = distance >= 1000 ? String.format(Locale.ROOT, "%.1fk", distance / 1000) : Long.toString(Math.round(distance));
            return Component.translatable(KEY + "row.flight", metres, Math.round(silo.flightSeconds()));
        }
        return Component.literal("—");
    }

    private void button(GuiGraphics g, LayoutRect rect, Component label, boolean active, boolean enabled, boolean danger, int mx, int my) {
        boolean hover = over(rect, mx, my);
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), enabled ? active ? 0xFF24606A : 0xFF15333C : 0xFF102029);
        sprite(g, SPRITE_BUTTONS, 0, (!enabled ? 2 : danger ? 3 : hover ? 1 : 0) * 16,
                rect.x(), rect.y(), rect.w(), rect.h(), 40, 15);
        g.renderOutline(rect.x(), rect.y(), rect.w(), rect.h(), !enabled ? FAINT : hover ? WHITE : DIM);
        float scale = Math.min(1, (rect.w() - 6) / (float) Math.max(1, font.width(label)));
        int labelWidth = (int) Math.ceil(font.width(label) * scale);
        fit(g, label, rect.x() + (rect.w() - labelWidth) / 2, rect.y() + 4, rect.w() - 6, enabled ? active ? WHITE : CYAN : DIM);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        HoloTracksPayload p = picture();
        if (selected == null && p != null) selected = p.board().selected();
        ConsoleDraw.line(g, font, title, TITLE.x() + 2, TITLE.y() + 2, 290, CYAN, 1);
        String scale = view >= 1000 ? (view / 1000) + " km" : view + " m";
        small(g, Component.literal(scale), TITLE.right() - SCALE_INSET + 14, TITLE.y() + 3, SCALE_W - 16, CYAN);
        HoloTracksPayload.Silo silo = p == null ? null : p.silo();
        boolean loaded = silo != null && silo.loaded() && controller(silo) != null;
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.centre) instanceof HoloTableBlockEntity table)
            salvo.removeIf(pos -> !table.silos().contains(net.minecraft.core.GlobalPos.of(minecraft.level.dimension(), pos)));
        button(g, ZOOM_OUT, Component.literal("−"), false, true, false, mouseX, mouseY);
        button(g, ZOOM_IN, Component.literal("+"), false, true, false, mouseX, mouseY);
        button(g, AUTO, text("auto"), auto, true, false, mouseX, mouseY);
        if (tab == TAB_SAT)
            button(g, DESIGNATE, text(designate ? "sat.anchor_on" : "sat.anchor_button"), designate, launcherRow(p) != null, false, mouseX, mouseY);
        else button(g, DESIGNATE, text(designate ? "designate_on" : "designate"), designate, tab != 3 && loaded, false, mouseX, mouseY);
        button(g, AIRBURST, text(airburst ? "airburst" : "groundburst"), airburst, true, false, mouseX, mouseY);
        LayoutRect[] tabs = {TAB_SILOS, TAB_RADARS, TAB_TRACKS, TAB_DEFENSE, TAB_SATELLITES};
        String[] names = {"silos", "radars", "tracks", "defense", "satellites"};
        for (int i = 0; i < tabs.length; i++) {
            LayoutRect rect = tabs[i];
            g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), tab == i ? 0xFF24606A : 0xFF15333C);
            sprite(g, SPRITE_TABS, 0, tab == i ? 16 : 0, rect.x(), rect.y(), rect.w(), rect.h(), 41, 14);
            fit(g, text(names[i]), rect.x() + 3, rect.y() + 3, rect.w() - 6, CYAN);
        }
        network(g, p);
        if (tab == 3) { renderDefense(g, p, mouseX, mouseY); return; }
        if (tab == TAB_SAT) { renderSatellites(g, p, mouseX, mouseY); return; }
        if (tab == 0) {
            int[] columns = {COL_NAME, COL_STATE, COL_INFO, COL_DELAY};
            String[] labels = {"silo", "state", "info", "delay"};
            for (int i = 0; i < columns.length; i++)
                small(g, text("column." + labels[i]), LIST.x() + columns[i], LIST.y() + 2,
                        (i + 1 < columns.length ? columns[i + 1] : COL_UNLINK) - columns[i] - 2, DIM);
        } else {
            small(g, text("column." + (tab == 1 ? "radar" : "track")), LIST.x() + COL_NAME, LIST.y() + 2, COL_INFO - COL_NAME - 2, DIM);
            small(g, text("column." + (tab == 1 ? "network" : "eta")), LIST.x() + COL_INFO, LIST.y() + 2, COL_UNLINK - COL_INFO - 2, DIM);
        }
        if (p != null) {
            int count = tab == 0 ? p.silos().size() : tab == 1 ? p.radars().size() : p.tracks().size();
            scroll = Mth.clamp(scroll, 0, Math.max(0, count - LIST_ROWS));
            for (int row = 0; row < Math.min(LIST_ROWS, count - scroll); row++) {
                int i = row + scroll, x = LIST.x(), y = LIST.y() + LIST_HEADER + row * LIST_ROW;
                boolean chosen = tab == 0 ? p.silos().get(i).index() == p.board().selectedSilo()
                        : tab == 1 ? i == radarRow : p.tracks().get(i).id().equals(selected);
                if (chosen) g.fill(x, y, x + LIST.w(), y + LIST_ROW, 0xFF234A52);
                if (tab == 0) {
                    HoloTracksPayload.Silo s = p.silos().get(i);
                    BlockPos at = controller(s);
                    boolean checked = at != null && salvo.contains(at);
                    g.renderOutline(x + COL_CHECK, y, 8, 8, checked ? GREEN : DIM);
                    if (checked) g.fill(x + COL_CHECK + 2, y + 2, x + COL_CHECK + 6, y + 6, GREEN);
                    sprite(g, SPRITE_CHECKS, checked ? 8 : 0, 0, x + COL_CHECK, y, 8, 8, 8, 8);
                    small(g, Component.literal("S" + (s.index() + 1)), x + COL_NAME, y + 1, COL_STATE - COL_NAME - 2, AMBER);
                    int lamp = s.loaded() ? Mth.clamp(s.state(), 0, 5) : 0;
                    g.fill(x + COL_STATE, y, x + COL_STATE + 7, y + 7, lamp >= 3 ? GREEN : AMBER);
                    sprite(g, SPRITE_LAMPS, lamp * 8, 0, x + COL_STATE, y, 7, 7, 7, 7);
                    small(g, state(s), x + COL_STATE + 9, y + 1, COL_INFO - COL_STATE - 11, WHITE);
                    small(g, rowInfo(s), x + COL_INFO, y + 1, COL_DELAY - COL_INFO - 2, s.error().isEmpty() ? CYAN : RED);
                    small(g, Component.literal(String.format(Locale.ROOT, "%.1fs", s.delay() / 20.0)), x + COL_DELAY, y + 1, COL_UNLINK - COL_DELAY - 2, DIM);
                } else if (tab == 1) {
                    var r = p.radars().get(i);
                    small(g, Component.literal("R" + (i + 1)), x + COL_NAME, y + 1, COL_INFO - COL_NAME - 2, r.online() ? GREEN : RED);
                    small(g, Component.literal(r.code()), x + COL_INFO, y + 1, COL_UNLINK - COL_INFO - 2, CYAN);
                } else {
                    var t = p.tracks().get(i);
                    small(g, Component.literal(t.designation()), x + COL_NAME, y + 1, COL_INFO - COL_NAME - 2, t.own() ? GREEN : t.threat() ? RED : CYAN);
                    small(g, Component.literal(Math.max(0, t.eta() / 20) + " s"), x + COL_INFO, y + 1, COL_UNLINK - COL_INFO - 2, CYAN);
                }
                if (tab < 2) {
                    g.drawString(font, "×", x + COL_UNLINK, y, RED, false);
                    boolean hover = mouseX - leftPos >= x + COL_UNLINK && mouseY - topPos >= y && mouseY - topPos < y + LIST_ROW;
                    sprite(g, SPRITE_UNLINK, hover ? 8 : 0, 0, x + COL_UNLINK, y, 8, 8, 8, 8);
                }
            }
            if (tab == 0 && silo != null) {
                Component target = silo.targeted() ? Component.translatable(KEY + "target", menu.centre.getX() + silo.tx(), menu.centre.getZ() + silo.tz()) : text("target.none");
                small(g, Component.translatable(KEY + "selected_target", state(silo), target), DETAIL.x() + 2, DETAIL.y() + 3, DETAIL.w() - 4, silo.error().isEmpty() ? WHITE : RED);
                small(g, Component.translatable(KEY + "flight_delay", String.format(Locale.ROOT, "%.1f", silo.flightSeconds()), String.format(Locale.ROOT, "%.1f", silo.delay() / 20.0)),
                        DETAIL.x() + 2, DETAIL.y() + 13, DETAIL.w() - 4, DIM);
            } else if (tab == 1 && radarRow >= 0 && radarRow < p.radars().size()) {
                var r = p.radars().get(radarRow);
                small(g, Component.literal((menu.centre.getX() + r.dx()) + ", " + r.dy() + ", " + (menu.centre.getZ() + r.dz())), DETAIL.x() + 2, DETAIL.y() + 3, 166, WHITE);
                small(g, text(r.online() ? "online" : "unloaded"), DETAIL.x() + 2, DETAIL.y() + 13, 166, CYAN);
            } else if (tab == 2 && selectedTrack() != null) {
                small(g, Component.translatable(KEY + "track_detail", selectedTrack().designation(), Math.round(selectedTrack().y())), DETAIL.x() + 2, DETAIL.y() + 3, 166, WHITE);
                small(g, text(selectedTrack().own() ? "own" : "foreign"), DETAIL.x() + 2, DETAIL.y() + 13, 166, CYAN);
            }
        }
        button(g, SALVO_MODE, text(sequence ? "sequence" : "time_on_target"), sequence, true, false, mouseX, mouseY);
        button(g, SPACING_DOWN, Component.literal("−"), false, sequence, false, mouseX, mouseY);
        button(g, SPACING, Component.literal(spacing + " s"), false, sequence, false, mouseX, mouseY);
        button(g, SPACING_UP, Component.literal("+"), false, sequence, false, mouseX, mouseY);
        button(g, ARM, text("arm"), false, loaded, false, mouseX, mouseY);
        button(g, DISARM, text("disarm"), false, loaded, false, mouseX, mouseY);
        button(g, SALVO, text("salvo"), false, !salvo.isEmpty(), false, mouseX, mouseY);
        button(g, ABORT, text("abort"), false, selectedTrack() != null && selectedTrack().own(), true, mouseX, mouseY);
    }

    private void renderDefense(GuiGraphics g, HoloTracksPayload p, int mx, int my) {
        if (p == null) return;
        int[] columns = {COL_NAME, COL_STATE, COL_INFO, COL_DELAY};
        String[] labels = {"shooter", "state", "info", "delay"};
        for (int i = 0; i < columns.length; i++)
            small(g, text("column." + labels[i]), LIST.x() + columns[i], LIST.y() + 2,
                    (i + 1 < columns.length ? columns[i + 1] : COL_UNLINK) - columns[i] - 2, DIM);
        scroll = Mth.clamp(scroll, 0, Math.max(0, p.shooters().size() - LIST_ROWS));
        if (shooterRow >= p.shooters().size()) shooterRow = -1;
        for (int row = 0; row < Math.min(LIST_ROWS, p.shooters().size() - scroll); row++) {
            int i = row + scroll, x = LIST.x(), y = LIST.y() + LIST_HEADER + row * LIST_ROW;
            var shooter = p.shooters().get(i);
            boolean battery = shooter.type() == 0;
            if (i == shooterRow) g.fill(x, y, x + LIST.w(), y + LIST_ROW, 0xFF234A52);
            small(g, Component.literal((battery ? "BAT-" : "SIL-") + (shooter.link() + 1)), x + COL_NAME, y + 1, COL_STATE - COL_NAME - 2, AMBER);
            int colour = !shooter.loaded() ? DIM : shooter.ready() > 0 ? GREEN : AMBER;
            g.fill(x + COL_STATE, y, x + COL_STATE + 7, y + 7, colour);
            Component state = text("unloaded");
            if (shooter.loaded()) {
                String name = battery
                        ? org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity.Status.values()[Mth.clamp(shooter.state(), 0, 7)].name()
                        : SiloControllerEntity.State.values()[Mth.clamp(shooter.state(), 0, SiloControllerEntity.State.values().length - 1)].name();
                state = Component.translatable((battery ? KEY + "defense.status." : "silo.create_nuclearindustry.state.") + name.toLowerCase(Locale.ROOT));
            }
            small(g, state, x + COL_STATE + 9, y + 1, COL_INFO - COL_STATE - 11, colour);
            small(g, Component.literal(battery ? shooter.ready() + "/" + shooter.capacity() : "KV"), x + COL_INFO, y + 1, COL_DELAY - COL_INFO - 2, CYAN);
            small(g, Component.literal(shooter.auto() ? "AUTO" : ""), x + COL_DELAY, y + 1, COL_UNLINK - COL_DELAY - 2, CYAN);
            if (battery) g.drawString(font, "×", x + COL_UNLINK, y, RED, false);
        }
        var track = selectedTrack();
        var intercept = ClientHolo.bestIntercept(p, selected);
        Component detail = track == null ? text("defense.pin") : intercept == null ? Component.literal(track.designation())
                : Component.translatable(KEY + "defense.estimate", track.designation(), Math.round(intercept.pk() * 100), Math.max(0, intercept.tti()) / 20);
        small(g, detail, DETAIL.x() + 2, DETAIL.y() + 3, DETAIL.w() - 4, WHITE);
        small(g, text(intercept == null ? "defense.no_estimate" : "defense.in_flight"), DETAIL.x() + 2, DETAIL.y() + 13, DETAIL.w() - 4, DIM);
        button(g, SALVO_MODE, text(p.defense().doctrine() == 0 ? "defense.shoot_look" : "defense.salvo_two"), false, true, false, mx, my);
        button(g, SPACING_DOWN, Component.literal("−"), false, p.defense().zoneRadius() > 0, false, mx, my);
        button(g, SPACING, p.defense().zoneRadius() == 0 ? text("defense.default") : Component.literal(p.defense().zoneRadius() + " m"), false, true, false, mx, my);
        button(g, SPACING_UP, Component.literal("+"), false, p.defense().zoneRadius() < org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity.MAX_ZONE, false, mx, my);
        boolean ready = shooterRow < 0 ? p.shooters().stream().anyMatch(sh -> sh.loaded() && sh.ready() > 0)
                : p.shooters().get(shooterRow).loaded() && p.shooters().get(shooterRow).ready() > 0;
        button(g, ARM, text("defense.engage"), false, track != null && !track.own() && ready, false, mx, my);
        button(g, DISARM, text("defense.auto"), p.defense().auto(), true, false, mx, my);
        button(g, SALVO, text("defense.cease"), false, true, false, mx, my);
        button(g, ABORT, text("abort"), false, p.intercepts().stream().anyMatch(i -> i.interceptor().equals(selected)), true, mx, my);
    }

    private boolean defenseClick(double mx, double my, HoloTracksPayload p) {
        if (p == null) return true;
        if (over(LIST, mx, my)) {
            int y = (int) (my - topPos - LIST.y() - LIST_HEADER);
            int row = y / LIST_ROW + scroll;
            if (y >= 0 && row < p.shooters().size()) {
                var shooter = p.shooters().get(row);
                if (mx - leftPos - LIST.x() >= COL_UNLINK && shooter.type() == 0
                        && minecraft.level.getBlockEntity(menu.centre) instanceof HoloTableBlockEntity table
                        && shooter.link() < table.batteries().size())
                    command(HoloCommandPayload.UNLINK_BATTERY, table.batteries().get(shooter.link()).pos(), 0, 0, 0, null, List.of());
                else shooterRow = shooterRow == row ? -1 : row;
            }
            return true;
        }
        if (over(SALVO_MODE, mx, my)) {
            command(HoloCommandPayload.SET_DOCTRINE, null, 0, 0, (p.defense().doctrine() + 1) % 2, null, List.of()); return true;
        }
        if (over(SPACING_DOWN, mx, my) || over(SPACING, mx, my) || over(SPACING_UP, mx, my)) {
            int radius = over(SPACING, mx, my) ? 0 : p.defense().zoneRadius() + (over(SPACING_UP, mx, my) ? 250 : -250);
            command(HoloCommandPayload.SET_ZONE, null, menu.centre.getX() + p.defense().zoneDx(), menu.centre.getZ() + p.defense().zoneDz(), radius, null, List.of()); return true;
        }
        if (over(ARM, mx, my)) {
            var track = selectedTrack();
            if (track != null && !track.own() && p.shooters().stream().anyMatch(sh -> sh.loaded() && sh.ready() > 0)) {
                int choice = -1;
                if (shooterRow >= 0 && shooterRow < p.shooters().size()) {
                    var shooter = p.shooters().get(shooterRow);
                    if (!shooter.loaded() || shooter.ready() <= 0) return true;
                    choice = shooter.type() == 0 ? shooter.link() : 100 + shooter.link();
                }
                command(HoloCommandPayload.ENGAGE, null, 0, 0, choice, track.id(), List.of());
            }
            return true;
        }
        if (over(DISARM, mx, my)) { command(HoloCommandPayload.SET_AUTO, null, 0, 0, p.defense().auto() ? 0 : 1, null, List.of()); return true; }
        if (over(SALVO, mx, my)) { command(HoloCommandPayload.CEASE, null, 0, 0, 0, null, List.of()); return true; }
        if (over(ABORT, mx, my)) {
            if (p.intercepts().stream().anyMatch(i -> i.interceptor().equals(selected))) command(HoloCommandPayload.ABORT, null, 0, 0, 0, selected, List.of());
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ satellites tab

    /** The network panel, on every tab: the uplink lamp, the network's code and how many satellites it has up. */
    private void network(GuiGraphics g, HoloTracksPayload p) {
        HoloTracksPayload.Orbital o = p == null ? HoloTracksPayload.Orbital.NONE : p.orbital();
        boolean up = uplink(p), any = !o.stations().isEmpty();
        int x = NETWORK.x(), y = NETWORK.y();
        int lamp = up ? 2 : any ? 0 : 5;
        if (!hasTexture(WIDGETS)) g.fill(x + 3, y + 3, x + 10, y + 10, up ? GREEN : any ? RED : DIM);
        sprite(g, SPRITE_LAMPS, lamp * 8, 0, x + 3, y + 3, 7, 7, 7, 7);
        small(g, text("sat.uplink"), x + 12, y + 4, 18, up ? GREEN : DIM);
        Component code = Component.literal(o.code().isEmpty() ? "----" : o.code());
        g.drawString(font, code, x + (NETWORK.w() - font.width(code)) / 2, y + 15, o.code().isEmpty() ? DIM : CYAN, false);
        long own = o.satellites().stream().filter(HoloTracksPayload.Sat::own).count();
        small(g, Component.translatable(KEY + "sat.count", own), x + 3, y + 24, NETWORK.w() - 6, DIM);
    }

    private static boolean uplink(HoloTracksPayload p) {
        return p != null && !p.orbital().code().isEmpty() && p.orbital().stations().stream().anyMatch(HoloTracksPayload.Station::online);
    }

    /** Where the header shows the {@code i}th ground station, in screen-texture pixels; its unlink cross 27 pixels on. */
    private static int stationX(int i) {
        return LIST.x() + 80 + i * 46;
    }

    private static final int ROW_OWN = 0, ROW_FOREIGN = 1, ROW_LAUNCHERS = 2, ROW_SAT = 3, ROW_LAUNCHER = 4;

    /** The list's rows: {@code {kind, index}}, a section heading (index its count) or a satellite or launcher (its index). */
    private static List<int[]> satRows(HoloTracksPayload p) {
        List<int[]> rows = new ArrayList<>();
        List<HoloTracksPayload.Sat> sats = p.orbital().satellites();
        for (boolean own : new boolean[]{true, false}) {
            int start = rows.size();
            rows.add(null);
            for (int i = 0; i < sats.size(); i++) if (sats.get(i).own() == own) rows.add(new int[]{ROW_SAT, i});
            rows.set(start, new int[]{own ? ROW_OWN : ROW_FOREIGN, rows.size() - start - 1});
        }
        rows.add(new int[]{ROW_LAUNCHERS, p.orbital().launchers().size()});
        for (int i = 0; i < p.orbital().launchers().size(); i++) rows.add(new int[]{ROW_LAUNCHER, i});
        return rows;
    }

    private HoloTracksPayload.SatLauncher launcherRow(HoloTracksPayload p) {
        if (p == null || launcher < 0) return null;
        for (HoloTracksPayload.SatLauncher l : p.orbital().launchers()) if (l.link() == launcher) return l;
        return null;
    }

    private HoloTracksPayload.Sat selectedSat(HoloTracksPayload p) {
        if (p == null || satellite == null) return null;
        for (HoloTracksPayload.Sat sat : p.orbital().satellites()) if (sat.id().equals(satellite)) return sat;
        return null;
    }

    private BlockPos controllerOf(HoloTracksPayload p, HoloTracksPayload.SatLauncher l) {
        if (p == null || l == null || l.link() < 0 || l.link() >= p.silos().size()) return null;
        return controller(p.silos().get(l.link()));
    }

    private boolean canLaunch(HoloTracksPayload p, HoloTracksPayload.SatLauncher l) {
        return l != null && uplink(p) && l.kind() >= 0 && reaches(l) && l.state() == SiloControllerEntity.State.READY.ordinal()
                && anchorStep == 2 && (geo || heading >= 0) && controllerOf(p, l) != null;
    }

    /** Whether the launcher's rocket reaches the orbit asked for. */
    private boolean reaches(HoloTracksPayload.SatLauncher l) {
        return l.reach() >= (geo ? 1 : 0);
    }

    private static boolean canEngage(HoloTracksPayload p, HoloTracksPayload.Sat target) {
        return target != null && !target.own() && target.orbit() != 1 && p.shooters().stream().anyMatch(sh -> sh.loaded() && sh.ready() > 0);
    }

    private void resetAnchor() {
        anchorStep = 0;
        heading = -1;
        designate = false;
    }

    private Component kindShort(byte kind) {
        return text("sat.kind_short." + (kind == 1 ? "comms" : "early_warning"));
    }

    private Component kindName(byte kind) {
        return Component.translatable("satellite.create_nuclearindustry.kind." + (kind == 1 ? "comms" : "early_warning"));
    }

    private Component orbitShort(byte orbit) {
        return text("sat.orbit_short." + (orbit == 1 ? "geo" : "leo"));
    }

    private Component orbitName(byte orbit) {
        return Component.translatable("satellite.create_nuclearindustry.orbit." + (orbit == 1 ? "geo" : "leo"));
    }

    private static Component launcherState(byte state) {
        var values = SiloControllerEntity.State.values();
        return Component.translatable("silo.create_nuclearindustry.state." + values[Mth.clamp(state, 0, values.length - 1)].name().toLowerCase(Locale.ROOT));
    }

    private Component pass(HoloTracksPayload.Sat sat) {
        return sat.nextPass() == 0 ? text("sat.over_now") : sat.nextPass() > 0 ? Component.translatable(KEY + "sat.next_pass", SatelliteViews.clock(sat.nextPass()))
                : text("sat.no_pass");
    }

    private void renderSatellites(GuiGraphics g, HoloTracksPayload p, int mx, int my) {
        if (p == null) return;
        HoloTracksPayload.Orbital o = p.orbital();
        // The header band: the network and its ground stations.
        if (o.code().isEmpty()) small(g, text("sat.no_station"), LIST.x() + 3, LIST.y() + 2, 74, AMBER);
        else small(g, Component.translatable(KEY + "sat.network", o.code()), LIST.x() + 3, LIST.y() + 2, 74, CYAN);
        for (int i = 0; i < o.stations().size(); i++) {
            HoloTracksPayload.Station st = o.stations().get(i);
            int x = stationX(i);
            if (!hasTexture(WIDGETS)) g.fill(x + 2, LIST.y() + 3, x + 6, LIST.y() + 7, st.online() ? GREEN : RED);
            sprite(g, SPRITE_STATIONS, st.online() ? 0 : 8, 0, x, LIST.y() + 1, 8, 8, 8, 8);
            small(g, Component.translatable(KEY + "sat.station", i + 1), x + 10, LIST.y() + 2, 16, st.online() ? GREEN : RED);
            boolean hover = mx - leftPos >= x + 27 && mx - leftPos < x + 35 && my - topPos >= LIST.y() && my - topPos < LIST.y() + LIST_HEADER;
            g.drawString(font, "×", x + 27, LIST.y() + 1, RED, false);
            sprite(g, SPRITE_UNLINK, hover ? 8 : 0, 0, x + 27, LIST.y() + 1, 8, 8, 8, 8);
        }
        // The rows: own satellites, foreign ones, launchers, each under its heading.
        List<int[]> rows = satRows(p);
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - LIST_ROWS));
        if (satellite != null && selectedSat(p) == null) satellite = null;
        if (launcher >= 0 && launcherRow(p) == null) { launcher = -1; resetAnchor(); }
        for (int row = 0; row < Math.min(LIST_ROWS, rows.size() - scroll); row++) {
            int[] r = rows.get(row + scroll);
            int x = LIST.x(), y = LIST.y() + LIST_HEADER + row * LIST_ROW;
            if (r[0] <= ROW_LAUNCHERS) {
                g.fill(x + 1, y, x + LIST.w() - 1, y + LIST_ROW, 0x5520505A);
                g.fill(x + 1, y + LIST_ROW - 1, x + LIST.w() - 1, y + LIST_ROW, r[0] == ROW_FOREIGN ? ScopeDraw.alpha(AMBER, 0.5f) : ScopeDraw.alpha(CYAN, 0.5f));
                String name = r[0] == ROW_OWN ? "sat.own" : r[0] == ROW_FOREIGN ? "sat.foreign" : "sat.launchers";
                Component label = Component.translatable(KEY + name, r[1]);
                if (r[1] == 0) label = label.copy().append(" · ").append(text("sat.none"));
                small(g, label, x + COL_CHECK + 1, y + 1, LIST.w() - 6, r[0] == ROW_FOREIGN ? AMBER : r[0] == ROW_OWN ? CYAN : WHITE);
            } else if (r[0] == ROW_SAT) {
                HoloTracksPayload.Sat sat = o.satellites().get(r[1]);
                boolean chosen = sat.id().equals(satellite);
                int c = sat.own() ? CYAN : AMBER;
                if (chosen) g.fill(x, y, x + LIST.w(), y + LIST_ROW, 0xFF234A52);
                sprite(g, SPRITE_SAT_KINDS, kindIndex(sat.kind()) * 8, sat.own() ? 0 : 8, x + COL_CHECK, y, 8, 8, 8, 8);
                small(g, Component.literal(sat.designation()), x + COL_NAME, y + 1, COL_STATE - COL_NAME - 2, chosen ? WHITE : c);
                sprite(g, SPRITE_ORBITS, sat.orbit() == 1 ? 8 : 0, 0, x + COL_STATE, y, 8, 8, 8, 8);
                small(g, Component.translatable(KEY + "sat.altitude", orbitShort(sat.orbit()), SatelliteViews.altitude(sat)), x + COL_STATE + 10, y + 1,
                        COL_INFO - COL_STATE - 12, WHITE);
                small(g, sat.life() < 0 ? text("sat.forever") : Component.literal(SatelliteViews.clock(sat.life())), x + COL_INFO, y + 1, COL_DELAY - COL_INFO - 2,
                        sat.life() >= 0 && sat.life() < 20 * 60 ? AMBER : DIM);
                Component pass;
                int colour;
                if (sat.pk() >= 0) { pass = Component.translatable(KEY + "defense.pk", Math.round(sat.pk() * 100)); colour = RED; }
                else if (sat.nextPass() == 0) { pass = text("sat.over"); colour = GREEN; }
                else if (sat.nextPass() > 0) { pass = Component.literal(SatelliteViews.clock(sat.nextPass())); colour = CYAN; }
                else { pass = Component.literal("—"); colour = DIM; }
                small(g, pass, x + COL_DELAY, y + 1, LIST.w() - COL_DELAY - 3, colour);
            } else {
                HoloTracksPayload.SatLauncher l = o.launchers().get(r[1]);
                boolean chosen = l.link() == launcher;
                if (chosen) g.fill(x, y, x + LIST.w(), y + LIST_ROW, 0xFF234A52);
                sprite(g, SPRITE_FAIRINGS, l.kind() >= 0 ? 0 : 8, 0, x + COL_CHECK, y, 8, 8, 8, 8);
                small(g, Component.translatable(KEY + "sat.launcher", l.link() + 1), x + COL_NAME, y + 1, COL_STATE - COL_NAME - 2, chosen ? WHITE : AMBER);
                int lamp = Mth.clamp(l.state(), 0, 5);
                g.fill(x + COL_STATE, y, x + COL_STATE + 7, y + 7, lamp == 2 ? GREEN : AMBER);
                sprite(g, SPRITE_LAMPS, lamp * 8, 0, x + COL_STATE, y, 7, 7, 7, 7);
                small(g, launcherState(l.state()), x + COL_STATE + 9, y + 1, COL_INFO - COL_STATE - 11, WHITE);
                small(g, l.kind() >= 0 ? kindShort(l.kind()) : text("sat.empty"), x + COL_INFO, y + 1, COL_DELAY - COL_INFO - 2, l.kind() >= 0 ? CYAN : RED);
                small(g, l.reach() >= 0 ? orbitShort(l.reach()) : Component.literal("—"), x + COL_DELAY, y + 1, LIST.w() - COL_DELAY - 3, l.reach() >= 0 ? GREEN : RED);
            }
        }
        // The detail strip.
        HoloTracksPayload.Sat sat = selectedSat(p);
        HoloTracksPayload.SatLauncher l = launcherRow(p);
        Component first, second;
        int firstColour = WHITE, secondColour = DIM;
        if (sat != null) {
            first = Component.translatable(KEY + "sat.detail", sat.designation(), kindName(sat.kind()), orbitName(sat.orbit()), SatelliteViews.altitude(sat));
            firstColour = sat.own() ? CYAN : AMBER;
            second = pass(sat).copy().append(" · ").append(sat.life() < 0 ? text("sat.stays")
                    : Component.translatable(KEY + "sat.decays", SatelliteViews.clock(sat.life())));
            if (sat.pk() >= 0) second = second.copy().append(" · ").append(Component.translatable(KEY + "sat.pk", Math.round(sat.pk() * 100)));
            secondColour = sat.nextPass() == 0 ? GREEN : DIM;
        } else if (l != null) {
            first = Component.translatable(KEY + "sat.launcher_detail", l.link() + 1, l.kind() >= 0 ? kindName(l.kind()) : text("sat.empty"),
                    l.reach() >= 0 ? orbitName(l.reach()) : text("sat.reach_none"), launcherState(l.state()));
            firstColour = l.kind() >= 0 && l.reach() >= 0 ? WHITE : RED;
            if (!reaches(l)) { second = text("sat.unreachable"); secondColour = RED; }
            else if (anchorStep == 0) second = text("sat.anchor_hint");
            else if (geo) { second = Component.translatable(KEY + "sat.parked", anchorX, anchorZ); secondColour = GREEN; }
            else if (anchorStep == 1 || heading < 0) second = Component.translatable(KEY + "sat.heading_hint", anchorX, anchorZ);
            else { second = Component.translatable(KEY + "sat.heading", anchorX, anchorZ, heading); secondColour = GREEN; }
        } else {
            first = text("sat.pick");
            second = text("sat.pick.hint");
        }
        small(g, first, DETAIL.x() + 4, DETAIL.y() + 3, DETAIL.w() - 8, firstColour);
        small(g, second, DETAIL.x() + 4, DETAIL.y() + 13, DETAIL.w() - 8, secondColour);
        // The keys: orbit and heading for a launch, launch, engage, clear the aim, deorbit.
        boolean steer = l != null && !geo && anchorStep > 0;
        button(g, SALVO_MODE, text(geo ? "sat.orbit_geo" : "sat.orbit_leo"), geo, l != null, false, mx, my);
        button(g, SPACING_DOWN, Component.literal("−"), false, steer, false, mx, my);
        button(g, SPACING, heading < 0 ? text("sat.heading_none") : Component.translatable(KEY + "sat.heading_value", heading), false, steer, false, mx, my);
        button(g, SPACING_UP, Component.literal("+"), false, steer, false, mx, my);
        button(g, ARM, text("sat.launch"), false, canLaunch(p, l), false, mx, my);
        button(g, DISARM, text("sat.engage"), false, canEngage(p, sat), false, mx, my);
        button(g, SALVO, text("sat.clear"), false, anchorStep > 0, false, mx, my);
        button(g, ABORT, text("sat.deorbit"), false, sat != null && sat.own(), true, mx, my);
    }

    private boolean satelliteClick(double mx, double my, HoloTracksPayload p) {
        if (p == null) return over(LIST, mx, my);
        HoloTracksPayload.Orbital o = p.orbital();
        if (over(LIST, mx, my)) {
            int ly = (int) (my - topPos - LIST.y());
            double lx = mx - leftPos;
            if (ly < LIST_HEADER) {
                for (int i = 0; i < o.stations().size(); i++) {
                    if (lx < stationX(i) + 27 || lx >= stationX(i) + 35) continue;
                    HoloTracksPayload.Station st = o.stations().get(i);
                    command(HoloCommandPayload.UNLINK_STATION, new BlockPos(menu.centre.getX() + st.dx(), st.dy(), menu.centre.getZ() + st.dz()), 0, 0, 0, null, List.of());
                }
                return true;
            }
            List<int[]> rows = satRows(p);
            int row = (ly - LIST_HEADER) / LIST_ROW + scroll;
            if (row < rows.size()) {
                int[] r = rows.get(row);
                if (r[0] == ROW_SAT) {
                    satellite = o.satellites().get(r[1]).id();
                    launcher = -1;
                    designate = false;
                } else if (r[0] == ROW_LAUNCHER) {
                    int link = o.launchers().get(r[1]).link();
                    if (link != launcher) {
                        launcher = link;
                        resetAnchor();
                        geo = o.launchers().get(r[1]).reach() == 1 && geo;
                    }
                    satellite = null;
                }
            }
            return true;
        }
        HoloTracksPayload.SatLauncher l = launcherRow(p);
        HoloTracksPayload.Sat sat = selectedSat(p);
        if (over(SALVO_MODE, mx, my)) {
            if (l != null) {
                geo = !geo;
                if (geo && anchorStep == 1) anchorStep = 2;
                if (!geo && anchorStep == 2 && heading < 0) anchorStep = 1;
            }
            return true;
        }
        if (over(SPACING_DOWN, mx, my) || over(SPACING_UP, mx, my)) {
            if (l != null && !geo && anchorStep > 0) {
                heading = Math.floorMod(Math.max(0, heading) + (over(SPACING_UP, mx, my) ? HEADING_STEP : -HEADING_STEP), 360);
                anchorStep = 2;
            }
            return true;
        }
        if (over(SPACING, mx, my)) return true;
        if (over(ARM, mx, my)) {
            if (canLaunch(p, l)) {
                command(HoloCommandPayload.LAUNCH_SAT, controllerOf(p, l), anchorX, anchorZ, (geo ? 1 : 0) | (geo ? 0 : heading) << 1, null, List.of());
                resetAnchor();
            }
            return true;
        }
        if (over(DISARM, mx, my)) {
            if (canEngage(p, sat)) command(HoloCommandPayload.ENGAGE_SAT, null, 0, 0, -1, sat.id(), List.of());
            return true;
        }
        if (over(SALVO, mx, my)) { resetAnchor(); return true; }
        if (over(ABORT, mx, my)) {
            if (sat != null && sat.own()) command(HoloCommandPayload.DEORBIT, null, 0, 0, 0, sat.id(), List.of());
            return true;
        }
        return false;
    }

    /** A click on the map while aiming a launch: the anchor, then for a low orbit the point it heads towards. */
    private void anchorClick(double mx, double my) {
        int x = (int) Math.floor(menu.centre.getX() + 0.5 + dx(mx)), z = (int) Math.floor(menu.centre.getZ() + 0.5 + dz(my));
        if (anchorStep == 1 && !geo) {
            if (x == anchorX && z == anchorZ) return;
            heading = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(x - anchorX, z - anchorZ))), 360);
            anchorStep = 2;
            designate = false;
            return;
        }
        anchorX = x;
        anchorZ = z;
        heading = -1;
        anchorStep = geo ? 2 : 1;
        if (geo) designate = false;
    }

    /** The satellite whose icon is under ({@code mx, my}), within 6 pixels. */
    private HoloTracksPayload.Sat satelliteNear(HoloTracksPayload p, double mx, double my) {
        double now = now(0), since = since(p, now), best = 6 * 6;
        HoloTracksPayload.Sat near = null;
        for (HoloTracksPayload.Sat sat : p.orbital().satellites()) {
            double[] at = SatelliteViews.offset(sat, menu.centre, now, since);
            double d2 = Math.pow(sx(at[0]) - mx, 2) + Math.pow(sy(at[1]) - my, 2);
            if (d2 < best) { best = d2; near = sat; }
        }
        return near;
    }

    private void satelliteTooltip(GuiGraphics g, int mx, int my) {
        HoloTracksPayload p = picture();
        if (p == null) return;
        Component tip = null;
        if (over(ARM, mx, my)) tip = text("sat.launch.tooltip");
        else if (over(DISARM, mx, my)) tip = text("sat.engage.tooltip");
        else if (over(ABORT, mx, my)) tip = text("sat.deorbit.tooltip");
        else if (over(SPACING_DOWN, mx, my) || over(SPACING, mx, my) || over(SPACING_UP, mx, my)) tip = text("sat.heading.tooltip");
        if (tip != null) { g.renderTooltip(font, font.split(tip, 220), mx, my); return; }
        if (!over(LIST, mx, my)) return;
        HoloTracksPayload.Orbital o = p.orbital();
        int ly = my - topPos - LIST.y();
        List<Component> lines = new ArrayList<>();
        if (ly < LIST_HEADER) {
            for (int i = 0; i < o.stations().size(); i++) {
                int x = mx - leftPos;
                if (x < stationX(i) || x >= stationX(i) + 35) continue;
                HoloTracksPayload.Station st = o.stations().get(i);
                lines.add(Component.translatable(KEY + "sat.station." + (st.online() ? "online" : "offline"), i + 1));
                lines.add(Component.translatable(KEY + "sat.station.where", menu.centre.getX() + st.dx(), st.dy(), menu.centre.getZ() + st.dz()));
            }
        } else {
            List<int[]> rows = satRows(p);
            int row = (ly - LIST_HEADER) / LIST_ROW + scroll;
            if (row < rows.size() && rows.get(row)[0] == ROW_SAT) {
                HoloTracksPayload.Sat sat = o.satellites().get(rows.get(row)[1]);
                lines.add(Component.translatable(KEY + "sat.detail", sat.designation(), kindName(sat.kind()), orbitName(sat.orbit()), SatelliteViews.altitude(sat)));
                lines.add(pass(sat));
                lines.add(sat.life() < 0 ? text("sat.stays") : Component.translatable(KEY + "sat.decays", SatelliteViews.clock(sat.life())));
                if (sat.pk() >= 0) lines.add(Component.translatable(KEY + "sat.pk", Math.round(sat.pk() * 100)));
            } else if (row < rows.size() && rows.get(row)[0] == ROW_LAUNCHER) {
                HoloTracksPayload.SatLauncher l = o.launchers().get(rows.get(row)[1]);
                lines.add(Component.translatable(KEY + "sat.launcher_detail", l.link() + 1, l.kind() >= 0 ? kindName(l.kind()) : text("sat.empty"),
                        l.reach() >= 0 ? orbitName(l.reach()) : text("sat.reach_none"), launcherState(l.state())));
            }
        }
        if (!lines.isEmpty()) g.renderComponentTooltip(font, lines, mx, my);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (tab < 3 && (over(SALVO, mouseX, mouseY) || over(ARM, mouseX, mouseY)))
            g.renderTooltip(font, font.split(text("redstone"), 240), mouseX, mouseY);
        if (over(NETWORK, mouseX, mouseY)) g.renderTooltip(font, font.split(text("sat.network.tooltip"), 220), mouseX, mouseY);
        if (tab == TAB_SAT) satelliteTooltip(g, mouseX, mouseY);
        if (over(LIST, mouseX, mouseY) && tab == 0 && picture() != null) {
            int localY = mouseY - topPos - LIST.y() - LIST_HEADER;
            int row = localY / LIST_ROW + scroll;
            if (localY < 0) g.renderTooltip(font, font.split(text("row.hint"), 220), mouseX, mouseY);
            else if (row < picture().silos().size()) {
                var silo = picture().silos().get(row);
                g.renderComponentTooltip(font, List.of(state(silo), rowInfo(silo)), mouseX, mouseY);
            }
        }
        if (over(DETAIL, mouseX, mouseY) && tab == 0 && picture() != null && picture().silo() != null) {
            var silo = picture().silo();
            List<Component> lines = new ArrayList<>();
            lines.add(error(silo));
            lines.add(Component.translatable(KEY + "reach", Math.round(silo.reach())));
            lines.add(Component.translatable(KEY + "cep", Math.round(silo.cep())));
            if (silo.targeted()) lines.add(Component.translatable(KEY + "target", menu.centre.getX() + silo.tx(), menu.centre.getZ() + silo.tz()));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            HoloTracksPayload p = picture();
            HoloTracksPayload.Silo s = p == null ? null : p.silo();
            BlockPos at = controller(s);
            if (over(DESIGNATE, mx, my)) {
                if (tab == TAB_SAT) { if (launcherRow(p) != null) designate = !designate; }
                else if (tab != 3 && at != null && s.loaded()) designate = !designate;
                return true;
            }
            if (over(AIRBURST, mx, my)) { airburst = !airburst; return true; }
            if (over(AUTO, mx, my)) { auto = true; return true; }
            if (over(ZOOM_IN, mx, my) || over(ZOOM_OUT, mx, my)) {
                auto = false;
                int index = 0;
                for (int i = 0; i < HoloView.VIEWS.length; i++) if (HoloView.VIEWS[i] == view) index = i;
                view = HoloView.VIEWS[Mth.clamp(index + (over(ZOOM_IN, mx, my) ? -1 : 1), 0, HoloView.VIEWS.length - 1)];
                return true;
            }
            LayoutRect[] tabs = {TAB_SILOS, TAB_RADARS, TAB_TRACKS, TAB_DEFENSE, TAB_SATELLITES};
            for (int i = 0; i < tabs.length; i++) if (over(tabs[i], mx, my)) {
                if (i >= 3 || tab == TAB_SAT) designate = false;
                tab = i; scroll = 0;
                return true;
            }
            if (tab == TAB_SAT) {
                if (satelliteClick(mx, my, p)) return true;
                if (overMap(mx, my) && p != null) {
                    if (designate) { anchorClick(mx, my); return true; }
                    HoloTracksPayload.Sat near = satelliteNear(p, mx, my);
                    if (near != null) { satellite = near.id(); launcher = -1; return true; }
                }
            }
            if (tab == 3) {
                if (defenseClick(mx, my, p)) return true;
                if (overMap(mx, my) && p != null) for (var intercept : p.intercepts()) {
                    if (Math.pow(sx(intercept.x()) - mx, 2) + Math.pow(sy(intercept.z()) - my, 2) < 64) {
                        selected = intercept.interceptor(); return true;
                    }
                }
            }
            if (over(SALVO_MODE, mx, my)) { sequence = !sequence; return true; }
            if (over(SPACING_DOWN, mx, my) || over(SPACING_UP, mx, my)) {
                if (sequence) spacing = Mth.clamp(spacing + (over(SPACING_UP, mx, my) ? 1 : -1), 0, MissileConfig.salvoMaxSpacingSeconds);
                return true;
            }
            if (over(ARM, mx, my) || over(DISARM, mx, my)) {
                if (at != null && s.loaded()) command(over(ARM, mx, my) ? HoloCommandPayload.ARM : HoloCommandPayload.DISARM, at, 0, 0, 0, null, List.of());
                return true;
            }
            if (over(SALVO, mx, my)) {
                if (!salvo.isEmpty()) command(HoloCommandPayload.SALVO, null, 0, 0, sequence ? spacing * 20 : 0, null, List.copyOf(salvo));
                return true;
            }
            if (over(ABORT, mx, my)) {
                var track = selectedTrack();
                if (track != null && track.own()) command(HoloCommandPayload.ABORT, null, 0, 0, 0, track.id(), List.of());
                return true;
            }
            if (over(LIST, mx, my) && p != null) {
                int row = (int) (my - topPos - LIST.y() - LIST_HEADER);
                if (row < 0) return true;
                row = row / LIST_ROW + scroll;
                double x = mx - leftPos - LIST.x();
                if (tab == 0 && row < p.silos().size()) {
                    BlockPos pos = controller(p.silos().get(row));
                    if (pos != null) {
                        if (x >= COL_UNLINK) { salvo.remove(pos); command(HoloCommandPayload.UNLINK_SILO, pos, 0, 0, 0, null, List.of()); }
                        else if (x < COL_NAME) { if (!salvo.remove(pos)) salvo.add(pos); }
                        else command(HoloCommandPayload.SELECT, pos, 0, 0, 0, null, List.of());
                    }
                } else if (tab == 1 && row < p.radars().size()) {
                    radarRow = row;
                    var radar = p.radars().get(row);
                    if (x >= COL_UNLINK) command(HoloCommandPayload.UNLINK_RADAR, menu.centre.offset(radar.dx(), radar.dy() - menu.centre.getY(), radar.dz()), 0, 0, 0, null, List.of());
                } else if (tab == 2 && row < p.tracks().size()) selected = p.tracks().get(row).id();
                return true;
            }
            if (overMap(mx, my)) {
                if (designate) {
                    int x = (int) Math.floor(menu.centre.getX() + 0.5 + dx(mx)), z = (int) Math.floor(menu.centre.getZ() + 0.5 + dz(my));
                    if (at != null) command(HoloCommandPayload.DESIGNATE, at, x, z, airburst ? 1 : 0, null, List.of());
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
            cx -= dx / MAP_SIZE * view;
            cz -= dy / MAP_SIZE * view;
            double limit = HoloView.VIEWS[HoloView.VIEWS.length - 1] / 2.0;
            cx = Mth.clamp(cx, -limit, limit);
            cz = Mth.clamp(cz, -limit, limit);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (over(LIST, mx, my)) { scroll += scrollY > 0 ? -1 : 1; return true; }
        if (overMap(mx, my)) {
            auto = false;
            int index = 0;
            for (int i = 0; i < HoloView.VIEWS.length; i++) if (HoloView.VIEWS[i] == view) index = i;
            int next = Mth.clamp(index + (scrollY > 0 ? -1 : 1), 0, HoloView.VIEWS.length - 1);
            double wx = dx(mx), wz = dz(my);
            view = HoloView.VIEWS[next];
            cx = wx - (mx - leftPos - MX - MAP_SIZE / 2.0) / MAP_SIZE * view;
            cz = wz - (my - topPos - MY - MAP_SIZE / 2.0) / MAP_SIZE * view;
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
