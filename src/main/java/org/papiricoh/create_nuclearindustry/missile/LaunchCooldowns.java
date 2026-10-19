package org.papiricoh.create_nuclearindustry.missile;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When each player's launch pads last fired, for {@code missile.launchCooldownSeconds}: one record for
 * the whole server, kept with the overworld and timed by its clock, so changing dimension does not
 * reset it.
 */
public final class LaunchCooldowns extends SavedData {
    private static final String NAME = "create_nuclearindustry_launch_cooldowns";

    private final Map<UUID, Long> lastLaunch = new HashMap<>();

    public static LaunchCooldowns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LaunchCooldowns::new, LaunchCooldowns::load, null), NAME);
    }

    /** The server's clock the cooldowns are timed by. */
    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** Game time of {@code player}'s last launch, or -1. */
    public long lastLaunch(UUID player) {
        return lastLaunch.getOrDefault(player, -1L);
    }

    public void record(UUID player, long gameTime) {
        lastLaunch.put(player, gameTime);
        setDirty();
    }

    /** Forgets {@code player}'s last launch; for tests and admins. */
    public void clear(UUID player) {
        if (lastLaunch.remove(player) != null) {
            setDirty();
        }
    }

    /** Ticks {@code player} still has to wait before launching again. */
    public long ticksLeft(MinecraftServer server, UUID player) {
        return LaunchRules.cooldownTicksLeft(lastLaunch(player), now(server), MissileConfig.launchCooldownSeconds);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        lastLaunch.forEach((player, time) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putLong("Time", time);
            list.add(entry);
        });
        tag.put("Launches", list);
        return tag;
    }

    static LaunchCooldowns load(CompoundTag tag, HolderLookup.Provider registries) {
        LaunchCooldowns cooldowns = new LaunchCooldowns();
        for (Tag t : tag.getList("Launches", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Player")) {
                cooldowns.lastLaunch.put(entry.getUUID("Player"), entry.getLong("Time"));
            }
        }
        return cooldowns;
    }
}
