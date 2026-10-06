package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Contaminated sites in a level: a melted-down reactor, a tokamak that vented its tritium.
 * Each source fades linearly to nothing. Stored per level so it outlives the blocks that
 * caused it.
 */
public class FalloutData extends SavedData {
    private static final String NAME = "create_nuclearindustry_fallout";
    private static final int DECAY_INTERVAL = 20;

    private final List<Source> sources = new ArrayList<>();

    public record Source(BlockPos pos, double strength, int duration, int remaining) {
        double currentStrength() {
            return strength * remaining / duration;
        }
    }

    public static FalloutData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(FalloutData::new, FalloutData::load, null), NAME);
    }

    /** Adds a contamination source next to {@code pos} that fades out over {@code durationTicks}. */
    public static void contaminate(Level level, BlockPos pos, double strength, int durationTicks) {
        if (level instanceof ServerLevel serverLevel) {
            FalloutData data = get(serverLevel);
            data.sources.add(new Source(pos.immutable(), strength, durationTicks, durationTicks));
            data.setDirty();
        }
    }

    public List<Source> sources() {
        return sources;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % DECAY_INTERVAL != 0) {
            return;
        }
        FalloutData data = get(level);
        if (data.sources.isEmpty()) {
            return;
        }
        data.sources.replaceAll(source -> new Source(source.pos(), source.strength(), source.duration(), source.remaining() - DECAY_INTERVAL));
        data.sources.removeIf(source -> source.remaining() <= 0);
        data.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Source source : sources) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", source.pos().asLong());
            entry.putDouble("strength", source.strength());
            entry.putInt("duration", source.duration());
            entry.putInt("remaining", source.remaining());
            list.add(entry);
        }
        tag.put("sources", list);
        return tag;
    }

    private static FalloutData load(CompoundTag tag, HolderLookup.Provider registries) {
        FalloutData data = new FalloutData();
        for (Tag element : tag.getList("sources", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            int duration = Math.max(1, entry.getInt("duration"));
            data.sources.add(new Source(BlockPos.of(entry.getLong("pos")), entry.getDouble("strength"), duration,
                    Math.min(duration, entry.getInt("remaining"))));
        }
        return data;
    }
}
