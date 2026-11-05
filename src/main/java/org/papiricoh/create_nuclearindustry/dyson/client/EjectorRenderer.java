package org.papiricoh.create_nuclearindustry.dyson.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorStatus;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws an EM rail ejector: the half shaft coming up into its plinth, the turntable, and the rails with their coils and sled raised
 * towards the sun. It tracks the sun while it charges and while its last sail is still climbing, slewing faster the faster it turns;
 * otherwise it stows, rails a little up. The coils are lit by their own charge, dull copper warming to white-blue, with a pulse
 * running up the bore as the charge nears full; stopped for any reason, they cool back to copper over {@link #COOL_TICKS} ticks
 * (the stored charge itself is left alone). On a launch the sled slams up the rails, the muzzle flashes, the rails kick back
 * and a shock ring spreads from the muzzle; the sail itself is {@link org.papiricoh.create_nuclearindustry.dyson.client.fx.DysonLaunchFx}'s.
 */
public class EjectorRenderer extends KineticBlockEntityRenderer<EmRailEjectorBlockEntity> {
    /** The stowed elevation, and the lowest it tracks to, degrees. */
    static final float PARK = 30, MIN_ELEVATION = 8;
    /** Ticks the coils take to cool from white-hot to dull copper once it stops charging. */
    private static final float COOL_TICKS = 40;
    /** Slew rate, degrees per tick: standing still (it settles under its own weight), per rpm, and at most. */
    private static final float SLEW_IDLE = 0.4f, SLEW_PER_RPM = 0.04f, MAX_SLEW = 6;
    /** Ticks after a launch it keeps tracking: the sail's climb and its unfolding. */
    static final double TRACK_AFTER_LAUNCH = 70;
    /** Ticks the sled takes to reach the muzzle (the sail leaves then), and pixels it travels. */
    public static final double SLAM_TICKS = 2;
    private static final float SLED_TRAVEL = 22, KICK = 1.6f;
    /** The bore's centre line and the muzzle, rail frame pixels. */
    private static final float BORE_X = 8, BORE_Y = 15, MUZZLE_Z = -14;
    /** The coil rings' centres along the bore, pixels. */
    private static final float[] COIL_Z = {0, -2.75f, -5.5f, -8.25f, -11};
    /** A coil ring's four sides round the bore, x0 y0 x1 y1 in pixels, for the glow that hugs them. */
    private static final float[] RING_MEMBERS = {3, 12.25f, 13, 13, 3, 18, 13, 18.75f, 3, 13, 3.75f, 18, 12.25f, 13, 13, 18};
    /** Coil tint by charge: dull copper, warm copper, hot, white, white-blue. */
    private static final float[] TINT_AT = {0, 0.45f, 0.75f, 0.92f, 1};
    private static final int[] TINT = {0x7A5038, 0xC27440, 0xFFB070, 0xFFF1DC, 0xC4E2FF};
    private static final int FLASH = 0xFFF0C8, BOLT = 0xFFC850;

    private static final Map<EmRailEjectorBlockEntity, State> STATES = new WeakHashMap<>();
    private final float[] cam = new float[3];
    private final double[] sun = new double[3];
    private final double[] point = new double[3];

    public EjectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private static final class State {
        final TurretAim aim = new TurretAim(PARK);
        long lastTick = Long.MIN_VALUE;
        boolean ready;
        /** How hot the coils look, 0 to 1: their charge while charging, cooling off otherwise. */
        float heat;
        double heatAt = Double.NaN;
    }

    @Override
    protected void renderSafe(EmRailEjectorBlockEntity be, float partial, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();
        boolean virtual = be.isVirtual();
        State st = STATES.computeIfAbsent(be, b -> new State());
        double anim = AnimationTickHolder.getRenderTime(level);
        double now = level.getGameTime() + partial;
        double since = be.lastLaunch() == Long.MIN_VALUE ? Double.MAX_VALUE : now - be.lastLaunch();

        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN), be, light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));

        if (virtual) {
            sun[0] = TurretAim.PONDER_SUN[0];
            sun[1] = TurretAim.PONDER_SUN[1];
            sun[2] = TurretAim.PONDER_SUN[2];
        } else {
            TurretAim.localSun(level, be.getBlockPos(), partial, sun);
        }
        boolean track = virtual || (sun[1] > 0 && (be.status() == EjectorStatus.CHARGING || since < TRACK_AFTER_LAUNCH));
        float slew = Math.min(MAX_SLEW, SLEW_IDLE + Math.abs(be.getSpeed()) * SLEW_PER_RPM);
        TurretAim aim = st.aim;
        aim.step(anim, slew, track, sun[0], sun[1], sun[2], MIN_ELEVATION, PARK);

        float charge = heat(be, st, partial, anim, virtual);
        float slide = slide(since), kick = kick(since);
        float flash = since < 6 ? (float) (1 - since / 6) : 0;

        float[] axis = DysonPartials.AXIS, pivot = DysonPartials.EJECTOR_PIVOT;
        CachedBuffers.partial(DysonPartials.EJECTOR_TURNTABLE, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
        upper(DysonPartials.EJECTOR_RAILS, state, aim, kick).light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
        upper(DysonPartials.EJECTOR_SLED, state, aim, kick - slide).light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
        int tint = LocalQuads.mix(tint(charge), 0xFFFFFF, flash);
        upper(DysonPartials.EJECTOR_COILS, state, aim, kick).color(0xFF000000 | tint).light(coilLight(light, Math.max(charge, flash)))
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));

        ms.pushPose();
        aim.turn(ms, axis);
        aim.raise(ms, pivot);
        ms.translate(0, 0, kick / 16f);
        cameraInRails(be, level, aim, kick, virtual);
        glow(ms.last(), buffers, charge, tint, flash, since, anim);
        ms.popPose();

        if (!virtual) tick(be, level, st, charge);
    }

    /** One of the parts raised with the rails, slid {@code along} pixels back along them. */
    private static SuperByteBuffer upper(PartialModel model, BlockState state, TurretAim aim,
                                         float along) {
        float[] axis = DysonPartials.AXIS, pivot = DysonPartials.EJECTOR_PIVOT;
        return CachedBuffers.partial(model, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .translate(pivot[0], pivot[1], pivot[2]).rotateXDegrees(aim.elevation).translate(-pivot[0], -pivot[1], -pivot[2])
                .translate(0, 0, along / 16f);
    }

    /**
     * How hot the coils look: the charge while charging (Ponder: a gentle glow), else easing down to cold, so a parked ejector
     * holding a charge overnight does not glow white.
     */
    private static float heat(EmRailEjectorBlockEntity be, State st, float partial, double anim, boolean virtual) {
        float target = Mth.clamp(be.charge(partial), 0, 1);
        if (virtual) target = Math.max(target, 0.4f + 0.1f * Mth.sin((float) anim * 0.08f));
        else if (be.status() != EjectorStatus.CHARGING) target = 0;
        double dt = Double.isNaN(st.heatAt) ? COOL_TICKS : Mth.clamp(anim - st.heatAt, 0, 20);
        st.heatAt = anim;
        st.heat = target >= st.heat ? target : Math.max(target, st.heat - (float) (dt / COOL_TICKS));
        return st.heat;
    }

    /** The coils' glow, the pulse up the bore, and a launch's muzzle flash and shock ring, in the rails' frame (blocks). */
    private void glow(PoseStack.Pose pose, MultiBufferSource buffers, float charge, int tint, float flash, double since, double anim) {
        float hot = smooth(0.25f, 1, charge);
        float ready = smooth(0.6f, 1, charge);
        float zp = Float.NaN;
        if (ready > 0) {
            float period = Mth.lerp(ready, 16, 5);
            float f = (float) ((anim / period) % 1.0);
            zp = Mth.lerp(f, 2, MUZZLE_Z - 1);
        }
        float cx = cam[0], cy = cam[1], cz = cam[2];
        float bx = BORE_X / 16f, by = BORE_Y / 16f;

        VertexConsumer light = buffers.getBuffer(DysonRenderTypes.LIGHT);
        for (float z : COIL_Z) {
            float pulse = Float.isNaN(zp) ? 0 : ready * (float) Math.exp(-sq((z - zp) / 2.2f));
            float a = 0.22f * hot + 0.55f * pulse + 0.6f * flash;
            if (a < 0.01f) continue;
            int c = LocalQuads.argb(a, LocalQuads.mix(tint, 0xE8F4FF, pulse));
            float z0 = (z - 0.85f) / 16, z1 = (z + 0.85f) / 16;
            for (int m = 0; m < RING_MEMBERS.length; m += 4) {
                LocalQuads.box(light, pose, (RING_MEMBERS[m] - 0.35f) / 16, (RING_MEMBERS[m + 1] - 0.35f) / 16, z0,
                        (RING_MEMBERS[m + 2] + 0.35f) / 16, (RING_MEMBERS[m + 3] + 0.35f) / 16, z1, c);
            }
        }
        if (!Float.isNaN(zp) && zp > MUZZLE_Z) {
            LocalQuads.disc(light, pose, bx, by, zp / 16, 0.22f + 0.12f * ready, LocalQuads.argb(0.75f * ready, 0xD8ECFF), cx, cy, cz);
        }
        if (ready > 0.3f) {
            // The bore, glowing between the rails.
            float a = 0.35f * ready * (0.8f + 0.2f * Mth.sin((float) anim * 1.7f));
            LocalQuads.strip(light, pose, bx, by, 1 / 16f, bx, by, MUZZLE_Z / 16, 0.07f, 0.05f, LocalQuads.argb(a, 0xBFE0FF),
                    LocalQuads.argb(a * 0.6f, 0xBFE0FF), cx, cy, cz);
        }
        float mz = (MUZZLE_Z - 1) / 16;
        double t = since - SLAM_TICKS;
        if (t >= -SLAM_TICKS && t < 8) {
            // Muzzle flash: a white-gold burst as the sled reaches the muzzle.
            float f = (float) Math.max(0, t) / 8;
            float a = (float) (t < 0 ? 0.4 * (1 + t / SLAM_TICKS) : (1 - f) * (1 - f));
            float r = 0.25f + 0.6f * (float) Math.sqrt(f);
            LocalQuads.disc(light, pose, bx, by, mz, r * 1.7f, LocalQuads.argb(0.4f * a, BOLT), cx, cy, cz);
            LocalQuads.disc(light, pose, bx, by, mz, r, LocalQuads.argb(a, FLASH), cx, cy, cz);
        }
        if (t >= 0 && t < 12) {
            // The shock ring, square to the rails, spreading from the muzzle.
            float f = (float) t / 12;
            float e = 1 - (1 - f) * (1 - f);
            LocalQuads.ring(light, pose, bx, by, mz - 0.2f * e, 0.15f + 1.2f * e, 0.18f + 0.2f * e, LocalQuads.argb(0.7f * (1 - f) * (1 - f), FLASH),
                    1, 0, 0, 0, 1, 0);
        }
        if (t >= 0 && t < 4) {
            VertexConsumer paint = buffers.getBuffer(DysonRenderTypes.paint());
            float a = (float) (1 - t / 4);
            LocalQuads.dot(paint, pose, bx, by, mz, 0.18f, LocalQuads.argb(a, 0xFFFBEF), LocalQuads.argb(0, FLASH), cx, cy, cz);
        }
    }

    /** Where the camera stands in the rails' frame (blocks): the billboards face it. Ponder has none, so a view from the north-west. */
    private void cameraInRails(EmRailEjectorBlockEntity be, Level level, TurretAim aim, float kick, boolean virtual) {
        double x, y, z;
        BlockPos pos = be.getBlockPos();
        if (virtual) {
            x = 0.5 - 18;
            y = 0.5 + 14;
            z = 0.5 - 18;
        } else {
            Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (ShipSpace.onShip(level, pos)) eye = ShipSpace.localView(level, pos, eye);
            x = eye.x - pos.getX();
            y = eye.y - pos.getY();
            z = eye.z - pos.getZ();
        }
        float[] axis = DysonPartials.AXIS, pivot = DysonPartials.EJECTOR_PIVOT;
        // Undo the mount's turn, then the rails' raise, then the kick.
        double az = Math.toRadians(aim.azimuth), c = Math.cos(az), s = Math.sin(az);
        double rx = x - axis[0], rz = z - axis[2];
        double tx = rx * c + rz * s, tz = -rx * s + rz * c;
        double el = Math.toRadians(-aim.elevation), ce = Math.cos(el), se = Math.sin(el);
        double py = y - pivot[1], pz = tz + axis[2] - pivot[2];
        cam[0] = (float) (tx + axis[0]);
        cam[1] = (float) (py * ce - pz * se + pivot[1]);
        cam[2] = (float) (py * se + pz * ce + pivot[2] - kick / 16f);
    }

    /** Once a game tick: the hum while charging, a chime when charged, and sparks off the coils near full. */
    private void tick(EmRailEjectorBlockEntity be, Level level, State st, float charge) {
        long tick = level.getGameTime();
        if (tick == st.lastTick) return;
        st.lastTick = tick;
        BlockPos pos = be.getBlockPos();
        Vec3 at = ShipSpace.toWorld(level, pos);
        boolean charging = be.status() == EjectorStatus.CHARGING;
        if (charging && Math.floorMod(tick + pos.hashCode(), 80) == 0)
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.45f, 0.6f + 0.9f * charge, false);
        if (charge < 0.5f) st.ready = false;
        else if (charging && charge > 0.93f && !st.ready) {
            st.ready = true;
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.3f, 1.5f, false);
        }
        if (charge > 0.8f && charging && level.random.nextFloat() < 0.5f * ClientConfig.dysonFxDetail) {
            float z = COIL_Z[level.random.nextInt(COIL_Z.length)];
            float side = level.random.nextBoolean() ? 3 : 13;
            world(be, level, st.aim, side, 12 + level.random.nextFloat() * 7, z, point);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, point[0], point[1], point[2],
                    (level.random.nextFloat() - 0.5f) * 0.1f, level.random.nextFloat() * 0.06f, (level.random.nextFloat() - 0.5f) * 0.1f);
        }
    }

    /** Pixels the sled is slid up the rails {@code since} ticks after a launch: slammed up, held, then drawn back for the next. */
    private static float slide(double since) {
        if (since < SLAM_TICKS) {
            float f = (float) (since / SLAM_TICKS);
            return SLED_TRAVEL * f * f;
        }
        if (since < 8) return SLED_TRAVEL;
        if (since < 40) return SLED_TRAVEL * (1 - smooth(0, 1, (float) (since - 8) / 32));
        return 0;
    }

    /** Pixels the rails are kicked back by the launch: a damped jolt. */
    private static float kick(double since) {
        double t = since - SLAM_TICKS;
        if (t < 0 || t > 24) return 0;
        return (float) (KICK * Math.exp(-t / 3.5) * Math.cos(t * 0.75));
    }

    private static int tint(float charge) {
        for (int i = 1; i < TINT_AT.length; i++) {
            if (charge <= TINT_AT[i]) return LocalQuads.mix(TINT[i - 1], TINT[i], (charge - TINT_AT[i - 1]) / (TINT_AT[i] - TINT_AT[i - 1]));
        }
        return TINT[TINT.length - 1];
    }

    /** The coils' light: their own glow, from dim to full-bright as they charge, or the world's where that is brighter. */
    private static int coilLight(int light, float charge) {
        int glow = Math.round(Mth.clamp(4 + charge * 18, 0, 15));
        return LightTexture.pack(Math.max(LightTexture.block(light), glow), Math.max(LightTexture.sky(light), glow));
    }

    /** A point given in rail-frame pixels, where it is in the world now, into {@code out}. */
    private static void world(EmRailEjectorBlockEntity be, Level level, TurretAim aim, double px, double py, double pz, double[] out) {
        local(aim, px, py, pz, out);
        BlockPos pos = be.getBlockPos();
        if (!ShipSpace.onShip(level, pos)) {
            out[0] += pos.getX();
            out[1] += pos.getY();
            out[2] += pos.getZ();
            return;
        }
        Vec3 w = ShipSpace.toWorld(level, new Vec3(pos.getX() + out[0], pos.getY() + out[1], pos.getZ() + out[2]));
        out[0] = w.x;
        out[1] = w.y;
        out[2] = w.z;
    }

    /** A point given in rail-frame pixels, in the block's frame (blocks), into {@code out}. */
    private static void local(TurretAim aim, double px, double py, double pz, double[] out) {
        float[] axis = DysonPartials.AXIS, pivot = DysonPartials.EJECTOR_PIVOT;
        double x = px / 16 - pivot[0], y = py / 16 - pivot[1], z = pz / 16 - pivot[2];
        double el = Math.toRadians(aim.elevation), ce = Math.cos(el), se = Math.sin(el);
        double ry = y * ce - z * se, rz = y * se + z * ce;
        x += pivot[0] - axis[0];
        rz += pivot[2] - axis[2];
        double az = Math.toRadians(-aim.azimuth), c = Math.cos(az), s = Math.sin(az);
        out[0] = x * c + rz * s + axis[0];
        out[1] = ry + pivot[1];
        out[2] = -x * s + rz * c + axis[2];
    }

    /**
     * Where a sail leaves {@code be}'s muzzle and which way, in the world, into {@code at} and {@code dir}: from the rails as this
     * renderer last drew them, or aimed at the sun if it never has.
     */
    public static void muzzle(EmRailEjectorBlockEntity be, double[] at, double[] dir) {
        Level level = be.getLevel();
        if (level == null) return;
        State st = STATES.get(be);
        TurretAim aim;
        if (st != null) {
            aim = st.aim;
        } else {
            aim = new TurretAim(PARK);
            double[] sun = new double[3];
            TurretAim.localSun(level, be.getBlockPos(), 0, sun);
            aim.step(0, 0, false, 0, 1, 0, MIN_ELEVATION, PARK);
            aim.step(1000, 1000, true, sun[0], sun[1], sun[2], MIN_ELEVATION, PARK);
        }
        world(be, level, aim, BORE_X, BORE_Y, MUZZLE_Z - 1, at);
        double[] sight = new double[3];
        aim.sight(sight);
        BlockPos pos = be.getBlockPos();
        if (ShipSpace.onShip(level, pos)) {
            Vec3 a = ShipSpace.toWorld(level, Vec3.atCenterOf(pos));
            Vec3 b = ShipSpace.toWorld(level, Vec3.atCenterOf(pos).add(sight[0], sight[1], sight[2]));
            sight[0] = b.x - a.x;
            sight[1] = b.y - a.y;
            sight[2] = b.z - a.z;
        }
        dir[0] = sight[0];
        dir[1] = sight[1];
        dir[2] = sight[2];
    }

    private static float smooth(float from, float to, float v) {
        float t = Mth.clamp((v - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float sq(float v) {
        return v * v;
    }

    @Override
    public boolean shouldRenderOffScreen(EmRailEjectorBlockEntity be) {
        return true;
    }

    /** The rails overhang the plinth by a block and a half when level; the shock ring spreads a little further. */
    @Override
    public AABB getRenderBoundingBox(EmRailEjectorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(3.5);
    }
}
