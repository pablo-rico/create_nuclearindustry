package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.ShockFront;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalBlastPayload;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.BlastFxModel;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CameraShake;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.ExplosionAudio;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.QualityPreset;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * What a player sees, hears and feels of a conventional blast ({@link ConventionalBlastPayload}): its fireball (fire tiles of the
 * atlas round a white core of light), sparks of casing and embers, chunks and dust of the ground in its colour, a shock ring and
 * a ring of dust racing out, the shaking, and the column of black smoke that rises, leans with the wind and hangs for a minute or
 * two ({@link BlastFxModel}) — the part a player arriving late still sees. A thermobaric charge first hangs a haze of fuel, then
 * burns it in a wide, low fireball that rolls out over the ground and keeps burning. Bomblets are small blasts, cheap: their smoke
 * shares one batch with every other blast's, and many going off at once are heard as one crackle.
 */
public final class ConventionalFx {
    private static final List<Blast> ACTIVE = new ArrayList<>();
    private static final PuffList SMOKE = new PuffList();
    private static final PuffList LIGHT = new PuffList();
    private static final double[] SHAKE = new double[3];
    private static final ExplosionAudio.CrackleWindow CRACKLE = new ExplosionAudio.CrackleWindow();
    /** Bomblets heard and their mean position, for where a crackle comes from. */
    private static double crackleX, crackleY, crackleZ;
    private static int crackleCount;

    private ConventionalFx() {}

    /** One blast being shown. */
    static final class Blast {
        final ConventionalBlastPayload payload;
        final BlastFxModel model;
        final long startTick;
        final double groundY;
        final BlockState ground;
        /** The colour of the dust it raises. */
        final ExplosionPalette.Rgb dust;
        final double phase;
        long played;
        boolean soundsDone;
        boolean thrown;

        Blast(ConventionalBlastPayload payload, BlastFxModel model, double groundY, @Nullable BlockState ground, ExplosionPalette.Rgb dust) {
            this.payload = payload;
            this.dust = ExplosionPalette.DUST.lerp(dust, 0.5);
            this.model = model;
            this.startTick = payload.startTick();
            this.groundY = groundY;
            this.ground = ground;
            this.phase = (payload.seed() & 0xFFFF) / 65536.0 * 6.283;
        }

        double age(float partial) {
            ClientLevel level = Minecraft.getInstance().level;
            long now = level == null ? startTick : level.getGameTime();
            return Math.max(0, (now - startTick + partial) / 20.0);
        }

        Vec3 centre() {
            return payload.centre();
        }

        boolean over(double age) {
            return model.over(age) && soundsDone;
        }
    }

    /** A conventional blast the server told us of. */
    public static void add(ConventionalBlastPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (Blast blast : ACTIVE) {
            // Sent again on coming back in range: it is already here.
            if (blast.payload.seed() == payload.seed() && blast.startTick == payload.startTick()) return;
        }
        Vec3 c = payload.centre();
        double groundY = c.y - Math.max(0, payload.burstHeight());
        BlockPos below = BlockPos.containing(c.x, groundY - 0.5, c.z);
        BlockState state = level.hasChunkAt(below) ? level.getBlockState(below) : null;
        if (state != null && state.isAir()) state = level.getBlockState(below.below());
        ExplosionPalette.Rgb ground = state == null ? ExplosionPalette.DUST : NuclearFx.ground(level, c.x, groundY, c.z);
        BlastFxModel model = new BlastFxModel(payload.kind(), payload.chargeKg(), payload.fireballRadius(), payload.fireRadius(),
                payload.airburst(), payload.seed(), ground);
        ACTIVE.add(new Blast(payload, model, groundY, state, ground));
    }

