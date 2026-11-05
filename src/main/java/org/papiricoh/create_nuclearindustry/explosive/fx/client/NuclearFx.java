package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.ClientSun;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.FalloutPlume;
import org.papiricoh.create_nuclearindustry.explosive.blast.ShockFront;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CameraShake;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudLighting;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.ExplosionAudio;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.FarView;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.QualityPreset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a player sees, hears and feels of a nuclear detonation ({@link DetonationPayload}): the mushroom cloud
 * ({@link CloudEvolution}) drawn from its cached mesh ({@link CloudMeshCache}), and beyond the far plane its impostor, or with
 * Distant Horizons the full cloud over its terrain ({@link #farMesh}); every frame the parts that move too fast to cache (the
 * dust front the shock sweeps along the ground, lightning in the young cloud, the fireball's corona); each tick its sounds, the
 * particles near the player (dust at the shock front, flames and soot of the fires the flash lit, ash and black rain downwind in
 * the fallout) and the sky's flash; and the shaking. Antimatter annihilation keeps its own brief look: a white-green flash and a
 * shell of sparks, no cloud. Everything is worked out from the detonation's age, so a player told of it late sees and hears it at
 * its true age.
 */
public final class NuclearFx {
    /** Seconds an antimatter annihilation is shown. */
    private static final double ANNIHILATION_SECONDS = 2.0;
    private static final int SHELL_SPARKS = 96;
    private static final int[] SHELL_COLOURS = {0x6BFF8A, 0x3FD8E8, 0xD8FFE0};

    private static final List<Nuke> ACTIVE = new ArrayList<>();
    private static final List<Nuke> SORTED = new ArrayList<>();
    private static final List<DetonationPayload> ANNIHILATIONS = new ArrayList<>();
    private static final List<Long> ANNIHILATION_TICKS = new ArrayList<>();
    private static final PuffList BATCH = new PuffList();
    private static final PuffList GLOW = new PuffList();
    private static final float[] HAZE = new float[4];
    private static final double[] SUN = new double[3];
    private static final double[] SHAKE = new double[3];
    private static final double[] FIRE_AT = new double[2];
    private static long frame;

    private NuclearFx() {}

    /** One mushroom cloud being shown. */
    public static final class Nuke {
        final DetonationPayload payload;
        final ExplosionPalette palette;
        final CloudEvolution cloud;
        /** Ground zero, at ground level. */
        final double ox, oy, oz;
        final long startTick;
        final CloudMeshCache cache = new CloudMeshCache();
        final double phase;
        final double[] lightningTicks;
        long played;
        long farFrame = -1;
        /** Which fires' ground has been looked up (a bit each). */
        int firesFound;
        boolean soundsDone;

        Nuke(DetonationPayload payload, ExplosionPalette.Rgb ground) {
            this.payload = payload;
            this.palette = ExplosionPalette.of(payload.style());
            this.ox = payload.centre().x;
            this.oy = payload.centre().y - Math.max(0, payload.burstHeight());
            this.oz = payload.centre().z;
            this.startTick = payload.startTick();
            this.cloud = new CloudEvolution(payload.seed(), palette, payload.scale(), payload.fireball(), payload.burstHeight(), payload.airburst(),
                    payload.waveSpeed(), payload.waveRadius(), payload.thermalRadius(), payload.windAngle(), ground);
            this.phase = (payload.seed() & 0xFFFF) / 65536.0 * 6.283;
            boolean lightning = QualityPreset.of(ClientConfig.explosionQuality).lightning();
            this.lightningTicks = new double[lightning ? CloudEvolution.BOLTS : 0];
            for (int k = 0; k < lightningTicks.length; k++) lightningTicks[k] = cloud.lightningTime(k) * 20;
        }

        /** Seconds since it went off. */
        public double age(float partial) {
            ClientLevel level = Minecraft.getInstance().level;
            long now = level == null ? startTick : level.getGameTime();
            return Math.max(0, (now - startTick + partial) / 20.0);
        }

        public CloudEvolution cloud() {
            return cloud;
        }

        public DetonationPayload payload() {
            return payload;
        }

        public ExplosionPalette palette() {
            return palette;
        }

        /** Where the fireball (later the cap) is now. */
        public Vec3 fireball(double age) {
            double d = cloud.drift(cloud.capCentre(age), age);
            return new Vec3(ox + Math.cos(payload.windAngle()) * d, oy + cloud.capCentre(age), oz + Math.sin(payload.windAngle()) * d);
        }

        public Vec3 groundZero() {
            return new Vec3(ox, oy, oz);
        }

        boolean over(double age) {
            return age > cloud.lifetime() && age * 20 * payload.waveSpeed() > payload.waveRadius() && soundsDone;
        }
    }

    // ------------------------------------------------------------------ bookkeeping

    /** A detonation the server told us of. */
    public static void add(DetonationPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (payload.style() == DetonationStyle.ANTIMATTER) {
            if (level.getGameTime() - payload.startTick() > ANNIHILATION_SECONDS * 20 || ANNIHILATIONS.contains(payload)) return;
            ANNIHILATIONS.add(payload);
            ANNIHILATION_TICKS.add(payload.startTick());
            if (level.getGameTime() - payload.startTick() < 10) annihilationParticles(payload);
            return;
        }
        if (payload.style() == DetonationStyle.DIRTY) {
            // A dirty bomb is a plain high-explosive blast; it looks like one.
            ConventionalFx.addDirty(payload);
            return;
        }
        for (Nuke nuke : ACTIVE) {
            // Sent again on coming back in range: it is already here.
            if (nuke.payload.seed() == payload.seed() && nuke.startTick == payload.startTick()) return;
        }
        Nuke nuke = new Nuke(payload, ground(level, payload.centre().x, payload.centre().y - Math.max(0, payload.burstHeight()), payload.centre().z));
        nuke.cloud.unknownFireGround();
        ACTIVE.add(nuke);
    }

    /** The colour of the ground at {@code (x, y, z)}, or plain dust where it is not loaded. */
    static ExplosionPalette.Rgb ground(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y - 0.5, z);
        if (!level.hasChunkAt(pos)) return ExplosionPalette.DUST;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) state = level.getBlockState(pos.below());
        int c = ExplosionParticles.groundColour(level, state, x, y, z);
        return new ExplosionPalette.Rgb(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
    }

    public static int active() {
        return ACTIVE.size() + ANNIHILATIONS.size();
    }

    public static List<Nuke> nukes() {
        return Collections.unmodifiableList(ACTIVE);
    }

    static void clear() {
        for (Nuke nuke : ACTIVE) nuke.cache.close();
        ACTIVE.clear();
        ANNIHILATIONS.clear();
        ANNIHILATION_TICKS.clear();
    }

    static void onFrame() {
        frame++;
    }

    // ------------------------------------------------------------------ each tick

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        for (int i = ANNIHILATIONS.size() - 1; i >= 0; i--) {
            if ((level.getGameTime() - ANNIHILATION_TICKS.get(i)) / 20.0 > ANNIHILATION_SECONDS) {
                ANNIHILATIONS.remove(i);
                ANNIHILATION_TICKS.remove(i);
            }
        }
        double flash = 0;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Nuke nuke = ACTIVE.get(i);
            double age = nuke.age(0);
            sounds(nuke, age, eye);
            if (nuke.over(age)) {
                nuke.cache.close();
                ACTIVE.remove(i);
                continue;
            }
            particles(level, nuke, age, eye);
            flash = Math.max(flash, skyFlash(nuke, age, eye));
        }
        if (flash > 0.3 && ClientConfig.explosionTerrainLight && ClientConfig.explosionFlash > 0) {
            level.setSkyFlashTime(2);
        }
    }

    private static void sounds(Nuke nuke, double age, Vec3 eye) {
        if (nuke.soundsDone) return;
        DetonationPayload p = nuke.payload;
        double distance = eye.distanceTo(p.centre());
        List<ExplosionAudio.Cue> cues = ExplosionAudio.nuclear(distance, p.waveSpeed(), p.waveRadius(), p.yieldKg(), p.fireball(),
                p.style() == DetonationStyle.THERMONUCLEAR, nuke.lightningTicks);
        double ticks = age * 20;
        nuke.played = ExplosionSounds.play(cues, ticks, nuke.played, p.centre());
        double last = 0;
        for (ExplosionAudio.Cue cue : cues) last = Math.max(last, cue.tick());
        if (ticks > last + ExplosionAudio.STALE_TICKS + 1) nuke.soundsDone = true;
    }

    /** How strongly the flash lights the sky where the player stands, 0-1, ignoring which way they face. */
    static double skyFlash(Nuke nuke, double age, Vec3 eye) {
        double distance = Math.max(1, eye.distanceTo(nuke.payload.centre()));
        double near = Math.min(1, 2.5 * Math.max(nuke.payload.thermalRadius(), 32) / distance);
        return Math.exp(-age / nuke.palette.flashFade()) * near;
    }

    private static void particles(ClientLevel level, Nuke nuke, double age, Vec3 eye) {
        DetonationPayload p = nuke.payload;
        RandomSource random = level.random;
        double dx = eye.x - nuke.ox, dz = eye.z - nuke.oz;
        double flat = Math.sqrt(dx * dx + dz * dz);
        // Dust raised where the shock front sweeps past the player.
        double front = nuke.cloud.shockRadius(age);
        if (front < p.waveRadius() && Math.abs(front - flat) < 24 && Math.abs(eye.y - nuke.oy) < 64 && flat > 1) {
            double heading = Math.atan2(dz, dx);
            int n = ExplosionParticles.quota(ExplosionParticles.scaled(8));
            for (int i = 0; i < n; i++) {
                double a = heading + (random.nextDouble() - 0.5) * 48 / Math.max(24, front);
                double r = front + (random.nextDouble() - 0.5) * 6;
                double x = nuke.ox + Math.cos(a) * r, z = nuke.oz + Math.sin(a) * r;
                BlockPos top = BlockPos.containing(x, nuke.oy, z);
                if (!level.hasChunkAt(top)) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, top.getX(), top.getZ());
                BlockState state = level.getBlockState(new BlockPos(top.getX(), y - 1, top.getZ()));
                double out = 0.5 + random.nextDouble() * 0.6;
                ExplosionParticles.dust(level, state, x, y + 0.3, z, Math.cos(a) * out, 0.12 + random.nextDouble() * 0.2, Math.sin(a) * out,
                        1.5f + random.nextFloat() * 1.5f, 50 + random.nextInt(40));
            }
        }
        // The Wilson cloud round the player, if it reached them.
        if (age > 0.3 && age < 2.5 && flat < nuke.cloud.shockRadius(age) && eye.distanceTo(p.centre()) < 4.5 * nuke.cloud.fireballRadius()) {
            int n = ExplosionParticles.quota(ExplosionParticles.scaled(3));
            for (int i = 0; i < n; i++) {
                ExplosionParticles.mist(level, eye.x + random.nextGaussian() * 10, eye.y + random.nextGaussian() * 4, eye.z + random.nextGaussian() * 10,
                        0, 0.01, 0, 2f + random.nextFloat() * 2, 30 + random.nextInt(20));
            }
        }
        // Flames and soot of the fires the flash lit, near the player.
        int fires = nuke.cloud.fireCount();
        findFireGround(level, nuke, fires);
        for (int f = 0; f < fires; f++) {
            if (Double.isNaN(nuke.cloud.fireGround(f))) continue;
            double lit = nuke.cloud.firePoint(f, FIRE_AT);
            if (age < lit || age > nuke.cloud.lifetime() * 0.9) continue;
            double x = nuke.ox + FIRE_AT[0], z = nuke.oz + FIRE_AT[1];
            double fdx = eye.x - x, fdz = eye.z - z;
            if (fdx * fdx + fdz * fdz > 160 * 160) continue;
            BlockPos column = BlockPos.containing(x, nuke.oy, z);
            if (!level.hasChunkAt(column)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            if (ExplosionParticles.quota(1) > 0) {
                ExplosionParticles.flame(level, x + random.nextGaussian() * 2, y + 0.3, z + random.nextGaussian() * 2, 0, 0.05 + random.nextDouble() * 0.05, 0,
                        0.8f + random.nextFloat(), 14 + random.nextInt(10), 0.35 + 0.2 * random.nextDouble());
            }
            if ((level.getGameTime() + f) % 3 == 0 && ExplosionParticles.quota(1) > 0) {
                ExplosionParticles.fireSmoke(level, x + random.nextGaussian() * 2, y + 1.5, z + random.nextGaussian() * 2,
                        Math.cos(p.windAngle()) * 0.03, 0.12 + random.nextDouble() * 0.06, Math.sin(p.windAngle()) * 0.03,
                        1.5f + random.nextFloat(), 100 + random.nextInt(60), 0x333130);
            }
        }
        // Ash, and later black rain, downwind inside the fallout's plume.
        if (ClientConfig.explosionAsh && p.falloutRadius() > 0 && age > 40 && age < nuke.cloud.lifetime()) {
            double share = FalloutPlume.share(dx, dz, p.falloutRadius(), p.windAngle(), p.windStretch());
            if (share > 0.02) {
                double windX = Math.cos(p.windAngle()) * 0.04, windZ = Math.sin(p.windAngle()) * 0.04;
                int ash = ExplosionParticles.quota(ExplosionParticles.scaled(share * 5));
                for (int i = 0; i < ash; i++) {
                    ExplosionParticles.ash(level, eye.x + (random.nextDouble() - 0.5) * 36, eye.y + 10 + random.nextDouble() * 10,
                            eye.z + (random.nextDouble() - 0.5) * 36, windX, windZ);
                }
                if (age > 90 && age < nuke.cloud.lifetime() * 0.85 && share > 0.25) {
                    int rain = ExplosionParticles.quota(ExplosionParticles.scaled(share * 8));
                    for (int i = 0; i < rain; i++) {
                        ExplosionParticles.rain(level, eye.x + (random.nextDouble() - 0.5) * 30, eye.y + 14 + random.nextDouble() * 6,
                                eye.z + (random.nextDouble() - 0.5) * 30);
                    }
                }
            }
        }
    }

    /** Looks up the ground under each fire once it is loaded, so its column stands on it; none over water. */
    private static void findFireGround(ClientLevel level, Nuke nuke, int fires) {
        for (int f = 0; f < fires && f < 31; f++) {
            if ((nuke.firesFound & 1 << f) != 0) continue;
            nuke.cloud.firePoint(f, FIRE_AT);
            BlockPos column = BlockPos.containing(nuke.ox + FIRE_AT[0], nuke.oy, nuke.oz + FIRE_AT[1]);
            if (!level.hasChunkAt(column)) continue;
            nuke.firesFound |= 1 << f;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            BlockState surface = level.getBlockState(new BlockPos(column.getX(), top - 1, column.getZ()));
            nuke.cloud.fireGround(f, surface.getFluidState().isEmpty() ? top - nuke.oy : Double.NaN);
            nuke.cache.invalidate();
        }
    }

    // ------------------------------------------------------------------ drawing

    /** The light of this frame: the sun's direction as drawn (a pack's tilted path too), the daylight, the fires under a cloud. */
    static CloudLighting.Sky sky(ClientLevel level, float partial, double fireBelow) {
        ClientSun.direction(level, partial, SUN);
        double day = Math.max(0, Math.min(1, (level.getSkyDarken(partial) - 0.2) / 0.8)) * (1 - 0.4 * level.getRainLevel(partial));
        return CloudLighting.sky(SUN[0], SUN[1], SUN[2], day, fireBelow);
    }

    /** How much the fires lit by a nuke light its cloud from below. */
    private static double fireBelow(Nuke nuke, double age) {
        if (nuke.payload.thermalRadius() <= 0) return 0;
        return 0.8 * Math.max(0, Math.min(1, (age - 10) / 30)) * Math.max(0, 1 - age / nuke.cloud.lifetime());
    }

    /** The air's colour and how much of it lies between the camera and something {@code distance} blocks off. */
    static float[] haze(double distance) {
        float[] fog = RenderSystem.getShaderFogColor();
        HAZE[0] = fog[0];
        HAZE[1] = fog[1];
        HAZE[2] = fog[2];
        HAZE[3] = (float) (0.65 * (1 - Math.exp(-Math.max(0, distance - 200) / 2500)));
        return HAZE;
    }

    /** Where the pulled-in far plane is: inside the game's own, closer under a shader pack, whose fog hides the far distance. */
    static double pullLimit(Minecraft mc) {
        double far = mc.gameRenderer.getDepthFar();
        return NukeRenderTypes.pack() ? Math.max(96, 0.25 * far) : 0.9 * far;
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != NukeRenderTypes.stage() || (ACTIVE.isEmpty() && ANNIHILATIONS.isEmpty())) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 eye = event.getCamera().getPosition();
        QualityPreset quality = QualityPreset.of(ClientConfig.explosionQuality);
        double limit = pullLimit(mc);
        double impostorFrom = 0.9 * mc.gameRenderer.getDepthFar();
        boolean textured = NukeRenderTypes.textured();
        SORTED.clear();
        SORTED.addAll(ACTIVE);
        SORTED.sort((a, b) -> Double.compare(b.fireball(b.age(partial)).distanceToSqr(eye), a.fireball(a.age(partial)).distanceToSqr(eye)));
        for (Nuke nuke : SORTED) {
            if (nuke.farFrame == frame) continue;
            double age = nuke.age(partial);
            if (age > nuke.cloud.lifetime() + 1 && nuke.cloud.shockRadius(age) >= nuke.payload.waveRadius()) continue;
            Vec3 centre = nuke.fireball(age);
            double distance = centre.distanceTo(eye);
            double reach = nuke.cloud.capRadius(age);
            if (distance - reach > impostorFrom && age > nuke.cloud.fireballSeconds() * 1.6) {
                if (ClientConfig.explosionFarImpostor) impostor(nuke, age, eye, level, partial, limit, quality);
                continue;
            }
            prepare(nuke, age, eye, level, partial, quality, textured, distance);
            // Pulled in about the camera by the same factor as its distance: the same angle, only nearer.
            double k = FarView.pullIn(distance, limit);
            Matrix4f modelView = new Matrix4f(event.getModelViewMatrix()).scale((float) k)
                    .translate((float) (nuke.ox - eye.x), (float) (nuke.oy - eye.y), (float) (nuke.oz - eye.z));
            nuke.cache.draw(modelView, event.getProjectionMatrix(), haze(distance));
            fastParts(nuke, age, eye, level, partial, limit, quality, distance);
        }
        if (!ANNIHILATIONS.isEmpty()) annihilations(level, eye, event.getCamera(), partial);
    }

    /** Brings a nuke's cached mesh up to date for a camera at {@code eye}. */
    private static void prepare(Nuke nuke, double age, Vec3 eye, ClientLevel level, float partial, QualityPreset quality, boolean textured,
                                double distance) {
        double lod = quality.lod(distance);
        double detail = ClientConfig.explosionCloudDetail * (textured ? 1 : 0.6);
        CloudEvolution.Counts counts = CloudEvolution.Counts.of(quality, detail, lod);
        nuke.cache.update(nuke.cloud, age, counts, (float) QualityPreset.lodGrowth(lod), sky(level, partial, fireBelow(nuke, age)),
                eye.x - nuke.ox, eye.y - nuke.oy, eye.z - nuke.oz, quality.rebuildSeconds(age, nuke.cloud.fireballSeconds()), textured);
    }

    /** The parts that move too fast for the cached mesh: the dust front, lightning, the fireball's corona. */
    private static void fastParts(Nuke nuke, double age, Vec3 eye, ClientLevel level, float partial, double limit, QualityPreset quality,
                                  double distance) {
        BATCH.clear();
        BATCH.offset(nuke.ox, nuke.oy, nuke.oz);
        int front = QualityPreset.scaled(quality.surgePuffs(), ClientConfig.explosionCloudDetail, quality.lod(distance));
        nuke.cloud.dustFront(age, front, BATCH);
        if (BATCH.size() > 0) {
            BATCH.light(sky(level, partial, 0));
            BATCH.sort(eye.x, eye.y, eye.z);
            drawBatch(BATCH, eye, limit, false, distance);
        }
        GLOW.clear();
        GLOW.offset(nuke.ox, nuke.oy, nuke.oz);
        if (quality.lightning()) nuke.cloud.lightning(age, GLOW);
        double fireSeconds = nuke.cloud.fireballSeconds();
        if (age < fireSeconds * 1.3) {
            // The corona: the air round the fireball glowing in its light.
            double heat = nuke.cloud.fireballHeat(age);
            ExplosionPalette.Rgb c = ExplosionPalette.fire(heat);
            float a = (float) (0.6 * Math.max(0, 1 - age / (fireSeconds * 1.3)));
            double d = nuke.cloud.drift(nuke.cloud.capCentre(age), age);
            GLOW.puff(Math.cos(nuke.payload.windAngle()) * d, nuke.cloud.capCentre(age), Math.sin(nuke.payload.windAngle()) * d,
                    nuke.cloud.fireballRadius(age) * 1.6, CloudEvolution.TILE_VEIL, 0, c.red(), c.green(), c.blue(), a, 0, 0, 0, 0, 0,
                    CloudEvolution.EMISSIVE);
        }
        double fade = nuke.palette.glowFade();
        if (age < fade * 5) {
            // The sky lit round the fireball: a wide, faint glow in its direction rather than a tint over the whole sky.
            ExplosionPalette.Rgb g = nuke.palette.glow();
            double d = nuke.cloud.drift(nuke.cloud.capCentre(age), age);
            GLOW.puff(Math.cos(nuke.payload.windAngle()) * d, nuke.cloud.capCentre(age), Math.sin(nuke.payload.windAngle()) * d,
                    nuke.cloud.fireballRadius() * 6, CloudEvolution.TILE_VEIL, 0, g.red(), g.green(), g.blue(),
                    (float) (0.35 * Math.exp(-age / (1.5 * fade))), 0, 0, 0, 0, 0, CloudEvolution.EMISSIVE);
        }
        if (GLOW.size() > 0) {
            GLOW.sort(eye.x, eye.y, eye.z);
            drawBatch(GLOW, eye, limit, true, distance);
        }
    }

    /**
     * Draws a batch of puffs in world coordinates now, camera-relative: as smoke (with fire and lightning in it when textured), or
     * as added light ({@code light}).
     */
    static void drawBatch(PuffList list, Vec3 eye, double limit, boolean light, double distance) {
        if (list.size() == 0) return;
        if (NukeRenderTypes.textured()) {
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            list.writeTextured(builder, eye.x, eye.y, eye.z, eye.x, eye.y, eye.z, limit, PuffList.Filter.ALL);
            MeshData mesh = builder.build();
            if (mesh == null) return;
            if (!light) {
                ShaderInstance shader = NukeRenderTypes.cloudShader();
                float[] haze = haze(distance);
                if (shader != null) shader.safeGetUniform("HazeColor").set(haze[0], haze[1], haze[2], haze[3]);
            }
            (light ? NukeRenderTypes.FIRE : NukeRenderTypes.CLOUD).draw(mesh);
            return;
        }
        fans(list, eye, limit, PuffList.Filter.SMOKE, NukeRenderTypes.cloud());
        fans(list, eye, limit, PuffList.Filter.LIGHT, NukeRenderTypes.fire());
    }

    private static void fans(PuffList list, Vec3 eye, double limit, PuffList.Filter filter, RenderType type) {
        if (!list.has(filter)) return;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        list.writeFans(builder, eye.x, eye.y, eye.z, eye.x, eye.y, eye.z, limit, filter);
        MeshData mesh = builder.build();
        if (mesh != null) type.draw(mesh);
    }

    /**
     * A cloud past the far plane, without Distant Horizons: its cap and stem as two or three big textured sheets (the impostor
     * atlas), lit on the sun's side and hazed by the air between, pulled in to the far plane so it is drawn the same size in the
     * same place. Under a shader pack, a few big soft puffs instead.
     */
    private static void impostor(Nuke nuke, double age, Vec3 eye, ClientLevel level, float partial, double limit, QualityPreset quality) {
        CloudEvolution cloud = nuke.cloud;
        double life = cloud.life(age);
        if (life <= 0.001) return;
        // One sheet stands for puffs several deep: as opaque as they are together.
        double together = 1 - Math.pow(1 - life, 4);
        double wind = nuke.payload.windAngle();
        FarView.Sheet capSheet = FarView.capSheet(cloud, age);
        FarView.Sheet stemSheet = FarView.stemSheet(cloud, age);
        double capY = cloud.capCentre(age);
        double drift = cloud.drift(capY, age);
        double cx = nuke.ox + Math.cos(wind) * drift, cz = nuke.oz + Math.sin(wind) * drift;
        double radius = cloud.visibleCapRadius(age);
        CloudLighting.Sky sky = sky(level, partial, fireBelow(nuke, age));
        ExplosionPalette.Rgb cap = cloud.capColour(age);
        ExplosionPalette.Rgb stem = cloud.stemColour(age);
        Vec3 centre = new Vec3(cx, nuke.oy + capY, cz);
        double distance = centre.distanceTo(eye);
        double stemTop = cloud.visibleCapBottom(age);
        if (!NukeRenderTypes.textured()) {
            // Soft puffs sized to the cloud's visible cap and stem (fans show about three quarters of their radius).
            BATCH.clear();
            double ring = 0.55 * radius, size = 0.6 * radius;
            double midY = (cloud.visibleCapTop(age) + cloud.visibleCapBottom(age)) / 2;
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                BATCH.puff(cx + Math.cos(a) * ring, nuke.oy + midY, cz + Math.sin(a) * ring, size, CloudEvolution.TILE_SOFT, 0, cap.red(),
                        cap.green(), cap.blue(), (float) (0.9 * together), 0, (float) Math.cos(a), 0.3f, (float) Math.sin(a), 0.1f, 0);
            }
            BATCH.puff(cx, nuke.oy + midY + 0.2 * size, cz, size, CloudEvolution.TILE_SOFT, 0, cap.red(), cap.green(), cap.blue(),
                    (float) (0.9 * together), 0, 0, 1, 0, 0, 0);
            double stemHalf = cloud.visibleStemRadius(age);
            int pieces = Math.max(3, (int) Math.ceil(stemTop / (2 * stemHalf)));
            for (int i = 0; i < pieces; i++) {
                double y = stemTop * (i + 0.5) / pieces;
                double sd = cloud.drift(y, age);
                BATCH.puff(nuke.ox + Math.cos(wind) * sd, nuke.oy + y, nuke.oz + Math.sin(wind) * sd, stemHalf * 1.3, CloudEvolution.TILE_DENSE,
                        0, stem.red(), stem.green(), stem.blue(), (float) (0.8 * together), 0, 0, 0, 0, 0.3f, 0);
            }
            BATCH.light(sky);
            BATCH.sort(eye.x, eye.y, eye.z);
            drawBatch(BATCH, eye, limit, false, distance);
            return;
        }
        // Every sheet pulled in about the camera by the same factor as its distance: the same angle, only nearer.
        double k = FarView.pullIn(distance, limit);
        // Across the line of sight, horizontally, and the sun's side of it.
        double vx = cx - eye.x, vz = cz - eye.z;
        double vl = Math.max(1e-6, Math.sqrt(vx * vx + vz * vz));
        double ax = -vz / vl, az = vx / vl;
        double right = CloudLighting.side(sky, ax, az), left = CloudLighting.side(sky, -ax, -az);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float[] lit = new float[3];
        float a = (float) together;
        if (quality.impostorLayers() >= 3) {
            // A rear cap, a little bigger and in shade, for depth.
            double back = radius * 0.3;
            sheet(builder, eye, k, cx + vx / vl * back, nuke.oy + capSheet.centreY(), cz + vz / vl * back, ax, az, capSheet.halfWidth() * 1.05,
                    capSheet.halfHeight() * 1.05, 0, 0.5f, shade(sky, cap.scale(0.85), -ax, -az, lit, left),
                    shade(sky, cap.scale(0.85), ax, az, lit, right), a * 0.8f);
        }
        double stemDrift = cloud.drift(stemSheet.centreY(), age);
        sheet(builder, eye, k, nuke.ox + Math.cos(wind) * stemDrift, nuke.oy + stemSheet.centreY(), nuke.oz + Math.sin(wind) * stemDrift, ax, az,
                stemSheet.halfWidth(), stemSheet.halfHeight(), 0.5f, 1.0f, shade(sky, stem, -ax, -az, lit, left),
                shade(sky, stem, ax, az, lit, right), a);
        sheet(builder, eye, k, cx, nuke.oy + capSheet.centreY(), cz, ax, az, capSheet.halfWidth(), capSheet.halfHeight(), 0, 0.5f,
                shade(sky, cap, -ax, -az, lit, left), shade(sky, cap, ax, az, lit, right), a);
        MeshData mesh = builder.build();
        if (mesh == null) return;
        ShaderInstance shader = NukeRenderTypes.cloudShader();
        float[] haze = haze(distance);
        if (shader != null) shader.safeGetUniform("HazeColor").set(haze[0], haze[1], haze[2], haze[3]);
        NukeRenderTypes.IMPOSTOR.draw(mesh);
    }

    private static int shade(CloudLighting.Sky sky, ExplosionPalette.Rgb colour, double sx, double sz, float[] lit, double side) {
        CloudLighting.light(sky, colour.red(), colour.green(), colour.blue(), (float) sx, 0.3f, (float) sz, (float) (0.4 * (1 - side)), 0, lit);
        return PuffList.argb(1, lit[0], lit[1], lit[2]);
    }

    /**
     * One upright sheet of the impostor: centred at {@code (x, y, z)}, {@code halfWidth} across the line of sight
     * ({@code (ax, az)}) and {@code halfHeight} up, from the impostor texture's {@code u0} to {@code u1}, coloured left to right,
     * pulled in towards the camera by {@code k}.
     */
    private static void sheet(BufferBuilder b, Vec3 eye, double k, double x, double y, double z, double ax, double az, double halfWidth,
                              double halfHeight, float u0, float u1, int leftColour, int rightColour, float alpha) {
        double px = (x - eye.x) * k, py = (y - eye.y) * k, pz = (z - eye.z) * k;
        double w = halfWidth * k, h = halfHeight * k;
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24;
        int l = leftColour & 0x00FFFFFF | a, r = rightColour & 0x00FFFFFF | a;
        // The impostor's UV moved right by 2 tells the shader to draw it as smoke.
        b.addVertex((float) (px - ax * w), (float) (py - h), (float) (pz - az * w)).setUv(2 + u0, 1).setColor(l);
        b.addVertex((float) (px + ax * w), (float) (py - h), (float) (pz + az * w)).setUv(2 + u1, 1).setColor(r);
        b.addVertex((float) (px + ax * w), (float) (py + h), (float) (pz + az * w)).setUv(2 + u1, 0).setColor(r);
        b.addVertex((float) (px - ax * w), (float) (py + h), (float) (pz - az * w)).setUv(2 + u0, 0).setColor(l);
    }

    /**
     * For Distant Horizons' pass, which comes before the vanilla terrain: the clouds lying wholly past {@code from} blocks (the
     * vanilla terrain's edge), textured and camera-relative, far to near, or null if there are none. The vanilla pass then leaves
     * them out this frame.
     */
    @Nullable
    public static MeshData farMesh(float partial, Camera camera, double from) {
        if (ACTIVE.isEmpty() || !NukeRenderTypes.textured()) return null;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return null;
        Vec3 eye = camera.getPosition();
        QualityPreset quality = QualityPreset.of(ClientConfig.explosionQuality);
        SORTED.clear();
        SORTED.addAll(ACTIVE);
        SORTED.sort((a, b) -> Double.compare(b.fireball(b.age(partial)).distanceToSqr(eye), a.fireball(a.age(partial)).distanceToSqr(eye)));
        BufferBuilder builder = null;
        for (Nuke nuke : SORTED) {
            double age = nuke.age(partial);
            if (age > nuke.cloud.lifetime()) continue;
            double distance = nuke.fireball(age).distanceTo(eye);
            double reach = nuke.cloud.capRadius(age) + nuke.cloud.capTop(age);
            if (distance - reach < from) continue;
            prepare(nuke, age, eye, level, partial, quality, true, distance);
            if (nuke.cache.puffs().size() == 0) continue;
            if (builder == null) builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            double cx = eye.x - nuke.ox, cy = eye.y - nuke.oy, cz = eye.z - nuke.oz;
            nuke.cache.puffs().writeTextured(builder, cx, cy, cz, cx, cy, cz, 0, PuffList.Filter.ALL);
            nuke.farFrame = frame;
        }
        return builder == null ? null : builder.build();
    }

    // ------------------------------------------------------------------ the camera

    /** Adds the shaking of every nuke to {@code sum} (yaw, pitch, roll, degrees). */
    static void shake(Vec3 eye, float partial, double[] sum) {
        for (Nuke nuke : ACTIVE) {
            double age = nuke.age(partial);
            double distance = eye.distanceTo(nuke.payload.centre());
            DetonationPayload p = nuke.payload;
            CameraShake.Strength strength = CameraShake.nuclear(distance, p.yieldKg(), p.waveRadius());
            double sinceSeismic = age - ShockFront.seismicArrivalTicks(distance) / 20;
            double sinceShock = age - ShockFront.arrivalTicks(distance, p.waveSpeed(), p.waveRadius()) / 20;
            if (!CameraShake.active(strength, sinceSeismic, sinceShock)) continue;
            CameraShake.sample(strength, sinceSeismic, sinceShock, nuke.phase, SHAKE);
            sum[0] += SHAKE[0];
            sum[1] += SHAKE[1];
            sum[2] += SHAKE[2];
        }
    }

    /** The fog drawn towards the fireball's glow while it lasts. */
    static void fog(ViewportEvent.ComputeFogColor event) {
        if (ACTIVE.isEmpty() || !ClientConfig.explosionSkyTint) return;
        double tint = 0;
        ExplosionPalette palette = ExplosionPalette.FISSION;
        Vec3 eye = event.getCamera().getPosition();
        for (Nuke nuke : ACTIVE) {
            double here = glow(nuke, nuke.age((float) event.getPartialTick()), eye) * nuke.palette.fogMax();
            if (here > tint) {
                tint = here;
                palette = nuke.palette;
            }
        }
        if (tint <= 0.01) return;
        // Light added to the haze, subtle: the land must not turn orange.
        float t = (float) tint * 0.7f;
        ExplosionPalette.Rgb fog = palette.fog();
        event.setRed(Math.min(1, event.getRed() + fog.red() * t));
        event.setGreen(Math.min(1, event.getGreen() + fog.green() * t));
        event.setBlue(Math.min(1, event.getBlue() + fog.blue() * t));
    }

    /**
     * How strongly a nuke's afterglow tints the air where the player stands, 0-1: by how big the fireball looks from there, so it
     * falls fast with distance, its bulk gone in seconds and only a faint warm afterglow after.
     */
    static double glow(Nuke nuke, double age, Vec3 eye) {
        return apparent(nuke, age, eye) * afterglow(nuke, age);
    }

    /** How big the fireball looks from {@code eye}, 0-1 (1 once it fills a sixth of the sky or more). */
    static double apparent(Nuke nuke, double age, Vec3 eye) {
        double distance = Math.max(1, eye.distanceTo(nuke.fireball(age)));
        return Math.min(1, 6 * nuke.cloud.fireballRadius() / distance);
    }

    /** The afterglow over time, 1 at the detonation: its bulk fading fast, a faint tail after. */
    static double afterglow(Nuke nuke, double age) {
        double fade = nuke.palette.glowFade();
        return Math.exp(-age / fade) + 0.1 * Math.exp(-age / (6 * fade));
    }

    /**
     * How bright a nuke's flash is on screen for a player at {@code eye} looking along {@code look}: brighter nearer and facing
     * the fireball, dimmer if something stands between.
     */
    static double flash(Nuke nuke, double age, Vec3 eye, Vec3 look, Minecraft mc) {
        double fade = Math.exp(-age / nuke.palette.flashFade());
        if (fade < 0.005) return 0;
        Vec3 fireball = nuke.payload.centre().add(0, nuke.payload.fireball() * 0.5, 0);
        Vec3 toward = fireball.subtract(eye);
        double distance = Math.max(1, toward.length());
        double near = Math.min(1, 2.5 * Math.max(nuke.payload.thermalRadius(), 32) / distance);
        double facing = 0.35 + 0.65 * Math.max(0, look.dot(toward.scale(1 / distance)));
        return fade * near * facing * (sees(mc, eye, fireball) ? 1 : 0.3);
    }

    private static boolean sees(Minecraft mc, Vec3 eye, Vec3 fireball) {
        if (mc.level == null || mc.player == null || eye.distanceToSqr(fireball) > 512 * 512) return true;
        HitResult hit = mc.level.clip(new ClipContext(eye, fireball, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(fireball) < 16;
    }

    // ------------------------------------------------------------------ antimatter

    static List<DetonationPayload> annihilations() {
        return ANNIHILATIONS;
    }

    static double annihilationAge(int i, float partial) {
        ClientLevel level = Minecraft.getInstance().level;
        long now = level == null ? 0 : level.getGameTime();
        return Math.max(0, (now - ANNIHILATION_TICKS.get(i) + partial) / 20.0);
    }

    /**
     * An annihilation: a blinding white-green heart that swells and is gone in half a second, a green halo, and a shell of
     * twinkling green, cyan and white sparks racing out to the blast's edge and fading there. All light, added on.
     */
    private static void annihilations(ClientLevel level, Vec3 eye, Camera camera, float partial) {
        GLOW.clear();
        ExplosionPalette palette = ExplosionPalette.ANTIMATTER;
        for (int n = 0; n < ANNIHILATIONS.size(); n++) {
            DetonationPayload p = ANNIHILATIONS.get(n);
            double seconds = annihilationAge(n, partial);
            Vec3 c = p.centre();
            double radius = Math.max(3, p.fireball() * 2.0);
            if (seconds < 0.6) {
                double t = seconds / 0.6;
                double size = p.fireball() * palette.fireballScale() * (0.6 + 1.2 * Math.min(1, seconds / 0.12));
                ExplosionPalette.Rgb heart = palette.flash();
                GLOW.puff(c.x, c.y, c.z, size, CloudEvolution.TILE_VEIL, 0, heart.red(), heart.green(), heart.blue(), (float) Math.pow(1 - t, 1.5),
                        0, 0, 0, 0, 0, CloudEvolution.EMISSIVE);
            }
            if (seconds < 1.2) {
                double t = seconds / 1.2;
                ExplosionPalette.Rgb glow = palette.glow();
                GLOW.puff(c.x, c.y, c.z, radius * (0.5 + 0.6 * (1 - Math.exp(-seconds / 0.2))), CloudEvolution.TILE_VEIL, 0, glow.red(),
                        glow.green(), glow.blue(), (float) (0.4 * (1 - t) * (1 - t)), 0, 0, 0, 0, 0, CloudEvolution.EMISSIVE);
            }
            RandomSource random = RandomSource.create(p.seed());
            double spin = random.nextDouble() * Math.PI * 2;
            double fade = Math.max(0, 1 - seconds / (ANNIHILATION_SECONDS * 0.85));
            if (fade <= 0) continue;
            double reach = radius * (1 - Math.exp(-seconds / 0.22));
            double golden = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < SHELL_SPARKS; i++) {
                double y = 1 - 2 * (i + 0.5) / SHELL_SPARKS;
                double ring = Math.sqrt(1 - y * y);
                double a = i * golden + spin;
                double out = reach * (0.8 + 0.35 * random.nextDouble());
                double twinkle = 0.55 + 0.45 * Math.sin(seconds * (18 + 10 * random.nextDouble()) + i * 1.7);
                int colour = SHELL_COLOURS[i % SHELL_COLOURS.length];
                double size = (0.12 + 0.025 * radius) * (0.7 + 0.6 * twinkle);
                GLOW.puff(c.x + out * ring * Math.cos(a), c.y + out * y, c.z + out * ring * Math.sin(a), size, CloudEvolution.TILE_VEIL, 0,
                        ((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f, (colour & 0xFF) / 255f, (float) (fade * twinkle),
                        0, 0, 0, 0, 0, CloudEvolution.EMISSIVE);
            }
        }
        if (GLOW.size() == 0) return;
        GLOW.sort(eye.x, eye.y, eye.z);
        drawBatch(GLOW, eye, pullLimit(Minecraft.getInstance()), true, 0);
    }

    /** The particles of an annihilation: green and cyan motes and white sparks thrown out to about the blast's edge. */
    private static void annihilationParticles(DetonationPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RandomSource random = mc.level.random;
        Vec3 c = p.centre();
        double radius = Math.max(3, p.fireball() * 2.0);
        DustParticleOptions green = new DustParticleOptions(new Vector3f(0x6B / 255f, 1.0f, 0x8A / 255f), 1.6f);
        DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0x3F / 255f, 0xD8 / 255f, 0xE8 / 255f), 1.4f);
        for (int i = 0; i < 90; i++) {
            Vec3 dir = randomDirection(random);
            double speed = radius / 25 * (0.6 + 0.5 * random.nextDouble());
            mc.level.addParticle(i % 2 == 0 ? green : cyan, c.x, c.y, c.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
        for (int i = 0; i < 40; i++) {
            Vec3 dir = randomDirection(random);
            double speed = radius / 30 * (0.5 + 0.6 * random.nextDouble());
            mc.level.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
        for (int i = 0; i < 24; i++) {
            Vec3 dir = randomDirection(random);
            double speed = radius / 12 * (0.5 + 0.5 * random.nextDouble());
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
        for (int i = 0; i < 5; i++) {
            Vec3 at = c.add(randomDirection(random).scale(radius * 0.4 * random.nextDouble()));
            mc.level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
        }
    }

    private static Vec3 randomDirection(RandomSource random) {
        double y = random.nextDouble() * 2 - 1;
        double a = random.nextDouble() * Math.PI * 2;
        double ring = Math.sqrt(1 - y * y);
        return new Vec3(ring * Math.cos(a), y, ring * Math.sin(a));
    }
}
