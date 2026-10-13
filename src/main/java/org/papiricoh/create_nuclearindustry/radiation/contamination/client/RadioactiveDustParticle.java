package org.papiricoh.create_nuclearindustry.radiation.contamination.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** A faint green fleck that lifts off the ground, glows in the dark and fades. */
public class RadioactiveDustParticle extends TextureSheetParticle {
    private final float maxAlpha;

    protected RadioactiveDustParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 30 + random.nextInt(30);
        quadSize = 0.04f + random.nextFloat() * 0.05f;
        xd = (random.nextDouble() - 0.5) * 0.01;
        yd = 0.004 + random.nextDouble() * 0.01;
        zd = (random.nextDouble() - 0.5) * 0.01;
        gravity = 0;
        hasPhysics = false;
        maxAlpha = 0.55f + random.nextFloat() * 0.35f;
        alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        float age = (float) this.age / lifetime;
        // Fade in over the first fifth, out over the last half.
        alpha = maxAlpha * Math.min(1, Math.min(age * 5, (1 - age) * 2));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double xd, double yd, double zd) {
            return new RadioactiveDustParticle(level, x, y, z, sprites);
        }
    }
}
