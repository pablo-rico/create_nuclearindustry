package org.papiricoh.create_nuclearindustry.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side index of loaded multiblock controllers. Each controller is indexed under
 * every chunk its {@link MultiblockController#interestBounds()} touches, so finding the
 * controllers affected by a block change costs one map lookup regardless of structure size.
 *
 * <p>The bounds are read once, when a controller is indexed or {@link #reindex reindexed}, and kept
 * with its entry: a block change only compares positions, without asking any controller to build
 * its bounds again. A controller whose bounds change must call {@link #reindex} (they all do when
 * their structure changes).
 */
public final class MultiblockRegistry {
    /**
     * Cleared for a level when it unloads ({@link #forget}): the entries hold the level's block
     * entities, which hold the level, so the weak key alone would never let it go.
     */
    private static final Map<Level, LevelIndex> LEVELS = new WeakHashMap<>();
    /** Calls of {@link MultiblockController#interestBounds()} made by the registry, for tests. */
    private static long boundsReads;
    /** Controllers the last {@link #unregisterChunk} looked at, for tests. */
    private static int lastUnloadVisited;

    private MultiblockRegistry() {}

    public static void register(MultiblockController controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        index(level).add(controller);
    }

    public static void unregister(MultiblockController controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        LevelIndex index = LEVELS.get(level);
        if (index != null) {
            index.remove(controller);
        }
    }

    /** Re-reads {@link MultiblockController#interestBounds()} after the structure changed. */
    public static void reindex(MultiblockController controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        LevelIndex index = LEVELS.get(level);
        if (index != null && index.entries.containsKey(controller)) {
            index.remove(controller);
            index.add(controller);
        }
    }

    /**
     * Drops the controllers standing in a chunk that unloads. Every controller is indexed under its
     * own chunk, so only that chunk's entries are looked at, not every controller of the level.
     */
    public static void unregisterChunk(Level level, ChunkPos chunk) {
        lastUnloadVisited = 0;
        LevelIndex index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        Set<MultiblockController> candidates = index.byChunk.get(chunk.toLong());
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        List<MultiblockController> inChunk = null;
        for (MultiblockController controller : candidates) {
            lastUnloadVisited++;
            BlockPos pos = controller.getBlockPos();
            if (SectionPos.blockToSectionCoord(pos.getX()) == chunk.x && SectionPos.blockToSectionCoord(pos.getZ()) == chunk.z) {
                if (inChunk == null) {
                    inChunk = new ArrayList<>();
                }
                inChunk.add(controller);
            }
        }
        if (inChunk != null) {
            inChunk.forEach(index::remove);
        }
    }

    /** Forgets everything indexed for a level that unloads. */
    public static void forget(LevelAccessor level) {
        if (level instanceof Level l) {
            LEVELS.remove(l);
        }
    }

    /** Live controllers whose interest bounds contain {@code pos}. Allocates nothing when none do. */
    public static List<MultiblockController> affecting(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return List.of();
        }
        LevelIndex index = LEVELS.get(level);
        if (index == null) {
            return List.of();
        }
        Set<MultiblockController> candidates = index.byChunk.get(ChunkPos.asLong(pos));
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<MultiblockController> result = null;
        List<MultiblockController> stale = null;
        for (MultiblockController controller : candidates) {
            if (!isLive(level, controller)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(controller);
                continue;
            }
            Entry entry = index.entries.get(controller);
            if (entry != null && entry.bounds.isInside(pos)) {
                if (result == null) {
                    result = new ArrayList<>(2);
                }
                result.add(controller);
            }
        }
        if (stale != null) {
            stale.forEach(index::remove);
        }
        return result == null ? List.of() : result;
    }

    /**
     * Asks every live controller whose indexed bounds meet {@code box} to check its structure again,
     * for changes made without block events (a nuclear crater digs with raw state changes). Walks the
     * level's controllers, which are few, rather than the chunks of a box that can be huge. Returns
     * how many were asked.
     */
    public static int areaChanged(Level level, BoundingBox box) {
        if (level.isClientSide) {
            return 0;
        }
        LevelIndex index = LEVELS.get(level);
        if (index == null || index.entries.isEmpty()) {
            return 0;
        }
        List<MultiblockController> hit = null;
        List<MultiblockController> stale = null;
        for (Map.Entry<MultiblockController, Entry> entry : index.entries.entrySet()) {
            MultiblockController controller = entry.getKey();
            if (!isLive(level, controller)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(controller);
            } else if (entry.getValue().bounds.intersects(box)) {
                if (hit == null) {
                    hit = new ArrayList<>(2);
                }
                hit.add(controller);
            }
        }
        if (stale != null) {
            stale.forEach(index::remove);
        }
        if (hit == null) {
            return 0;
        }
        // Asked after the walk: a controller may reindex itself while answering.
        hit.forEach(controller -> controller.requestStructureRevalidation(null));
        return hit.size();
    }

    public static <C extends MultiblockController> List<C> affecting(Level level, BlockPos pos, Class<C> type) {
        List<C> result = new ArrayList<>();
        for (MultiblockController controller : affecting(level, pos)) {
            if (type.isInstance(controller)) {
                result.add(type.cast(controller));
            }
        }
        return result;
    }

    /** Every live controller of {@code type} in {@code level}. */
    public static <C extends MultiblockController> List<C> all(Level level, Class<C> type) {
        LevelIndex index = LEVELS.get(level);
        if (index == null) {
            return List.of();
        }
        List<C> result = new ArrayList<>();
        for (MultiblockController controller : index.entries.keySet()) {
            if (type.isInstance(controller) && isLive(level, controller)) {
                result.add(type.cast(controller));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** The bounds a controller is indexed with, or null when it is not indexed. For tests. */
    public static BoundingBox indexedBounds(MultiblockController controller) {
        Level level = controller.getLevel();
        LevelIndex index = level == null ? null : LEVELS.get(level);
        Entry entry = index == null ? null : index.entries.get(controller);
        return entry == null ? null : entry.bounds;
    }

    /** How many times the registry has asked a controller for its interest bounds. For tests. */
    public static long boundsReads() {
        return boundsReads;
    }

    /** How many controllers the last chunk unload looked at. For tests. */
    public static int lastUnloadVisited() {
        return lastUnloadVisited;
    }

    /**
     * A block entity that was replaced, or whose chunk unloaded, is marked removed: that is enough to
     * tell a stale entry without looking the block entity up in the level.
     */
    private static boolean isLive(Level level, MultiblockController controller) {
        return !controller.isRemoved() && controller.getLevel() == level;
    }

    private static LevelIndex index(Level level) {
        return LEVELS.computeIfAbsent(level, ignored -> new LevelIndex());
    }

    /** What a controller is indexed with: its bounds as they were read, and the chunks they touch. */
    private record Entry(BoundingBox bounds, long[] chunks) {}

    private static final class LevelIndex {
        private final Map<BlockPos, MultiblockController> byPos = new HashMap<>();
        private final Map<MultiblockController, Entry> entries = new IdentityHashMap<>();
        private final Long2ObjectMap<Set<MultiblockController>> byChunk = new Long2ObjectOpenHashMap<>();

        private void add(MultiblockController controller) {
            MultiblockController previous = byPos.put(controller.getBlockPos().immutable(), controller);
            if (previous != null && previous != controller) {
                removeFromChunks(previous);
            }
            // Registered again (loaded twice): drop the old entry so no chunk keeps a stale one.
            removeFromChunks(controller);
            boundsReads++;
            BoundingBox bounds = controller.interestBounds();
            long[] chunks = chunksTouched(bounds, controller.getBlockPos());
            entries.put(controller, new Entry(bounds, chunks));
            for (long chunk : chunks) {
                byChunk.computeIfAbsent(chunk, ignored -> new LinkedHashSet<>()).add(controller);
            }
        }

        private void remove(MultiblockController controller) {
            byPos.remove(controller.getBlockPos(), controller);
            removeFromChunks(controller);
        }

        private void removeFromChunks(MultiblockController controller) {
            Entry entry = entries.remove(controller);
            if (entry == null) {
                return;
            }
            for (long chunk : entry.chunks) {
                Set<MultiblockController> set = byChunk.get(chunk);
                if (set != null) {
                    set.remove(controller);
                    if (set.isEmpty()) {
                        byChunk.remove(chunk);
                    }
                }
            }
        }

        /** The chunks the bounds touch, plus the controller's own chunk so an unload finds it there. */
        private static long[] chunksTouched(BoundingBox bounds, BlockPos controller) {
            int minX = SectionPos.blockToSectionCoord(bounds.minX());
            int maxX = SectionPos.blockToSectionCoord(bounds.maxX());
            int minZ = SectionPos.blockToSectionCoord(bounds.minZ());
            int maxZ = SectionPos.blockToSectionCoord(bounds.maxZ());
            int ownX = SectionPos.blockToSectionCoord(controller.getX());
            int ownZ = SectionPos.blockToSectionCoord(controller.getZ());
            boolean ownInside = ownX >= minX && ownX <= maxX && ownZ >= minZ && ownZ <= maxZ;
            long[] chunks = new long[(maxX - minX + 1) * (maxZ - minZ + 1) + (ownInside ? 0 : 1)];
            int i = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    chunks[i++] = ChunkPos.asLong(x, z);
                }
            }
            if (!ownInside) {
                chunks[i] = ChunkPos.asLong(ownX, ownZ);
            }
            return chunks;
        }
    }
}
