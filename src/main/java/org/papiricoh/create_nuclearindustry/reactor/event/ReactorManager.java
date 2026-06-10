package org.papiricoh.create_nuclearindustry.reactor.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.*;

/**
 * Global registry of active reactors indexed by position.
 * Replaces expensive spatial searches with O(1) lookups.
 */
public class ReactorManager {
    private static final Map<Level, Map<BlockPos, ReactorBlockEntity>> reactorsByLevel = new WeakHashMap<>();

    /**
     * Register a reactor when it's loaded/created.
     */
    public static void registerReactor(Level level, BlockPos pos, ReactorBlockEntity reactor) {
        if (level.isClientSide) return;
        reactorsByLevel.computeIfAbsent(level, k -> new HashMap<>())
                .put(pos, reactor);
    }

    /**
     * Unregister a reactor when it's unloaded/destroyed.
     */
    public static void unregisterReactor(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Map<BlockPos, ReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors != null) {
            reactors.remove(pos);
        }
    }

    /**
     * Unregister all reactors from an unloading chunk.
     */
    public static void unregisterReactorsInChunk(Level level, ChunkPos chunkPos) {
        if (level.isClientSide) return;
        Map<BlockPos, ReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors == null) return;
        reactors.entrySet().removeIf(entry -> new ChunkPos(entry.getKey()).equals(chunkPos));
    }

    /**
     * Get all reactors within range of a position (only checks registered reactors).
     */
    public static List<ReactorBlockEntity> getReactorsInRange(Level level, BlockPos center, int range) {
        if (level.isClientSide) return List.of();

        List<ReactorBlockEntity> result = new ArrayList<>();
        Map<BlockPos, ReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors == null) return result;

        Iterator<Map.Entry<BlockPos, ReactorBlockEntity>> iterator = reactors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ReactorBlockEntity> entry = iterator.next();
            ReactorBlockEntity reactor = entry.getValue();
            if (reactor == null
                    || reactor.isRemoved()
                    || reactor.getLevel() != level
                    || level.getBlockEntity(entry.getKey()) != reactor) {
                iterator.remove();
                continue;
            }
            if (entry.getKey().closerThan(center, range)) {
                result.add(reactor);
            }
        }
        return result;
    }
}
