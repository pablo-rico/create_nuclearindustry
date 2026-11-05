package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.QualityPreset;

import java.util.ArrayList;
import java.util.List;

/**
 * The particles explosions throw: flames of a fireball, sparks of its casing, chunks and dust of the ground in the ground's own
 * colour, soot from fires, condensation, ash and black rain under a cloud. The effects make them directly with every setting
 * (colour, size, life) and keep count, so a sky full of bomblets never spends more than the quality allows
 * ({@link QualityPreset#particleBudget}); the types' providers only serve {@code /particle}.
 */
public final class ExplosionParticles {
    /** Translucent puffs without depth writes, so one drawn first never punches holes in those behind. */
    static final ParticleRenderType SHEET = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "create_nuclearindustry:explosion_sheet";
        }
    };

    private static final int FULL_BRIGHT = 0xF000F0;

    @Nullable
    private static SpriteSet ash, rain, smoke, flame, spark, mist, dust, debris;
    private static final List<Particle> ALIVE = new ArrayList<>();
    @Nullable
    private static ClientLevel aliveIn;

    private ExplosionParticles() {}

    static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ExplosionFxRegistry.NUKE_ASH.get(), s -> simple(ash = s, Kind.ASH));
        event.registerSpriteSet(ExplosionFxRegistry.BLACK_RAIN.get(), s -> simple(rain = s, Kind.RAIN));
        event.registerSpriteSet(ExplosionFxRegistry.FIRE_SMOKE.get(), s -> simple(smoke = s, Kind.SMOKE));
        event.registerSpriteSet(ExplosionFxRegistry.FIREBALL_FLAME.get(), s -> simple(flame = s, Kind.FLAME));
        event.registerSpriteSet(ExplosionFxRegistry.SHRAPNEL_SPARK.get(), s -> simple(spark = s, Kind.SPARK));
        event.registerSpriteSet(ExplosionFxRegistry.CONDENSATION_MIST.get(), s -> simple(mist = s, Kind.MIST));
        event.registerSpriteSet(ExplosionFxRegistry.BLAST_DUST.get(), s -> {
            dust = s;
            return (BlockParticleOption o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) ->
                    new Puff(level, x, y, z, vx, vy, vz, s, groundColour(level, o.getState(), x, y, z), 0.7f, 1.5f, 4f, 80, 0.01f, 0.92f, false);
        });
        event.registerSpriteSet(ExplosionFxRegistry.DEBRIS_CHUNK.get(), s -> {
            debris = s;
            return (BlockParticleOption o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) ->
                    new Chunk(level, x, y, z, vx, vy, vz, s, groundColour(level, o.getState(), x, y, z), 0.3f, 60);
        });
    }

    private enum Kind { ASH, RAIN, SMOKE, FLAME, SPARK, MIST }

    private static net.minecraft.client.particle.ParticleProvider<SimpleParticleType> simple(SpriteSet s, Kind kind) {
        return (type, level, x, y, z, vx, vy, vz) -> switch (kind) {
            case ASH -> new Ash(level, x, y, z, s, 0.12f, 200);
            case RAIN -> new Rain(level, x, y, z, s, 60);
            case SMOKE -> new Puff(level, x, y, z, vx, vy, vz, s, 0x3A3836, 0.8f, 1f, 4f, 120, -0.004f, 0.96f, false);
            case FLAME -> new Puff(level, x, y, z, vx, vy, vz, s, 0xFFB050, 0.9f, 0.6f, 1.2f, 16, -0.01f, 0.9f, true);
            case SPARK -> new Spark(level, x, y, z, vx, vy, vz, s, 0.12f, 24);
            case MIST -> new Puff(level, x, y, z, vx, vy, vz, s, 0xF2F3F5, 0.5f, 2f, 5f, 40, 0, 0.9f, false);
        };
    }

    // ------------------------------------------------------------------ the budget

    /** Explosion particles alive now. */
    static int alive() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != aliveIn) {
            ALIVE.clear();
            aliveIn = level;
        }
        return ALIVE.size();
    }

    /** How many more may be added now, out of {@code wanted}. */
    static int quota(int wanted) {
        int left = QualityPreset.of(ClientConfig.explosionQuality).particleBudget() - alive();
        return Math.max(0, Math.min(wanted, left));
    }

    /** {@code count} scaled by the quality's particle scale. */
    static int scaled(double count) {
        return (int) Math.round(count * QualityPreset.of(ClientConfig.explosionQuality).particleScale());
    }

    static void prune() {
        ALIVE.removeIf(p -> !p.isAlive());
    }

    static void clear() {
        ALIVE.clear();
        aliveIn = null;
    }

    private static boolean add(@Nullable Particle particle) {
        if (particle == null || alive() >= QualityPreset.of(ClientConfig.explosionQuality).particleBudget()) return false;
        Minecraft.getInstance().particleEngine.add(particle);
        ALIVE.add(particle);
        return true;
    }

    // ------------------------------------------------------------------ making them

    /** A puff of soot from a fire or a smoke column: dark grey, rising, swelling and thinning. */
    static boolean fireSmoke(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life, int rgb) {
        return smoke != null && add(new Puff(level, x, y, z, vx, vy, vz, smoke, rgb, 0.75f, size, size * 3.5f, life, -0.004f, 0.96f, false));
    }

    /** A tongue of flame: glows, shrinks, rises a little. {@code heat} 1 white-hot to 0 dull red. */
    static boolean flame(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life, double heat) {
        return flame != null && add(new Puff(level, x, y, z, vx, vy, vz, flame, ExplosionPalette.fire(heat).rgb(), 0.95f, size, size * 0.4f, life,
                -0.012f, 0.88f, true));
    }

    /** A hot fragment of casing: a bright streak, falling, cooling. */
    static boolean spark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        return spark != null && add(new Spark(level, x, y, z, vx, vy, vz, spark, size, life));
    }

    /** Dust raised off {@code state}, in its colour. */
    static boolean dust(ClientLevel level, BlockState state, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        return dust != null && add(new Puff(level, x, y, z, vx, vy, vz, dust, groundColour(level, state, x, y, z), 0.7f, size, size * 3f, life,
                0.004f, 0.93f, false));
    }

    /** A chunk of {@code state} thrown out, in its colour: falls and bounces. */
    static boolean debris(ClientLevel level, BlockState state, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        return debris != null && add(new Chunk(level, x, y, z, vx, vy, vz, debris, groundColour(level, state, x, y, z), size, life));
    }

    /** A wisp of condensation, white, swelling and gone in a moment. */
    static boolean mist(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        return mist != null && add(new Puff(level, x, y, z, vx, vy, vz, mist, 0xF2F3F5, 0.45f, size, size * 2.5f, life, 0, 0.9f, false));
    }

    /** A flake of ash drifting down. */
    static boolean ash(ClientLevel level, double x, double y, double z, double driftX, double driftZ) {
        if (ash == null) return false;
        Ash a = new Ash(level, x, y, z, ash, 0.08f + level.random.nextFloat() * 0.08f, 160 + level.random.nextInt(80));
        a.setDrift(driftX, driftZ);
        return add(a);
    }

    /** A drop of black rain. */
    static boolean rain(ClientLevel level, double x, double y, double z) {
        return rain != null && add(new Rain(level, x, y, z, rain, 40 + level.random.nextInt(20)));
    }

    /**
     * The colour of dust or debris from {@code state}: its map colour, browned towards plain dust (grass and leaves are soil
     * and twigs once blown apart), never darker than soot.
     */
    static int groundColour(ClientLevel level, BlockState state, double x, double y, double z) {
        if (state == null || state.isAir()) return ExplosionPalette.DUST.rgb();
        MapColor map;
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            map = MapColor.DIRT;
        } else {
            try {
                map = state.getMapColor(level, BlockPos.containing(x, y, z));
            } catch (RuntimeException e) {
                map = MapColor.DIRT;
            }
        }
        int c = map == null || map == MapColor.NONE ? ExplosionPalette.DUST.rgb() : map.col;
        float r = ((c >> 16) & 0xFF) / 255f, g = ((c >> 8) & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        ExplosionPalette.Rgb mixed = new ExplosionPalette.Rgb(r, g, b).lerp(ExplosionPalette.DUST, 0.35);
        return mixed.rgb();
    }

    private static int light(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (y >= level.getMaxBuildHeight() || !level.hasChunkAt(pos)) {
            return LevelRenderer.getLightColor(level, BlockPos.containing(x, Math.min(y, level.getMaxBuildHeight() - 1), z)) | 15 << 20;
        }
        return LevelRenderer.getLightColor(level, pos);
    }

    // ------------------------------------------------------------------ the particles

    /** Smoke, dust, flame or mist: slows in the air, rises or sinks, swells and fades. */
    static class Puff extends TextureSheetParticle {
        private final float startSize, endSize, peak;
        private final boolean glows;
        private final float spin;

        Puff(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, int rgb, float alpha,
             float size, float grown, int life, float gravity, float friction, boolean glows) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = Math.max(4, life);
            this.gravity = gravity;
            this.friction = friction;
            this.hasPhysics = false;
            this.startSize = size;
            this.endSize = grown;
            this.peak = alpha;
            this.glows = glows;
            this.quadSize = size;
            this.alpha = 0;
            setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
            this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
            this.spin = (random.nextFloat() - 0.5f) * 0.04f;
            setSize(0.2f, 0.2f);
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += spin;
            float t = (float) age / lifetime;
            quadSize = startSize + (endSize - startSize) * (1 - (1 - t) * (1 - t));
            alpha = peak * Math.min(1, Math.min(age / 2f, (1 - t) * 2.2f));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return glows ? FULL_BRIGHT : light(level, x, y, z);
        }
    }

    /** A fragment of hot casing: flies out fast, falls, cools from white to red and goes out. */
    static final class Spark extends TextureSheetParticle {
        private final float startSize;

        Spark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, float size, int life) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = Math.max(3, life);
            this.gravity = 0.5f;
            this.friction = 0.97f;
            this.hasPhysics = true;
            this.startSize = size;
            this.quadSize = size;
            setColor(1.0f, 0.95f, 0.8f);
            this.alpha = 1;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            ExplosionPalette.Rgb c = ExplosionPalette.fire(0.95 - 0.8 * t);
            setColor(c.red(), c.green(), c.blue());
            quadSize = startSize * (1 - 0.5f * t);
            alpha = 1 - t * t;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** A chunk of ground thrown out of a crater: falls under gravity and comes to rest. */
    static final class Chunk extends TextureSheetParticle {
        Chunk(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, int rgb, float size, int life) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = Math.max(10, life);
            this.gravity = 1.0f;
            this.friction = 0.98f;
            this.hasPhysics = true;
            this.quadSize = size;
            setColor(((rgb >> 16) & 0xFF) / 255f * 0.85f, ((rgb >> 8) & 0xFF) / 255f * 0.85f, (rgb & 0xFF) / 255f * 0.85f);
            this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
            setSize(0.25f, 0.25f);
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            if (!onGround) roll += 0.3f;
            float t = (float) age / lifetime;
            alpha = t > 0.8f ? (1 - t) * 5 : 1;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SHEET;
        }
    }

    /** A flake of ash: falls slowly, fluttering, carried by the wind. */
    static final class Ash extends TextureSheetParticle {
        private double driftX, driftZ;
        private final float phase;

        Ash(ClientLevel level, double x, double y, double z, SpriteSet sprites, float size, int life) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.lifetime = life;
            this.gravity = 0.004f;
            this.friction = 0.98f;
            this.hasPhysics = true;
            this.quadSize = size;
            float grey = 0.45f + random.nextFloat() * 0.2f;
            setColor(grey, grey * 0.98f, grey * 0.95f);
            this.alpha = 0;
            this.phase = random.nextFloat() * Mth.TWO_PI;
            this.yd = -0.03;
        }

        void setDrift(double x, double z) {
            this.driftX = x;
            this.driftZ = z;
        }

        @Override
        public void tick() {
            super.tick();
            double flutter = Math.sin(age * 0.2 + phase) * 0.015;
            xd = driftX + flutter;
            zd = driftZ + Math.cos(age * 0.17 + phase) * 0.015;
            yd = Math.max(yd, -0.05);
            oRoll = roll;
            roll += 0.05f * Mth.sin(age * 0.1f + phase);
            float t = (float) age / lifetime;
            alpha = Math.min(1, Math.min(age / 10f, (1 - t) * 4)) * 0.9f;
            if (onGround) remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return light(level, x, y, z);
        }
    }

    /** A drop of black rain: falls straight and fast, gone where it lands. */
    static final class Rain extends TextureSheetParticle {
        Rain(ClientLevel level, double x, double y, double z, SpriteSet sprites, int life) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.lifetime = life;
            this.gravity = 0.06f;
            this.friction = 1.0f;
            this.hasPhysics = true;
            this.quadSize = 0.1f;
            this.yd = -0.6;
            setColor(0.24f, 0.23f, 0.22f);
            this.alpha = 0.85f;
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return light(level, x, y, z);
        }
    }
}
