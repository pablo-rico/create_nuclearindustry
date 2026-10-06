package org.papiricoh.create_nuclearindustry.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
 */
public final class MultiblockRegistry {
    private static final Map<Level, LevelIndex> LEVELS = new WeakHashMap<>();

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
        if (index != null && index.chunksOf.containsKey(controller)) {
            index.remove(controller);
            index.add(controller);
        }
    }

    public static void unregisterChunk(Level level, ChunkPos chunk) {
        LevelIndex index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        List<MultiblockController> inChunk = new ArrayList<>();
        for (MultiblockController controller : index.chunksOf.keySet()) {
            if (new ChunkPos(controller.getBlockPos()).equals(chunk)) {
                inChunk.add(controller);
            }
        }
        inChunk.forEach(index::remove);
    }

    /** Live controllers whose interest bounds contain {@code pos}. */
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
        List<MultiblockController> result = new ArrayList<>(candidates.size());
        List<MultiblockController> stale = null;
        for (MultiblockController controller : candidates) {
            if (!isLive(level, controller)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(controller);
                continue;
            }
            if (controller.interestBounds().isInside(pos)) {
                result.add(controller);
            }
        }
        if (stale != null) {
            stale.forEach(index::remove);
        }
        return result;
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
        for (MultiblockController controller : index.chunksOf.keySet()) {
            if (type.isInstance(controller) && isLive(level, controller)) {
                result.add(type.cast(controller));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean isLive(Level level, MultiblockController controller) {
        return !controller.isRemoved()
                && controller.getLevel() == level
                && level.getBlockEntity(controller.getBlockPos()) == controller;
    }

    private static LevelIndex index(Level level) {
        return LEVELS.computeIfAbsent(level, ignored -> new LevelIndex());
    }

    private static final class LevelIndex {
        private final Map<BlockPos, MultiblockController> byPos = new HashMap<>();
        private final Map<MultiblockController, long[]> chunksOf = new IdentityHashMap<>();
        private final Long2ObjectMap<Set<MultiblockController>> byChunk = new Long2ObjectOpenHashMap<>();

        private void add(MultiblockController controller) {
            MultiblockController previous = byPos.put(controller.getBlockPos().immutable(), controller);
            if (previous != null && previous != controller) {
                removeFromChunks(previous);
            }
            long[] chunks = chunksTouched(controller.interestBounds());
            chunksOf.put(controller, chunks);
            for (long chunk : chunks) {
                byChunk.computeIfAbsent(chunk, ignored -> new LinkedHashSet<>()).add(controller);
            }
        }

        private void remove(MultiblockController controller) {
            byPos.remove(controller.getBlockPos(), controller);
            removeFromChunks(controller);
        }

        private void removeFromChunks(MultiblockController controller) {
            long[] chunks = chunksOf.remove(controller);
            if (chunks == null) {
                return;
            }
            for (long chunk : chunks) {
                Set<MultiblockController> set = byChunk.get(chunk);
                if (set != null) {
                    set.remove(controller);
                    if (set.isEmpty()) {
                        byChunk.remove(chunk);
                    }
                }
            }
        }

        private static long[] chunksTouched(BoundingBox bounds) {
            int minX = SectionPos.blockToSectionCoord(bounds.minX());
            int maxX = SectionPos.blockToSectionCoord(bounds.maxX());
            int minZ = SectionPos.blockToSectionCoord(bounds.minZ());
            int maxZ = SectionPos.blockToSectionCoord(bounds.maxZ());
            long[] chunks = new long[(maxX - minX + 1) * (maxZ - minZ + 1)];
            int i = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    chunks[i++] = ChunkPos.asLong(x, z);
                }
            }
            return chunks;
        }
    }
}