    /** An old-style dirty detonation payload: plain high explosive of its size. */
    static void addDirty(DetonationPayload p) {
        double charge = Math.max(1, Math.pow(Math.max(1, p.yieldKg()) / 50.0, 1.0));
        add(new ConventionalBlastPayload(p.centre(), ConventionalKind.DIRTY, p.seed(), p.startTick(), (float) Math.min(192, charge),
                p.burstHeight(), p.airburst(), Math.max(2, p.fireball()), 0, Math.max(8, p.waveRadius()),
                p.waveSpeed() > 0 ? p.waveSpeed() : (float) CivilDefenseConfig.blastWaveSpeed, p.yieldKg(), 0));
    }

    public static int active() {
        return ACTIVE.size();
    }

    static List<Blast> blasts() {
        return ACTIVE;
    }

    static void clear() {
        ACTIVE.clear();
        CRACKLE.clear();
        crackleCount = 0;
    }

    // ------------------------------------------------------------------ each tick

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            ACTIVE.clear();
            return;
        }
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Blast blast = ACTIVE.get(i);
            double age = blast.age(0);
            sounds(level, blast, age, eye);
            if (blast.over(age)) {
                ACTIVE.remove(i);
                continue;
            }
            particles(level, blast, age, eye);
        }
    }

    private static void sounds(ClientLevel level, Blast blast, double age, Vec3 eye) {
        if (blast.soundsDone) return;
        ConventionalBlastPayload p = blast.payload;
        double distance = eye.distanceTo(p.centre());
        double ticks = age * 20;
        List<ExplosionAudio.Cue> cues = ExplosionAudio.conventional(distance, p.kind(), p.chargeKg(), p.waveSpeed(), p.waveRadius(), p.airburst());
        blast.played = ExplosionSounds.play(cues, ticks, blast.played, p.centre());
        double arrival = ShockFront.arrivalTicks(distance, p.waveSpeed(), p.waveRadius());
        if (p.kind() == ConventionalKind.BOMBLET && (blast.played & (1L << 62)) == 0 && ticks >= arrival) {
            blast.played |= 1L << 62;
            if (ticks - arrival <= ExplosionAudio.STALE_TICKS) {
                crackleX += p.centre().x;
                crackleY += p.centre().y;
                crackleZ += p.centre().z;
                crackleCount++;
                if (CRACKLE.hear(level.getGameTime())) {
                    Vec3 at = new Vec3(crackleX / crackleCount, crackleY / crackleCount, crackleZ / crackleCount);
                    double far = eye.distanceTo(at);
                    ExplosionSounds.now(ExplosionAudio.Sound.CLUSTER_CRACKLE, at,
                            (float) (ExplosionAudio.Sound.CLUSTER_CRACKLE.gain() * Math.max(0.15, 1 / (1 + far / 300))), 1.0f);
                    crackleX = crackleY = crackleZ = 0;
                    crackleCount = 0;
                }
            }
        }
        double last = arrival + 40;
        for (ExplosionAudio.Cue cue : cues) last = Math.max(last, cue.tick());
        if (ticks > last + ExplosionAudio.STALE_TICKS + 1) blast.soundsDone = true;
    }

    /**
     * The particles of a blast, once, when it goes off (skipped for a player told late): sparks of casing, embers, flames, chunks
     * and dust of the ground; while a thermobaric cloud burns, flames over the ground it covers.
     */
    private static void particles(ClientLevel level, Blast blast, double age, Vec3 eye) {
        ConventionalBlastPayload p = blast.payload;
        BlastFxModel m = blast.model;
        double distance = eye.distanceTo(p.centre());
        if (distance > 192) return;
        RandomSource random = level.random;
        Vec3 c = p.centre();
        boolean bomblet = p.kind() == ConventionalKind.BOMBLET;
        if (!blast.thrown && age >= m.hazeSeconds()) {
            blast.thrown = true;
            if (age > m.fireEnd() + 0.5) return;
            double size = Math.max(0.15, Math.min(1.5, m.size()));
            double r = p.fireballRadius();
            int sparks = ExplosionParticles.quota(ExplosionParticles.scaled((bomblet ? 6 : 16 + 40 * size) * (m.thermobaric() ? 0.4 : 1)));
            for (int i = 0; i < sparks; i++) {
                Vec3 dir = direction(random, p.airburst());
                double speed = (bomblet ? 0.8 : 1.2 + 1.5 * size) * (0.5 + random.nextDouble());
                ExplosionParticles.spark(level, c.x + dir.x * r * 0.3, c.y + dir.y * r * 0.3, c.z + dir.z * r * 0.3, dir.x * speed,
                        dir.y * speed + 0.15, dir.z * speed, 0.1f + random.nextFloat() * 0.1f, 14 + random.nextInt(16));
            }
            int flames = ExplosionParticles.quota(ExplosionParticles.scaled(bomblet ? 4 : 10 + 20 * size));
            for (int i = 0; i < flames; i++) {
                Vec3 dir = direction(random, p.airburst());
                double speed = 0.15 + 0.25 * random.nextDouble();
                ExplosionParticles.flame(level, c.x + dir.x * r * 0.6, c.y + dir.y * r * 0.4 + 0.3, c.z + dir.z * r * 0.6, dir.x * speed,
                        Math.abs(dir.y) * speed + 0.05, dir.z * speed, (float) (0.5 + 0.6 * r * 0.3), 10 + random.nextInt(10), 0.6 + 0.3 * random.nextDouble());
            }
            int embers = ExplosionParticles.quota(ExplosionParticles.scaled(bomblet ? 3 : 6 + 14 * size));
            for (int i = 0; i < embers; i++) {
                Vec3 dir = direction(random, p.airburst());
                double speed = 0.3 + 0.4 * random.nextDouble();
                level.addParticle(RocketFxRegistry.EMBER.get(), c.x, c.y + 0.5, c.z, dir.x * speed, dir.y * speed + 0.3, dir.z * speed);
            }
            boolean ground = !p.airburst() && blast.ground != null && c.y - blast.groundY < r + 2;
            if (ground) {
                int chunks = ExplosionParticles.quota(ExplosionParticles.scaled((bomblet ? 3 : 8 + 30 * size) * (m.thermobaric() ? 0.3 : 1)));
                for (int i = 0; i < chunks; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double out = 0.3 + 0.5 * random.nextDouble() * (1 + size);
                    ExplosionParticles.debris(level, blast.ground, c.x + Math.cos(a) * r * 0.3, blast.groundY + 0.5, c.z + Math.sin(a) * r * 0.3,
                            Math.cos(a) * out, 0.5 + random.nextDouble() * (0.6 + 0.8 * size), Math.sin(a) * out,
                            0.15f + random.nextFloat() * 0.2f, 60 + random.nextInt(40));
                }
                int dust = ExplosionParticles.quota(ExplosionParticles.scaled(bomblet ? 4 : 10 + 24 * size));
                for (int i = 0; i < dust; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double out = 0.2 + 0.4 * random.nextDouble() * (1 + size);
                    ExplosionParticles.dust(level, blast.ground, c.x + Math.cos(a) * r * 0.5, blast.groundY + 0.4, c.z + Math.sin(a) * r * 0.5,
                            Math.cos(a) * out, 0.05 + 0.1 * random.nextDouble(), Math.sin(a) * out, (float) (1 + 1.5 * size), 60 + random.nextInt(40));
                }
            }
        }
        // A thermobaric cloud burns over the ground it covers.
        if (m.thermobaric() && age > m.hazeSeconds() && age < m.fireEnd() + 2) {
            double reach = Math.max(m.fireballWidth(Math.min(age, m.fireEnd() - 0.01)), p.fireballRadius());
            int n = ExplosionParticles.quota(ExplosionParticles.scaled(3 + 4 * m.size()));
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double out = reach * Math.sqrt(random.nextDouble());
                ExplosionParticles.flame(level, c.x + Math.cos(a) * out, blast.groundY + 0.5 + random.nextDouble(), c.z + Math.sin(a) * out,
                        0, 0.06 + 0.06 * random.nextDouble(), 0, (float) (1 + random.nextDouble()), 14 + random.nextInt(12), 0.45 + 0.2 * random.nextDouble());
            }
        }
    }

    private static Vec3 direction(RandomSource random, boolean sphere) {
        double y = sphere ? random.nextDouble() * 2 - 1 : random.nextDouble() * 0.9 + 0.1;
        double a = random.nextDouble() * Math.PI * 2;
        double ring = Math.sqrt(1 - y * y);
        return new Vec3(ring * Math.cos(a), y, ring * Math.sin(a));
    }

    // ------------------------------------------------------------------ drawing

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != NukeRenderTypes.stage() || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 eye = event.getCamera().getPosition();
        QualityPreset quality = QualityPreset.of(ClientConfig.explosionQuality);
        double limit = NuclearFx.pullLimit(mc);
        SMOKE.clear();
        LIGHT.clear();
        double nearest = Double.POSITIVE_INFINITY;
        for (Blast blast : ACTIVE) {
            double age = blast.age(partial);
            if (blast.model.over(age)) continue;
            Vec3 c = blast.centre();
            double distance = c.distanceTo(eye);
            nearest = Math.min(nearest, distance);
            double lod = quality.lod(distance * 4);
            SMOKE.offset(c.x, c.y, c.z);
            SMOKE.scale((float) QualityPreset.lodGrowth(lod), 1);
            blast.model.smoke(age, (int) Math.max(3, blast.model.smokePuffs(quality.smokePerBlast()) * lod * ClientConfig.explosionCloudDetail), SMOKE);
            fireball(blast, age, c, quality);
            shockRing(blast, age);
        }
        SMOKE.offset(0, 0, 0);
        SMOKE.scale(1, 1);
        if (SMOKE.size() > 0) {
            SMOKE.light(NuclearFx.sky(level, partial, 0));
            SMOKE.sort(eye.x, eye.y, eye.z);
            NuclearFx.drawBatch(SMOKE, eye, limit, false, nearest);
        }
        if (LIGHT.size() > 0) {
            LIGHT.sort(eye.x, eye.y, eye.z);
            NuclearFx.drawBatch(LIGHT, eye, limit, true, nearest);
        }
        groundRings(eye, partial);
    }

    /**
     * The fireball: fire tiles in the smoke batch (they sort with the smoke and glow by their temperature), a corona of light,
     * and for a thermobaric charge first the haze of its fuel.
     */
    private static void fireball(Blast blast, double age, Vec3 c, QualityPreset quality) {
        BlastFxModel m = blast.model;
        double[] haze = m.haze(age);
        if (haze != null) {
            int n = 8;
            for (int i = 0; i < n; i++) {
                double a = i * 2 * Math.PI / n + blast.phase;
                SMOKE.puff(Math.cos(a) * haze[0] * 0.5, -c.y + blast.groundY + haze[0] * 0.25, Math.sin(a) * haze[0] * 0.5, haze[0] * 0.55,
                        CloudEvolution.TILE_VEIL, (float) a, 0.86f, 0.85f, 0.82f, (float) haze[1], 0, 0, 0.6f, 0, 0, 0);
            }
        }
        double width = m.fireballWidth(age);
        if (width <= 0) return;
        double height = m.fireballHeight(age);
        double heat = m.fireballHeat(age);
        double s = age - m.hazeSeconds();
        float fade = (float) (1 - CloudEvolution.smooth((s - 0.6 * m.fireballSeconds()) / (0.4 * m.fireballSeconds())));
        if (fade <= 0.01) return;
        boolean thermobaric = m.thermobaric();
        double baseY = thermobaric ? blast.groundY - c.y + height * 0.8 : 0;
        int count = thermobaric ? Math.max(8, quality.smokePerBlast() / 3) : Math.max(5, quality.smokePerBlast() / 6);
        for (int i = 0; i < count; i++) {
            double u = (i + 0.5) / count;
            double a = i * 2.39996 + blast.phase;
            double rr = Math.sqrt(u);
            double x = Math.cos(a) * rr * width * 0.6, z = Math.sin(a) * rr * width * 0.6;
            double y = baseY + Math.sin(i * 1.7) * height * 0.35 + s * (thermobaric ? 0.4 : 1.5);
            ExplosionPalette.Rgb fire = ExplosionPalette.fire(Math.min(1, heat + 0.15 * (1 - rr)));
            double size = (thermobaric ? 0.55 : 0.6) * Math.max(height, width * (thermobaric ? 0.35 : 0.6)) * (0.8 + 0.3 * ((i * 7) % 5) / 4.0);
            SMOKE.puff(x, y, z, size, CloudEvolution.TILE_FIRE + (i & 3), (float) (a + s), fire.red(), fire.green(), fire.blue(), fade, 1,
                    0, 0, 0, 0, CloudEvolution.EMISSIVE);
        }
        ExplosionPalette.Rgb core = ExplosionPalette.fire(heat);
        LIGHT.offset(c.x, c.y, c.z);
        LIGHT.puff(0, baseY, 0, Math.max(width, height) * 1.6, CloudEvolution.TILE_VEIL, 0, core.red(), core.green(), core.blue(),
                (float) (0.7 * fade * Math.min(1, heat * 1.5)), 0, 0, 0, 0, 0, CloudEvolution.EMISSIVE);
        LIGHT.offset(0, 0, 0);
    }

    /** The shock front racing out: a ring of dust puffs along the ground while it still carries. */
    private static void shockRing(Blast blast, double age) {
        ConventionalBlastPayload p = blast.payload;
        double front = Math.min(p.waveRadius(), p.waveSpeed() * 20 * age);
        double travel = p.waveRadius() / Math.max(0.1, p.waveSpeed() * 20);
        if (front <= p.fireballRadius() || age > travel + 1.5 || p.waveRadius() < 6) return;
        double settle = 1 - Math.max(0, Math.min(1, (age - travel) / 1.5));
        double strength = Math.pow(Math.max(0, 1 - front / p.waveRadius()), 0.5) * 0.6 + 0.2;
        double alpha = 0.55 * strength * settle * (p.airburst() ? 0.5 : 1);
        if (alpha < 0.02) return;
        double rel = blast.groundY - p.centre().y;
        int n = Math.max(10, Math.min(48, (int) (front * 0.6)));
        double size = Math.max(1.2, Math.min(8, 0.6 * 2 * Math.PI * front / n));
        ExplosionPalette.Rgb dust = blast.dust;
        for (int i = 0; i < n; i++) {
            double a = (i + 0.5) * 2 * Math.PI / n + blast.phase;
            SMOKE.puff(Math.cos(a) * front, rel + size * 0.4, Math.sin(a) * front, size, CloudEvolution.TILE_DENSE + (i & 3), (float) a,
                    dust.red(), dust.green(), dust.blue(), (float) alpha, 0, (float) Math.cos(a), 0.5f, (float) Math.sin(a), 0, 0);
        }
    }

    /**
     * The shock's ring seen on the ground: the atlas' annulus laid flat, spreading with the front, gone as it passes. Only when
     * textured; under a shader pack the dust ring alone shows it.
     */
    private static void groundRings(Vec3 eye, float partial) {
        if (!NukeRenderTypes.textured()) return;
        BufferBuilder builder = null;
        float u0 = (CloudEvolution.TILE_RING % 4) / 4f, v0 = (CloudEvolution.TILE_RING / 4) / 4f, u1 = u0 + 0.25f, v1 = v0 + 0.25f;
        for (Blast blast : ACTIVE) {
            ConventionalBlastPayload p = blast.payload;
            if (p.airburst() && p.burstHeight() > p.waveRadius() * 0.5) continue;
            double age = blast.age(partial);
            double front = Math.min(p.waveRadius(), p.waveSpeed() * 20 * age);
            double travel = p.waveRadius() / Math.max(0.1, p.waveSpeed() * 20);
            if (front <= p.fireballRadius() || age > travel + 0.5) continue;
            float alpha = (float) (0.5 * (1 - front / p.waveRadius()) * (1 - Math.max(0, (age - travel) / 0.5)));
            if (alpha < 0.02) continue;
            if (builder == null) builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            float x = (float) (p.centre().x - eye.x), y = (float) (blast.groundY + 0.3 - eye.y), z = (float) (p.centre().z - eye.z);
            float r = (float) (front * 1.05);
            int colour = PuffList.argb(alpha, 0.85f, 0.83f, 0.8f);
            builder.addVertex(x - r, y, z - r).setUv(u0, v0).setColor(colour);
            builder.addVertex(x - r, y, z + r).setUv(u0, v1).setColor(colour);
            builder.addVertex(x + r, y, z + r).setUv(u1, v1).setColor(colour);
            builder.addVertex(x + r, y, z - r).setUv(u1, v0).setColor(colour);
        }
        if (builder == null) return;
        MeshData mesh = builder.build();
        if (mesh == null) return;
        ShaderInstance shader = NukeRenderTypes.cloudShader();
        if (shader != null) shader.safeGetUniform("HazeColor").set(0f, 0f, 0f, 0f);
        NukeRenderTypes.CLOUD.draw(mesh);
    }

    // ------------------------------------------------------------------ the camera and the screen

    /** Adds the shaking of every blast to {@code sum}. */
    static void shake(Vec3 eye, float partial, double[] sum) {
        for (Blast blast : ACTIVE) {
            ConventionalBlastPayload p = blast.payload;
            double distance = eye.distanceTo(p.centre());
            CameraShake.Strength strength = CameraShake.conventional(distance, p.yieldKg(), p.waveRadius());
            if (strength.none()) continue;
            double age = blast.age(partial);
            double sinceSeismic = age - ShockFront.seismicArrivalTicks(distance) / 20;
            double sinceShock = age - ShockFront.arrivalTicks(distance, p.waveSpeed(), p.waveRadius()) / 20;
            if (!CameraShake.active(strength, sinceSeismic, sinceShock)) continue;
            CameraShake.sample(strength, sinceSeismic, sinceShock, blast.phase, SHAKE);
            sum[0] += SHAKE[0];
            sum[1] += SHAKE[1];
            sum[2] += SHAKE[2];
        }
    }

    /** How bright the nearest big blast's flash is for a player at {@code eye}, 0-1: a fireball close by lights the view briefly. */
    static double flash(Vec3 eye, Vec3 look, float partial) {
        double best = 0;
        for (Blast blast : ACTIVE) {
            BlastFxModel m = blast.model;
            double age = blast.age(partial);
            if (age > m.fireEnd() + 0.3) continue;
            double heat = m.fireballHeat(age);
            Vec3 toward = blast.centre().subtract(eye);
            double distance = Math.max(1, toward.length());
            double near = Math.min(1, 6 * Math.max(1, blast.payload.fireballRadius()) / distance);
            double facing = 0.3 + 0.7 * Math.max(0, look.dot(toward.scale(1 / distance)));
            best = Math.max(best, 0.35 * heat * near * near * facing);
        }
        return best;
    }

    /** The hottest fireball near the player, for the heat haze: centre, radius and strength; null if none. */
    @Nullable
    static double[] hotSpot(Vec3 eye, float partial) {
        double[] best = null;
        double bestScore = 0;
        for (Blast blast : ACTIVE) {
            BlastFxModel m = blast.model;
            double age = blast.age(partial);
            if (age > m.fireEnd() + 2) continue;
            double distance = eye.distanceTo(blast.centre());
            double score = Math.max(0, 1 - (age - m.fireEnd()) / 2) * Math.min(1, 40 / Math.max(1, distance));
            if (score > bestScore) {
                bestScore = score;
                double width = Math.max(m.fireballWidth(Math.min(age, m.fireEnd() - 0.01)), blast.payload.fireballRadius());
                Vec3 c = blast.centre();
                best = new double[]{c.x, c.y, c.z, width, 0.6 * score, blast.groundY, blast.payload.waveSpeed() * 20 * age, blast.payload.waveRadius()};
            }
        }
        return best;
    }
}
