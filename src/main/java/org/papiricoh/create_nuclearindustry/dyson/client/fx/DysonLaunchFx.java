package org.papiricoh.create_nuclearindustry.dyson.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.EjectorRenderer;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SunPath;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * A sail's launch, seen from the ground: a glowing golden bolt leaves the ejector's muzzle along its rails with a bright trail,
 * speeds up and bends onto the sun's line, a streak climbing out of sight, and ends with a small flash high in the sky as the sail
 * unfolds into the swarm. The bolts live in a client-side list, placed by game time (their path is worked out, not simulated), and
 * are drawn after the weather like the rockets, sized by distance so they stay visible from far off; past the far plane they are
 * pulled in along their line of sight. A launch also flashes and sparks at the muzzle and sounds a thunk, a crack and a whoosh.
 */
public final class DysonLaunchFx {
    /** Ticks a bolt climbs, and its unfolding flash lasts. */
    private static final double CLIMB = 40, UNFOLD = 18;
    /** Blocks a tick it leaves the muzzle at, and its acceleration (blocks a tick, a tick). */
    private static final double V0 = 1.6, ACC = 0.55;
    /** Blocks over which it bends from the rails' line onto the sun's. */
    private static final double BEND = 48;
    /** Ticks of path its trail covers, and how long the glow it leaves low down lingers. */
    private static final double TRAIL_TICKS = 8, AFTERGLOW = 45;
    /** Bolts kept at once, at most. */
    private static final int MAX_BOLTS = 48;
    private static final int GOLD = 0xFFC850, HOT = 0xFFF2C8, EMBER = 0xFF8A30, SAIL = 0xFFE07A;

    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final DysonQuads GLOW = new DysonQuads(), PAINT = new DysonQuads();
    private static final double[] P = new double[3], Q = new double[3], SUN = new double[3];
    private static boolean initialised;

    private DysonLaunchFx() {
    }

    /** A sail in flight: where and which way it left, the sun's line it bends onto, and when. */
    private static final class Bolt {
        final double ox, oy, oz, dx, dy, dz, sx, sy, sz;
        final long start;
        final int seed;

        Bolt(double[] at, double[] dir, double[] sun, long start, int seed) {
            ox = at[0];
            oy = at[1];
            oz = at[2];
            double l = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            if (l < 1.0e-6) {
                dx = sun[0];
                dy = sun[1];
                dz = sun[2];
            } else {
                dx = dir[0] / l;
                dy = dir[1] / l;
                dz = dir[2] / l;
            }
            sx = sun[0];
            sy = sun[1];
            sz = sun[2];
            this.start = start;
            this.seed = seed;
        }

        /** Where it is {@code s} blocks along its path, into {@code out}. */
        void at(double s, double[] out) {
            double g = bent(s);
            out[0] = ox + dx * s + (sx - dx) * g;
            out[1] = oy + dy * s + (sy - dy) * g;
            out[2] = oz + dz * s + (sz - dz) * g;
        }
    }

