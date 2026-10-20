package org.papiricoh.create_nuclearindustry.missile.radar.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;

import java.util.function.BooleanSupplier;

/**
 * The radar's audible cues, all off with {@code radar.sounds}: a ping as the sweep crosses an echo, a tone for a new track, a
 * klaxon that loops while a threat is not acknowledged, and a chirp on acknowledging.
 */
public final class RadarSounds {
    private RadarSounds() {
    }

    private static void ui(SoundEvent sound, float pitch, float volume) {
        if (!ClientConfig.radarSounds) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void ping(float pitch) {
        ui(RocketFxRegistry.RADAR_PING.get(), pitch, 0.35f);
    }

    public static void newTrack() {
        ui(RocketFxRegistry.RADAR_NEW_TRACK.get(), 1, 0.6f);
    }

    public static void acknowledged() {
        ui(RocketFxRegistry.RADAR_ACK.get(), 1, 0.7f);
    }

    /**
     * Starts the klaxon, looping while {@code sounding} holds; at {@code pos} in the world, or as a screen sound when null.
     * Returns the instance so its owner can tell whether it still plays.
     */
    public static Klaxon klaxon(BooleanSupplier sounding, BlockPos pos, float volume) {
        Klaxon klaxon = new Klaxon(sounding, pos, volume);
        if (ClientConfig.radarSounds) Minecraft.getInstance().getSoundManager().play(klaxon);
        return klaxon;
    }

    /** A looping klaxon that stops itself once it should no longer sound. */
    public static final class Klaxon extends AbstractTickableSoundInstance {
        private final BooleanSupplier sounding;

        Klaxon(BooleanSupplier sounding, BlockPos pos, float volume) {
            super(RocketFxRegistry.RADAR_KLAXON.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.sounding = sounding;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            if (pos == null) {
                this.relative = true;
                this.attenuation = Attenuation.NONE;
            } else {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
            }
        }

        @Override
        public void tick() {
            if (!ClientConfig.radarSounds || !sounding.getAsBoolean()) stop();
        }

        public boolean playing() {
            return !isStopped();
        }
    }
}
