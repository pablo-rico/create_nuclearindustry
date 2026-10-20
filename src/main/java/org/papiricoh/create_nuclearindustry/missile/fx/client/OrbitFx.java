package org.papiricoh.create_nuclearindustry.missile.fx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;
import org.papiricoh.create_nuclearindustry.missile.fx.FxCurves;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;
import org.papiricoh.create_nuclearindustry.missile.fx.SatelliteFxCurves;
import org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteKind;
import org.papiricoh.create_nuclearindustry.missile.orbit.client.OrbitPartials;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The satellites' moments, high over the players' heads: a launch vehicle reaching orbit (its last stage cuts off, the fairing
 * peels open and tumbles away, the satellite drifts out), a satellite shot down (a white-blue flash and a glittering cloud of
 * fragments) and a decaying one burning up (a fireball streaking down the sky and breaking up). Up close the fairing halves and
 * the satellite are their block models; from the ground, kilometres below, everything is kept a few pixels across at least, as
 * glints and flashes, the way the rocket effects keep a far plume in sight. Most of it is worked out from its age alone
 * ({@link SatelliteFxCurves}).
 */
final class OrbitFx {
    static final OrbitFx INSTANCE = new OrbitFx();
    /** Fragments of shot-down satellites drawn at once, at most. */
    private static final int MAX_SHARDS = 400;
    /** Fragments one kill throws. */
    private static final int SHARDS_PER_KILL = 56;
    /** Ticks of a fireball's path kept: the glowing streak is the newest {@link #STREAK} of them, the smoke all of them. */
    private static final int PATH = 120, STREAK = 36;
    /** Within this many blocks the models are in plain sight and the glints standing in for them fade out. */
    private static final double GLINT_FROM = 48, GLINT_FULL = 160;

    private final List<Insertion> insertions = new ArrayList<>();
    private final List<Shard> shards = new ArrayList<>();
    private final List<Fireball> fireballs = new ArrayList<>();

    private OrbitFx() {
    }

    /**
     * One orbit insertion, all worked out from its age.
     *
     * @param base     the fairing block's lowest corner, in the world
     * @param attitude the vehicle's frame at the moment
     */
    private record Insertion(long start, Vec3 base, Vec3 velocity, Quaternionf attitude, SatelliteKind kind, int seed) {
        Vec3 axis() {
            return RocketFxManager.rotate(attitude, 0, 1, 0);
        }

        /** The fairing's centre: where the satellite rode. */
        Vec3 centre() {
            return base.add(RocketFxManager.rotate(attitude, 0.5f, 0.5f, 0.5f));
        }

        /** Where a fairing half's hinge is {@code age} ticks on: {@code side} -1 west (left), +1 east (right). */
        Vec3 hinge(int side, double age) {
            float[] h = side < 0 ? OrbitPartials.FAIRING_LEFT_HINGE : OrbitPartials.FAIRING_RIGHT_HINGE;
            float drift = (float) (side * SatelliteFxCurves.fairingDrift(age));
            return base.add(RocketFxManager.rotate(attitude, h[0] + drift, h[1], h[2])).add(velocity.scale(age))
                    .subtract(0, SatelliteFxCurves.fairingFall(age), 0);
        }

        Vec3 satellite(double age) {
            return centre().add(velocity.scale(age)).add(axis().scale(SatelliteFxCurves.releaseDrift(age)));
        }

        boolean over(long now) {
            return now - start > Math.max(SatelliteFxCurves.FAIRING_LIFE, SatelliteFxCurves.SATELLITE_LIFE);
        }
    }

    /** A fragment of a satellite shot down, glittering as it tumbles. */
    private record Shard(long start, int life, V3 from, V3 velocity, float size, int colour, int seed) {
        Vec3 at(double age) {
            V3 p = SatelliteFxCurves.shard(from, velocity, age);
            return new Vec3(p.x(), p.y(), p.z());
        }
    }

