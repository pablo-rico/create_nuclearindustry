package org.papiricoh.create_nuclearindustry.missile.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * The rocket effects' particles: puffs of exhaust smoke (and steam and dust, by their colour), thick billowing exhaust and
 * glowing embers. The effects make them directly with every setting (colour, size, life), so they show well past the 32
 * blocks vanilla spawns particles within, and keep count so no more than {@link FxBudget#MAX_PARTICLES} live at once. The
 * types' providers only serve {@code /particle}.
 */
final class RocketParticles {
    /** Smoke without depth writes, like the hydrogen haze: a puff drawn first must not punch holes in the ones behind. */
    static final ParticleRenderType SMOKE_SHEET = new ParticleRenderType() {
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
            return "create_nuclearindustry:rocket_smoke_sheet";
        }
    };

    @Nullable
    private static SpriteSet smokeSprites, denseSprites, emberSprites;
    /** Every rocket particle alive, to count them; emptied when the level changes (the engine drops them unticked). */
    private static final List<Particle> ALIVE = new ArrayList<>();
    @Nullable
    private static ClientLevel aliveIn;

    private RocketParticles() {
    }

    static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RocketFxRegistry.ROCKET_SMOKE.get(), sprites -> {
            smokeSprites = sprites;
            return new Provider(sprites, Kind.SMOKE);
        });
        event.registerSpriteSet(RocketFxRegistry.ROCKET_SMOKE_DENSE.get(), sprites -> {
            denseSprites = sprites;
            return new Provider(sprites, Kind.DENSE);
        });
        event.registerSpriteSet(RocketFxRegistry.EMBER.get(), sprites -> {
            emberSprites = sprites;
            return new Provider(sprites, Kind.EMBER);
        });
    }

    /** Rocket particles alive now. */
    static int alive() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != aliveIn) {
            ALIVE.clear();
            aliveIn = level;
        }
        return ALIVE.size();
    }

    /** Forgets the particles that have died; once a tick, before any are added. */
    static void prune() {
        ALIVE.removeIf(p -> !p.isAlive());
    }

    static void clear() {
        ALIVE.clear();
        aliveIn = null;
    }

    private enum Kind { SMOKE, DENSE, EMBER }

    /** How a puff looks: its colour and opacity, its size now and when grown, how long it lives and whether it glows young. */
    record Puff(float red, float green, float blue, float alpha, float size, float grownSize, int life, int glowTicks, float buoyancy) {
    }

    /** Adds a puff at {@code (x, y, z)} moving at {@code (vx, vy, vz)}; false if the budget is spent. */
    static boolean smoke(ClientLevel level, boolean dense, double x, double y, double z, double vx, double vy, double vz, Puff puff) {
        SpriteSet sprites = dense ? denseSprites : smokeSprites;
        if (sprites == null || alive() >= FxBudget.MAX_PARTICLES) return false;
        add(new Smoke(level, x, y, z, vx, vy, vz, puff, sprites));
        return true;
    }

    /** Adds an ember; false if the budget is spent. */
    static boolean ember(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        if (emberSprites == null || alive() >= FxBudget.MAX_PARTICLES) return false;
        add(new Ember(level, x, y, z, vx, vy, vz, size, life, emberSprites));
        return true;
    }

    private static void add(Particle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
        ALIVE.add(particle);
    }

    /** Light at a particle: the sky's where the world there is not loaded or it is above the build limit. */
    private static int light(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (y >= level.getMaxBuildHeight() || !level.hasChunkAt(pos)) {
            return LevelRenderer.getLightColor(level, BlockPos.containing(x, Math.min(y, level.getMaxBuildHeight() - 1), z)) | 15 << 20;
        }
        return LevelRenderer.getLightColor(level, pos);
    }

    /**
     * A puff of smoke, steam or dust: it slows in the air, rises a little or sinks, swells as it spreads and thins out. Young
     * exhaust glows with the flame that made it.
     */
    static final class Smoke extends TextureSheetParticle {
        private final Puff puff;
        private final float spin;

        Smoke(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, Puff puff, SpriteSet sprites) {
            super(level, x, y, z);
            this.puff = puff;
            pickSprite(sprites);
            xd = vx;
            yd = vy;
            zd = vz;
            lifetime = Math.max(4, puff.life());
            gravity = -puff.buoyancy();
            friction = 0.9f;
            hasPhysics = false;
            quadSize = puff.size();
            alpha = 0;
            setColor(puff.red(), puff.green(), puff.blue());
            roll = oRoll = random.nextFloat() * Mth.TWO_PI;
            spin = (random.nextFloat() - 0.5f) * 0.03f;
            setSize(0.2f, 0.2f);
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += spin;
            float t = (float) age / lifetime;
            // Swells fast at first, then slowly: the cloud spreading as it mixes in.
            quadSize = puff.size() + (puff.grownSize() - puff.size()) * (1 - (1 - t) * (1 - t));
            alpha = puff.alpha() * Math.min(1, Math.min(age / 3f, (1 - t) * 2.2f));
            if (age < puff.glowTicks()) {
                float g = 1 - (float) age / Math.max(1, puff.glowTicks());
                setColor(Mth.lerp(g, puff.red(), 1.0f), Mth.lerp(g, puff.green(), 0.72f), Mth.lerp(g, puff.blue(), 0.38f));
            } else {
                setColor(puff.red(), puff.green(), puff.blue());
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SMOKE_SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            if (age < puff.glowTicks()) return FULL_BRIGHT;
            return light(level, x, y, z);
        }
    }

    /** Full block and sky light. */
    private static final int FULL_BRIGHT = 0xF000F0;

    /** A glowing fleck from a solid motor: thrown out, falls, cools from yellow to red and goes out. */
    static final class Ember extends TextureSheetParticle {
        private final float startSize;

        Ember(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            xd = vx;
            yd = vy;
            zd = vz;
            lifetime = Math.max(3, life);
            gravity = 0.6f;
            friction = 0.94f;
            hasPhysics = false;
            startSize = size;
            quadSize = size;
            setColor(1.0f, 0.9f, 0.55f);
            alpha = 1;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            setColor(1.0f, Mth.lerp(t, 0.9f, 0.3f), Mth.lerp(t, 0.55f, 0.08f));
            quadSize = startSize * (1 - 0.6f * t);
            alpha = 1 - t * t;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SMOKE_SHEET;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** For {@code /particle}: a plain grey puff, thick exhaust or an ember with the given motion. */
    private record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy,
                                       double vz) {
            return switch (kind) {
                case SMOKE -> new Smoke(level, x, y, z, vx, vy, vz, new Puff(0.78f, 0.77f, 0.75f, 0.6f, 1.0f, 3.0f, 80, 0, 0.002f), sprites);
                case DENSE -> new Smoke(level, x, y, z, vx, vy, vz, new Puff(0.72f, 0.7f, 0.68f, 0.85f, 2.0f, 5.0f, 120, 0, 0.003f), sprites);
                case EMBER -> new Ember(level, x, y, z, vx, vy, vz, 0.15f, 20, sprites);
            };
        }
    }
}
