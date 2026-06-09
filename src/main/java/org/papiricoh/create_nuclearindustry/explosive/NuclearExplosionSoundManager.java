package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NuclearExplosionSoundManager {
    private static final double SOUND_SPEED_BLOCKS_PER_TICK = 17.15;
    private static final double FULL_VOLUME_DISTANCE = 96.0;
    private static final double FALLOFF_DISTANCE = 640.0;
    private static final float MAX_VOLUME = 4.0f;
    private static final float MIN_VOLUME = 0.12f;
    private static final List<PendingSound> PENDING_SOUNDS = new ArrayList<>();

    private NuclearExplosionSoundManager() {}

    public static void playDelayedForWorld(ServerLevel level, Vec3 origin) {
        long gameTime = level.getGameTime();
        long seed = level.random.nextLong();
        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceTo(origin);
            int delayTicks = (int) Math.round(distance / SOUND_SPEED_BLOCKS_PER_TICK);
            float volume = volumeForDistance(distance);
            PENDING_SOUNDS.add(new PendingSound(level, player, origin, gameTime + delayTicks, volume, seed));
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<PendingSound> iterator = PENDING_SOUNDS.iterator();
        while (iterator.hasNext()) {
            PendingSound pending = iterator.next();
            if (pending.level != level) {
                continue;
            }
            if (pending.player.isRemoved() || pending.player.level() != level) {
                iterator.remove();
                continue;
            }
            if (gameTime < pending.playAtTick) {
                continue;
            }
            sendSound(pending);
            iterator.remove();
        }
    }

    private static void sendSound(PendingSound pending) {
        Vec3 audiblePosition = audiblePositionFor(pending.player, pending.origin);
        pending.player.connection.send(new ClientboundSoundPacket(
                Holder.direct(AllNuclearSounds.NUCLEAR_EXPLOSION.get()),
                SoundSource.WEATHER,
                audiblePosition.x,
                audiblePosition.y,
                audiblePosition.z,
                pending.volume,
                1.0f,
                pending.seed
        ));
    }

    private static Vec3 audiblePositionFor(ServerPlayer player, Vec3 origin) {
        Vec3 playerPos = player.position();
        Vec3 direction = origin.subtract(playerPos);
        if (direction.lengthSqr() < 1.0e-6) {
            return playerPos;
        }
        return playerPos.add(direction.normalize().scale(8.0));
    }

    private static float volumeForDistance(double distance) {
        if (distance <= FULL_VOLUME_DISTANCE) {
            return MAX_VOLUME;
        }
        double falloff = (distance - FULL_VOLUME_DISTANCE) / FALLOFF_DISTANCE;
        double volume = MAX_VOLUME / (1.0 + falloff * falloff * 4.0);
        return (float) Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }

    private record PendingSound(ServerLevel level, ServerPlayer player, Vec3 origin, long playAtTick, float volume, long seed) {}
}