    /** A satellite burning up, or one of the pieces it broke into. */
    private static final class Fireball {
        final long start;
        final float size;
        final int seed;
        final double groundY;
        final boolean fragment;
        Vec3 position, previous;
        V3 velocity;
        final ArrayDeque<Vec3> path = new ArrayDeque<>();

        Fireball(long start, Vec3 position, V3 velocity, float size, int seed, double groundY, boolean fragment) {
            this.start = start;
            this.position = position;
            this.previous = position;
            this.velocity = velocity;
            this.size = size;
            this.seed = seed;
            this.groundY = groundY;
            this.fragment = fragment;
        }

        Vec3 at(float partial) {
            return previous.lerp(position, partial);
        }
    }

    boolean isEmpty() {
        return insertions.isEmpty() && shards.isEmpty() && fireballs.isEmpty();
    }

    void clear() {
        insertions.clear();
        shards.clear();
        fireballs.clear();
    }

    // ------------------------------------------------------------------ moments

    /**
     * A launch vehicle reaches orbit: what is left of it (the payload, its spent stages long gone) gives a last puff as its
     * engine cuts off for good, the fairing's bolts fire and its halves swing open and tumble away, and the satellite
     * ({@code kind}) drifts out of it.
     */
    void insertion(ClientLevel level, @Nullable FlightFx fx, SatelliteKind kind, Vec3 pos, Vec3 vel, long now, double distance,
                   List<Burst> bursts, RandomSource random) {
        RocketShape shape = fx != null ? fx.shape : RocketShape.LEGACY;
        Quaternionf attitude = fx != null ? new Quaternionf(fx.attitude) : new Quaternionf();
        int height = shape.height();
        Vec3 axis = RocketFxManager.rotate(attitude, 0, 1, 0);
        Vec3 base = pos.add(RocketFxManager.rotate(attitude, -0.5f, height - 1, -0.5f));
        Insertion insertion = new Insertion(now, base, vel, attitude, kind, random.nextInt());
        insertions.add(insertion);

        // The engine cutting off: a last flicker under the payload and the propellant left venting as a cloud.
        Vec3 tail = pos.add(axis.scale(fx != null ? shape.attachedBottom(fx.stage, fx.has(FlightFxState.BURNED_OUT)) : 0));
        bursts.add(new Burst(Burst.Kind.FLASH, tail, null, now, 6, 2.5f, 0xFFD8A8, 0));
        bursts.add(new Burst(Burst.Kind.CLOUD, tail.subtract(axis.scale(2)), null, now, 80, 10, 0xF2F4F8, 0));
        // The fairing's bolts, then the satellite let go: brief flashes and a ring across the axis where the halves part.
        Vec3 joint = pos.add(axis.scale(height - 1));
        bursts.add(new Burst(Burst.Kind.FLASH, joint, axis, now + SatelliteFxCurves.FAIRING_DELAY, 8, 2, 0xFFF2DC, 0));
        bursts.add(new Burst(Burst.Kind.RING, joint, axis, now + SatelliteFxCurves.FAIRING_DELAY, 16, 5, 0xFFE8C8, 0));
        bursts.add(new Burst(Burst.Kind.FLASH, insertion.centre(), null, now + SatelliteFxCurves.RELEASE_DELAY, 14, 3, 0xE8F2FF, 0));
        if (distance < FxBudget.MID) {
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12 + random.nextDouble() * 0.3;
                Vec3 out = RocketFxManager.rotate(attitude, (float) Math.cos(a), 0, (float) Math.sin(a));
                Vec3 at = joint.add(out.scale(0.6));
                Vec3 v = out.scale(0.2 + 0.1 * random.nextDouble()).add(vel);
                RocketParticles.smoke(level, false, at.x, at.y, at.z, v.x, v.y, v.z,
                        new RocketParticles.Puff(0.95f, 0.95f, 0.96f, 0.6f, 0.3f, 1.6f, 30 + random.nextInt(15), 0, 0));
            }
        }
        RocketSounds.oneShot(RocketFxRegistry.FAIRING_SEPARATION.get(), joint, distance, 3200, 0.9f, 1.0f, true);
    }

    /**
     * A satellite shot down high up: a white-blue flash seen from far off, a ring and a cloud of debris swelling out, fragments
     * glittering as they drift and slowly fall, and the boom long after.
     */
    void killed(ClientLevel level, Vec3 pos, Vec3 vel, long now, double distance, List<Burst> bursts, RandomSource random) {
        float flash = (float) Math.max(36, distance * 0.012);
        bursts.add(new Burst(Burst.Kind.FLASH, pos, null, now, 30, flash, 0xEAF2FF, 0));
        bursts.add(new Burst(Burst.Kind.FLASH, pos, null, now + 3, 50, flash * 0.6f, 0xC8DAFF, 0));
        bursts.add(new Burst(Burst.Kind.RING, pos, vel.lengthSqr() < 1.0e-6 ? null : vel.normalize(), now, 40, 72, 0xD0E0FF, 0));
        bursts.add(new Burst(Burst.Kind.CLOUD, pos, null, now, 320, 48, 0xB4B8C0, 0));
        V3 from = new V3(pos.x, pos.y, pos.z);
        V3 carried = new V3(vel.x, vel.y, vel.z).scale(0.25);
        int count = Math.min(SHARDS_PER_KILL, MAX_SHARDS - shards.size());
        for (int i = 0; i < count; i++) {
            V3 dir = new V3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            double length = dir.length();
            dir = length < 1.0e-6 ? V3.UP : dir.scale(1 / length);
            double speed = 0.25 + 0.95 * random.nextDouble();
            int colour = switch (random.nextInt(4)) {
                case 0 -> 0xFFE6A8;
                case 1 -> 0xD8E6FF;
                default -> 0xF4F6FF;
            };
            shards.add(new Shard(now, 220 + random.nextInt(180), from, carried.add(dir.scale(speed)), 0.25f + random.nextFloat() * 0.45f, colour,
                    random.nextInt()));
        }
        if (distance < FxBudget.MID * 2) {
            RocketParticles.smoke(level, true, pos.x, pos.y, pos.z, 0, 0.02, 0,
                    new RocketParticles.Puff(0.5f, 0.52f, 0.56f, 0.8f, 6f, 22f, 120, 6, 0.0f));
            RocketParticles.debris(level, pos, vel, 24, 2.5f, random);
        }
        RocketSounds.oneShot(RocketFxRegistry.SATELLITE_BREAKUP.get(), pos, distance, 6000, 1.0f, 0.75f, true);
    }

    /** A decaying satellite coming down: a fireball plunging steeply across the sky, breaking up, burnt out before the ground. */
    void reentry(ClientLevel level, Vec3 pos, Vec3 vel, long now, double distance, List<Burst> bursts, RandomSource random) {
        double ground = RocketFxManager.groundBelow(level, pos);
        V3 start = SatelliteFxCurves.reentryStart(new V3(vel.x, vel.y, vel.z));
        fireballs.add(new Fireball(now, pos, start, 1, random.nextInt(), Math.min(ground, pos.y - 100), false));
        bursts.add(new Burst(Burst.Kind.FLASH, pos, null, now, 16, (float) Math.max(6, distance * 0.006), 0xFFC890, 0));
    }

    // ------------------------------------------------------------------ each tick

    void tick(ClientLevel level, long now, Vec3 eye, List<Burst> bursts, RandomSource random) {
        insertions.removeIf(i -> i.over(now));
        shards.removeIf(s -> now - s.start > s.life);
        List<Fireball> born = new ArrayList<>();
        Iterator<Fireball> it = fireballs.iterator();
        while (it.hasNext()) {
            Fireball fb = it.next();
            long age = now - fb.start;
            if (age >= SatelliteFxCurves.REENTRY_LIFE || fb.position.y < fb.groundY + SatelliteFxCurves.REENTRY_FLOOR * 0.5) {
                it.remove();
                continue;
            }
            fb.previous = fb.position;
            fb.velocity = SatelliteFxCurves.reentryStep(fb.velocity);
            fb.position = fb.position.add(fb.velocity.x(), fb.velocity.y(), fb.velocity.z());
            fb.path.addLast(fb.position);
            while (fb.path.size() > PATH) fb.path.removeFirst();
            double distance = eye.distanceTo(fb.position);
            if (!fb.fragment && age == SatelliteFxCurves.REENTRY_BREAKUP) {
                // It breaks up: pieces fan out from it, each burning on its own.
                for (int i = 0; i < 4; i++) {
                    V3 spread = new V3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian()).scale(0.12 * fb.velocity.length());
                    Fireball piece = new Fireball(fb.start, fb.position, fb.velocity.add(spread), 0.45f + random.nextFloat() * 0.25f,
                            random.nextInt(), fb.groundY, true);
                    piece.path.addLast(fb.position);
                    born.add(piece);
                }
                bursts.add(new Burst(Burst.Kind.FLASH, fb.position, null, now, 20, (float) Math.max(8, distance * 0.009), 0xFFE0B0, 0));
                RocketSounds.oneShot(RocketFxRegistry.SATELLITE_BREAKUP.get(), fb.position, distance, 5000, 0.7f, 0.6f, true);
            }
            if (distance < FxBudget.MID && ClientConfig.rocketDetail > 0) {
                double glow = SatelliteFxCurves.reentryGlow(age, fb.position.y, fb.groundY);
                if (glow > 0.2 && random.nextFloat() < 0.6f * fb.size) {
                    RocketParticles.ember(level, fb.position.x, fb.position.y, fb.position.z, fb.velocity.x() * 0.3 + random.nextGaussian() * 0.1,
                            fb.velocity.y() * 0.3, fb.velocity.z() * 0.3 + random.nextGaussian() * 0.1, 0.4f * fb.size, 20 + random.nextInt(10));
                }
            }
        }
        fireballs.addAll(born);
    }

    // ------------------------------------------------------------------ the frame: models

    /**
     * Draws the fairing halves and the satellite as their block models, before the effects' light and smoke. Past the far
     * plane ({@code limit}) each is pulled in along its line of sight and shrunk to match, so it keeps its place and size.
     */
    void renderModels(Vec3 eye, float partial, long now, double limit) {
        if (insertions.isEmpty()) return;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        BlockState state = SiloRegistry.SATELLITE_FAIRING.get().defaultBlockState();
        int light = LightTexture.pack(0, 15);
        PoseStack ms = new PoseStack();
        for (Insertion insertion : insertions) {
            double age = now - insertion.start + partial;
            if (age < 0) continue;
            double halves = SatelliteFxCurves.fade(age, SatelliteFxCurves.FAIRING_LIFE, 40);
            if (halves > 0.02) {
                double angle = SatelliteFxCurves.peelDegrees(age);
                for (int side = -1; side <= 1; side += 2) {
                    float[] h = side < 0 ? OrbitPartials.FAIRING_LEFT_HINGE : OrbitPartials.FAIRING_RIGHT_HINGE;
                    ms.pushPose();
                    place(ms, insertion.hinge(side, age), eye, limit);
                    ms.mulPose(insertion.attitude);
                    // Left turns +angle about z, right -angle; once free each also rolls a little about x, its own way.
                    ms.mulPose(Axis.ZP.rotationDegrees((float) (-side * angle)));
                    ms.mulPose(Axis.XP.rotationDegrees((float) (side * Math.max(0, angle - 60) * 0.3)));
                    ms.translate(-h[0], -h[1], -h[2]);
                    draw(side < 0 ? OrbitPartials.FAIRING_HALF_LEFT : OrbitPartials.FAIRING_HALF_RIGHT, state, ms, buffers, light, halves);
                    ms.popPose();
                }
            }
            double shown = age - SatelliteFxCurves.RELEASE_DELAY;
            double alpha = SatelliteFxCurves.fade(age, SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_FADE);
            if (shown >= 0 && alpha > 0.02) {
                float[] c = OrbitPartials.SATELLITE_CENTRE;
                ms.pushPose();
                place(ms, insertion.satellite(age), eye, limit);
                // Level, looking down at the Earth, turning slowly as it settles.
                ms.mulPose(Axis.YP.rotationDegrees((float) (shown * 0.8 + (insertion.seed & 0xFF))));
                ms.translate(-c[0], -c[1], -c[2]);
                PartialModel model = insertion.kind == SatelliteKind.COMMS ? OrbitPartials.SATELLITE_COMMS : OrbitPartials.SATELLITE_EARLY_WARNING;
                draw(model, state, ms, buffers, light, alpha);
                ms.popPose();
            }
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
    }

    /** Moves {@code ms} to {@code at}, seen from {@code eye}: pulled in and shrunk to match past {@code limit}. */
    private static void place(PoseStack ms, Vec3 at, Vec3 eye, double limit) {
        Vec3 rel = at.subtract(eye);
        float k = (float) FxBudget.pullIn(rel.length(), limit);
        ms.translate(rel.x * k, rel.y * k, rel.z * k);
        ms.scale(k, k, k);
    }

    private static void draw(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers, int light, double alpha) {
        int a = (int) Math.round(255 * Math.max(0, Math.min(1, alpha)));
        RenderType type = Sheets.translucentCullBlockSheet();
        // Asked for right before drawing: never a buffer held over another type's drawing.
        CachedBuffers.partial(model, state).light(light).color(255, 255, 255, a).renderInto(ms, buffers.getBuffer(type));
    }

    // ------------------------------------------------------------------ the frame: light and smoke

    void draw(FxQuads glow, FxQuads smoke, float partial, long now, Vec3 eye, RocketFxRender.Sky sky) {
        double time = now + partial;
        for (Insertion insertion : insertions) drawInsertion(glow, insertion, partial, now, eye);
        for (Shard shard : shards) {
            double age = time - shard.start;
            if (age < 0) continue;
            Vec3 at = shard.at(age);
            double distance = eye.distanceTo(at);
            if (hidden(distance)) continue;
            double a = SatelliteFxCurves.fade(age, shard.life, 90) * SatelliteFxCurves.glitter(time, shard.seed);
            // Hot at first, then only the sun on it.
            int colour = RocketFxRender.mix(0xFFB070, shard.colour, Math.min(1, age / 30));
            glow.disc(at.x, at.y, at.z, Math.max(shard.size, distance * 0.0022), RocketFxRender.argb(0.9 * a, colour), shard.seed);
        }
        for (Fireball fb : fireballs) drawFireball(glow, smoke, fb, partial, now, eye, sky);
    }

    private static boolean hidden(double distance) {
        return distance > FxBudget.MID && !ClientConfig.rocketFarTrails;
    }

    private static void drawInsertion(FxQuads glow, Insertion insertion, float partial, long now, Vec3 eye) {
        double age = now - insertion.start + partial;
        double time = now + partial;
        // The fairing halves catching the sun as they tumble.
        double halves = SatelliteFxCurves.fade(age, SatelliteFxCurves.FAIRING_LIFE, 40);
        if (age > SatelliteFxCurves.FAIRING_DELAY && halves > 0) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 at = insertion.hinge(side, age);
                double distance = eye.distanceTo(at);
                if (hidden(distance)) continue;
                glint(glow, at, distance, 0.75 * halves * SatelliteFxCurves.glitter(time, insertion.seed + side * 977), 0xFFFFFF,
                        insertion.seed + side);
            }
        }
        // The satellite itself, drifting on: a steady point with its panels flashing now and then.
        double shown = age - SatelliteFxCurves.RELEASE_DELAY;
        double alpha = SatelliteFxCurves.fade(age, SatelliteFxCurves.SATELLITE_LIFE, SatelliteFxCurves.SATELLITE_FADE);
        if (shown >= 0 && alpha > 0) {
            Vec3 at = insertion.satellite(age);
            double distance = eye.distanceTo(at);
            if (!hidden(distance)) {
                int colour = insertion.kind == SatelliteKind.COMMS ? 0xE6EEFF : 0xFFE8B8;
                glint(glow, at, distance, alpha * (0.55 + 0.45 * SatelliteFxCurves.glitter(time * 0.5, insertion.seed + 31)), colour,
                        insertion.seed + 3);
            }
        }
    }

    /** A point of sunlight on something too far off to make out: at least a couple of pixels across, gone up close. */
    private static void glint(FxQuads glow, Vec3 at, double distance, double alpha, int colour, int seed) {
        double far = FxCurves.smoothstep(GLINT_FROM, GLINT_FULL, distance);
        if (far * alpha <= 0.01) return;
        glow.disc(at.x, at.y, at.z, Math.max(0.5, distance * 0.003), RocketFxRender.argb(far * alpha, colour), seed);
        glow.disc(at.x, at.y, at.z, Math.max(1.5, distance * 0.009), RocketFxRender.argb(0.2 * far * alpha, colour), seed + 1);
    }

    /** A fireball: its glowing head and halo, the streak of white-hot air behind it and the long thin smoke it leaves. */
    private static void drawFireball(FxQuads glow, FxQuads smoke, Fireball fb, float partial, long now, Vec3 eye, RocketFxRender.Sky sky) {
        Vec3 head = fb.at(partial);
        double distance = eye.distanceTo(head);
        if (hidden(distance)) return;
        double age = now - fb.start + partial;
        double heat = SatelliteFxCurves.reentryGlow(age, head.y, fb.groundY);
        if (heat <= 0.01) return;
        double s = fb.size;
        int hot = RocketFxRender.mix(0xFF6A28, 0xFFF2DC, Math.min(1, heat));
        glow.disc(head.x, head.y, head.z, Math.max(1.6 * s, distance * 0.006 * s), RocketFxRender.argb(Math.min(1, 0.95 * heat), hot), fb.seed);
        glow.disc(head.x, head.y, head.z, Math.max(6 * s, distance * 0.022 * s), RocketFxRender.argb(0.22 * heat, 0xFF9A50), fb.seed + 1);

        // The path, newest last: the streak is its newest stretch, the smoke all of it.
        int n = fb.path.size();
        if (n < 2) return;
        Vec3[] path = fb.path.toArray(new Vec3[0]);
        path[n - 1] = head;
        double width = Math.max(1.2 * s, distance * 0.0035 * s);
        int from = Math.max(0, n - STREAK);
        for (int i = from; i + 1 < n; i++) {
            double t0 = (double) (i - from) / (n - from), t1 = (double) (i + 1 - from) / (n - from);
            Vec3 a = path[i], b = path[i + 1];
            glow.strip(a.x, a.y, a.z, b.x, b.y, b.z, width * (0.3 + 0.7 * t0), width * (0.3 + 0.7 * t1),
                    RocketFxRender.argb(0.7 * heat * t0 * t0, 0xFF7A38), RocketFxRender.argb(0.7 * heat * t1 * t1, 0xFFC890), i * 3f, (i + 1) * 3f);
        }
        if (fb.fragment) return;
        int grey = RocketFxRender.lit(0xCFCBC6, sky, 0.8);
        double fade = Math.min(1, heat * 2);
        for (int i = 0; i + 3 < n; i += 3) {
            Vec3 a = path[i], b = path[Math.min(n - 1, i + 3)];
            // Older smoke is wider and thinner.
            double oldA = (double) (n - i) / n, oldB = (double) (n - i - 3) / n;
            double seen = eye.distanceTo(a);
            double wa = Math.max(1 + 4 * oldA, seen * 0.0025), wb = Math.max(1 + 4 * oldB, seen * 0.0025);
            smoke.strip(a.x, a.y, a.z, b.x, b.y, b.z, wa, wb, RocketFxRender.argb(0.28 * fade * (1 - oldA), grey),
                    RocketFxRender.argb(0.28 * fade * (1 - oldB), grey), i * 0.2f, (i + 3) * 0.2f);
        }
    }
}
