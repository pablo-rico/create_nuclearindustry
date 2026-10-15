package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * A pale, translucent wisp of hydrogen. Lighter than air, it drifts upwards until the ceiling
 * stops it and hovers there, wandering a little and turning back from walls, fading in and out
 * over a few seconds. Many of them under a roof read as a haze. Vanilla particle physics would
 * freeze it for good at the first touch, so it checks the blocks around it itself.
 */
public class HydrogenGasParticle extends TextureSheetParticle {
    /** Upward push each tick: buoyancy against the drag below gives a slow, steady rise. */
    private static final double BUOYANCY = 0.0012;
    private static final double WANDER = 0.0012;
    /** Lowest block light the wisps are drawn with, so the haze shows in an unlit hall. */
    private static final int MIN_BLOCK_LIGHT = 11;

    /**
     * The translucent particle sheet without depth writes: a wisp drawn first would otherwise hide
     * the ones behind it wherever it shows, punching hard-edged holes in a thick haze.
     */
    public static final ParticleRenderType GAS_SHEET = new ParticleRenderType() {
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
            return "create_nuclearindustry:hydrogen_gas_sheet";
        }
    };

    private final float maxAlpha;

    protected HydrogenGasParticle(ClientLevel level, double x, double y, double z, double strength, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 70 + random.nextInt(70);
        quadSize = 0.9f + random.nextFloat() * 0.6f;
        setSize(0.3f, 0.3f);
        xd = (random.nextDouble() - 0.5) * 0.01;
        yd = 0.004 + random.nextDouble() * 0.008;
        zd = (random.nextDouble() - 0.5) * 0.01;
        gravity = 0;
        friction = 0.92f;
        hasPhysics = false;
        float tint = random.nextFloat() * 0.04f;
        setColor(0.84f + tint, 0.92f + tint, 1.0f);
        // the server's hydrogen fraction, as a 0-1 strength, makes thicker gas a little more opaque too
        maxAlpha = (0.16f + random.nextFloat() * 0.10f) * (float) (0.6 + 0.4 * Math.max(0.0, Math.min(1.0, strength)));
        alpha = 0;
        roll = oRoll = random.nextFloat() * (float) (Math.PI * 2);
    }

    @Override
    public void tick() {
        yd += BUOYANCY;
        xd += (random.nextDouble() - 0.5) * WANDER;
        zd += (random.nextDouble() - 0.5) * WANDER;
        oRoll = roll;
        roll += 0.004f;
        // hover under the ceiling and turn back from walls: full blocks only, as the gas sees them
        // the quad turns to face the camera, so it reaches about its half-size out every way
        double reach = quadSize * 1.2;
        if (solid(x, y + yd + reach, z)) {
            yd = Math.min(yd, 0.0) - 0.004;
        }
        if (solid(x + xd + Math.signum(xd) * reach, y, z)) {
            xd = -xd;
        }
        if (solid(x, y, z + zd + Math.signum(zd) * reach)) {
            zd = -zd;
        }
        super.tick();
        float age = (float) this.age / lifetime;
        // fade in over the first quarter, out over the last third
        alpha = maxAlpha * Math.min(1, Math.min(age * 4, (1 - age) * 3));
    }

    private boolean solid(double px, double py, double pz) {
        BlockPos pos = BlockPos.containing(px, py, pz);
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GAS_SHEET;
    }

    @Override
    protected int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        int block = Math.max(MIN_BLOCK_LIGHT << 4, light & 0xFFFF);
        return (light & 0xFFFF0000) | block;
    }

    /** {@code xd} carries the strength (0-1) of the gas it belongs to; the rest of the speed is the particle's own. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double xd, double yd, double zd) {
            return new HydrogenGasParticle(level, x, y, z, xd, sprites);
        }
    }
}
