package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;

/**
 * Deafness from a shock wave ({@link ExplosionFxRegistry#DEAFENED}, applied by the server): the whole world goes quiet at once
 * (the listener's gain, the master volume's own setting, lowered by the effect's strength, so sounds already playing fade too)
 * and comes back slowly as the effect wears off, while a ringing plays in the ears ({@code explosion.tinnitus}), loud enough to
 * hear through the muffling and fading with it. Nothing is changed for good: the master volume is put back the moment the
 * effect ends or the player leaves.
 */
public final class Deafness {
    /** The quietest the world gets, as a share of the master volume. */
    private static final float MIN_GAIN = 0.1f;

    private static float muffle = 1;
    private static float applied = -1;
    private static int startDuration;
    private static int amplifier = -1;
    @Nullable
    private static Ringing ringing;
    private static float ring;

    private Deafness() {}

    static void tick(Minecraft mc) {
        float target = 1;
        float ringTarget = 0;
        MobEffectInstance effect = mc.player == null ? null : mc.player.getEffect(ExplosionFxRegistry.DEAFENED);
        if (effect != null) {
            int remaining = effect.getDuration();
            if (effect.getAmplifier() != amplifier || remaining > startDuration) {
                amplifier = effect.getAmplifier();
                startDuration = Math.max(1, remaining);
            }
            double left = Math.max(0, Math.min(1, remaining / (double) startDuration));
            // Full strength until the last third, then hearing comes back.
            double envelope = Math.min(1, left / 0.35);
            double depth = 0.45 + 0.15 * Math.min(3, amplifier);
            target = (float) Math.max(MIN_GAIN, 1 - depth * envelope);
            ringTarget = (float) ((0.35 + 0.2 * Math.min(3, amplifier)) * envelope);
        } else {
            amplifier = -1;
            startDuration = 0;
        }
        // Deafened at once, recovering slowly.
        muffle += (target - muffle) * (target < muffle ? 0.6f : 0.05f);
        if (Math.abs(muffle - 1) < 0.002f) muffle = 1;
        applyGain(mc);
        ring += (ringTarget - ring) * (ringTarget > ring ? 0.3f : 0.05f);
        if (ClientConfig.explosionTinnitus && ring > 0.01f) {
            if (ringing == null || ringing.isStopped()) {
                ringing = new Ringing();
                mc.getSoundManager().play(ringing);
            }
        }
    }

    private static void applyGain(Minecraft mc) {
        float master = mc.options.getSoundSourceVolume(SoundSource.MASTER);
        if (master <= 0) return;
        if (muffle >= 1) {
            if (applied >= 0) {
                mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, master);
                applied = -1;
            }
            return;
        }
        float gain = master * muffle;
        if (Math.abs(gain - applied) < 0.003f) return;
        mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, Math.max(0.001f, gain));
        applied = gain;
    }

    /** Puts the master volume back and stops the ringing. */
    static void reset() {
        Minecraft mc = Minecraft.getInstance();
        muffle = 1;
        ring = 0;
        amplifier = -1;
        startDuration = 0;
        if (applied >= 0) {
            float master = mc.options.getSoundSourceVolume(SoundSource.MASTER);
            if (master > 0) mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, master);
            applied = -1;
        }
        if (ringing != null) {
            mc.getSoundManager().stop(ringing);
            ringing = null;
        }
    }

    /** The ringing in the ears: inside the head, looped, as loud as the deafness asks and heard through the muffling. */
    private static final class Ringing extends AbstractTickableSoundInstance {
        Ringing() {
            super(ExplosionFxRegistry.TINNITUS.get(), ExplosionFxRegistry.TINNITUS_CATEGORY, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01f;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        @Override
        public void tick() {
            if (!ClientConfig.explosionTinnitus || (ring <= 0.005f && muffle >= 1)) {
                stop();
                return;
            }
            // The file is very quiet; the muffling lowers everything, this too, so it is raised to make up for it.
            volume = Math.max(0.001f, Math.min(1, ring / Math.max(0.2f, muffle)));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
