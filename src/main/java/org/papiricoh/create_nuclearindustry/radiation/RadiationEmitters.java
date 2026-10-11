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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The radiation emitters loaded in each level, indexed by the chunk of their origin so a query
 * only looks at the chunks within range. An emitter's origin can move (a reactor's is the middle
 * of its core, known once it forms): the index files every emitter again at most once every
 * {@link #REINDEX_INTERVAL} ticks, and queries look one chunk beyond their range. Server side only.
 */
public final class RadiationEmitters {
    /** Ticks between two refilings of a level's emitters under the chunk of their current origin. */
    static final int REINDEX_INTERVAL = 20;

    private static final Map<Level, Index> INDEX = new WeakHashMap<>();

    private static final class Index {
        final Long2ObjectOpenHashMap<Set<RadiationEmitter>> byChunk = new Long2ObjectOpenHashMap<>();
        final Map<RadiationEmitter, Long> chunkOf = new IdentityHashMap<>();
        long lastReindex = Long.MIN_VALUE / 2;

        void file(RadiationEmitter emitter, long chunk) {
            Long previous = chunkOf.put(emitter, chunk);
            if (previous != null && previous != chunk) {
                unfile(emitter, previous);
            }
            byChunk.computeIfAbsent(chunk, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(emitter);
        }

        void remove(RadiationEmitter emitter) {
            Long chunk = chunkOf.remove(emitter);
            if (chunk != null) {
                unfile(emitter, chunk);
            }
        }

        private void unfile(RadiationEmitter emitter, long chunk) {
            Set<RadiationEmitter> set = byChunk.get(chunk);
            if (set != null) {
                set.remove(emitter);
                if (set.isEmpty()) {
                    byChunk.remove(chunk);
                }
            }
        }

        /** Drops removed emitters and files the rest under the chunk of where they are now. */
        void reindex() {
            for (RadiationEmitter emitter : new ArrayList<>(chunkOf.keySet())) {
                if (emitter.isRemoved()) {
                    remove(emitter);
                } else {
                    file(emitter, chunkKey(emitter.radiationOrigin()));
                }
            }
        }
    }

    private RadiationEmitters() {}

    public static void register(RadiationEmitter emitter) {
        Level level = emitter.getLevel();
        if (level != null && !level.isClientSide) {
            INDEX.computeIfAbsent(level, key -> new Index()).file(emitter, chunkKey(emitter.radiationOrigin()));
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

    /** Files every emitter of the level under the chunk of its current origin now; for tests. */
    public static void reindex(Level level) {
        Index index = INDEX.get(level);
        if (index != null) {
            index.reindex();
            index.lastReindex = level.getGameTime();
        }
    }

    /** Emitters registered in the level, active or not. */
    public static int count(Level level) {
        Index index = INDEX.get(level);
        return index == null ? 0 : index.chunkOf.size();
    }

    /** Active emitters within {@code range} of {@code at}, nearest first, at most {@code limit}. */
    public static List<RadiationEmitter> near(Level level, Vec3 at, double range, int limit) {
        Index index = INDEX.get(level);
        if (index == null || index.chunkOf.isEmpty()) {
            return List.of();
        }
        long now = level.getGameTime();
        if (now - index.lastReindex >= REINDEX_INTERVAL || now < index.lastReindex) {
            index.reindex();
            index.lastReindex = now;
        }
        List<RadiationEmitter> found = new ArrayList<>();
        double rangeSqr = range * range;
        int minX = SectionPos.blockToSectionCoord(at.x - range) - 1, maxX = SectionPos.blockToSectionCoord(at.x + range) + 1;
        int minZ = SectionPos.blockToSectionCoord(at.z - range) - 1, maxZ = SectionPos.blockToSectionCoord(at.z + range) + 1;
        RadiationStats stats = RadiationStats.current();
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > index.byChunk.size()) {
            // Fewer occupied chunks than chunks in range: walk the occupied ones.
            for (Long2ObjectMap.Entry<Set<RadiationEmitter>> entry : index.byChunk.long2ObjectEntrySet()) {
                int x = ChunkPos.getX(entry.getLongKey()), z = ChunkPos.getZ(entry.getLongKey());
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    stats.emitterChunks++;
                    collect(entry.getValue(), at, rangeSqr, found);
                }
            }
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Set<RadiationEmitter> set = index.byChunk.get(ChunkPos.asLong(x, z));
                    if (set != null) {
                        stats.emitterChunks++;
                        collect(set, at, rangeSqr, found);
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(emitter -> emitter.radiationOrigin().distanceToSqr(at)));
        return found.size() > limit ? found.subList(0, limit) : found;
    }

    private static void collect(Set<RadiationEmitter> set, Vec3 at, double rangeSqr, List<RadiationEmitter> found) {
        for (RadiationEmitter emitter : set) {
            if (!emitter.isRemoved() && emitter.radiationStrength() > 0 && emitter.radiationOrigin().distanceToSqr(at) <= rangeSqr) {
                found.add(emitter);
            }
        }
    }

    private static long chunkKey(Vec3 origin) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(origin.x), SectionPos.blockToSectionCoord(origin.z));
    }
}
