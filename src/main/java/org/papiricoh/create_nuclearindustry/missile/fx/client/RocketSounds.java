package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketAcoustics;

/**
 * Plays the rocket effects' sounds. Each works out its own loudness from the distance ({@link RocketAcoustics}) rather than
 * the game's sixteen-block falloff, so a launch is heard kilometres away, and arrives as late as sound takes to get there.
 */
final class RocketSounds {
    private RocketSounds() {
    }

    /**
     * Plays {@code sound} from {@code at}, {@code distance} blocks off, heard to {@code reach} blocks at up to {@code loudness}:
     * once the sound has had time to get here when {@code delayed}.
     */
    static void oneShot(SoundEvent sound, Vec3 at, double distance, double reach, float loudness, float pitch, boolean delayed) {
        float volume = (float) (loudness * RocketAcoustics.oneShotVolume(distance, reach));
        if (volume < 0.01f) return;
        SimpleSoundInstance instance = new SimpleSoundInstance(sound.getLocation(), SoundSource.AMBIENT, Math.min(1, volume), pitch,
                RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, at.x, at.y, at.z, false);
        int delay = delayed ? RocketAcoustics.delayTicks(distance) : 0;
        if (delay > 0) {
            Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
        } else {
            Minecraft.getInstance().getSoundManager().play(instance);
        }
    }

    /** What a looping sound follows: whether it is still wanted, how loud and high, and where from. */
    interface Source {
        boolean wanted();

        float volume();

        float pitch();

        Vec3 position();
    }

    /**
     * A looping sound that follows a {@link Source}: it fades in, tracks the loudness the source asks for (its own, by
     * distance), fades out instead of cutting, and stops for good once no longer wanted.
     */
    static final class Loop extends AbstractTickableSoundInstance {
        private static final float FADE_IN = 0.08f;
        private static final float FADE_OUT = 0.05f;

        private final Source source;
        private float fade;

        Loop(SoundEvent sound, Source source) {
            super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.source = source;
            this.looping = true;
            this.delay = 0;
            // Our own falloff: the engine would cut it off sixteen blocks out.
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01f;
            this.pitch = source.pitch();
            place();
        }

        @Override
        public void tick() {
            boolean wanted = source.wanted();
            fade = Math.max(0, Math.min(1, fade + (wanted ? FADE_IN : -FADE_OUT)));
            if (!wanted && fade <= 0) {
                stop();
                return;
            }
            volume = Math.max(0.001f, Math.min(1, fade * source.volume()));
            pitch = source.pitch();
            place();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void place() {
            Vec3 at = source.position();
            x = at.x;
            y = at.y;
            z = at.z;
        }

        boolean done() {
            return isStopped();
        }
    }
}
