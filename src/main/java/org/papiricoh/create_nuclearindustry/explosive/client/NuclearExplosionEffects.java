package org.papiricoh.create_nuclearindustry.explosive.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.MushroomCloudModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Client side: what a player sees and feels of a nuclear detonation ({@link DetonationPayload}). A white
 * flash, brighter facing the fireball and nearer it, and an orange glow that lingers in the sky and fog;
 * the fireball rising into a mushroom cloud ({@link MushroomCloudModel}) seen from far beyond the render
 * distance's fog; a ring of dust racing out along the ground with the shock wave, and a brief dome of
 * condensation round the fireball; the view shaking when the shock wave passes. The flash and the shake
 * follow {@code explosion.*} in the client config. Colours and timings come from the detonation's
 * {@link ExplosionPalette}: a thermonuclear one flashes harder, burns longer and leaves sooty smoke.
 * Annihilating antimatter raises no cloud and no smoke: a white-green flash at its heart, a shell of
 * green and cyan sparks thrown out to the edge of the blast and a green afterglow, over in two seconds.
 * <p>
 * The cloud and the ring are built once a frame. Without Distant Horizons all of it is drawn after the
 * weather, and what lies past the far plane is cut off. With it, its pass takes what lies beyond the
 * vanilla terrain first ({@link #farMesh}), drawn with its far-reaching projection over its distant
 * terrain, and the vanilla pass only the rest.
 */
public final class NuclearExplosionEffects {
    /** Seconds the shaking lasts. */
    private static final double SHAKE_SECONDS = 1.6;
    private static final int PUFF_SIDES = 10;
    /** The share of a puff's radius that is solid before it fades out. */
    private static final double PUFF_CORE = 0.6;
    /** Seconds an antimatter annihilation is shown. */
    private static final double ANNIHILATION_SECONDS = 2.0;
    /** Sparks in an annihilation's shell. */
    private static final int SHELL_SPARKS = 96;
    /** The shell's colours: the vial's green, the small trap's cyan and a near white. */
    private static final int[] SHELL_COLOURS = {0x6BFF8A, 0x3FD8E8, 0xD8FFE0};

    private static final List<Explosion> ACTIVE = new ArrayList<>();

    /** Frames rendered, to know which frame the quads and the far pass belong to. */
    private static long frame;
    private static long builtFrame = -1;
    private static List<Quad> built = List.of();
    /** The frame Distant Horizons last took the far quads in, and from how far (squared) they were its. */
    private static long farFrame = -1;
    private static double farFrom;

    private NuclearExplosionEffects() {}

    /** One detonation being shown. */
    private static final class Explosion {
        final DetonationPayload payload;
        final ExplosionPalette palette;
        final MushroomCloudModel cloud;
        final double groundY;
        final long startTick;
        /** Seconds after the detonation the shock wave passed the camera, or below 0 before it has. */
        double shookAt = -1;
        double shake;

        Explosion(DetonationPayload payload, long startTick) {
            this.payload = payload;
            this.palette = ExplosionPalette.of(payload.style());
            this.cloud = new MushroomCloudModel(payload.seed(), payload.scale(), payload.fireball(), payload.airburst(), payload.burstHeight(),
                    palette);
            this.groundY = payload.centre().y - Math.max(0, payload.burstHeight());
            this.startTick = startTick;
        }

        double seconds(float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            long now = mc.level == null ? startTick : mc.level.getGameTime();
            return Math.max(0, (now - startTick + partialTick) / 20.0);
        }

        boolean over(double seconds) {
            double lifetime = antimatter() ? ANNIHILATION_SECONDS : payload.dirty() ? 10 : MushroomCloudModel.LIFETIME;
            return seconds > lifetime && waveRadius(seconds) >= payload.waveRadius();
        }

        boolean antimatter() {
            return payload.style() == DetonationStyle.ANTIMATTER;
        }

        /** The blast's radius, for the shell of sparks: the payload's fireball is half of it. */
        double blastRadius() {
            return Math.max(3, payload.fireball() * 2.0);
        }

        double waveRadius(double seconds) {
            return Math.min(payload.waveRadius(), payload.waveSpeed() * seconds * 20);
        }
    }

    /** A detonation the server told us of. */
    public static void add(DetonationPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Explosion explosion = new Explosion(payload, mc.level.getGameTime());
        ACTIVE.add(explosion);
        if (explosion.antimatter()) {
            annihilationParticles(explosion);
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        built = List.of();
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        frame++;
    }

    /** How many detonations are being shown; for tests and the tour. */
    public static int active() {
        return ACTIVE.size();
    }

    // ------------------------------------------------------------------ the world: cloud and shock wave

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || ACTIVE.isEmpty()) {
            return;
        }
        List<Quad> quads = quads(event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getCamera());
        // What Distant Horizons drew already this frame is left out.
        double until = farFrame == frame ? farFrom : Double.POSITIVE_INFINITY;
        MeshData mesh = mesh(quads, 0, until);
        if (mesh != null) {
            RenderType.debugQuads().draw(mesh);
        }
    }

    /**
     * For Distant Horizons' pass, which comes before the vanilla terrain: this frame's quads at least
     * {@code from} blocks from the camera, far to near and relative to it, or null if there are none. The
     * vanilla pass then leaves them out.
     */
    @Nullable
    public static MeshData farMesh(float partial, Camera camera, double from) {
        if (ACTIVE.isEmpty()) {
            return null;
        }
        farFrame = frame;
        farFrom = from * from;
        return mesh(quads(partial, camera), farFrom, Double.POSITIVE_INFINITY);
    }

    /** This frame's quads, far to near; built on the first call of the frame. */
    private static List<Quad> quads(float partial, Camera camera) {
        if (builtFrame == frame) {
            return built;
        }
        builtFrame = frame;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return built = List.of();
        }
        Vec3 eye = camera.getPosition();
        float daylight = mc.level.getSkyDarken(partial);
        List<Quad> quads = new ArrayList<>();
        Iterator<Explosion> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            double seconds = explosion.seconds(partial);
            if (explosion.over(seconds)) {
                iterator.remove();
                continue;
            }
            if (explosion.antimatter()) {
                annihilation(explosion, seconds, camera, eye, quads);
            } else if (!explosion.payload.dirty()) {
                puffs(explosion, seconds, camera, eye, daylight, quads);
                condensation(explosion, seconds, eye, quads);
            }
            shockRing(explosion, seconds, eye, daylight, quads);
        }
        // Far to near, so the translucent puffs blend over what is behind them.
        quads.sort(Comparator.comparingDouble(Quad::depth).reversed());
        return built = quads;
    }

    /** The quads at a squared distance in [{@code from}, {@code until}), or null if none. */
    @Nullable
    private static MeshData mesh(List<Quad> quads, double from, double until) {
        BufferBuilder builder = null;
        for (Quad quad : quads) {
            if (quad.depth() >= from && quad.depth() < until) {
                if (builder == null) {
                    builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                }
                quad.emit(builder);
            }
        }
        return builder == null ? null : builder.build();
    }

    /** A quad already relative to the camera, with its distance for sorting. */
    private record Quad(float[] xyz, int[] argb, double depth) {
        void emit(BufferBuilder builder) {
            for (int i = 0; i < 4; i++) {
                builder.addVertex(xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2]).setColor(argb[i]);
            }
        }
    }

    /** The fireball and the cloud's puffs, each a soft disc turned to the camera. */
    private static void puffs(Explosion explosion, double seconds, Camera camera, Vec3 eye, float daylight, List<Quad> quads) {
        Vec3 base = new Vec3(explosion.payload.centre().x, explosion.groundY, explosion.payload.centre().z);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        for (MushroomCloudModel.Puff puff : explosion.cloud.puffs(seconds, ClientConfig.explosionCloudDetail)) {
            if (puff.alpha() <= 0.01f) {
                continue;
            }
            double cx = base.x + puff.x() - eye.x;
            double cy = base.y + puff.y() - eye.y;
            double cz = base.z + puff.z() - eye.z;
            float light = puff.glowing() ? 1.0f : 0.25f + 0.75f * daylight;
            int centre = argb(puff.alpha(), puff.red() * light, puff.green() * light, puff.blue() * light);
            int rim = centre & 0x00FFFFFF;
            disc(quads, cx, cy, cz, puff.radius(), left, up, centre, rim);
        }
    }

    /**
     * A disc of {@code radius} round a centre turned to the camera: solid out to {@link #PUFF_CORE} of its
     * radius, then fading to clear at its edge, so puffs read as soft but full balls.
     */
    private static void disc(List<Quad> quads, double cx, double cy, double cz, double radius, Vector3f left, Vector3f up,
                             int centre, int rim) {
        double depth = cx * cx + cy * cy + cz * cz;
        double core = radius * PUFF_CORE;
        for (int k = 0; k < PUFF_SIDES; k++) {
            double a0 = k * 2 * Math.PI / PUFF_SIDES;
            double a1 = (k + 1) * 2 * Math.PI / PUFF_SIDES;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            float[] inner = new float[12];
            put(inner, 0, cx, cy, cz);
            edge(inner, 1, cx, cy, cz, core, c0, s0, left, up);
            edge(inner, 2, cx, cy, cz, core, c1, s1, left, up);
            put(inner, 3, cx, cy, cz);
            quads.add(new Quad(inner, new int[]{centre, centre, centre, centre}, depth));
            float[] outer = new float[12];
            edge(outer, 0, cx, cy, cz, core, c0, s0, left, up);
            edge(outer, 1, cx, cy, cz, radius, c0, s0, left, up);
            edge(outer, 2, cx, cy, cz, radius, c1, s1, left, up);
            edge(outer, 3, cx, cy, cz, core, c1, s1, left, up);
            quads.add(new Quad(outer, new int[]{centre, rim, rim, centre}, depth));
        }
    }

    private static void edge(float[] xyz, int vertex, double cx, double cy, double cz, double r, double cos, double sin,
                             Vector3f left, Vector3f up) {
        put(xyz, vertex, cx + r * (cos * left.x() + sin * up.x()), cy + r * (cos * left.y() + sin * up.y()),
                cz + r * (cos * left.z() + sin * up.z()));
    }

    /** The shock wave's front: a ring of dust running along the ground, thinning out as it goes. */
    private static void shockRing(Explosion explosion, double seconds, Vec3 eye, float daylight, List<Quad> quads) {
        double max = explosion.payload.waveRadius();
        double radius = explosion.waveRadius(seconds);
        if (max <= 0 || radius >= max || radius <= 0) {
            return;
        }
        double fade = 1 - radius / max;
        double height = 3 + 0.04 * radius;
        float light = 0.35f + 0.65f * daylight;
        ExplosionPalette.Rgb dust = explosion.palette.shock();
        int bottom = argb((float) (0.55 * fade), dust.red() * light, dust.green() * light, dust.blue() * light);
        int top = bottom & 0x00FFFFFF;
        int segments = 96;
        Vec3 c = explosion.payload.centre();
        for (int k = 0; k < segments; k++) {
            double a0 = k * 2 * Math.PI / segments;
            double a1 = (k + 1) * 2 * Math.PI / segments;
            double x0 = c.x + radius * Math.cos(a0) - eye.x;
            double z0 = c.z + radius * Math.sin(a0) - eye.z;
            double x1 = c.x + radius * Math.cos(a1) - eye.x;
            double z1 = c.z + radius * Math.sin(a1) - eye.z;
            double y0 = explosion.groundY - eye.y;
            float[] xyz = new float[12];
            put(xyz, 0, x0, y0, z0);
            put(xyz, 1, x1, y0, z1);
            put(xyz, 2, x1, y0 + height, z1);
            put(xyz, 3, x0, y0 + height, z0);
            double mx = (x0 + x1) / 2;
            double mz = (z0 + z1) / 2;
            quads.add(new Quad(xyz, new int[]{bottom, bottom, top, top}, mx * mx + mz * mz + y0 * y0));
        }
    }

    /**
     * A brief white dome round the fireball as the shock wave leaves it: the condensation cloud. A
     * thermonuclear blast raises a second, smaller one inside the first.
     */
    private static void condensation(Explosion explosion, double seconds, Vec3 eye, List<Quad> quads) {
        if (seconds > 2.5 || explosion.payload.dirty()) {
            return;
        }
        double outer = Math.min(explosion.waveRadius(seconds), 0.7 * explosion.payload.scale());
        float alpha = (float) (explosion.palette.condensationAlpha() * (1 - seconds / 2.5));
        int color = argb(alpha, 0.95f, 0.95f, 0.97f);
        for (int dome = 0; dome < explosion.palette.condensationDomes(); dome++) {
            double radius = outer * Math.pow(0.6, dome);
            if (radius >= explosion.payload.fireball()) {
                condensationDome(explosion, radius, color, eye, quads);
            }
        }
    }

    private static void condensationDome(Explosion explosion, double radius, int color, Vec3 eye, List<Quad> quads) {
        int clear = color & 0x00FFFFFF;
        Vec3 c = explosion.payload.centre();
        int around = 32;
        int rings = 6;
        for (int i = 0; i < rings; i++) {
            double p0 = i * (Math.PI / 2) / rings;
            double p1 = (i + 1) * (Math.PI / 2) / rings;
            for (int k = 0; k < around; k++) {
                double a0 = k * 2 * Math.PI / around;
                double a1 = (k + 1) * 2 * Math.PI / around;
                float[] xyz = new float[12];
                dome(xyz, 0, c, eye, radius, p0, a0);
                dome(xyz, 1, c, eye, radius, p0, a1);
                dome(xyz, 2, c, eye, radius, p1, a1);
                dome(xyz, 3, c, eye, radius, p1, a0);
                double mx = xyz[0];
                double my = xyz[1];
                double mz = xyz[2];
                // Clearer towards the top, where the dome is thinnest.
                int lower = i == 0 ? clear : color;
                quads.add(new Quad(xyz, new int[]{lower, lower, color, color}, mx * mx + my * my + mz * mz));
            }
        }
    }

    private static void dome(float[] xyz, int vertex, Vec3 c, Vec3 eye, double radius, double pitch, double yaw) {
        double ring = radius * Math.cos(pitch);
        put(xyz, vertex, c.x + ring * Math.cos(yaw) - eye.x, c.y + radius * Math.sin(pitch) - eye.y, c.z + ring * Math.sin(yaw) - eye.z);
    }

    private static void put(float[] xyz, int vertex, double x, double y, double z) {
        xyz[vertex * 3] = (float) x;
        xyz[vertex * 3 + 1] = (float) y;
        xyz[vertex * 3 + 2] = (float) z;
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return Math.round(clamp(alpha) * 255) << 24 | Math.round(clamp(red) * 255) << 16 | Math.round(clamp(green) * 255) << 8
                | Math.round(clamp(blue) * 255);
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    // ------------------------------------------------------------------ antimatter: light and sparks, no smoke

    /**
     * An annihilation: a blinding white-green heart that swells and is gone in half a second, a green
     * halo, and a shell of twinkling green, cyan and white sparks racing out to the blast's edge and
     * fading there. Everything is light, drawn at full brightness whatever the daylight.
     */
    private static void annihilation(Explosion explosion, double seconds, Camera camera, Vec3 eye, List<Quad> quads) {
        Vec3 c = explosion.payload.centre();
        double cx = c.x - eye.x, cy = c.y - eye.y, cz = c.z - eye.z;
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double radius = explosion.blastRadius();
        ExplosionPalette palette = explosion.palette;
        if (seconds < palette.fireballSeconds()) {
            double t = seconds / palette.fireballSeconds();
            double size = explosion.payload.fireball() * palette.fireballScale() * (0.6 + 1.2 * Math.min(1, seconds / 0.12));
            float alpha = (float) Math.pow(1 - t, 1.5);
            ExplosionPalette.Rgb heart = palette.flash();
            int centre = argb(alpha, heart.red(), heart.green(), heart.blue());
            disc(quads, cx, cy, cz, size, left, up, centre, centre & 0x00FFFFFF);
        }
        double haloEnd = 1.2;
        if (seconds < haloEnd) {
            double t = seconds / haloEnd;
            double size = radius * (0.5 + 0.6 * (1 - Math.exp(-seconds / 0.2)));
            ExplosionPalette.Rgb glow = palette.glow();
            int centre = argb((float) (0.4 * (1 - t) * (1 - t)), glow.red(), glow.green(), glow.blue());
            // A little behind the sparks, so they draw over it.
            disc(quads, cx, cy, cz, size, left, up, centre, centre & 0x00FFFFFF, 0.5);
        }
        RandomSource random = RandomSource.create(explosion.payload.seed());
        double spin = random.nextDouble() * Math.PI * 2;
        double fade = Math.max(0, 1 - seconds / (ANNIHILATION_SECONDS * 0.85));
        if (fade <= 0) {
            return;
        }
        double reach = radius * (1 - Math.exp(-seconds / 0.22));
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < SHELL_SPARKS; i++) {
            // A Fibonacci sphere, turned by the seed, each spark a little nearer or farther.
            double y = 1 - 2 * (i + 0.5) / SHELL_SPARKS;
            double ring = Math.sqrt(1 - y * y);
            double a = i * golden + spin;
            double out = reach * (0.8 + 0.35 * random.nextDouble());
            double twinkle = 0.55 + 0.45 * Math.sin(seconds * (18 + 10 * random.nextDouble()) + i * 1.7);
            int colour = SHELL_COLOURS[i % SHELL_COLOURS.length];
            float alpha = (float) (fade * twinkle);
            int centre = argb(alpha, ((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f, (colour & 0xFF) / 255f);
            double size = (0.12 + 0.025 * radius) * (0.7 + 0.6 * twinkle);
            disc(quads, cx + out * ring * Math.cos(a), cy + out * y, cz + out * ring * Math.sin(a), size, left, up, centre,
                    centre & 0x00FFFFFF);
        }
    }

    /** {@link #disc} sorted as if {@code back} blocks farther from the camera than it is. */
    private static void disc(List<Quad> quads, double cx, double cy, double cz, double radius, Vector3f left, Vector3f up, int centre,
                             int rim, double back) {
        int from = quads.size();
        disc(quads, cx, cy, cz, radius, left, up, centre, rim);
        double distance = Math.sqrt(cx * cx + cy * cy + cz * cz) + back;
        for (int i = from; i < quads.size(); i++) {
            Quad quad = quads.get(i);
            quads.set(i, new Quad(quad.xyz(), quad.argb(), distance * distance));
        }
    }

    /**
     * The particles of an annihilation, spawned once on the client: green and cyan motes and white
     * sparks thrown out in every direction to about the blast's edge, and a few flashes inside it.
     */
    private static void annihilationParticles(Explosion explosion) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        RandomSource random = mc.level.random;
        Vec3 c = explosion.payload.centre();
        double radius = explosion.blastRadius();
        DustParticleOptions green = new DustParticleOptions(new Vector3f(0x6B / 255f, 1.0f, 0x8A / 255f), 1.6f);
        DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0x3F / 255f, 0xD8 / 255f, 0xE8 / 255f), 1.4f);
        for (int i = 0; i < 90; i++) {
            Vec3 dir = randomDirection(random);
            // Dust slows by 4 % a tick: it travels about 25 times its first speed.
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

    // ------------------------------------------------------------------ the screen: flash and glow

    /** The flash, white and brief, and the orange glow after it, over the whole screen. */
    public static void renderOverlay(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || mc.player == null || mc.level == null) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        double flash = 0;
        double glow = 0;
        ExplosionPalette.Rgb flashColour = ExplosionPalette.FISSION.flash();
        ExplosionPalette.Rgb glowColour = ExplosionPalette.FISSION.glow();
        Vec3 eye = mc.player.getEyePosition(partial);
        Vec3 look = mc.player.getViewVector(partial);
        for (Explosion explosion : ACTIVE) {
            double seconds = explosion.seconds(partial);
            Vec3 fireball = explosion.payload.centre().add(0, explosion.payload.fireball() * 0.5, 0);
            Vec3 toward = fireball.subtract(eye);
            double distance = Math.max(1, toward.length());
            double near = explosion.antimatter() ? Math.min(1, 6 * explosion.blastRadius() / distance)
                    : explosion.payload.dirty() ? Math.min(1, 24 / distance)
                    : Math.min(1, 2.5 * Math.max(explosion.payload.thermalRadius(), 32) / distance);
            double facing = 0.35 + 0.65 * Math.max(0, look.dot(toward.scale(1 / distance)));
            boolean seen = sees(mc, eye, fireball);
            ExplosionPalette palette = explosion.palette;
            double flashHere = Math.exp(-seconds / palette.flashFade()) * near * facing * (seen ? 1 : 0.3)
                    * (explosion.payload.dirty() && !explosion.antimatter() ? 0.5 : 1);
            if (flashHere > flash) {
                flash = flashHere;
                flashColour = palette.flash();
            }
            if (!explosion.payload.dirty() || explosion.antimatter()) {
                double glowHere = Math.exp(-seconds / palette.glowFade()) * Math.min(1, near * 1.5) * palette.glowAlpha();
                if (glowHere > glow) {
                    glow = glowHere;
                    glowColour = palette.glow();
                }
            }
        }
        flash *= ClientConfig.explosionFlash;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (glow > 0.01 * ExplosionPalette.FISSION.glowAlpha()) {
            graphics.fill(0, 0, width, height, argb((float) glow, glowColour.red(), glowColour.green(), glowColour.blue()));
        }
        if (flash > 0.01) {
            graphics.fill(0, 0, width, height, argb((float) Math.min(1, flash), flashColour.red(), flashColour.green(), flashColour.blue()));
        }
    }

    private static boolean sees(Minecraft mc, Vec3 eye, Vec3 fireball) {
        if (eye.distanceToSqr(fireball) > 512 * 512) {
            return true;
        }
        HitResult hit = mc.level.clip(new ClipContext(eye, fireball, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(fireball) < 16;
    }

    /** The fog takes the glow's orange too, which tints the horizon and the distance. */
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || mc.player == null) {
            return;
        }
        double tint = 0;
        ExplosionPalette palette = ExplosionPalette.FISSION;
        Vec3 eye = event.getCamera().getPosition();
        for (Explosion explosion : ACTIVE) {
            if (explosion.payload.dirty() && !explosion.antimatter()) {
                continue;
            }
            double seconds = explosion.seconds((float) event.getPartialTick());
            double distance = Math.max(1, eye.distanceTo(explosion.payload.centre()));
            double reach = explosion.antimatter() ? 4 * explosion.blastRadius() : explosion.payload.thermalRadius();
            double here = Math.min(explosion.palette.fogMax(), Math.exp(-seconds / explosion.palette.glowFade())
                    * Math.min(1, 3.0 * reach / distance));
            if (here > tint) {
                tint = here;
                palette = explosion.palette;
            }
        }
        if (tint <= 0.01) {
            return;
        }
        float t = (float) tint;
        ExplosionPalette.Rgb fog = palette.fog();
        event.setRed(event.getRed() + (fog.red() - event.getRed()) * t);
        event.setGreen(event.getGreen() + (fog.green() - event.getGreen()) * t);
        event.setBlue(event.getBlue() + (fog.blue() - event.getBlue()) * t);
    }

    /** The shock wave shakes the view when it reaches the camera, harder the stronger it still is. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || mc.player == null || !ClientConfig.explosionShake) {
            return;
        }
        Vec3 eye = event.getCamera().getPosition();
        double yaw = 0;
        double pitch = 0;
        double roll = 0;
        for (Explosion explosion : ACTIVE) {
            double seconds = explosion.seconds((float) event.getPartialTick());
            double distance = eye.distanceTo(explosion.payload.centre());
            if (explosion.shookAt < 0) {
                if (explosion.waveRadius(seconds) < distance || distance > explosion.payload.waveRadius() * 1.5) {
                    continue;
                }
                explosion.shookAt = seconds;
                double kPa = BlastWaveModel.overpressure(distance, explosion.payload.yieldKg());
                explosion.shake = Math.max(0.15, Math.min(1.5, kPa / 15));
            }
            double since = seconds - explosion.shookAt;
            if (since > SHAKE_SECONDS) {
                continue;
            }
            double amount = explosion.shake * 3.0 * (1 - since / SHAKE_SECONDS);
            yaw += amount * Math.sin(since * 47);
            pitch += amount * Math.sin(since * 61 + 1.3);
            roll += amount * 0.6 * Math.sin(since * 37 + 2.1);
        }
        event.setYaw(event.getYaw() + (float) yaw);
        event.setPitch(event.getPitch() + (float) pitch);
        event.setRoll(event.getRoll() + (float) roll);
    }
}
