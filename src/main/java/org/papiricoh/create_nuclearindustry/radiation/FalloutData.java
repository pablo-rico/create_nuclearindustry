package org.papiricoh.create_nuclearindustry.radiation;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Contaminated sites in a level: a melted-down reactor, a tokamak that vented its tritium.
 * Each source fades linearly to nothing. Stored per level so it outlives the blocks that
 * caused it.
 * <p>
 * A source remembers the game time it runs out at, so fading changes nothing that is saved: the
 * data is only marked for saving when a source comes or goes. Sources are also kept by chunk
 * for {@link #forEachNear}, so a player's update looks only at the sources around them, not at
 * every source in the level; that index is rebuilt whenever the list changes, including when
 * other code edits {@link #sources()} directly.
 */
public class FalloutData extends SavedData {
    private static final String NAME = "create_nuclearindustry_fallout";
    private static final int DECAY_INTERVAL = 20;

    /**
     * The list of sources, counting its changes so the chunk index knows when to rebuild. Every
     * mutation of an {@link AbstractList} goes through {@code set}, {@code add(int, E)} or
     * {@code remove(int)} (iterators and sub-lists included), so counting those, and the bulk
     * operations overridden for speed, sees every change other code can make.
     */
    private static final class SourceList extends AbstractList<Source> implements RandomAccess {
        private final ArrayList<Source> list = new ArrayList<>();
        private int version;

        int version() {
            return version;
        }

        @Override
        public Source get(int index) {
            return list.get(index);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public Source set(int index, Source element) {
            Source previous = list.set(index, element);
            version++;
            return previous;
        }

        @Override
        public void add(int index, Source element) {
            list.add(index, element);
            changed();
        }

        @Override
        public Source remove(int index) {
            Source removed = list.remove(index);
            changed();
            return removed;
        }

        @Override
        public boolean removeIf(Predicate<? super Source> filter) {
            if (list.removeIf(filter)) {
                changed();
                return true;
            }
            return false;
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            if (fromIndex < toIndex) {
                list.subList(fromIndex, toIndex).clear();
                changed();
            }
        }

        @Override
        public void clear() {
            if (!list.isEmpty()) {
                list.clear();
                changed();
            }
        }

        private void changed() {
            version++;
            modCount++;
        }
    }

    private final SourceList sources = new SourceList();
    private final Long2ObjectOpenHashMap<List<Source>> byChunk = new Long2ObjectOpenHashMap<>();
    private int indexedVersion = -1;
    /**
     * Latest game time this data has seen, for the ticks left written next to each end time: builds
     * that predate end times read only those.
     */
    private long lastSeen;
    /** Sources loaded from the old format, which saved ticks left rather than an end time. */
    private boolean pendingLegacy;

    /** A contaminated site, fading from {@code strength} to nothing until game time {@code expires}. */
    public static final class Source {
        private final BlockPos pos;
        private final double strength;
        private final int duration;
        private long expires;
        /** Ticks left as saved by the old format, until {@link #resolveLegacy} turns them into {@code expires}. */
        private int legacyRemaining = -1;

        public Source(BlockPos pos, double strength, int duration, long expires) {
            this.pos = pos;
            this.strength = strength;
            this.duration = Math.max(1, duration);
            this.expires = expires;
        }

        public BlockPos pos() {
            return pos;
        }

        public double strength() {
            return strength;
        }

        public int duration() {
            return duration;
        }

        /** Game time it is gone at. */
        public long expires() {
            return expires;
        }

        /** Ticks left at {@code now}. */
        public long remaining(long now) {
            return Math.max(0, Math.min(duration, expires - now));
        }

        /** Dose rate at {@code now}, falling linearly to zero. */
        public double currentStrength(long now) {
            return strength * remaining(now) / duration;
        }
    }

    public static FalloutData get(ServerLevel level) {
        FalloutData data = level.getDataStorage().computeIfAbsent(new Factory<>(FalloutData::new, FalloutData::load, null), NAME);
        data.lastSeen = Math.max(data.lastSeen, level.getGameTime());
        if (data.pendingLegacy) {
            data.resolveLegacy(level.getGameTime());
        }
        return data;
    }

    /** Adds a contamination source next to {@code pos} that fades out over {@code durationTicks}. */
    public static void contaminate(Level level, BlockPos pos, double strength, int durationTicks) {
        if (level instanceof ServerLevel serverLevel) {
            get(serverLevel).add(pos, strength, durationTicks, level.getGameTime());
        }
    }

    /** Adds a source starting at game time {@code now}. */
    public void add(BlockPos pos, double strength, int durationTicks, long now) {
        int duration = Math.max(1, durationTicks);
        lastSeen = Math.max(lastSeen, now);
        sources.add(new Source(pos.immutable(), strength, duration, now + duration));
        setDirty();
    }

    public List<Source> sources() {
        return sources;
    }

    /**
     * Calls {@code action} for the sources in the chunks within {@code range} of {@code at} (and
     * maybe a few just beyond it: callers still check the distance).
     */
    public void forEachNear(Vec3 at, double range, Consumer<Source> action) {
        if (sources.isEmpty()) {
            return;
        }
        if (indexedVersion != sources.version()) {
            reindex();
        }
        int minX = SectionPos.blockToSectionCoord(at.x - range), maxX = SectionPos.blockToSectionCoord(at.x + range);
        int minZ = SectionPos.blockToSectionCoord(at.z - range), maxZ = SectionPos.blockToSectionCoord(at.z + range);
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > byChunk.size()) {
            for (Long2ObjectMap.Entry<List<Source>> entry : byChunk.long2ObjectEntrySet()) {
                int x = ChunkPos.getX(entry.getLongKey()), z = ChunkPos.getZ(entry.getLongKey());
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    entry.getValue().forEach(action);
                }
            }
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    List<Source> list = byChunk.get(ChunkPos.asLong(x, z));
                    if (list != null) {
                        list.forEach(action);
                    }
                }
            }
        }
    }

    private void reindex() {
        byChunk.clear();
        for (Source source : sources) {
            long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(source.pos.getX()), SectionPos.blockToSectionCoord(source.pos.getZ()));
            byChunk.computeIfAbsent(chunk, key -> new ArrayList<>(2)).add(source);
        }
        indexedVersion = sources.version();
    }

    /** Drops the sources that have run out by {@code now}; marks the data for saving only if one did. */
    public void tick(long now) {
        lastSeen = Math.max(lastSeen, now);
        if (sources.removeIf(source -> source.expires <= now)) {
            setDirty();
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % DECAY_INTERVAL != 0) {
            return;
        }
        FalloutData data = get(level);
        if (!data.sources.isEmpty()) {
            data.tick(level.getGameTime());
        }
    }

    private void resolveLegacy(long now) {
        lastSeen = Math.max(lastSeen, now);
        for (Source source : sources) {
            if (source.legacyRemaining >= 0) {
                source.expires = now + source.legacyRemaining;
                source.legacyRemaining = -1;
            }
        }
        pendingLegacy = false;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Source source : sources) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", source.pos.asLong());
            entry.putDouble("strength", source.strength);
            entry.putInt("duration", source.duration);
            entry.putLong("expires", source.expires);
            // Ticks left, as the old format saved them, so an older build still loads sensible values.
            entry.putInt("remaining", (int) (source.legacyRemaining >= 0 ? source.legacyRemaining : source.remaining(lastSeen)));
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
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            double strength = entry.getDouble("strength");
            if (entry.contains("expires", Tag.TAG_LONG)) {
                data.sources.add(new Source(pos, strength, duration, entry.getLong("expires")));
            } else {
                // Saved before end times: ticks left, counted from whenever the level is first used.
                Source source = new Source(pos, strength, duration, Long.MAX_VALUE);
                source.legacyRemaining = Math.max(0, Math.min(duration, entry.getInt("remaining")));
                data.sources.add(source);
                data.pendingLegacy = true;
            }
        }
        return data;
    }

    /** Loads saved data as the level does; for tests of old saves. */
    public static FalloutData loadForTest(CompoundTag tag, HolderLookup.Provider registries, long now) {
        FalloutData data = load(tag, registries);
        if (data.pendingLegacy) {
            data.resolveLegacy(now);
        }
        return data;
    }
}
