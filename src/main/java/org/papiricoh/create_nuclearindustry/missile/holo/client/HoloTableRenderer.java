package org.papiricoh.create_nuclearindustry.missile.holo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.controlroom.client.MonitorRenderTypes;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloProjection;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTable;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloView;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;
import org.papiricoh.create_nuclearindustry.missile.radar.client.RadarSounds;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.client.ClientTerrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The command table's hologram, drawn from its centre over all nine pieces ({@link HoloProjection}): the land, a faint grid and a
 * scan line; each flight as a small missile riding its arc (nose along its course, a flame while it boosts, a lone re-entry
 * vehicle once it falls), solid where the radars plotted it and dashed where it is still to fly; impact ellipses, the silo and
 * the launch sites as silos, the radars as turning antennas with their coverage, the silo's reach and target, a marked target
 * and rings where a weapon went off. Looking at the hologram from close by picks what is under the eye: it lights up and
 * gets a label; on bare glass a cursor shows the place. A pinned track gets a panel over the table. Coloured shapes go through
 * {@link MonitorRenderTypes#FLAT}, asked for just before each shape; the text goes last.
 */
public class HoloTableRenderer implements BlockEntityRenderer<HoloTableBlockEntity> {
    static final float MIN = HoloProjection.MIN, MAX = HoloProjection.MAX, SIZE = HoloProjection.SIZE, MID = HoloProjection.MID, Y = HoloProjection.Y;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    static final int CYAN = 0xFF6FF4FF, GREEN = 0xFF7DFF9A, RED = 0xFFFF4A3A, AMBER = 0xFFFFB347, WHITE = 0xFFE8FFFF;
    /** The missile icons' length, blocks: the same whatever the view. */
    static final float ICON = 0.17f;
    private static final String KEY = "holo_table.create_nuclearindustry.";

    /** What the eye was on last frame, to blip when it moves to something else. */
    @Nullable
    private static String lastHover;

    private final Font font;

    public HoloTableRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    /** Something under the eye. */
    private record Hover(String key, float x, float y, float z, List<Component> lines, int colour, @Nullable HoloTracksPayload.Track track) {
    }

    @Override
    public void render(HoloTableBlockEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !HoloTable.intact(mc.level, table.getBlockPos())) return;
        BlockPos centre = table.getBlockPos();
        HoloTracksPayload picture = ClientHolo.picture(centre);
        long millis = Util.getMillis();
        double time = mc.level.getGameTime() + partial;
        HoloProjection p = picture == null ? HoloProjection.of(HoloView.VIEWS[0], 0, 0, 50) : picture.projection();
        double since = picture == null ? 0 : Math.max(0, Math.min(20, time - picture.sample()));
        List<Runnable> texts = new ArrayList<>();
        pose.pushPose();
        Matrix4f m = pose.last().pose();
        boolean land = terrain(pose, buffers, centre, p);
        grid(m, buffers, land, millis);
        Hover hover = picture == null ? null : hover(table, p, picture, since, partial);
        if (picture != null) {
            radars(pose, buffers, p, picture, time, hover);
            silo(pose, buffers, p, picture, millis, hover);
            HoloTracksPayload.Track pinned = null;
            for (HoloTracksPayload.Track track : picture.tracks()) {
                boolean lit = hover != null && hover.track() == track;
                boolean held = track.id().equals(picture.board().selected());
                if (held) pinned = track;
                track(pose, buffers, p, picture, track, since, millis, lit || held);
            }
            defense(pose, buffers, p, picture, since, texts);
            mark(m, buffers, p, picture, centre, texts, pose);
            if (pinned != null) panel(pose, buffers, picture, pinned, since, texts);
        }
        flashes(m, buffers, p, centre, millis);
        String key = hover == null ? null : hover.key();
        if (key != null && !key.equals(lastHover) && !key.startsWith("glass")) RadarSounds.holoBlip();
        if (hover != null) lastHover = key;
        else if (lastHover != null && isLookingAt(table)) lastHover = null;
        if (hover != null) {
            Hover h = hover;
            ring(flat(buffers), m, h.x(), h.z(), h.track() == null && h.key().startsWith("glass") ? 0.04f : 0.07f, Y + 0.016f, 0.008f, WHITE,
                    h.key().startsWith("glass"));
            for (int i = 0; i < h.lines().size(); i++) {
                Component line = h.lines().get(i);
                float y = Math.max(h.y(), Y) + 0.3f + (h.lines().size() - 1 - i) * 0.12f;
                int colour = i == 0 ? h.colour() : WHITE;
                texts.add(() -> label(pose, buffers, h.x(), y, h.z(), line, colour));
            }
        }
        // Text last of all: no buffer of another kind is used after it.
        texts.forEach(Runnable::run);
        pose.popPose();
    }

    private boolean isLookingAt(HoloTableBlockEntity table) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.distanceToSqr(table.getBlockPos().getCenter()) < HoloTableBlockEntity.REACH * HoloTableBlockEntity.REACH;
    }

    // ------------------------------------------------------------------ shapes

    private static VertexConsumer flat(MultiBufferSource buffers) {
        return buffers.getBuffer(MonitorRenderTypes.FLAT);
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, float z, int argb) {
        v.addVertex(m, x, y, z).setColor(argb).setLight(LIGHT);
    }

    /** A quad seen from both sides. */
    private static void quad(VertexConsumer v, Matrix4f m, float[] a, float[] b, float[] c, float[] d, int ca, int cb) {
        vertex(v, m, a[0], a[1], a[2], ca);
        vertex(v, m, b[0], b[1], b[2], cb);
        vertex(v, m, c[0], c[1], c[2], cb);
        vertex(v, m, d[0], d[1], d[2], ca);
        vertex(v, m, d[0], d[1], d[2], ca);
        vertex(v, m, c[0], c[1], c[2], cb);
        vertex(v, m, b[0], b[1], b[2], cb);
        vertex(v, m, a[0], a[1], a[2], ca);
    }

    /** A triangle seen from both sides. */
    private static void tri(VertexConsumer v, Matrix4f m, float[] a, float[] b, float[] c, int ca, int cb) {
        quad(v, m, a, b, c, c, ca, cb);
    }

    /** A flat strip on the glass from (x0, z0) to (x1, z1). */
    private static void strip(VertexConsumer v, Matrix4f m, float x0, float z0, float x1, float z1, float y, float width, int c0, int c1) {
        float dx = x1 - x0, dz = z1 - z0, len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-5f) return;
        float nx = -dz / len * width / 2, nz = dx / len * width / 2;
        quad(v, m, new float[]{x0 + nx, y, z0 + nz}, new float[]{x1 + nx, y, z1 + nz}, new float[]{x1 - nx, y, z1 - nz}, new float[]{x0 - nx, y, z0 - nz}, c0, c1);
    }

    /** A ribbon in space: a flat strip and an upright one through the segment, so it shows from every side. */
    private static void ribbon(VertexConsumer v, Matrix4f m, float[] a, float[] b, float width, int ca, int cb) {
        float dx = b[0] - a[0], dz = b[2] - a[2], len = (float) Math.sqrt(dx * dx + dz * dz);
        float nx = len < 1e-5f ? width / 2 : -dz / len * width / 2, nz = len < 1e-5f ? 0 : dx / len * width / 2;
        quad(v, m, new float[]{a[0] + nx, a[1], a[2] + nz}, new float[]{b[0] + nx, b[1], b[2] + nz}, new float[]{b[0] - nx, b[1], b[2] - nz},
                new float[]{a[0] - nx, a[1], a[2] - nz}, ca, cb);
        float h = width / 2;
        quad(v, m, new float[]{a[0], a[1] + h, a[2]}, new float[]{b[0], b[1] + h, b[2]}, new float[]{b[0], b[1] - h, b[2]}, new float[]{a[0], a[1] - h, a[2]}, ca, cb);
    }

    /** A ribbon with a faint halo three times as wide round it. */
    private static void glow(VertexConsumer v, Matrix4f m, float[] a, float[] b, float width, int ca, int cb) {
        ribbon(v, m, a, b, width * 3, alpha(ca, 0.22f), alpha(cb, 0.22f));
        ribbon(v, m, a, b, width, ca, cb);
    }

    private static void ring(VertexConsumer v, Matrix4f m, float cx, float cz, float r, float y, float width, int argb, boolean dashed) {
        int segments = Math.max(24, Math.min(128, (int) (r * 120)));
        for (int i = 0; i < segments; i++) {
            if (dashed && (i & 1) == 1) continue;
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = cx + (float) Math.sin(a0) * r, z0 = cz - (float) Math.cos(a0) * r, x1 = cx + (float) Math.sin(a1) * r, z1 = cz - (float) Math.cos(a1) * r;
            if (!inGlass(x0, z0) && !inGlass(x1, z1)) continue;
            strip(v, m, clamp(x0), clamp(z0), clamp(x1), clamp(z1), y, width, argb, argb);
        }
    }

    private static boolean inGlass(float x, float z) {
        return HoloProjection.onGlass(x, z);
    }

    private static float clamp(float v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    static int alpha(int argb, float a) {
        return (argb & 0xFFFFFF) | Math.max(0, Math.min(255, (int) ((argb >>> 24) * a))) << 24;
    }

    // ------------------------------------------------------------------ icons

    /**
     * A small missile, its axis along {@code course} (in table space), centred at {@code at}: a square body, a pointed nose, four
     * fins and, while it boosts, a flickering flame. A re-entry vehicle is a lone cone.
     */
    private static void missile(PoseStack pose, MultiBufferSource buffers, float[] at, V3 course, double heading, int colour, boolean lit,
                                boolean boost, boolean reentry, long millis) {
        RocketAttitude.Quat q = RocketAttitude.orientation(course, heading);
        pose.pushPose();
        pose.translate(at[0], at[1], at[2]);
        pose.mulPose(new Quaternionf((float) q.x(), (float) q.y(), (float) q.z(), (float) q.w()));
        float l = lit ? ICON * 1.25f : ICON;
        pose.translate(0, -l / 2, 0);
        Matrix4f m = pose.last().pose();
        int body = lit ? WHITE : colour, dark = alpha(lit ? colour : colour, 0.75f);
        VertexConsumer v = flat(buffers);
        if (reentry) {
            float r = l * 0.18f, h = l * 0.6f;
            cone(v, m, 0, l * 0.2f, r, h, 6, dark, body);
        } else {
            float w = l * 0.075f, top = l * 0.68f;
            float[][] c = {{-w, 0, -w}, {w, 0, -w}, {w, 0, w}, {-w, 0, w}};
            for (int i = 0; i < 4; i++) {
                float[] a = c[i], b = c[(i + 1) & 3];
                quad(v, m, a, b, new float[]{b[0], top, b[2]}, new float[]{a[0], top, a[2]}, dark, body);
            }
            cone(v, m, 0, top, w * 1.25f, l - top, 4, body, WHITE);
            // Four fins at the tail.
            float fin = l * 0.2f, finTop = l * 0.24f;
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.PI / 4;
                float sx = (float) Math.sin(a), sz = (float) Math.cos(a);
                tri(v, m, new float[]{sx * w, 0, sz * w}, new float[]{sx * (w + fin), -l * 0.04f, sz * (w + fin)}, new float[]{sx * w, finTop, sz * w}, dark, body);
            }
            if (boost) {
                float flicker = 0.75f + 0.25f * (float) Math.sin(millis / 37.0) * (float) Math.cos(millis / 23.0);
                float len = l * 0.55f * flicker;
                for (int i = 0; i < 2; i++) {
                    float sx = i == 0 ? w * 1.4f : 0, sz = i == 0 ? 0 : w * 1.4f;
                    quad(v, m, new float[]{-sx, 0, -sz}, new float[]{-sx * 0.2f, -len, -sz * 0.2f}, new float[]{sx * 0.2f, -len, sz * 0.2f},
                            new float[]{sx, 0, sz}, 0xF0FFE6A0, 0x00FF7A20);
                }
            }
        }
        if (lit) {
            // A soft shell round it.
            float r = l * 0.7f;
            VertexConsumer g = flat(buffers);
            quad(g, m, new float[]{-r, l / 2, 0}, new float[]{0, l / 2 + r, 0}, new float[]{r, l / 2, 0}, new float[]{0, l / 2 - r, 0}, alpha(WHITE, 0.12f), alpha(colour, 0.05f));
        }
        pose.popPose();
    }

    /** A cone (or pyramid of {@code sides}) standing on y = {@code base}, radius {@code r}, height {@code h}. */
    private static void cone(VertexConsumer v, Matrix4f m, float x, float base, float r, float h, int sides, int foot, int tip) {
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2 * i / sides + Math.PI / sides, a1 = Math.PI * 2 * (i + 1) / sides + Math.PI / sides;
            tri(v, m, new float[]{x + (float) Math.sin(a0) * r, base, (float) Math.cos(a0) * r}, new float[]{x + (float) Math.sin(a1) * r, base, (float) Math.cos(a1) * r},
                    new float[]{x, base + h, 0}, foot, tip);
        }
    }

    /** A silo: an open square tube with a nose peeping out, standing on the glass. */
    private static void siloIcon(VertexConsumer v, Matrix4f m, float x, float z, float size, int colour, boolean lit) {
        float w = size / 2, h = size * 0.8f, y = Y + 0.005f;
        int c = lit ? WHITE : colour, base = alpha(colour, 0.5f);
        float[][] k = {{x - w, z - w}, {x + w, z - w}, {x + w, z + w}, {x - w, z + w}};
        for (int i = 0; i < 4; i++) {
            float[] a = k[i], b = k[(i + 1) & 3];
            quad(v, m, new float[]{a[0], y, a[1]}, new float[]{b[0], y, b[1]}, new float[]{b[0], y + h, b[1]}, new float[]{a[0], y + h, a[1]}, base, c);
        }
        Matrix4f at = new Matrix4f(m).translate(x, 0, z);
        cone(v, at, 0, y + h * 0.5f, w * 0.45f, h * 0.9f, 4, base, c);
    }

    /** A radar: a mast and a dish turning with its sweep. */
    private static void radarIcon(VertexConsumer v, Matrix4f m, float x, float z, double bearing, int colour, boolean lit) {
        float y = Y + 0.005f, mast = 0.09f;
        int c = lit ? WHITE : colour, base = alpha(colour, 0.5f);
        quad(v, m, new float[]{x - 0.006f, y, z}, new float[]{x - 0.004f, y + mast, z}, new float[]{x + 0.004f, y + mast, z}, new float[]{x + 0.006f, y, z}, base, c);
        quad(v, m, new float[]{x, y, z - 0.006f}, new float[]{x, y + mast, z - 0.004f}, new float[]{x, y + mast, z + 0.004f}, new float[]{x, y, z + 0.006f}, base, c);
        float sin = (float) Math.sin(Math.toRadians(bearing)), cos = (float) -Math.cos(Math.toRadians(bearing));
        // The dish: a curved reflector facing the bearing, across it.
        float ax = cos, az = -sin, half = 0.045f, top = y + mast;
        for (int i = -2; i < 2; i++) {
            float t0 = (float) i / 2, t1 = (float) (i + 1) / 2;
            float d0 = 0.012f * (1 - t0 * t0), d1 = 0.012f * (1 - t1 * t1);
            quad(v, m, new float[]{x + ax * half * t0 - sin * d0, top - 0.02f, z + az * half * t0 - cos * d0},
                    new float[]{x + ax * half * t1 - sin * d1, top - 0.02f, z + az * half * t1 - cos * d1},
                    new float[]{x + ax * half * t1 - sin * d1, top + 0.025f, z + az * half * t1 - cos * d1},
                    new float[]{x + ax * half * t0 - sin * d0, top + 0.025f, z + az * half * t0 - cos * d0}, c, c);
        }
    }

    // ------------------------------------------------------------------ layers

    private boolean terrain(PoseStack pose, MultiBufferSource buffers, BlockPos centre, HoloProjection p) {
        int lod = HoloView.lod(p.view());
        double half = p.view() / 2.0;
        double cx = centre.getX() + 0.5, cz = centre.getZ() + 0.5;
        double x0 = cx + p.ox() - half, z0 = cz + p.oz() - half, x1 = cx + p.ox() + half, z1 = cz + p.oz() + half;
        long[] keys = TerrainTile.covering(lod, (int) Math.floor(x0), (int) Math.floor(z0), (int) Math.ceil(x1), (int) Math.ceil(z1));
        ClientTerrain.want(centre, keys);
        // The next coarser level too: far fewer tiles, quicker to survey, shown wherever the finer one has not come yet.
        int coarse = Math.min(TerrainTile.LODS - 1, lod + 1);
        if (coarse != lod)
            ClientTerrain.want(centre, TerrainTile.covering(coarse, (int) Math.floor(x0), (int) Math.floor(z0), (int) Math.ceil(x1), (int) Math.ceil(z1)));
        boolean any = false;
        int fine = TerrainTile.span(lod);
        for (long wanted : keys) {
            double fx = TerrainTile.origin(lod, TerrainTile.tx(wanted)), fz = TerrainTile.origin(lod, TerrainTile.tz(wanted));
            double ax = Math.max(fx, x0), az = Math.max(fz, z0), bx = Math.min(fx + fine, x1), bz = Math.min(fz + fine, z1);
            if (ax >= bx || az >= bz) continue;
            long key = wanted;
            ResourceLocation texture = ClientTerrain.texture(key);
            if (texture == null && coarse != lod) {
                key = TerrainTile.key(coarse, TerrainTile.tileOf(coarse, (int) Math.floor(fx)), TerrainTile.tileOf(coarse, (int) Math.floor(fz)));
                texture = ClientTerrain.texture(key);
            }
            if (texture == null) continue;
            any = true;
            int tileLod = TerrainTile.lod(key), span = TerrainTile.span(tileLod);
            double ox = TerrainTile.origin(tileLod, TerrainTile.tx(key)), oz = TerrainTile.origin(tileLod, TerrainTile.tz(key));
            float u0 = (float) ((ax - ox) / span), v0 = (float) ((az - oz) / span), u1 = (float) ((bx - ox) / span), v1 = (float) ((bz - oz) / span);
            VertexConsumer v = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
            Matrix4f m = pose.last().pose();
            float lx0 = p.x(ax - cx), lz0 = p.z(az - cz), lx1 = p.x(bx - cx), lz1 = p.z(bz - cz);
            int colour = 0xD8FFFFFF;
            // Just over the glass's own face, so the two never fight.
            float y = Y + 0.003f;
            textured(v, pose, m, lx0, y, lz0, u0, v0, colour);
            textured(v, pose, m, lx0, y, lz1, u0, v1, colour);
            textured(v, pose, m, lx1, y, lz1, u1, v1, colour);
            textured(v, pose, m, lx1, y, lz0, u1, v0, colour);
        }
        return any;
    }

    private static void textured(VertexConsumer v, PoseStack pose, Matrix4f m, float x, float y, float z, float u, float vv, int argb) {
        v.addVertex(m, x, y, z).setColor(argb).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(pose.last(), 0, 1, 0);
    }

    private void grid(Matrix4f m, MultiBufferSource buffers, boolean land, long millis) {
        VertexConsumer v = flat(buffers);
        int alpha = land ? 0x38 : (int) (0x38 + 0x28 * Math.sin(millis / 160.0));
        int colour = alpha << 24 | 0x40E0FF;
        for (int i = 0; i <= 8; i++) {
            float t = MIN + SIZE * i / 8;
            strip(v, m, t, MIN, t, MAX, Y + 0.006f, 0.006f, colour, colour);
            strip(v, m, MIN, t, MAX, t, Y + 0.006f, 0.006f, colour, colour);
        }
        // The scan line sweeping the glass, a fading band behind it.
        float f = (millis % 5000) / 5000f, z = MIN + f * SIZE;
        v = flat(buffers);
        for (int i = 0; i < 6; i++) {
            float z1 = z - i * 0.04f, z0 = z1 - 0.04f;
            if (z0 < MIN) break;
            int a = (int) (0x38 * (1 - i / 6f));
            strip(v, m, MIN, (z0 + z1) / 2, MAX, (z0 + z1) / 2, Y + 0.007f, 0.04f, a << 24 | 0x6FF4FF, a << 24 | 0x6FF4FF);
        }
        // The table itself, at the centre.
        ring(flat(buffers), m, MID, MID, 0.05f, Y + 0.008f, 0.012f, WHITE, false);
    }

    private void radars(PoseStack pose, MultiBufferSource buffers, HoloProjection p, HoloTracksPayload picture, double time, @Nullable Hover hover) {
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < picture.radars().size(); i++) {
            HoloTracksPayload.Radar radar = picture.radars().get(i);
            float x = p.x(radar.dx() + 0.5), z = p.z(radar.dz() + 0.5);
            int colour = radar.online() ? GREEN : 0xFF607060;
            boolean lit = hover != null && hover.key().equals("radar" + i);
            if (inGlass(x, z)) radarIcon(flat(buffers), m, x, z, radar.online() ? (time * 6) % 360 : 0, colour, lit);
            if (radar.reach() > 0) ring(flat(buffers), m, x, z, radar.reach() * p.scale(), Y + 0.009f, lit ? 0.012f : 0.008f, alpha(colour, lit ? 0.9f : 0.5f), true);
        }
    }

    private void silo(PoseStack pose, MultiBufferSource buffers, HoloProjection p, HoloTracksPayload picture, long millis, @Nullable Hover hover) {
        Matrix4f m = pose.last().pose();
        // Where the tracks came from, as small silos.
        List<float[]> sites = new ArrayList<>();
        for (HoloTracksPayload.Track t : picture.tracks()) {
            float fx = p.x(t.fx()), fz = p.z(t.fz());
            boolean known = false;
            for (float[] s : sites) known |= Math.abs(s[0] - fx) < 0.03f && Math.abs(s[1] - fz) < 0.03f;
            if (!known && inGlass(fx, fz)) sites.add(new float[]{fx, fz});
        }
        HoloTracksPayload.Silo selectedSilo = picture.silo();
        float sx = selectedSilo == null ? 0 : p.x(selectedSilo.dx() + 0.5), sz = selectedSilo == null ? 0 : p.z(selectedSilo.dz() + 0.5);
        for (float[] s : sites) {
            if (selectedSilo != null && Math.abs(s[0] - sx) < 0.03f && Math.abs(s[1] - sz) < 0.03f) continue;
            siloIcon(flat(buffers), m, s[0], s[1], 0.045f, alpha(GREEN, 0.85f), false);
        }
        for (HoloTracksPayload.Silo silo : picture.silos()) {
            if (!silo.loaded()) continue;
            sx = p.x(silo.dx() + 0.5); sz = p.z(silo.dz() + 0.5);
            boolean lit = hover != null && hover.key().equals("silo" + silo.index());
            VertexConsumer v = flat(buffers);
            if (inGlass(sx, sz)) siloIcon(v, m, sx, sz, 0.06f, AMBER, lit);
            if (silo.index() == picture.board().selectedSilo() && silo.reach() > 0) ring(flat(buffers), m, sx, sz, silo.reach() * p.scale(), Y + 0.011f, lit ? 0.014f : 0.01f, alpha(AMBER, lit ? 0.95f : 0.6f), false);
            if (silo.targeted()) {
                float tx = p.x(silo.tx() + 0.5), tz = p.z(silo.tz() + 0.5);
                if (inGlass(tx, tz)) {
                    float pulse = (millis % 1200) / 1200f;
                    v = flat(buffers);
                    ring(v, m, tx, tz, 0.03f + pulse * 0.08f, Y + 0.012f, 0.01f, alpha(RED, 1 - pulse), false);
                    strip(v, m, tx - 0.04f, tz - 0.04f, tx + 0.04f, tz + 0.04f, Y + 0.012f, 0.012f, RED, RED);
                    strip(v, m, tx - 0.04f, tz + 0.04f, tx + 0.04f, tz - 0.04f, Y + 0.012f, 0.012f, RED, RED);
                    if (silo.cep() > 0) ring(v, m, tx, tz, Math.max(0.01f, silo.cep() * p.scale()), Y + 0.012f, 0.006f, alpha(RED, 0.6f), true);
                    v = flat(buffers);
                    int dashes = 24;
                    for (int i = 0; i < dashes; i += 2) {
                        float a = (float) i / dashes, b = (float) (i + 1) / dashes;
                        if (!inGlass(sx + (tx - sx) * a, sz + (tz - sz) * a)) continue;
                        strip(v, m, clamp(sx + (tx - sx) * a), clamp(sz + (tz - sz) * a), clamp(sx + (tx - sx) * b), clamp(sz + (tz - sz) * b), Y + 0.011f, 0.008f,
                                alpha(RED, 0.7f), alpha(RED, 0.7f));
                    }
                }
            }
        }
    }

    private void defense(PoseStack pose, MultiBufferSource buffers, HoloProjection p, HoloTracksPayload picture, double since, List<Runnable> texts) {
        Matrix4f m = pose.last().pose();
        for (var shooter : picture.shooters()) {
            if (shooter.type() != 0 || !p.shows(shooter.dx() + 0.5, shooter.dz() + 0.5)) continue;
            float x = p.x(shooter.dx() + 0.5), z = p.z(shooter.dz() + 0.5);
            int colour = shooter.loaded() ? GREEN : AMBER;
            ring(flat(buffers), m, x, z, shooter.range() * SIZE / p.view(), Y + 0.012f, 0.006f, alpha(colour, 0.3f), true);
            siloIcon(flat(buffers), m, x, z, 0.12f, colour, false);
            strip(flat(buffers), m, x - 0.08f, z, x + 0.08f, z, Y + 0.1f, 0.035f, colour, colour);
        }
        for (var intercept : picture.intercepts()) {
            double x = intercept.x() + intercept.vx() * since, z = intercept.z() + intercept.vz() * since;
            if (!p.shows(x, z)) continue;
            float[] at = {p.x(x), p.y(intercept.y() + intercept.vy() * since), p.z(z)};
            float[] point = {clamp(p.x(intercept.px())), p.y(intercept.py()), clamp(p.z(intercept.pz()))};
            ribbon(flat(buffers), m, new float[]{at[0] - 0.025f, at[1], at[2]}, new float[]{at[0] + 0.025f, at[1], at[2]}, 0.04f, GREEN, WHITE);
            for (int i = 0; i < 24; i += 2) {
                float a = i / 24f, b = (i + 1) / 24f;
                float[] from = new float[3], to = new float[3];
                for (int j = 0; j < 3; j++) { from[j] = at[j] + (point[j] - at[j]) * a; to[j] = at[j] + (point[j] - at[j]) * b; }
                ribbon(flat(buffers), m, from, to, 0.008f, GREEN, GREEN);
            }
            for (int direction : new int[]{-1, 1})
                ribbon(flat(buffers), m, new float[]{point[0] - 0.04f, point[1], point[2] - direction * 0.04f},
                        new float[]{point[0] + 0.04f, point[1], point[2] + direction * 0.04f}, 0.012f, AMBER, AMBER);
            texts.add(() -> label(pose, buffers, point[0], point[1] + 0.12f, point[2], Component.translatable(KEY + "screen.defense.pk", Math.round(intercept.pk() * 100)), AMBER));
        }
        for (var track : picture.tracks()) {
            long count = picture.intercepts().stream().filter(i -> i.target().equals(track.id())).count();
            if (count == 0 || !p.shows(track.x(), track.z())) continue;
            texts.add(() -> label(pose, buffers, p.x(track.x()), p.y(track.y()) + 0.15f, p.z(track.z()), Component.literal("×" + count), AMBER));
        }
    }

    private void track(PoseStack pose, MultiBufferSource buffers, HoloProjection p, HoloTracksPayload picture, HoloTracksPayload.Track t, double since,
                       long millis, boolean lit) {
        Matrix4f m = pose.last().pose();
        int colour = picture.intercepts().stream().anyMatch(i -> i.target().equals(t.id())) ? AMBER : t.own() ? GREEN : t.threat() ? RED : CYAN;
        float g = Math.max(1e-5f, picture.gravity());
        double ground = Math.min(RadarModel.SEA_LEVEL, t.fy());
        double[] at = t.at(since, g);
        // Where it has been: the plotted history up to now, solid.
        float[] h = t.history();
        VertexConsumer v = flat(buffers);
        float[] last = null;
        int points = h.length / 3;
        float width = lit ? 0.026f : 0.018f;
        for (int i = 0; i < points; i++) {
            float[] here = {p.x(h[i * 3]), p.y(h[i * 3 + 1]), p.z(h[i * 3 + 2])};
            if (last != null && inGlass(here[0], here[2]) && inGlass(last[0], last[2]))
                glow(v, m, last, here, width, alpha(colour, 0.25f + 0.6f * i / points), alpha(colour, 0.25f + 0.6f * (i + 1) / points));
            last = here;
        }
        float[] now = {p.x(at[0]), p.y(at[1]), p.z(at[2])};
        if (last != null && inGlass(now[0], now[2])) glow(v, m, last, now, width, alpha(colour, 0.85f), alpha(colour, 0.85f));
        // Where it goes: the free-fall arc to the predicted impact, dashed.
        double vy = t.vy() - g * since;
        double fall = RadarModel.timeToGround(new V3(at[0], at[1], at[2]), new V3(t.vx(), vy, t.vz()), g, ground);
        if (!Double.isFinite(fall)) fall = 0;
        v = flat(buffers);
        int steps = 32;
        float[] prev = now;
        int future = lit ? WHITE : t.threat() ? RED : AMBER;
        for (int i = 1; i <= steps; i++) {
            double s = fall * i / steps;
            float[] next = {p.x(at[0] + t.vx() * s), p.y(at[1] + vy * s - 0.5 * g * s * s), p.z(at[2] + t.vz() * s)};
            if ((i & 1) == 1 && inGlass(prev[0], prev[2]) && inGlass(next[0], next[2]))
                glow(v, m, prev, next, lit ? 0.018f : 0.012f, alpha(future, 0.75f), alpha(future, 0.75f));
            prev = next;
        }
        // Its impact on the glass.
        float ix = p.x(t.ix()), iz = p.z(t.iz());
        if (inGlass(ix, iz)) {
            float a = (float) Math.max(0.014, t.along() * p.scale()), b = (float) Math.max(0.01, t.across() * p.scale());
            int edge = lit ? WHITE : t.threat() ? RED : AMBER;
            ellipse(flat(buffers), m, ix, iz, a, b, t.heading(), alpha(edge, 0.9f), alpha(t.threat() ? RED : AMBER, lit ? 0.3f : 0.16f), lit ? 0.012f : 0.007f);
        }
        // The missile itself, riding the arc, nose along its course as drawn; a drop line to its shadow on the glass.
        if (inGlass(now[0], now[2])) {
            v = flat(buffers);
            quad(v, m, new float[]{now[0], Y + 0.01f, now[2]}, new float[]{now[0], now[1], now[2]}, new float[]{now[0] + 0.002f, now[1], now[2]},
                    new float[]{now[0] + 0.002f, Y + 0.01f, now[2]}, alpha(colour, 0.1f), alpha(colour, 0.45f));
            ring(v, m, now[0], now[2], 0.018f, Y + 0.011f, 0.006f, alpha(colour, 0.6f), false);
            V3 course = new V3(t.vx() * p.scale(), vy * p.heightScale(), t.vz() * p.scale());
            byte phase = t.phase();
            missile(pose, buffers, now, course, t.heading(), colour, lit, phase == 0, phase >= 2, millis);
        }
    }

    private static void ellipse(VertexConsumer v, Matrix4f m, float cx, float cz, float a, float b, double heading, int edge, int fill, float width) {
        float sin = (float) Math.sin(heading), cos = (float) Math.cos(heading);
        int segments = 32;
        float px = 0, pz = 0;
        for (int i = 0; i <= segments; i++) {
            double t = Math.PI * 2 * i / segments;
            float u = (float) Math.cos(t) * a, w = (float) Math.sin(t) * b;
            // u along the heading (world direction (sin, cos)), w across it (cos, -sin).
            float x = cx + u * sin + w * cos, z = cz + u * cos - w * sin;
            if (i > 0 && inGlass(x, z) && inGlass(px, pz)) {
                quad(v, m, new float[]{cx, Y + 0.01f, cz}, new float[]{px, Y + 0.01f, pz}, new float[]{x, Y + 0.01f, z}, new float[]{x, Y + 0.01f, z}, fill, fill);
                strip(v, m, px, pz, x, z, Y + 0.011f, width, edge, edge);
            }
            px = x;
            pz = z;
        }
    }

    /** A target marked on the glass, green if the silo can take it, red if not, waiting for its confirming click. */
    private void mark(Matrix4f m, MultiBufferSource buffers, HoloProjection p, HoloTracksPayload picture, BlockPos centre, List<Runnable> texts, PoseStack pose) {
        HoloTracksPayload.Mark mark = picture.board().mark();
        if (mark == null || Minecraft.getInstance().level == null) return;
        long age = Minecraft.getInstance().level.getGameTime() - mark.at();
        if (age > HoloTableBlockEntity.MARK_TICKS || age < 0) return;
        float x = p.x(mark.dx()), z = p.z(mark.dz());
        if (!inGlass(x, z)) return;
        boolean ok = mark.result() == 0;
        int colour = ok ? GREEN : RED;
        float fade = 1 - (float) age / HoloTableBlockEntity.MARK_TICKS * 0.6f;
        VertexConsumer v = flat(buffers);
        float pulse = (Util.getMillis() % 800) / 800f;
        ring(v, m, x, z, 0.045f, Y + 0.014f, 0.012f, alpha(colour, fade), false);
        ring(v, m, x, z, 0.045f + pulse * 0.06f, Y + 0.014f, 0.008f, alpha(colour, (1 - pulse) * fade), false);
        strip(v, m, x - 0.06f, z, x + 0.06f, z, Y + 0.015f, 0.008f, colour, colour);
        strip(v, m, x, z - 0.06f, x, z + 0.06f, Y + 0.015f, 0.008f, colour, colour);
        quad(v, m, new float[]{x, Y, z}, new float[]{x, Y + 0.35f, z}, new float[]{x + 0.003f, Y + 0.35f, z}, new float[]{x + 0.003f, Y, z},
                alpha(colour, 0.9f), alpha(colour, 0));
        HoloTableBlockEntity.Designation[] all = HoloTableBlockEntity.Designation.values();
        HoloTableBlockEntity.Designation result = all[Math.max(0, Math.min(all.length - 1, mark.result()))];
        int wx = centre.getX() + mark.dx(), wz = centre.getZ() + mark.dz();
        Component first = Component.literal(wx + ", " + wz);
        Component second = ok ? Component.translatable(KEY + "label.mark_ok")
                : Component.translatable(KEY + "designate." + result.name().toLowerCase(Locale.ROOT), wx, wz);
        texts.add(() -> label(pose, buffers, x, Y + 0.48f, z, first, colour));
        texts.add(() -> label(pose, buffers, x, Y + 0.38f, z, second, ok ? WHITE : RED));
    }

    private void flashes(Matrix4f m, MultiBufferSource buffers, HoloProjection p, BlockPos centre, long millis) {
        for (ClientHolo.Flash flash : ClientHolo.flashes()) {
            float x = p.x(flash.x() - centre.getX() - 0.5), z = p.z(flash.z() - centre.getZ() - 0.5);
            if (!inGlass(x, z)) continue;
            float age = (millis - flash.at()) / (float) ClientHolo.FLASH_MILLIS;
            float r = Math.max(0.03f, flash.radius() * p.scale());
            VertexConsumer v = flat(buffers);
            for (int k = 0; k < 3; k++) {
                float wave = (age * 3 + k * 0.33f) % 1;
                ring(v, m, x, z, r * (0.3f + wave * 2.2f), Y + 0.014f, 0.016f, alpha(0xFFFFF0C0, (1 - wave) * (1 - age)), false);
            }
            // A pillar of light the first moments.
            float glow = Math.max(0, 1 - age * 3);
            if (glow > 0) quad(v, m, new float[]{x - 0.03f, Y, z}, new float[]{x - 0.01f, Y + 1.2f, z}, new float[]{x + 0.01f, Y + 1.2f, z},
                    new float[]{x + 0.03f, Y, z}, alpha(0xFFFFFFE0, glow), alpha(0xFFFFB347, 0));
        }
    }

    // ------------------------------------------------------------------ what the eye is on

    /**
     * What the player's eye is on, if they stand at the table: the nearest missile, silo, launch site or radar along their look
     * ({@link HoloProjection#pick}, as the server does for clicks), else the place on the glass under it.
     */
    @Nullable
    private Hover hover(HoloTableBlockEntity table, HoloProjection p, HoloTracksPayload picture, double since, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !isLookingAt(table)) return null;
        Camera camera = mc.gameRenderer.getMainCamera();
        BlockPos c = table.getBlockPos();
        Vec3 eyeWorld = camera.getPosition();
        Vec3 look = mc.player.getViewVector(partial);
        double[] eye = {eyeWorld.x - c.getX(), eyeWorld.y - c.getY(), eyeWorld.z - c.getZ()}, dir = {look.x, look.y, look.z};
        double[] marks = picture.marks(p, since);
        int picked = HoloProjection.pick(eye, dir, marks, HoloTableBlockEntity.PICK);
        if (picked >= 0) {
            HoloTracksPayload.Track t = picture.tracks().get(picked);
            double[] at = t.at(since, picture.gravity());
            double vy = t.vy() - picture.gravity() * since;
            double speed = Math.sqrt(t.vx() * t.vx() + vy * vy + t.vz() * t.vz()) * 20;
            double eta = Math.max(0, (t.eta() - since) / 20);
            List<Component> lines = List.of(
                    Component.literal(t.designation() + " · ").append(Component.translatable("radar.create_nuclearindustry.phase." + phase(t.phase()))),
                    Component.translatable(KEY + "label.altitude", Math.round(at[1])),
                    Component.translatable(KEY + "label.speed", Math.round(speed), Math.round(eta)));
            return new Hover(t.id().toString(), (float) marks[picked * 3], (float) marks[picked * 3 + 1], (float) marks[picked * 3 + 2], lines,
                    t.own() ? GREEN : t.threat() ? RED : CYAN, t);
        }
        // The silo, the radars and the launch sites, each a point a little over the glass.
        List<double[]> spots = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (HoloTracksPayload.Silo silo : picture.silos()) if (silo.loaded() && p.shows(silo.dx() + 0.5, silo.dz() + 0.5)) {
            spots.add(new double[]{p.x(silo.dx() + 0.5), Y + 0.03, p.z(silo.dz() + 0.5)});
            keys.add("silo" + silo.index());
        }
        for (int i = 0; i < picture.radars().size(); i++) {
            HoloTracksPayload.Radar r = picture.radars().get(i);
            if (!p.shows(r.dx() + 0.5, r.dz() + 0.5)) continue;
            spots.add(new double[]{p.x(r.dx() + 0.5), Y + 0.06, p.z(r.dz() + 0.5)});
            keys.add("radar" + i);
        }
        for (int i = 0; i < picture.tracks().size(); i++) {
            HoloTracksPayload.Track t = picture.tracks().get(i);
            if (!p.shows(t.fx(), t.fz())) continue;
            spots.add(new double[]{p.x(t.fx()), Y + 0.02, p.z(t.fz())});
            keys.add("site" + i);
        }
        double[] flatSpots = new double[spots.size() * 3];
        for (int i = 0; i < spots.size(); i++) System.arraycopy(spots.get(i), 0, flatSpots, i * 3, 3);
        int spot = HoloProjection.pick(eye, dir, flatSpots, HoloTableBlockEntity.PICK * 0.8);
        if (spot >= 0) {
            String key = keys.get(spot);
            float x = (float) flatSpots[spot * 3], y = (float) flatSpots[spot * 3 + 1], z = (float) flatSpots[spot * 3 + 2];
            if (key.startsWith("silo")) {
                HoloTracksPayload.Silo silo = picture.silos().get(Integer.parseInt(key.substring(4)));
                SiloControllerEntity.State[] states = SiloControllerEntity.State.values();
                String state = states[Math.max(0, Math.min(states.length - 1, silo.state()))].name().toLowerCase(Locale.ROOT);
                List<Component> lines = silo.reach() > 0
                        ? List.of(Component.translatable(KEY + "label.silo", Component.translatable("silo.create_nuclearindustry.state." + state)),
                        Component.translatable(KEY + "label.silo_reach", Math.round(silo.reach()), Math.round(silo.cep())))
                        : List.of(Component.translatable(KEY + "label.silo", Component.translatable("silo.create_nuclearindustry.state." + state)));
                return new Hover(key, x, y, z, lines, AMBER, null);
            }
            if (key.startsWith("radar")) {
                HoloTracksPayload.Radar r = picture.radars().get(Integer.parseInt(key.substring(5)));
                Component code = r.code().isEmpty() ? Component.translatable(KEY + "label.no_code") : Component.literal(r.code());
                Component second = !r.online() ? Component.translatable(KEY + "label.radar_off")
                        : Component.translatable(KEY + "label.radar_reach", Math.round(r.reach()));
                return new Hover(key, x, y, z, List.of(Component.translatable(KEY + "label.radar", code), second), r.online() ? GREEN : RED, null);
            }
            HoloTracksPayload.Track t = picture.tracks().get(Integer.parseInt(key.substring(4)));
            return new Hover(key, x, y, z, List.of(Component.translatable(KEY + "label.launch_site", t.designation()),
                    Component.literal(Math.round(c.getX() + 0.5 + t.fx()) + ", " + Math.round(c.getZ() + 0.5 + t.fz()))), GREEN, null);
        }
        double[] onGlass = HoloProjection.onGlassPlane(eye, dir);
        if (onGlass == null || !HoloProjection.onGlass(onGlass[0], onGlass[2])) return null;
        int wx = (int) Math.floor(c.getX() + 0.5 + p.worldDx(onGlass[0])), wz = (int) Math.floor(c.getZ() + 0.5 + p.worldDz(onGlass[2]));
        return new Hover("glass", (float) onGlass[0], Y, (float) onGlass[2], List.of(Component.literal(wx + ", " + wz)), WHITE, null);
    }

    /** The pinned track's panel, standing over the table. */
    private void panel(PoseStack pose, MultiBufferSource buffers, HoloTracksPayload picture, HoloTracksPayload.Track t, double since, List<Runnable> texts) {
        BlockPos c = picture.centre();
        double[] at = t.at(since, picture.gravity());
        double vy = t.vy() - picture.gravity() * since;
        double speed = Math.sqrt(t.vx() * t.vx() + vy * vy + t.vz() * t.vz()) * 20;
        double eta = Math.max(0, (t.eta() - since) / 20);
        List<Component> lines = List.of(
                Component.translatable(KEY + "label.pinned").append(" " + t.designation() + " · ")
                        .append(Component.translatable("radar.create_nuclearindustry.phase." + phase(t.phase()))),
                Component.translatable(KEY + "label.altitude", Math.round(at[1])),
                Component.translatable(KEY + "label.speed", Math.round(speed), Math.round(eta)),
                Component.translatable(KEY + "label.impact", Math.round(c.getX() + 0.5 + t.ix()), Math.round(c.getZ() + 0.5 + t.iz())));
        int colour = t.own() ? GREEN : t.threat() ? RED : CYAN;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            float y = Y + 2.05f - i * 0.13f;
            int lineColour = i == 0 ? colour : WHITE;
            texts.add(() -> label(pose, buffers, MID, y, MID, line, lineColour, 0.016f));
        }
    }

    private static String phase(byte ordinal) {
        String[] names = {"boost", "midcourse", "reentry", "terminal", "impact"};
        return names[Math.max(0, Math.min(names.length - 1, ordinal))];
    }

    private void label(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, Component text, int argb) {
        label(pose, buffers, x, y, z, text, argb, 0.015f);
    }

    private void label(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, Component text, int argb, float scale) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(scale, -scale, scale);
        float w = -font.width(text) / 2f;
        // As a name tag: faintly through whatever is in front, fully where nothing is.
        font.drawInBatch(text, w, 0, alpha(argb, 0.35f), false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x60081418, LIGHT);
        font.drawInBatch(text, w, 0, argb, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LIGHT);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HoloTableBlockEntity table) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return (int) HoloTableBlockEntity.SIGHT * 2;
    }

    @Override
    public AABB getRenderBoundingBox(HoloTableBlockEntity table) {
        return table.renderBox();
    }
}
