package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.client.RadarSounds;
import org.papiricoh.create_nuclearindustry.missile.radar.client.ScopeDraw;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.client.ClientTerrain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.papiricoh.create_nuclearindustry.missile.radar.RadarLayout.*;

/**
 * The radar's console. On the left, a plan position indicator: the land under a phosphor glass, range rings and the beam sweeping
 * round with the antenna; each echo is lit as the beam crosses it and fades until it comes round again, trailing where it was,
 * pointing where it goes and ringing where it will come down. On the right, the track table (sorted and filtered), the selected
 * track's detail and its side view, altitude over ground with the arc still to fly. Below, zoom, recentre, the defended zone,
 * the network code and the alarm acknowledge. Opening it never loads a chunk; terrain comes from the survey.
 */
public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/radar_screen.png");
    private static final ResourceLocation MASK = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/radar_scope_mask.png");
    private static final double[] RANGES = {250, 500, 1000, 2000, 4000, 8000, 16000, 32000};
    private static final int GREEN = 0xFF7DFF9A, GREEN_DIM = 0xFF2E8A45, GREEN_FAINT = 0xFF1C5A2C, AMBER = 0xFFFFB347, RED = 0xFFFF4A3A,
            WHITE = 0xFFE8FFEC, CYAN = 0xFF6FF4FF;
    private static final String KEY = "radar.create_nuclearindustry.screen.";

    private int rangeIndex = 4;
    private double viewX, viewZ;
    private boolean panned, dragging, zoneMode;
    private UUID selected;
    private int sort, filter, scroll;
    private EditBox code;
    /** Echo state: where the beam last painted each track, and when (client ticks). */
    private final Map<UUID, double[]> painted = new HashMap<>();
    private final Set<UUID> known = new HashSet<>();
    private boolean primed;
    private double lastSweep = Double.NaN;
    private double lastPing = -100;
    private RadarSounds.Klaxon klaxon;
    private int pendingZoneRadius = -1;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    private RadarBlockEntity radar() {
        return menu.radar();
    }

    @Override
    protected void init() {
        super.init();
        viewX = menu.pos.getX() + 0.5;
        viewZ = menu.pos.getZ() + 0.5;
        code = new EditBox(font, leftPos + CODE.x() + 4, topPos + CODE.y() + 4, CODE.w() - 6, 10, Component.translatable(KEY + "net"));
        code.setBordered(false);
        code.setMaxLength(LinkCode.LENGTH);
        code.setTextColor(GREEN);
        code.setFilter(text -> text.chars().allMatch(c -> LinkCode.allowed((char) c)));
        if (radar() != null) code.setValue(radar().code());
        code.setResponder(text -> {
            String clean = LinkCode.sanitize(text);
            if (radar() != null && (clean.isEmpty() || LinkCode.complete(clean)) && !clean.equals(radar().code()))
                send(RadarConfigurePayload.SET_CODE, clean, 0, 0, 0);
        });
        addRenderableWidget(code);
        // Start with the whole reach in view.
        if (radar() != null && radar().reach() > 0) {
            rangeIndex = 0;
            while (rangeIndex < RANGES.length - 1 && RANGES[rangeIndex] < radar().reach() * 0.8) rangeIndex++;
        }
    }

    private void send(int action, String text, int x, int z, int radius) {
        PacketDistributor.sendToServer(new RadarConfigurePayload(menu.pos, action, text, x, z, radius));
    }

    private double range() {
        return RANGES[rangeIndex];
    }

    // ------------------------------------------------------------------ geometry

    private float sx(double worldX) {
        return (float) (leftPos + PPI_CX + (worldX - viewX) / range() * PPI_RADIUS);
    }

    private float sy(double worldZ) {
        return (float) (topPos + PPI_CY + (worldZ - viewZ) / range() * PPI_RADIUS);
    }

    private double worldX(double screenX) {
        return viewX + (screenX - leftPos - PPI_CX) / PPI_RADIUS * range();
    }

    private double worldZ(double screenY) {
        return viewZ + (screenY - topPos - PPI_CY) / PPI_RADIUS * range();
    }

    private boolean overScope(double mx, double my) {
        double dx = mx - leftPos - PPI_CX, dy = my - topPos - PPI_CY;
        return dx * dx + dy * dy <= PPI_RADIUS * PPI_RADIUS;
    }

    private boolean over(RadarLayout.Rect r, double mx, double my) {
        return r.contains(mx - leftPos, my - topPos);
    }

    /** Bearing from the radar, degrees clockwise from north. */
    private double bearing(double x, double z) {
        double b = Math.toDegrees(Math.atan2(x - (menu.pos.getX() + 0.5), -(z - (menu.pos.getZ() + 0.5))));
        return b < 0 ? b + 360 : b;
    }

    private double now(float partial) {
        return minecraft.level == null ? 0 : minecraft.level.getGameTime() + partial;
    }

    /** Where a track is now, flown on from its last sample. */
    private V3 position(RadarTrack.Snapshot track, double now) {
        double t = Math.max(0, Math.min(40, now - menu.sample));
        V3 p = track.position(), v = track.velocity();
        return new V3(p.x() + v.x() * t, p.y() + v.y() * t - 0.5 * menu.gravity * t * t, p.z() + v.z() * t);
    }

    /** The antenna's bearing now, degrees: Create's kinetic angle, which the dish turns by too. */
    private double sweep(double now) {
        if (radar() == null || menu.status != 1) return Double.NaN;
        double angle = (now * radar().getSpeed() * 0.3) % 360;
        return angle < 0 ? angle + 360 : angle;
    }

    private List<RadarTrack.Snapshot> shown() {
        List<RadarTrack.Snapshot> tracks = new ArrayList<>();
        RadarBlockEntity radar = radar();
        for (RadarTrack.Snapshot track : menu.tracks()) {
            boolean threat = radar != null && track.threatens(radar.zoneX(), radar.zoneZ(), zoneRadius());
            if (filter == 1 && !threat) continue;
            if (filter == 2 && track.phaseEnum() != MissileFlight.Phase.BOOST) continue;
            tracks.add(track);
        }
        Comparator<RadarTrack.Snapshot> order = switch (sort) {
            case 1 -> Comparator.comparing(RadarTrack.Snapshot::designation);
            case 2 -> Comparator.comparingDouble(t -> horizontal(t.position()));
            default -> Comparator.comparingInt(RadarTrack.Snapshot::eta);
        };
        tracks.sort(order);
        return tracks;
    }

    private int zoneRadius() {
        if (pendingZoneRadius >= 0) return pendingZoneRadius;
        return radar() == null ? 0 : radar().zoneRadius();
    }

    private double horizontal(V3 p) {
        return Math.hypot(p.x() - menu.pos.getX() - 0.5, p.z() - menu.pos.getZ() - 0.5);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        RadarBlockEntity radar = radar();
        if (radar == null) return;
        if (pendingZoneRadius >= 0 && radar.zoneRadius() == pendingZoneRadius) pendingZoneRadius = -1;
        // New tracks chime once; the first picture after opening is not news.
        List<RadarTrack.Snapshot> tracks = menu.tracks();
        if (menu.sample != 0) {
            boolean fresh = false;
            for (RadarTrack.Snapshot track : tracks) fresh |= known.add(track.id()) && primed;
            if (fresh) RadarSounds.newTrack();
            primed = true;
        }
        if (radar.alarmUnacked() && (klaxon == null || !klaxon.playing()))
            klaxon = RadarSounds.klaxon(() -> minecraft.screen == this && radar.alarmUnacked(), null, 0.6f);
        if (!panned) {
            viewX = menu.pos.getX() + 0.5;
            viewZ = menu.pos.getZ() + 0.5;
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        double now = now(partial);
        List<RadarTrack.Snapshot> tracks = menu.tracks();
        int x0 = leftPos + PPI.x() + 1, y0 = topPos + PPI.y() + 1, x1 = leftPos + PPI.right() - 1, y1 = topPos + PPI.bottom() - 1;
        g.enableScissor(x0, y0, x1, y1);
        drawTerrain(g);
        drawGrid(g);
        drawSweep(g, now);
        drawTracks(g, tracks, now);
        g.disableScissor();
        // The bezel rounds the picture off.
        g.blit(MASK, leftPos + PPI.x(), topPos + PPI.y(), 0, 0, PPI.w(), PPI.h(), PPI.w(), PPI.h());
        drawProfile(g, now);
        drawButtons(g, mouseX, mouseY, now);
    }

    private void drawTerrain(GuiGraphics g) {
        double blocksPerPixel = range() / PPI_RADIUS / Math.max(1, minecraft.getWindow().getGuiScale());
        int lod = TerrainTile.pickLod(blocksPerPixel * 2);
        int minX = (int) Math.floor(viewX - range()), maxX = (int) Math.ceil(viewX + range());
        int minZ = (int) Math.floor(viewZ - range()), maxZ = (int) Math.ceil(viewZ + range());
        long[] keys = TerrainTile.covering(lod, minX, minZ, maxX, maxZ);
        if (keys.length > 64) return;
        ClientTerrain.want(menu.pos, keys);
        for (long key : keys) {
            ResourceLocation texture = ClientTerrain.texture(key);
            if (texture == null) continue;
            int span = TerrainTile.span(lod);
            double ox = TerrainTile.origin(lod, TerrainTile.tx(key)), oz = TerrainTile.origin(lod, TerrainTile.tz(key));
            ScopeDraw.texture(g, texture, sx(ox), sy(oz), sx(ox + span), sy(oz + span), 0, 0, 1, 1, 0x8CFFFFFF);
        }
    }

    private void drawGrid(GuiGraphics g) {
        float cx = leftPos + PPI_CX, cy = topPos + PPI_CY;
        double step = ringStep(range());
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            // Rings round the radar itself, and the cardinal lines through it.
            float rx = sx(menu.pos.getX() + 0.5), ry = sy(menu.pos.getZ() + 0.5);
            for (double r = step; r <= range() * 2.9; r += step) {
                boolean major = Math.round(r / step) % 2 == 0;
                d.circle(rx, ry, (float) (r / range() * PPI_RADIUS), major ? 0.8f : 0.6f, major ? 0x5552D27A : 0x3352D27A, false);
            }
            d.line(rx, ry - 2 * PPI_RADIUS - 400, rx, ry + 2 * PPI_RADIUS + 400, 0.6f, 0x2E52D27A);
            d.line(rx - 2 * PPI_RADIUS - 400, ry, rx + 2 * PPI_RADIUS + 400, ry, 0.6f, 0x2E52D27A);
            // The defended zone.
            RadarBlockEntity radar = radar();
            if (radar != null && zoneRadius() > 0) {
                float zx = sx(radar.zoneX() + 0.5), zy = sy(radar.zoneZ() + 0.5);
                d.circle(zx, zy, (float) (zoneRadius() / range() * PPI_RADIUS), 1.1f, 0xCCFFB347, true);
                d.line(zx - 3, zy, zx + 3, zy, 1, 0xCCFFB347);
                d.line(zx, zy - 3, zx, zy + 3, 1, 0xCCFFB347);
            }
            // The radar itself when the view is panned off it.
            d.rect(rx - 1.5f, ry - 1.5f, rx + 1.5f, ry + 1.5f, 0xCC9CFFB0);
        }
        // Ring labels along the lower right diagonal, and bearings round the edge.
        float rx = sx(menu.pos.getX() + 0.5), ry = sy(menu.pos.getZ() + 0.5);
        for (double r = step * 2; r <= range() * 1.4; r += step * 2) {
            float px = rx + (float) (r / range() * PPI_RADIUS * 0.7071), py = ry + (float) (r / range() * PPI_RADIUS * 0.7071);
            small(g, distance(r), px + 1, py + 1, 0x9968D88A);
        }
        for (int deg = 0; deg < 360; deg += 30) {
            double a = Math.toRadians(deg);
            float px = cx + (float) Math.sin(a) * (PPI_RADIUS - 7), py = cy - (float) Math.cos(a) * (PPI_RADIUS - 7);
            String label = String.format(Locale.ROOT, "%03d", deg);
            small(g, label, px - font.width(label) / 4f, py - 2, 0xAA9CFFB0);
        }
    }

    private static double ringStep(double range) {
        for (double step : new double[]{25, 50, 100, 250, 500, 1000, 2500, 5000, 10000})
            if (range / step <= 5) return step;
        return 10000;
    }

    private static String distance(double blocks) {
        return blocks >= 1000 ? (blocks % 1000 == 0 ? (int) (blocks / 1000) + "k" : String.format(Locale.ROOT, "%.1fk", blocks / 1000)) : (int) blocks + "";
    }

    private void drawSweep(GuiGraphics g, double now) {
        double sweep = sweep(now);
        if (Double.isNaN(sweep)) {
            lastSweep = Double.NaN;
            return;
        }
        float rx = sx(menu.pos.getX() + 0.5), ry = sy(menu.pos.getZ() + 0.5);
        float reach = PPI_RADIUS * 3;
        boolean clockwise = radar().getSpeed() >= 0;
        double trail = clockwise ? -40 : 40;
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            d.wedge(rx, ry, reach, sweep + trail, sweep, 0x0040FF70, 0x5540FF70);
            double a = Math.toRadians(sweep);
            d.line(rx, ry, rx + (float) Math.sin(a) * reach, ry - (float) Math.cos(a) * reach, 1.4f, 0xDDB8FFC8, 0x6640FF70);
        }
    }

    private void drawTracks(GuiGraphics g, List<RadarTrack.Snapshot> tracks, double now) {
        RadarBlockEntity radar = radar();
        double sweep = sweep(now);
        boolean phosphor = ClientConfig.radarPhosphor;
        double period = radar == null || radar.getSpeed() == 0 ? 40 : 360 / (Math.abs(radar.getSpeed()) * 0.3);
        // The beam lights every echo it crossed since the last frame.
        for (RadarTrack.Snapshot track : tracks) {
            V3 p = position(track, now);
            double[] echo = painted.get(track.id());
            boolean crossed = !Double.isNaN(sweep) && !Double.isNaN(lastSweep) && between(bearing(p.x(), p.z()), lastSweep, sweep, radar.getSpeed() >= 0);
            if (echo == null || crossed || !phosphor) {
                painted.put(track.id(), new double[]{p.x(), p.y(), p.z(), now});
                if (crossed && now - lastPing > 4) {
                    lastPing = now;
                    RadarSounds.ping(0.9f + (float) Math.min(0.4, track.eta() / 6000.0));
                }
            }
        }
        lastSweep = sweep;
        painted.keySet().removeIf(id -> tracks.stream().noneMatch(t -> t.id().equals(id)));
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            for (RadarTrack.Snapshot track : tracks) {
                boolean threat = radar != null && track.threatens(radar.zoneX(), radar.zoneZ(), zoneRadius());
                boolean isSelected = track.id().equals(selected);
                double[] echo = painted.get(track.id());
                if (echo == null) continue;
                float glow = phosphor ? (float) (0.22 + 0.78 * Math.exp(-(now - echo[3]) / (period * 0.7))) : 1;
                int colour = isSelected ? WHITE : threat ? RED : GREEN;
                // Where it has been: the plotted history, fading towards its start.
                List<V3> history = track.history();
                for (int i = 1; i < history.size(); i++) {
                    float age = (float) i / history.size();
                    V3 a = history.get(i - 1), b = history.get(i);
                    d.line(sx(a.x()), sy(a.z()), sx(b.x()), sy(b.z()), 1, ScopeDraw.alpha(colour, 0.08f + 0.4f * age * glow));
                }
                float ex = sx(echo[0]), ey = sy(echo[2]);
                // Where it is going: thirty seconds of its velocity.
                V3 v = track.velocity();
                d.line(ex, ey, sx(echo[0] + v.x() * 600), sy(echo[2] + v.z() * 600), 1, ScopeDraw.alpha(colour, 0.7f * glow), ScopeDraw.alpha(colour, 0.05f));
                // Where it comes down, and how sure that is.
                RadarModel.Ellipse e = track.ellipse();
                float scale = (float) (PPI_RADIUS / range());
                int ring = threat ? RED : AMBER;
                d.ellipse(sx(track.impact().x()), sy(track.impact().z()), Math.max(2, (float) e.along() * scale), Math.max(1.5f, (float) e.across() * scale),
                        e.heading(), 0.9f, ScopeDraw.alpha(ring, 0.75f), true, ScopeDraw.alpha(ring, threat ? 0.12f : 0.06f));
                float ix = sx(track.impact().x()), iy = sy(track.impact().z());
                d.line(ix - 2.5f, iy - 2.5f, ix + 2.5f, iy + 2.5f, 1, ScopeDraw.alpha(ring, 0.9f));
                d.line(ix - 2.5f, iy + 2.5f, ix + 2.5f, iy - 2.5f, 1, ScopeDraw.alpha(ring, 0.9f));
                // The echo itself: a hot core in a soft halo.
                d.disc(ex, ey, 5.5f, ScopeDraw.alpha(colour, 0.35f * glow), ScopeDraw.alpha(colour, 0));
                d.disc(ex, ey, 2.2f, ScopeDraw.alpha(colour, glow), ScopeDraw.alpha(colour, 0.6f * glow));
                if (isSelected) d.circle(ex, ey, 7, 0.8f, 0xCCFFFFFF, true);
            }
        }
        for (RadarTrack.Snapshot track : tracks) {
            double[] echo = painted.get(track.id());
            if (echo == null) continue;
            boolean threat = radar != null && track.threatens(radar.zoneX(), radar.zoneZ(), zoneRadius());
            float glow = phosphor ? (float) (0.35 + 0.65 * Math.exp(-(now - echo[3]) / (period * 0.7))) : 1;
            small(g, track.designation(), sx(echo[0]) + 5, sy(echo[2]) - 6, ScopeDraw.alpha(threat ? RED : GREEN, glow));
        }
    }

    /** Whether bearing {@code b} lies on the arc the beam swept from {@code from} to {@code to}. */
    private static boolean between(double b, double from, double to, boolean clockwise) {
        double span = clockwise ? Mth.positiveModulo(to - from, 360) : Mth.positiveModulo(from - to, 360);
        double off = clockwise ? Mth.positiveModulo(b - from, 360) : Mth.positiveModulo(from - b, 360);
        return span < 180 && off <= span;
    }

    private void drawProfile(GuiGraphics g, double now) {
        RadarTrack.Snapshot track = selectedTrack();
        int x0 = leftPos + PROFILE.x() + 3, y0 = topPos + PROFILE.y() + 3, x1 = leftPos + PROFILE.right() - 3, y1 = topPos + PROFILE.bottom() - 4;
        if (track == null) return;
        V3 start = track.firstSeen();
        V3 here = position(track, now);
        double gravity = Math.max(1e-5, menu.gravity);
        // The arc still to fly: free fall from here to the predicted impact.
        List<double[]> arc = new ArrayList<>();
        double ground = Math.min(RadarModel.SEA_LEVEL, start.y());
        double t = RadarModel.timeToGround(here, track.velocity(), gravity, ground);
        if (!Double.isFinite(t)) t = 0;
        for (int i = 0; i <= 32; i++) {
            double s = t * i / 32;
            V3 v = track.velocity();
            arc.add(new double[]{Math.hypot(here.x() + v.x() * s - start.x(), here.z() + v.z() * s - start.z()), here.y() + v.y() * s - 0.5 * gravity * s * s});
        }
        double maxX = 1, minY = ground, maxY = ground + 10;
        for (V3 p : track.history()) {
            maxX = Math.max(maxX, Math.hypot(p.x() - start.x(), p.z() - start.z()));
            maxY = Math.max(maxY, p.y());
        }
        for (double[] p : arc) {
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        maxX *= 1.05;
        maxY += (maxY - minY) * 0.1;
        double fMaxX = maxX, fMinY = minY, fMaxY = maxY;
        java.util.function.DoubleUnaryOperator px = d -> x0 + d / fMaxX * (x1 - x0);
        java.util.function.DoubleUnaryOperator py = h -> y1 - (h - fMinY) / (fMaxY - fMinY) * (y1 - y0);
        g.enableScissor(x0 - 1, y0 - 1, x1 + 2, y1 + 2);
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            d.line(x0, (float) py.applyAsDouble(ground), x1, (float) py.applyAsDouble(ground), 1, 0x8852D27A);
            for (int i = 1; i < arc.size(); i += 2) {
                double[] a = arc.get(i - 1), b = arc.get(i);
                d.line((float) px.applyAsDouble(a[0]), (float) py.applyAsDouble(a[1]), (float) px.applyAsDouble(b[0]), (float) py.applyAsDouble(b[1]), 1, 0xCCFFB347);
            }
            List<V3> history = track.history();
            for (int i = 1; i < history.size(); i++) {
                V3 a = history.get(i - 1), b = history.get(i);
                d.line((float) px.applyAsDouble(Math.hypot(a.x() - start.x(), a.z() - start.z())), (float) py.applyAsDouble(a.y()),
                        (float) px.applyAsDouble(Math.hypot(b.x() - start.x(), b.z() - start.z())), (float) py.applyAsDouble(b.y()), 1.2f, 0xDD7DFF9A);
            }
            float hx = (float) px.applyAsDouble(Math.hypot(here.x() - start.x(), here.z() - start.z())), hy = (float) py.applyAsDouble(here.y());
            d.disc(hx, hy, 2.5f, 0xFFE8FFEC, 0x447DFF9A);
            double[] end = arc.get(arc.size() - 1);
            float ex = (float) px.applyAsDouble(end[0]), ey = (float) py.applyAsDouble(end[1]);
            d.line(ex - 2, ey - 2, ex + 2, ey + 2, 1, 0xFFFF4A3A);
            d.line(ex - 2, ey + 2, ex + 2, ey - 2, 1, 0xFFFF4A3A);
        }
        g.disableScissor();
        small(g, distance(fMaxY), x0 + 1, y0 + 1, 0x9968D88A);
        small(g, distance(fMaxX), x1 - font.width(distance(fMaxX)) / 2f - 1, y1 - 5, 0x9968D88A);
    }

    private void drawButtons(GuiGraphics g, int mouseX, int mouseY, double now) {
        RadarBlockEntity radar = radar();
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            for (RadarLayout.Rect r : List.of(ZOOM_OUT, ZOOM_IN, CENTRE, ZONE, FILTER)) {
                if (over(r, mouseX, mouseY)) d.rect(leftPos + r.x(), topPos + r.y(), leftPos + r.right(), topPos + r.bottom(), 0x22FFFFFF);
            }
            if (zoneMode) d.rect(leftPos + ZONE.x(), topPos + ZONE.y(), leftPos + ZONE.right(), topPos + ZONE.bottom(), 0x33FFB347);
            if (radar != null && radar.alarmUnacked() && ((int) (now / 8) & 1) == 0)
                d.rect(leftPos + ACK.x() + 2, topPos + ACK.y() + 2, leftPos + ACK.right() - 2, topPos + ACK.bottom() - 2, 0x99FF3020);
            // The status lamp in the title strip.
            int lamp = radar == null || menu.status != 1 ? 0xFF404040 : radar.threatCount() > 0 ? (((int) (now / 6) & 1) == 0 ? RED : 0xFF601810) : 0xFF40FF70;
            float lx = leftPos + TITLE.right() - 6.5f, ly = topPos + TITLE.y() + 6.5f;
            d.disc(lx, ly, 2.6f, lamp, ScopeDraw.alpha(lamp, 0.8f));
            d.disc(lx, ly, 6, ScopeDraw.alpha(lamp, 0.25f), ScopeDraw.alpha(lamp, 0));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        RadarBlockEntity radar = radar();
        g.drawString(font, title.copy().withStyle(s -> s.withBold(true)), TITLE.x() + 4, TITLE.y() + 3, 0xFFD9F2C8, false);
        Component status = switch (menu.status) {
            case 1 -> Component.translatable(KEY + "online", menu.tracks().size(), radar == null ? 0 : radar.threatCount());
            case 2 -> Component.translatable(KEY + "static");
            default -> Component.translatable(KEY + "offline");
        };
        g.drawString(font, status, TITLE.right() - 14 - font.width(status), TITLE.y() + 3, menu.status == 1 ? 0xFF9CFFB0 : 0xFFFF8A70, false);
        // Under the scope.
        g.drawString(font, Component.translatable(KEY + "range", distance(range())), RANGE.x() + 4, RANGE.y() + 4, GREEN, false);
        Component zone = zoneMode ? Component.translatable(KEY + "zone_edit")
                : zoneRadius() > 0 ? Component.translatable(KEY + "zone", distance(zoneRadius())) : Component.translatable(KEY + "zone_any");
        g.drawString(font, zone, ZONE.x() + 16, ZONE.y() + 4, zoneMode ? AMBER : 0xFFD9F2C8, false);
        Component filterLabel = Component.translatable(KEY + "filter." + (filter == 1 ? "threat" : filter == 2 ? "boost" : "all"));
        g.drawCenteredString(font, filterLabel, FILTER.x() + FILTER.w() / 2, FILTER.y() + 4, 0xFFD9F2C8);
        g.drawCenteredString(font, Component.translatable(KEY + "ack"), ACK.x() + ACK.w() / 2, ACK.y() + 4, 0xFFFFF0E0);
        if (code.getValue().isEmpty() && !code.isFocused()) g.drawString(font, Component.translatable(KEY + "net"), CODE.x() + 4, CODE.y() + 4, GREEN_FAINT, false);
        if (menu.status != 1) {
            Component off = Component.translatable(KEY + (menu.status == 2 ? "static" : "offline"));
            g.drawCenteredString(font, off, PPI_CX, PPI_CY - 4, ((int) (now(0) / 10) & 1) == 0 ? 0xFFFF8A70 : 0xFF803020);
        }
        drawTable(g, mouseX, mouseY);
        drawDetail(g);
    }

    private void drawTable(GuiGraphics g, int mouseX, int mouseY) {
        int x = TABLE.x() + 2, y = TABLE.y() + 2;
        String[] heads = {"head.track", "head.phase", "head.range", "head.eta"};
        int[] cols = {COL_NAME, COL_PHASE, COL_RANGE, COL_ETA};
        int[] sortOf = {1, -1, 2, 0};
        for (int i = 0; i < heads.length; i++) {
            String text = Component.translatable(KEY + heads[i]).getString() + (sortOf[i] == sort ? "▾" : "");
            g.drawString(font, text, x + cols[i], y + 1, sortOf[i] == sort ? 0xFFE8FFEC : 0xFF9CFFB0, false);
        }
        List<RadarTrack.Snapshot> tracks = shown();
        scroll = Math.max(0, Math.min(scroll, tracks.size() - TABLE_ROWS));
        RadarBlockEntity radar = radar();
        double now = now(0);
        for (int row = 0; row < TABLE_ROWS && scroll + row < tracks.size(); row++) {
            RadarTrack.Snapshot track = tracks.get(scroll + row);
            int ry = TABLE.y() + HEADER + 1 + row * ROW;
            boolean threat = radar != null && track.threatens(radar.zoneX(), radar.zoneZ(), zoneRadius());
            boolean isSelected = track.id().equals(selected);
            if (isSelected) g.fill(TABLE.x() + 2, ry, TABLE.right() - 2, ry + ROW - 1, 0x5540FF70);
            int colour = isSelected ? WHITE : threat ? 0xFFFF7A60 : GREEN;
            g.drawString(font, track.designation(), x + COL_NAME, ry + 1, colour, false);
            g.drawString(font, Component.translatable("radar.create_nuclearindustry.phase_short." + track.phaseEnum().name().toLowerCase(Locale.ROOT)),
                    x + COL_PHASE, ry + 1, phaseColour(track.phaseEnum()), false);
            g.drawString(font, distance(horizontal(position(track, now))), x + COL_RANGE, ry + 1, colour, false);
            g.drawString(font, clock(etaSeconds(track, now)), x + COL_ETA, ry + 1, colour, false);
            if (threat) g.drawString(font, "!", x + COL_FLAG, ry + 1, ((int) (now / 6) & 1) == 0 ? RED : 0xFF802018, false);
        }
        if (tracks.isEmpty()) g.drawCenteredString(font, Component.translatable(KEY + "no_tracks"), TABLE.x() + TABLE.w() / 2, TABLE.y() + 44, GREEN_FAINT);
    }

    private static int phaseColour(MissileFlight.Phase phase) {
        return switch (phase) {
            case BOOST -> 0xFFFFD27A;
            case MIDCOURSE -> 0xFF8CE8FF;
            case REENTRY, TERMINAL -> 0xFFFF9A6A;
            case IMPACT -> 0xFF808080;
        };
    }

    private double etaSeconds(RadarTrack.Snapshot track, double now) {
        return Math.max(0, (track.eta() - (now - menu.sample)) / 20.0);
    }

    private static String clock(double seconds) {
        int s = (int) Math.ceil(seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private RadarTrack.Snapshot selectedTrack() {
        if (selected == null) return null;
        for (RadarTrack.Snapshot track : menu.tracks()) if (track.id().equals(selected)) return track;
        return null;
    }

    private void drawDetail(GuiGraphics g) {
        int x = DETAIL.x() + 5, y = DETAIL.y() + 4;
        RadarTrack.Snapshot track = selectedTrack();
        if (track == null) {
            g.drawCenteredString(font, Component.translatable(KEY + "no_track"), DETAIL.x() + DETAIL.w() / 2, DETAIL.y() + 26, GREEN_FAINT);
            return;
        }
        double now = now(0);
        V3 p = position(track, now);
        RadarBlockEntity radar = radar();
        boolean threat = radar != null && track.threatens(radar.zoneX(), radar.zoneZ(), zoneRadius());
        g.drawString(font, track.designation(), x, y, WHITE, false);
        g.drawString(font, Component.translatable("radar.create_nuclearindustry.phase." + track.phaseEnum().name().toLowerCase(Locale.ROOT)),
                x + 42, y, phaseColour(track.phaseEnum()), false);
        if (threat) g.drawString(font, Component.translatable(KEY + "threat"), DETAIL.right() - 6 - font.width(Component.translatable(KEY + "threat")), y,
                ((int) (now / 6) & 1) == 0 ? RED : 0xFF902818, false);
        double speed = track.velocity().length() * 20;
        line(g, x, y + 10, "alt", String.format(Locale.ROOT, "%.0f", p.y()), "spd", String.format(Locale.ROOT, "%.0f", speed));
        line(g, x, y + 19, "apo", String.format(Locale.ROOT, "%.0f", Math.max(track.apogee(), p.y())), "eta", clock(etaSeconds(track, now)));
        g.drawString(font, Component.translatable(KEY + "impact", (int) track.impact().x(), (int) track.impact().z()), x, y + 28, AMBER, false);
        g.drawString(font, Component.translatable(KEY + "bearing", (int) bearing(p.x(), p.z()), distance(horizontal(p))), x, y + 37, GREEN, false);
        g.drawString(font, Component.translatable(KEY + "sensors", menu.sensors, radar == null || radar.code().isEmpty() ? "-" : radar.code()), x, y + 46,
                GREEN_DIM, false);
    }

    private void line(GuiGraphics g, int x, int y, String a, String av, String b, String bv) {
        g.drawString(font, Component.translatable(KEY + a, av), x, y, GREEN, false);
        g.drawString(font, Component.translatable(KEY + b, bv), x + 74, y, GREEN, false);
    }

    /** Half-size text for the scope's annotations. */
    private void small(GuiGraphics g, String text, float x, float y, int argb) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1);
        g.drawString(font, text, 0, 0, argb, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        String tip = over(ZOOM_OUT, mouseX, mouseY) ? "zoom_out" : over(ZOOM_IN, mouseX, mouseY) ? "zoom_in" : over(CENTRE, mouseX, mouseY) ? "centre"
                : over(ZONE, mouseX, mouseY) ? "zone" : over(FILTER, mouseX, mouseY) ? "filter" : over(ACK, mouseX, mouseY) ? "ack"
                : over(CODE, mouseX, mouseY) ? "code" : mouseY - topPos < TABLE.y() + HEADER && over(TABLE, mouseX, mouseY) ? "sort" : null;
        if (tip != null) g.renderTooltip(font, font.split(Component.translatable(KEY + "tip." + tip), 200), mouseX, mouseY);
        if (zoneMode && overScope(mouseX, mouseY)) {
            g.drawString(font, distance(Math.hypot(worldX(mouseX) - viewX, worldZ(mouseY) - viewZ)), mouseX + 6, mouseY - 10, AMBER, false);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (code.isMouseOver(mx, my) || over(CODE, mx, my)) {
            setFocused(code);
            return true;
        }
        setFocused(null);
        if (button == 0) {
            if (over(ZOOM_OUT, mx, my)) return zoom(1, leftPos + PPI_CX, topPos + PPI_CY);
            if (over(ZOOM_IN, mx, my)) return zoom(-1, leftPos + PPI_CX, topPos + PPI_CY);
            if (over(CENTRE, mx, my)) {
                panned = false;
                viewX = menu.pos.getX() + 0.5;
                viewZ = menu.pos.getZ() + 0.5;
                return true;
            }
            if (over(ZONE, mx, my)) {
                zoneMode = !zoneMode;
                return true;
            }
            if (over(FILTER, mx, my)) {
                filter = (filter + 1) % 3;
                scroll = 0;
                return true;
            }
            if (over(ACK, mx, my)) {
                send(RadarConfigurePayload.ACKNOWLEDGE, "", 0, 0, 0);
                RadarSounds.acknowledged();
                return true;
            }
            if (over(TABLE, mx, my)) {
                double ly = my - topPos - TABLE.y();
                if (ly < HEADER) {
                    double lx = mx - leftPos - TABLE.x() - 2;
                    sort = lx < COL_PHASE ? 1 : lx >= COL_RANGE && lx < COL_ETA ? 2 : lx >= COL_ETA ? 0 : sort;
                    return true;
                }
                int row = (int) ((ly - HEADER - 1) / ROW);
                List<RadarTrack.Snapshot> tracks = shown();
                if (row >= 0 && row < TABLE_ROWS && scroll + row < tracks.size()) selected = tracks.get(scroll + row).id();
                return true;
            }
            if (overScope(mx, my)) {
                if (zoneMode) {
                    int radius = zoneRadius() > 0 ? zoneRadius() : (int) Math.max(50, range() / 8);
                    pendingZoneRadius = radius;
                    send(RadarConfigurePayload.SET_ZONE, "", (int) Math.floor(worldX(mx)), (int) Math.floor(worldZ(my)), radius);
                    return true;
                }
                RadarTrack.Snapshot nearest = null;
                double best = 8 * 8;
                for (RadarTrack.Snapshot track : menu.tracks()) {
                    double[] echo = painted.get(track.id());
                    if (echo == null) continue;
                    double dx = sx(echo[0]) - mx, dy = sy(echo[2]) - my;
                    if (dx * dx + dy * dy < best) {
                        best = dx * dx + dy * dy;
                        nearest = track;
                    }
                }
                if (nearest != null) selected = nearest.id();
                else dragging = true;
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
        if (dragging && button == 0) {
            viewX -= dx / PPI_RADIUS * range();
            viewZ -= dy / PPI_RADIUS * range();
            panned = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (overScope(mx, my)) {
            if (zoneMode && radar() != null) {
                int step = (int) Math.max(10, range() / 40);
                int radius = Math.max(0, Math.min(RadarBlockEntity.MAX_ZONE, zoneRadius() + (scrollY > 0 ? step : -step)));
                pendingZoneRadius = radius;
                send(RadarConfigurePayload.SET_ZONE, "", radar().zoneX(), radar().zoneZ(), radius);
                return true;
            }
            return zoom(scrollY > 0 ? -1 : 1, mx, my);
        }
        if (over(TABLE, mx, my)) {
            scroll += scrollY > 0 ? -1 : 1;
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    /** Steps the range out ({@code +1}) or in, keeping the point under ({@code mx, my}) where it is. */
    private boolean zoom(int direction, double mx, double my) {
        int next = Mth.clamp(rangeIndex + direction, 0, RANGES.length - 1);
        if (next == rangeIndex) return true;
        double wx = worldX(mx), wz = worldZ(my);
        rangeIndex = next;
        viewX = wx - (mx - leftPos - PPI_CX) / PPI_RADIUS * range();
        viewZ = wz - (my - topPos - PPI_CY) / PPI_RADIUS * range();
        panned = panned || Math.abs(mx - leftPos - PPI_CX) > 1 || Math.abs(my - topPos - PPI_CY) > 1;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (code.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                setFocused(null);
                return true;
            }
            return code.keyPressed(key, scan, modifiers) || true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        if (klaxon != null) Minecraft().getSoundManager().stop(klaxon);
    }

    private static net.minecraft.client.Minecraft Minecraft() {
        return net.minecraft.client.Minecraft.getInstance();
    }
}