    /** Hooks the frame and the tick; call once from the client side's init. */
    public static void init() {
        if (initialised) return;
        initialised = true;
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, DysonLaunchFx::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, DysonLaunchFx::onTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BOLTS.clear());
    }

    /** {@code DysonFxHooks.ejectorFired}: an ejector near this client launched a sail. */
    public static void fired(EmRailEjectorBlockEntity ejector, int orbit) {
        if (!(ejector.getLevel() instanceof ClientLevel level) || ejector.isVirtual()) return;
        double[] at = new double[3], dir = new double[3], sun = new double[3];
        EjectorRenderer.muzzle(ejector, at, dir);
        org.papiricoh.create_nuclearindustry.dyson.client.ClientSun.direction(level, 1, sun);
        if (BOLTS.size() >= MAX_BOLTS) BOLTS.remove(0);
        long now = level.getGameTime();
        BOLTS.add(new Bolt(at, dir, sun, now + (long) Math.ceil(EjectorRenderer.SLAM_TICKS), ejector.getBlockPos().hashCode() * 31 + (int) now));

        level.playLocalSound(at[0], at[1], at[2], SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 0.55f, false);
        level.playLocalSound(at[0], at[1], at[2], SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.18f, 1.9f, false);
        level.playLocalSound(at[0], at[1], at[2], SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.4f, 0.55f, false);

        RandomSource random = level.random;
        double detail = ClientConfig.dysonFxDetail;
        int sparks = (int) Math.round(16 * detail);
        for (int i = 0; i < sparks; i++) {
            double v = 0.3 + random.nextDouble() * 0.6;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at[0], at[1], at[2], dir[0] * v + random.nextGaussian() * 0.12,
                    dir[1] * v + random.nextGaussian() * 0.12, dir[2] * v + random.nextGaussian() * 0.12);
        }
        int puffs = (int) Math.round(5 * detail);
        for (int i = 0; i < puffs; i++) {
            level.addParticle(ParticleTypes.POOF, at[0], at[1], at[2], random.nextGaussian() * 0.06 + dir[0] * 0.05,
                    random.nextGaussian() * 0.04 + 0.02, random.nextGaussian() * 0.06 + dir[2] * 0.05);
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (BOLTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            BOLTS.clear();
            return;
        }
        long now = level.getGameTime();
        BOLTS.removeIf(b -> now - b.start > CLIMB + UNFOLD || now < b.start - 40);
        if (mc.isPaused()) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        int sparks = (int) Math.max(1, Math.round(2 * ClientConfig.dysonFxDetail));
        for (Bolt b : BOLTS) {
            double age = now - b.start;
            if (age < 0 || age > 14) continue;
            b.at(distance(age), P);
            if (eye.distanceToSqr(P[0], P[1], P[2]) > 160 * 160) continue;
            // Sparks shed along the stretch it covered this tick.
            double s0 = distance(Math.max(0, age - 1)), s1 = distance(age);
            for (int i = 0; i < sparks; i++) {
                b.at(s0 + (s1 - s0) * level.random.nextDouble(), Q);
                level.addParticle(age < 4 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD, Q[0], Q[1], Q[2],
                        level.random.nextGaussian() * 0.03, level.random.nextGaussian() * 0.03, level.random.nextGaussian() * 0.03);
            }
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || BOLTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Camera camera = event.getCamera();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partial;
        Vec3 eye = camera.getPosition();
        double far = mc.gameRenderer.getDepthFar();
        boolean shaded = RocketRenderTypes.shaded();
        GLOW.begin(eye, far * 0.9, shaded, camera.getLeftVector(), camera.getUpVector());
        PAINT.begin(eye, far * 0.9, false, camera.getLeftVector(), camera.getUpVector());
        int segments = (int) Math.max(3, Math.min(14, Math.round(7 * ClientConfig.dysonFxDetail)));
        for (int i = 0, n = BOLTS.size(); i < n; i++) {
            Bolt b = BOLTS.get(i);
            double age = now - b.start;
            if (age < 0) continue;
            if (age <= CLIMB) climb(b, age, segments);
            else unfold(b, (age - CLIMB) / UNFOLD);
        }
        GLOW.draw(RocketRenderTypes.glow(shaded));
        PAINT.draw(DysonRenderTypes.paint());
    }

    /** The bolt {@code age} ticks out: its trail, its head, and the glow it left low down. */
    private static void climb(Bolt b, double age, int segments) {
        double s = distance(age);
        double tail = distance(Math.max(0, age - TRAIL_TICKS));
        // Fades as it nears its height, where the flash takes over.
        double fadeOut = Math.min(1, (CLIMB - age) / 6 + 0.25);
        b.at(tail, P);
        double pd = GLOW.distance(P[0], P[1], P[2]);
        for (int k = 1; k <= segments; k++) {
            double t0 = (k - 1) / (double) segments, t1 = k / (double) segments;
            b.at(tail + (s - tail) * t1, Q);
            double qd = GLOW.distance(Q[0], Q[1], Q[2]);
            double r0 = width(pd) * (0.3 + 0.7 * t0), r1 = width(qd) * (0.3 + 0.7 * t1);
            int c0 = DysonQuads.argb(0.85 * t0 * t0 * fadeOut, t0 < 0.5 ? EMBER : GOLD);
            int c1 = DysonQuads.argb(0.85 * t1 * t1 * fadeOut, t1 < 0.5 ? EMBER : GOLD);
            GLOW.strip(P[0], P[1], P[2], Q[0], Q[1], Q[2], r0 * 2.2, r1 * 2.2, c0, c1, (float) (tail + (s - tail) * t0), (float) (tail + (s - tail) * t1));
            GLOW.strip(P[0], P[1], P[2], Q[0], Q[1], Q[2], r0 * 0.7, r1 * 0.7, DysonQuads.argb(t0 * fadeOut, HOT), DysonQuads.argb(t1 * fadeOut, HOT),
                    (float) (tail + (s - tail) * t0), (float) (tail + (s - tail) * t1));
            // A solid thread down its middle, so the streak reads against the daylit sky.
            PAINT.strip(P[0], P[1], P[2], Q[0], Q[1], Q[2], core(pd) * t0, core(qd) * t1, DysonQuads.argb(0.9 * t0 * fadeOut, SAIL),
                    DysonQuads.argb(0.95 * t1 * fadeOut, 0xFFF4D0), 0, 0);
            P[0] = Q[0];
            P[1] = Q[1];
            P[2] = Q[2];
            pd = qd;
        }
        double d = GLOW.distance(P[0], P[1], P[2]);
        double flicker = 0.9 + 0.1 * Math.sin(age * 2.3 + b.seed);
        GLOW.disc(P[0], P[1], P[2], Math.max(1.0, d * 0.03) * flicker, DysonQuads.argb(0.6 * fadeOut, GOLD), b.seed);
        GLOW.disc(P[0], P[1], P[2], Math.max(0.35, d * 0.009), DysonQuads.argb(fadeOut, HOT), b.seed + 1);
        PAINT.dot(P[0], P[1], P[2], Math.max(0.14, d * 0.004), DysonQuads.argb(fadeOut, 0xFFFBEA), DysonQuads.argb(0.6 * fadeOut, SAIL));
        if (age < AFTERGLOW) {
            // The ionised air it tore through, low down, fading.
            double a = 0.3 * (1 - age / AFTERGLOW);
            double top = Math.min(s, 36);
            b.at(0, P);
            b.at(top, Q);
            GLOW.strip(P[0], P[1], P[2], Q[0], Q[1], Q[2], 0.12, 0.35, DysonQuads.argb(a, GOLD), DysonQuads.argb(a * 0.2, GOLD), 0, (float) top);
        }
    }

    /** The sail unfolding where the bolt topped out, {@code f} of the way through: a flash, a ring and four opening vanes. */
    private static void unfold(Bolt b, double f) {
        if (f >= 1) return;
        b.at(distance(CLIMB), P);
        double d = GLOW.distance(P[0], P[1], P[2]);
        double e = 1 - (1 - f) * (1 - f);
        double fade = (1 - f) * (1 - f);
        double r = Math.max(2.5, d * 0.03);
        GLOW.disc(P[0], P[1], P[2], r * (0.6 + 1.6 * e), DysonQuads.argb(0.7 * fade, GOLD), b.seed);
        GLOW.disc(P[0], P[1], P[2], r * 0.45, DysonQuads.argb(fade, HOT), b.seed + 2);
        GLOW.ring(P[0], P[1], P[2], r * (0.5 + 2.6 * e), DysonQuads.argb(0.6 * fade, SAIL));
        var left = GLOW.left();
        var up = GLOW.up();
        double spin = (b.seed & 0xFF) / 255.0 * Math.PI + f * 0.8;
        double vane = r * (0.4 + 1.8 * e);
        for (int k = 0; k < 4; k++) {
            double a = spin + k * Math.PI / 2, c = Math.cos(a), s = Math.sin(a);
            double vx = left.x() * c + up.x() * s, vy = left.y() * c + up.y() * s, vz = left.z() * c + up.z() * s;
            GLOW.strip(P[0], P[1], P[2], P[0] + vx * vane, P[1] + vy * vane, P[2] + vz * vane, r * 0.16, r * 0.02,
                    DysonQuads.argb(0.9 * fade, SAIL), DysonQuads.argb(0, SAIL), 0, (float) vane);
            PAINT.strip(P[0], P[1], P[2], P[0] + vx * vane * 0.8, P[1] + vy * vane * 0.8, P[2] + vz * vane * 0.8, r * 0.05, r * 0.01,
                    DysonQuads.argb(0.85 * fade, 0xFFF0C0), DysonQuads.argb(0, SAIL), 0, 0);
        }
        PAINT.dot(P[0], P[1], P[2], Math.max(0.3, d * 0.007), DysonQuads.argb(Math.min(1, 1.3 * fade), 0xFFF6D8),
                DysonQuads.argb(0.6 * fade, SAIL));
    }

    /** Blocks along its path {@code age} ticks out. */
    private static double distance(double age) {
        return V0 * age + 0.5 * ACC * age * age;
    }

    /** How far a path {@code s} long has turned from the rails' line onto the sun's, in blocks across: a smooth bend. */
    private static double bent(double s) {
        if (s <= 0) return 0;
        if (s >= BEND) return s - BEND / 2;
        double t = s / BEND;
        return BEND * (t * t * t - t * t * t * t / 2);
    }

    /** A trail's half-width at {@code d} blocks from the camera: real up close, never thinner than a few pixels far off. */
    private static double width(double d) {
        return Math.max(0.18, d * 0.0045);
    }

    /** The solid thread's half-width at {@code d} blocks: about two pixels whatever the distance. */
    private static double core(double d) {
        return Math.max(0.05, d * 0.0025);
    }
}
