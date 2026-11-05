package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.ExplosionAudio;

import java.util.List;

/**
 * Plays an explosion's sounds as {@link ExplosionAudio} schedules them. Each detonation keeps which of its cues it has played;
 * once a tick its cues are worked out again for where the player stands now (so walking towards a far blast brings its roll
 * sooner, as it should) and those due start, with their own loudness and no falloff of the game's, from the direction of the
 * blast. A cue whose moment passed while the player was not told of the detonation is skipped.
 */
public final class ExplosionSounds {
    private ExplosionSounds() {}

    /** The sound event of a cue. */
    static SoundEvent event(ExplosionAudio.Sound sound) {
        return switch (sound) {
            case NUKE_CRACK -> ExplosionFxRegistry.NUKE_CRACK.get();
            case NUKE_BOOM -> ExplosionFxRegistry.NUKE_BOOM.get();
            case NUKE_RUMBLE -> ExplosionFxRegistry.NUKE_RUMBLE.get();
            case NUKE_RUMBLE_FAR -> ExplosionFxRegistry.NUKE_RUMBLE_FAR.get();
            case NUKE_RETURN_WIND -> ExplosionFxRegistry.NUKE_RETURN_WIND.get();
            case SEISMIC_RUMBLE -> ExplosionFxRegistry.SEISMIC_RUMBLE.get();
            case CLOUD_LIGHTNING -> ExplosionFxRegistry.CLOUD_LIGHTNING.get();
            case EXPLOSION_SMALL -> ExplosionFxRegistry.EXPLOSION_SMALL.get();
            case EXPLOSION_MEDIUM -> ExplosionFxRegistry.EXPLOSION_MEDIUM.get();
            case EXPLOSION_LARGE -> ExplosionFxRegistry.EXPLOSION_LARGE.get();
            case EXPLOSION_DISTANT -> ExplosionFxRegistry.EXPLOSION_DISTANT.get();
            case DEBRIS_RAIN -> ExplosionFxRegistry.DEBRIS_RAIN.get();
            case THERMOBARIC_BLAST -> ExplosionFxRegistry.THERMOBARIC_BLAST.get();
            case THERMOBARIC_DISTANT -> ExplosionFxRegistry.THERMOBARIC_DISTANT.get();
            case CLUSTER_OPEN -> ExplosionFxRegistry.CLUSTER_OPEN.get();
            case CLUSTER_CRACKLE -> ExplosionFxRegistry.CLUSTER_CRACKLE.get();
        };
    }

    /**
     * Starts the cues of {@code cues} that are due {@code ageTicks} after the detonation and not yet in {@code played} (a bit per
     * cue id); returns the new mask. Stale cues are marked played without a sound.
     */
    static long play(List<ExplosionAudio.Cue> cues, double ageTicks, long played, Vec3 from) {
        for (ExplosionAudio.Cue cue : cues) {
            long bit = 1L << cue.id();
            if ((played & bit) != 0) continue;
            if (ExplosionAudio.stale(cue, ageTicks)) {
                played |= bit;
            } else if (ageTicks >= cue.tick()) {
                played |= bit;
                now(cue.sound(), from, cue.volume(), cue.pitch());
            }
        }
        return played;
    }

    /** Plays {@code sound} now from {@code from}, at {@code volume} whatever the distance. */
    static void now(ExplosionAudio.Sound sound, Vec3 from, float volume, float pitch) {
        if (volume < 0.01f) return;
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(event(sound).getLocation(), ExplosionFxRegistry.CATEGORY,
                Math.min(1, volume), pitch, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, from.x, from.y, from.z, false));
    }

    /** The casing of a cluster warhead blown open at {@code at}, {@code distance} blocks off: heard once sound gets here. */
    public static void clusterOpen(Vec3 at, double distance) {
        ExplosionAudio.Cue cue = ExplosionAudio.clusterOpen(distance);
        if (cue.volume() < 0.01f) return;
        SimpleSoundInstance instance = new SimpleSoundInstance(event(cue.sound()).getLocation(), ExplosionFxRegistry.CATEGORY,
                cue.volume(), cue.pitch(), RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, at.x, at.y, at.z, false);
        int delay = (int) Math.round(cue.tick());
        if (delay > 0) {
            Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
        } else {
            Minecraft.getInstance().getSoundManager().play(instance);
        }
    }
}
