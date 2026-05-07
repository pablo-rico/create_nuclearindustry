package org.papiricoh.create_nuclearindustry.reactor.event;

import net.minecraft.core.BlockPos;
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
     * Get all reactors within range of a position (only checks registered reactors).
     */
    public static List<ReactorBlockEntity> getReactorsInRange(Level level, BlockPos center, int range) {
        if (level.isClientSide) return List.of();

        List<ReactorBlockEntity> result = new ArrayList<>();
        Map<BlockPos, ReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors == null) return result;

        for (Map.Entry<BlockPos, ReactorBlockEntity> entry : reactors.entrySet()) {
            if (entry.getKey().closerThan(center, range)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
}
