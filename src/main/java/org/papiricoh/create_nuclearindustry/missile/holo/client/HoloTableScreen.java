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

/** The shared tactical map and tabbed device lists. Commands refer to linked controllers, never a launch trigger. */
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
    private int tab, scroll, radarRow = -1, spacing;
    private boolean sequence;
    private final Set<BlockPos> salvo = new LinkedHashSet<>();

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
        double since = Math.max(0, Math.min(20, now - picture.sample()));
        float g = Math.max(1e-5f, picture.gravity());
        for (HoloTracksPayload.Track t : picture.tracks()) {
            int c = t.id().equals(selected) ? WHITE : t.own() ? GREEN : t.threat() ? RED : CYAN;
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
        ConsoleDraw.line(g, font, title, TITLE.x() + 2, TITLE.y() + 2, 290, CYAN, 1);
        String scale = view >= 1000 ? (view / 1000) + " km" : view + " m";
        small(g, Component.literal(scale), TITLE.right() - 86, TITLE.y() + 3, 50, DIM);
        HoloTracksPayload.Silo silo = p == null ? null : p.silo();
        boolean loaded = silo != null && silo.loaded() && controller(silo) != null;
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.centre) instanceof HoloTableBlockEntity table)
            salvo.removeIf(pos -> !table.silos().contains(net.minecraft.core.GlobalPos.of(minecraft.level.dimension(), pos)));
        button(g, ZOOM_OUT, Component.literal("−"), false, true, false, mouseX, mouseY);
        button(g, ZOOM_IN, Component.literal("+"), false, true, false, mouseX, mouseY);
        button(g, AUTO, text("auto"), auto, true, false, mouseX, mouseY);
        button(g, DESIGNATE, text(designate ? "designate_on" : "designate"), designate, loaded, false, mouseX, mouseY);
        button(g, AIRBURST, text(airburst ? "airburst" : "groundburst"), airburst, true, false, mouseX, mouseY);
        LayoutRect[] tabs = {TAB_SILOS, TAB_RADARS, TAB_TRACKS};
        String[] names = {"silos", "radars", "tracks"};
        for (int i = 0; i < tabs.length; i++) {
            LayoutRect rect = tabs[i];
            g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), tab == i ? 0xFF24606A : 0xFF15333C);
            sprite(g, SPRITE_TABS, 0, tab == i ? 16 : 0, rect.x(), rect.y(), rect.w(), rect.h(), 58, 14);
            fit(g, text(names[i]), rect.x() + 3, rect.y() + 3, rect.w() - 6, CYAN);
        }
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (over(SALVO, mouseX, mouseY) || over(ARM, mouseX, mouseY))
            g.renderTooltip(font, font.split(text("redstone"), 240), mouseX, mouseY);
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
            if (over(DESIGNATE, mx, my)) { if (at != null && s.loaded()) designate = !designate; return true; }
            if (over(AIRBURST, mx, my)) { airburst = !airburst; return true; }
            if (over(AUTO, mx, my)) { auto = true; return true; }
            if (over(ZOOM_IN, mx, my) || over(ZOOM_OUT, mx, my)) {
                auto = false;
                int index = 0;
                for (int i = 0; i < HoloView.VIEWS.length; i++) if (HoloView.VIEWS[i] == view) index = i;
                view = HoloView.VIEWS[Mth.clamp(index + (over(ZOOM_IN, mx, my) ? -1 : 1), 0, HoloView.VIEWS.length - 1)];
                return true;
            }
            LayoutRect[] tabs = {TAB_SILOS, TAB_RADARS, TAB_TRACKS};
            for (int i = 0; i < tabs.length; i++) if (over(tabs[i], mx, my)) { tab = i; scroll = 0; return true; }
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
