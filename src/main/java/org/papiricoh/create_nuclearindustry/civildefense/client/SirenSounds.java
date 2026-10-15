package org.papiricoh.create_nuclearindustry.civildefense.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.siren.SoundingSiren;

import java.util.HashMap;
import java.util.Map;

/**
 * Client tick of every siren: keeps one looping siren sound per sounding siren within earshot. The
 * recording is one steady tone; the instance bends its pitch every tick to make the wail.
 */
public final class SirenSounds {
    private static final Map<BlockPos, SirenSound> PLAYING = new HashMap<>();

    private SirenSounds() {}

    public static void tick(SoundingSiren siren) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = siren.getLevel();
        // Ponder scenes run in their own client level; keep them silent.
        if (level == null || level != minecraft.level) {
            return;
        }
        BlockPos pos = siren.getBlockPos();
        SirenSound sound = PLAYING.get(pos);
        if (sound != null && (sound.isStopped() || !minecraft.getSoundManager().isActive(sound))) {
            PLAYING.remove(pos);
            sound = null;
        }
        if (sound == null && siren.isSounding() && inEarshot(minecraft.player, pos)) {
            sound = new SirenSound(siren);
            PLAYING.put(pos.immutable(), sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    static boolean inEarshot(LocalPlayer player, BlockPos pos) {
        double range = CivilDefenseConfig.sirenAudibleRange;
        return player != null && player.distanceToSqr(org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.toWorld(player.level(), pos)) < range * range;
    }

    /** The looping tone of one siren; stops itself when the siren falls silent, goes away or is out of earshot. */
    static final class SirenSound extends AbstractTickableSoundInstance {
        private final SoundingSiren siren;
        /** Fades in as the rotor spins up and out as it winds down, instead of cutting. */
        private float fade;
        private boolean windingDown;

        SirenSound(SoundingSiren siren) {
            super(AllNuclearSounds.AIR_RAID_SIREN.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.siren = siren;
            BlockPos pos = siren.getBlockPos();
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.looping = true;
            this.delay = 0;
            this.attenuation = Attenuation.LINEAR;
            // The engine fixes the sound's range from its starting volume (volume x 16 blocks).
            this.volume = fullVolume();
            this.pitch = tone(0);
        }

        private float tone(float partial) {
            Level level = siren.getLevel();
            return level == null ? 1.0f : siren.pitch(level.getGameTime() + partial);
        }

        private static float fullVolume() {
            return Math.max(1.0f, CivilDefenseConfig.sirenAudibleRange / 16.0f);
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            boolean gone = siren.isRemoved() || siren.getLevel() != minecraft.level || !inEarshot(minecraft.player, siren.getBlockPos());
            if (gone) {
                stop();
                return;
            }
            windingDown = !siren.isSounding();
            fade = Math.max(0, Math.min(1, fade + (windingDown ? -0.05f : 0.05f)));
            if (windingDown && fade <= 0) {
                stop();
                return;
            }
            // Gain is capped at 1, so anything over 1 plays at full loudness; below it, it fades.
            volume = fade >= 1 ? fullVolume() : Math.max(0.01f, fade);
            pitch = tone(0) * (windingDown ? 0.7f + 0.3f * fade : 1.0f);
        }
    }
}
