package org.papiricoh.create_nuclearindustry.dyson.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.fx.DysonRenderTypes;
import org.papiricoh.create_nuclearindustry.dyson.client.fx.LocalQuads;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.receiver.ReceiverStatus;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a ray receiver: its mount and dish turned to the sun while the sun is up (never lower than {@link #MIN_ELEVATION}, where
 * the dish would cut into its plinth), parked half raised otherwise, and its focus lit by what it takes. While it takes power, the
 * swarm's beam comes down the sun's line into the focus: a white-gold core in a soft halo, as bright as its share, with packets of
 * light running down it and a shimmer round the focus. Its first light flares. The beam is long and seen from far off, so this
 * renders off screen and far away; {@link ClientConfig#dysonBeams} turns it off.
 */
public class ReceiverRenderer extends SafeBlockEntityRenderer<RayReceiverBlockEntity> {
    /** The parked elevation, and the lowest it tracks to, degrees. */
    static final float PARK = 45, MIN_ELEVATION = 25;
    /** Slew rate, degrees per tick: driven by its own motors, not a shaft. */
    private static final float SLEW = 1.2f;
    /** Blocks of beam at most, and in Ponder. */
    private static final float BEAM_MAX = 384, BEAM_PONDER = 10;
    private static final int VIEW_DISTANCE = 384;
    /** How bright Ponder's receivers are: its level has no swarm to share. */
    private static final float PONDER_GLOW = 0.55f;
    /** Ticks a first light's flare lasts. */
    private static final double FIRST_LIGHT = 30;
    private static final int CORE = 0xFFF6DE, HALO = 0xFFC860, PACKET = 0xFFF0C0;
    /** Where the beam widens: blocks from the focus (then its top), and the core's and halo's half-widths there. */
    private static final float[] BEAM_AT = {0, 3, 20, 80};
    private static final float[] CORE_R = {0.05f, 0.06f, 0.09f, 0.14f, 0.25f};
    private static final float[] HALO_R = {0.3f, 0.38f, 0.5f, 0.7f, 1.1f};
    /** Blocks from the beam's axis within which it is hidden, and beyond which it shows in full: from on its axis it fills the view. */
    private static final float AXIS_HIDE = 1.5f, AXIS_SHOW = 8;
    /** Sine of the sun's elevation at which the beam is softest (1 degree) and from which it is full (10 degrees). */
    private static final float LOW_SUN = 0.0175f, HIGH_SUN = 0.1736f;

    private static final Map<RayReceiverBlockEntity, State> STATES = new WeakHashMap<>();
    private final double[] sun = new double[3];
    private final double[] sight = new double[3];
    private final float[] cam = new float[3];

    public ReceiverRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final class State {
        final TurretAim aim = new TurretAim(PARK);
        float glow;
        double glowAt = Double.NaN;
        double litAt = Double.NEGATIVE_INFINITY;
        long lastTick = Long.MIN_VALUE;
    }

    @Override
    protected void renderSafe(RayReceiverBlockEntity be, float partial, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();
        boolean virtual = be.isVirtual();
        State st = STATES.computeIfAbsent(be, b -> new State());
        double anim = AnimationTickHolder.getRenderTime(level);
        double now = level.getGameTime() + partial;

        if (virtual) {
            sun[0] = TurretAim.PONDER_SUN[0];
            sun[1] = TurretAim.PONDER_SUN[1];
            sun[2] = TurretAim.PONDER_SUN[2];
        } else {
            TurretAim.localSun(level, be.getBlockPos(), partial, sun);
        }
        boolean track = virtual || (sun[1] > 0 && be.status() != ReceiverStatus.WRONG_DIMENSION);
        TurretAim aim = st.aim;
        aim.step(anim, SLEW, track, sun[0], sun[1], sun[2], MIN_ELEVATION, PARK);
        float glow = glow(be, st, anim, virtual);
        double lit = now - st.litAt;
        float flare = lit >= 0 && lit < FIRST_LIGHT ? (float) (1 - lit / FIRST_LIGHT) : 0;

        float[] axis = DysonPartials.AXIS, pivot = DysonPartials.RECEIVER_PIVOT;
        CachedBuffers.partial(DysonPartials.RECEIVER_MOUNT, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
        CachedBuffers.partial(DysonPartials.RECEIVER_DISH, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .translate(pivot[0], pivot[1], pivot[2]).rotateXDegrees(aim.elevation).translate(-pivot[0], -pivot[1], -pivot[2])
                .light(light).renderInto(ms, buffers.getBuffer(RenderType.cutout()));
        float lift = Math.max(glow, flare);
        int focusTint = lift <= 0.01f ? 0xFFFFFF : LocalQuads.mix(0xD8D0C0, 0xFFF4D6, lift);
        CachedBuffers.partial(DysonPartials.RECEIVER_FOCUS_GLOW, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .translate(pivot[0], pivot[1], pivot[2]).rotateXDegrees(aim.elevation).translate(-pivot[0], -pivot[1], -pivot[2])
                .color(0xFF000000 | focusTint).light(lift <= 0.01f ? light : litLight(light, lift))
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));

        if (lift > 0.01f) {
            aim.sight(sight);
            float d = DysonPartials.RECEIVER_FOCUS / 16f;
            float fx = (float) (pivot[0] + sight[0] * d), fy = (float) (pivot[1] + sight[1] * d), fz = (float) (pivot[2] + sight[2] * d);
            camera(be, level, virtual, fx, fy, fz);
            PoseStack.Pose pose = ms.last();
            if (glow > 0.01f && ClientConfig.dysonBeams) beam(pose, buffers, fx, fy, fz, glow, flare, anim, virtual);
            focus(pose, buffers, fx, fy, fz, glow, flare, lit, anim);
            if (!virtual) tick(be, level, st, glow, fx, fy, fz);
        }
    }

    /** How lit it is, 0 to 1: its share, eased so the beam swells and fades rather than blinks between updates. */
    private static float glow(RayReceiverBlockEntity be, State st, double anim, boolean virtual) {
        float target = virtual ? PONDER_GLOW : be.share() > 0 ? Math.max(0.12f, be.intensity()) : 0;
        double dt = Double.isNaN(st.glowAt) ? 20 : Mth.clamp(anim - st.glowAt, 0, 20);
        st.glowAt = anim;
        st.glow += Mth.clamp(target - st.glow, (float) (-0.03 * dt), (float) (0.06 * dt));
        return st.glow;
    }

    /** The beam from the sky into the focus at {@code f}, along the sun's line. */
    private void beam(PoseStack.Pose pose, MultiBufferSource buffers, float fx, float fy, float fz, float glow, float flare, double anim,
                      boolean virtual) {
        float length = virtual ? BEAM_PONDER : (float) Math.min(BEAM_MAX, Minecraft.getInstance().gameRenderer.getDepthFar() * 0.85);
        float sx = (float) sun[0], sy = (float) sun[1], sz = (float) sun[2];
        float cx = cam[0], cy = cam[1], cz = cam[2];
        // How far the camera stands from the beam's axis, between the focus and its top.
        float along = Mth.clamp((cx - fx) * sx + (cy - fy) * sy + (cz - fz) * sz, 0, length);
        float ox = cx - fx - sx * along, oy = cy - fy - sy * along, oz = cz - fz - sz * along;
        float offAxis = smooth(AXIS_HIDE, AXIS_SHOW, (float) Math.sqrt(ox * ox + oy * oy + oz * oz));
        // A low sun's beam is softer: it crosses the sky at eye height and through miles of air.
        float lowSun = virtual ? 1 : 0.2f + 0.8f * smooth(LOW_SUN, HIGH_SUN, sy);
        float strength = (0.35f + 0.65f * glow) * (1 + 1.5f * flare) * offAxis * lowSun;
        if (strength < 0.005f) return;
        float shimmer = 0.9f + 0.1f * Mth.sin((float) anim * 0.9f);

        VertexConsumer light = buffers.getBuffer(DysonRenderTypes.LIGHT);
        float prev = 0;
        for (int i = 1; i <= BEAM_AT.length; i++) {
            float at = i == BEAM_AT.length ? length : Math.min(length, BEAM_AT[i]);
            if (at <= prev) break;
            float t0 = fade(prev, length), t1 = fade(at, length);
            // The core starts a little dimmer at the focus, where the shimmer takes over.
            float k0 = i == 1 ? 0 : 1;
            float ax = fx + sx * prev, ay = fy + sy * prev, az = fz + sz * prev;
            float bx = fx + sx * at, by = fy + sy * at, bz = fz + sz * at;
            LocalQuads.strip(light, pose, ax, ay, az, bx, by, bz, HALO_R[i - 1], HALO_R[i],
                    LocalQuads.argb(0.3f * strength * t0 * shimmer, HALO), LocalQuads.argb(0.3f * strength * t1 * shimmer, HALO), cx, cy, cz);
            LocalQuads.strip(light, pose, ax, ay, az, bx, by, bz, CORE_R[i - 1] * 1.6f, CORE_R[i] * 1.6f,
                    LocalQuads.argb((0.5f + 0.4f * k0) * strength * t0, CORE), LocalQuads.argb(0.9f * strength * t1, CORE), cx, cy, cz);
            prev = at;
        }
        // Packets of light running down into the focus.
        float span = Math.min(length, virtual ? BEAM_PONDER : 96);
        int packets = Math.max(1, Math.round((2 + 5 * glow) * (float) ClientConfig.dysonFxDetail));
        float speed = virtual ? 0.25f : 2.4f;
        for (int k = 0; k < packets; k++) {
            float f = (float) (((anim * speed / span) + k / (float) packets) % 1.0);
            float s = span * (1 - f);
            float tail = Math.min(span, s + 1.5f + 2.5f * glow);
            float a = strength * Math.min(1, s / 1.5f) * fade(s, span) * 0.85f;
            if (a < 0.01f) continue;
            float r = 0.12f + 0.006f * s;
            LocalQuads.strip(light, pose, fx + sx * s, fy + sy * s, fz + sz * s, fx + sx * tail, fy + sy * tail, fz + sz * tail, r, r * 0.6f,
                    LocalQuads.argb(a, PACKET), LocalQuads.argb(0, PACKET), cx, cy, cz);
            LocalQuads.disc(light, pose, fx + sx * s, fy + sy * s, fz + sz * s, r * 2.2f, LocalQuads.argb(a * 0.6f, PACKET), cx, cy, cz);
        }

        // A solid core near the ground, so the beam reads against a bright sky.
        VertexConsumer paint = buffers.getBuffer(DysonRenderTypes.paint());
        float near = Math.min(length, 48);
        LocalQuads.strip(paint, pose, fx, fy, fz, fx + sx * near, fy + sy * near, fz + sz * near, 0.05f, 0.11f,
                LocalQuads.argb(0.75f * Math.min(1, strength), CORE), LocalQuads.argb(0, CORE), cx, cy, cz);
    }

    /** The shimmer round the focus, and a first light's flare. */
    private void focus(PoseStack.Pose pose, MultiBufferSource buffers, float fx, float fy, float fz, float glow, float flare, double lit,
                       double anim) {
        float cx = cam[0], cy = cam[1], cz = cam[2];
        float t = (float) anim;
        VertexConsumer light = buffers.getBuffer(DysonRenderTypes.LIGHT);
        float a = glow * (0.55f + 0.15f * Mth.sin(t * 1.3f) + 0.1f * Mth.sin(t * 3.1f + 1));
        if (a > 0.01f) {
            LocalQuads.disc(light, pose, fx, fy, fz, 0.55f + 0.06f * Mth.sin(t * 0.7f), LocalQuads.argb(a * 0.6f, HALO), cx, cy, cz);
            LocalQuads.disc(light, pose, fx, fy, fz, 0.28f, LocalQuads.argb(a, CORE), cx, cy, cz);
            float f = (t % 16) / 16;
            float[] fr = frameToward(fx - cx, fy - cy, fz - cz);
            if (fr != null) {
                LocalQuads.ring(light, pose, fx, fy, fz, 0.2f + 0.7f * f, 0.12f, LocalQuads.argb(glow * 0.5f * (1 - f), CORE),
                        fr[0], fr[1], fr[2], fr[3], fr[4], fr[5]);
            }
        }
        if (flare > 0) {
            float f = 1 - flare;
            float e = 1 - (1 - f) * (1 - f);
            LocalQuads.disc(light, pose, fx, fy, fz, 0.4f + 1.8f * e, LocalQuads.argb(0.8f * flare * flare, CORE), cx, cy, cz);
            float[] fr = frameToward(fx - cx, fy - cy, fz - cz);
            if (fr != null) {
                LocalQuads.ring(light, pose, fx, fy, fz, 0.3f + 2.5f * e, 0.2f + 0.3f * e, LocalQuads.argb(0.7f * flare, HALO),
                        fr[0], fr[1], fr[2], fr[3], fr[4], fr[5]);
            }
            VertexConsumer paint = buffers.getBuffer(DysonRenderTypes.paint());
            LocalQuads.dot(paint, pose, fx, fy, fz, 0.2f + 0.15f * e, LocalQuads.argb(flare, 0xFFFFF4), LocalQuads.argb(0, CORE), cx, cy, cz);
        }
    }

    private final float[] frame = new float[6];

    /** Two unit axes square to the line of sight {@code d}, for a ring facing the camera. */
    private float[] frameToward(float dx, float dy, float dz) {
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-5f) return null;
        dx /= len;
        dy /= len;
        dz /= len;
        float ax = -dz, ay = 0, az = dx;
        if (Math.abs(dy) > 0.95f) {
            ax = 1;
            az = 0;
        }
        float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        ax /= al;
        az /= al;
        ay /= al;
        frame[0] = ax;
        frame[1] = ay;
        frame[2] = az;
        frame[3] = dy * az - dz * ay;
        frame[4] = dz * ax - dx * az;
        frame[5] = dx * ay - dy * ax;
        return frame;
    }

    /** Where the camera stands in the block's frame: the billboards face it. Ponder has none, so a view from the north-west. */
    private void camera(RayReceiverBlockEntity be, Level level, boolean virtual, float fx, float fy, float fz) {
        if (virtual) {
            cam[0] = fx - 18;
            cam[1] = fy + 14;
            cam[2] = fz - 18;
            return;
        }
        BlockPos pos = be.getBlockPos();
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (ShipSpace.onShip(level, pos)) eye = ShipSpace.localView(level, pos, eye);
        cam[0] = (float) (eye.x - pos.getX());
        cam[1] = (float) (eye.y - pos.getY());
        cam[2] = (float) (eye.z - pos.getZ());
    }

    /** Once a game tick while lit: motes of light drifting down the beam into the focus. */
    private void tick(RayReceiverBlockEntity be, Level level, State st, float glow, float fx, float fy, float fz) {
        long tick = level.getGameTime();
        if (tick == st.lastTick) return;
        st.lastTick = tick;
        if (!ClientConfig.dysonBeams || level.random.nextFloat() >= 0.6f * glow * ClientConfig.dysonFxDetail) return;
        BlockPos pos = be.getBlockPos();
        float up = 1.2f + level.random.nextFloat() * 2.5f;
        Vec3 at = new Vec3(pos.getX() + fx + sun[0] * up + (level.random.nextFloat() - 0.5f) * 0.4,
                pos.getY() + fy + sun[1] * up, pos.getZ() + fz + sun[2] * up + (level.random.nextFloat() - 0.5f) * 0.4);
        if (ShipSpace.onShip(level, pos)) at = ShipSpace.toWorld(level, at);
        level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, -sun[0] * 0.08, -sun[1] * 0.08, -sun[2] * 0.08);
    }

    /** A receiver on this client took its first light: flare its focus, with a chime and a burst of sparks. */
    public static void firstLight(RayReceiverBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || be.isVirtual()) return;
        State st = STATES.computeIfAbsent(be, b -> new State());
        st.litAt = level.getGameTime();
        BlockPos pos = be.getBlockPos();
        double[] sight = new double[3];
        st.aim.sight(sight);
        float[] pivot = DysonPartials.RECEIVER_PIVOT;
        float d = DysonPartials.RECEIVER_FOCUS / 16f;
        Vec3 at = new Vec3(pos.getX() + pivot[0] + sight[0] * d, pos.getY() + pivot[1] + sight[1] * d, pos.getZ() + pivot[2] + sight[2] * d);
        if (ShipSpace.onShip(level, pos)) at = ShipSpace.toWorld(level, at);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9f, 1.3f, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 1.6f, false);
        int sparks = Math.round(14 * (float) ClientConfig.dysonFxDetail);
        for (int i = 0; i < sparks; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (level.random.nextFloat() - 0.5f) * 0.5f,
                    level.random.nextFloat() * 0.35f, (level.random.nextFloat() - 0.5f) * 0.5f);
        }
    }

    private static float smooth(float from, float to, float v) {
        float t = Mth.clamp((v - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** How much of the beam shows {@code s} blocks up a beam {@code length} long: it thins out into the sky. */
    private static float fade(float s, float length) {
        float t = Mth.clamp(s / Math.max(1, length), 0, 1);
        return (1 - t) * (1 - t);
    }

    private static int litLight(int light, float glow) {
        int level = Math.round(Mth.clamp(5 + glow * 16, 0, 15));
        return LightTexture.pack(Math.max(LightTexture.block(light), level), Math.max(LightTexture.sky(light), level));
    }

    @Override
    public boolean shouldRenderOffScreen(RayReceiverBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    /** The beam reaches far up the sun's line, wherever on the sky the sun stands. */
    @Override
    public AABB getRenderBoundingBox(RayReceiverBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(BEAM_MAX);
    }
}
