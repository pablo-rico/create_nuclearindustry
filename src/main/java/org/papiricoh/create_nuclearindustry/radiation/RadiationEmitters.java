package org.papiricoh.create_nuclearindustry.radiation;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The radiation emitters loaded in each level, indexed by the chunk of their origin so a query
 * only looks at the chunks within range. An emitter's origin can move (a reactor's is the middle
 * of its core, known once it forms): at most once every {@link #REINDEX_INTERVAL} ticks the index
 * asks every emitter where it is and re-files only those that left their chunk; queries look one
 * chunk beyond their range. Queries filter and sort by the origin remembered then, so a query
 * never asks an emitter for its origin (a reactor's builds a new vector each time).
 * <p>
 * The index is dropped when its level unloads or the server stops (see
 * {@link org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches}): its emitters hold
 * their level, so the weak map alone would never let go of it. Server side only.
 */
public final class RadiationEmitters {
    /** Ticks between two refilings of a level's emitters under the chunk of their current origin. */
    static final int REINDEX_INTERVAL = 20;

    private static final Map<Level, Index> INDEX = new WeakHashMap<>();

    /** An emitter with where it was when last asked. */
    private static final class Filed {
        final RadiationEmitter emitter;
        Vec3 origin;
        long chunk;

        Filed(RadiationEmitter emitter, Vec3 origin) {
            this.emitter = emitter;
            this.origin = origin;
            this.chunk = chunkKey(origin);
        }
    }

    /** An emitter found by a query, with its squared distance, so the sort asks nothing of it. */
    private record Found(RadiationEmitter emitter, double distanceSqr) {}

    private static final Comparator<Found> NEAREST_FIRST = Comparator.comparingDouble(Found::distanceSqr);

    private static final class Index {
        final Long2ObjectOpenHashMap<Set<Filed>> byChunk = new Long2ObjectOpenHashMap<>();
        final Map<RadiationEmitter, Filed> filed = new IdentityHashMap<>();
        long lastReindex = Long.MIN_VALUE / 2;

        void file(RadiationEmitter emitter, Vec3 origin) {
            Filed entry = filed.get(emitter);
            if (entry == null) {
                entry = new Filed(emitter, origin);
                filed.put(emitter, entry);
                add(entry);
            } else {
                move(entry, origin);
            }
        }

        void remove(RadiationEmitter emitter) {
            Filed entry = filed.remove(emitter);
            if (entry != null) {
                unfile(entry);
            }
        }

        /** Remembers {@code origin}; true if that took the entry to another chunk. */
        boolean move(Filed entry, Vec3 origin) {
            if (origin.equals(entry.origin)) {
                return false;
            }
            entry.origin = origin;
            long chunk = chunkKey(origin);
            if (chunk == entry.chunk) {
                return false;
            }
            unfile(entry);
            entry.chunk = chunk;
            add(entry);
            return true;
        }

        private void add(Filed entry) {
            byChunk.computeIfAbsent(entry.chunk, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entry);
        }

        private void unfile(Filed entry) {
            Set<Filed> set = byChunk.get(entry.chunk);
            if (set != null) {
                set.remove(entry);
                if (set.isEmpty()) {
                    byChunk.remove(entry.chunk);
                }
            }
        }

        /** Drops removed emitters and re-files those whose origin left its chunk; returns how many moved. */
        int reindex() {
            int moved = 0;
            Iterator<Filed> iterator = filed.values().iterator();
            while (iterator.hasNext()) {
                Filed entry = iterator.next();
                if (entry.emitter.isRemoved()) {
                    iterator.remove();
                    unfile(entry);
                } else if (move(entry, entry.emitter.radiationOrigin())) {
                    moved++;
                }
            }
            return moved;
        }
    }

    private RadiationEmitters() {}

    public static void register(RadiationEmitter emitter) {
        Level level = emitter.getLevel();
        if (level != null && !level.isClientSide) {
            INDEX.computeIfAbsent(level, key -> new Index()).file(emitter, emitter.radiationOrigin());
        }
    }

    public static void unregister(RadiationEmitter emitter) {
        Level level = emitter.getLevel();
        if (level != null) {
            Index index = INDEX.get(level);
            if (index != null) {
                index.remove(emitter);
            }
        }
    }

    /**
     * Asks every emitter of the level where it is now and re-files those that left their chunk,
     * at once; for tests. Returns how many were re-filed.
     */
    public static int reindex(Level level) {
        Index index = INDEX.get(level);
        if (index == null) {
            return 0;
        }
        index.lastReindex = level.getGameTime();
        return index.reindex();
    }

    /** Emitters registered in the level, active or not. */
    public static int count(Level level) {
        Index index = INDEX.get(level);
        return index == null ? 0 : index.filed.size();
    }

    /** Whether the level has an index; for tests of its clean-up. */
    public static boolean indexed(Level level) {
        return INDEX.containsKey(level);
    }

    /** Forgets the level's index, when it unloads. */
    public static void forget(Level level) {
        INDEX.remove(level);
    }

    /** Forgets every level's index, when the server stops. */
    public static void forgetAll() {
        INDEX.clear();
    }

    /**
     * Active emitters within {@code range} of {@code at}, nearest first, at most {@code limit};
     * where each one is comes from the last reindex, at most {@link #REINDEX_INTERVAL} ticks ago.
     */
    public static List<RadiationEmitter> near(Level level, Vec3 at, double range, int limit) {
        Index index = INDEX.get(level);
        if (index == null || index.filed.isEmpty()) {
            return List.of();
        }
        long now = level.getGameTime();
        if (now - index.lastReindex >= REINDEX_INTERVAL || now < index.lastReindex) {
            index.reindex();
            index.lastReindex = now;
        }
        List<Found> found = new ArrayList<>();
        double rangeSqr = range * range;
        int minX = SectionPos.blockToSectionCoord(at.x - range) - 1, maxX = SectionPos.blockToSectionCoord(at.x + range) + 1;
        int minZ = SectionPos.blockToSectionCoord(at.z - range) - 1, maxZ = SectionPos.blockToSectionCoord(at.z + range) + 1;
        RadiationStats stats = RadiationStats.current();
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > index.byChunk.size()) {
            // Fewer occupied chunks than chunks in range: walk the occupied ones.
            for (Long2ObjectMap.Entry<Set<Filed>> entry : index.byChunk.long2ObjectEntrySet()) {
                int x = ChunkPos.getX(entry.getLongKey()), z = ChunkPos.getZ(entry.getLongKey());
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    stats.emitterChunks++;
                    collect(entry.getValue(), at, rangeSqr, found);
                }
            }
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Set<Filed> set = index.byChunk.get(ChunkPos.asLong(x, z));
                    if (set != null) {
                        stats.emitterChunks++;
                        collect(set, at, rangeSqr, found);
                    }
                }
            }
        }
        if (found.isEmpty()) {
            return List.of();
        }
        found.sort(NEAREST_FIRST);
        int size = Math.min(limit, found.size());
        List<RadiationEmitter> nearest = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            nearest.add(found.get(i).emitter());
        }
        return nearest;
    }

    private static void collect(Set<Filed> set, Vec3 at, double rangeSqr, List<Found> found) {
        for (Filed entry : set) {
            if (entry.emitter.isRemoved() || entry.emitter.radiationStrength() <= 0) {
                continue;
            }
            double distanceSqr = entry.origin.distanceToSqr(at);
            if (distanceSqr <= rangeSqr) {
                found.add(new Found(entry.emitter, distanceSqr));
            }
        }
    }

    private static long chunkKey(Vec3 origin) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(origin.x), SectionPos.blockToSectionCoord(origin.z));
    }
}
