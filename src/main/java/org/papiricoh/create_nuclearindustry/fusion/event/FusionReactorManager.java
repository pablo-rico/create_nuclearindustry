package org.papiricoh.create_nuclearindustry.fusion.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Global registry of active fusion reactors indexed by position (mirror of {@code ReactorManager}).
 */
public class FusionReactorManager {
    private static final Map<Level, Map<BlockPos, FusionReactorBlockEntity>> reactorsByLevel = new WeakHashMap<>();

    public static void registerReactor(Level level, BlockPos pos, FusionReactorBlockEntity reactor) {
        if (level.isClientSide) return;
        reactorsByLevel.computeIfAbsent(level, k -> new HashMap<>()).put(pos, reactor);
    }

    public static void unregisterReactor(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Map<BlockPos, FusionReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors != null) {
            reactors.remove(pos);
        }
    }

    public static void unregisterReactorsInChunk(Level level, ChunkPos chunkPos) {
        if (level.isClientSide) return;
        Map<BlockPos, FusionReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors == null) return;
        reactors.entrySet().removeIf(entry -> new ChunkPos(entry.getKey()).equals(chunkPos));
    }

    public static List<FusionReactorBlockEntity> getReactorsInRange(Level level, BlockPos center, int range) {
        if (level.isClientSide) return List.of();

        List<FusionReactorBlockEntity> result = new ArrayList<>();
        Map<BlockPos, FusionReactorBlockEntity> reactors = reactorsByLevel.get(level);
        if (reactors == null) return result;

        Iterator<Map.Entry<BlockPos, FusionReactorBlockEntity>> iterator = reactors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, FusionReactorBlockEntity> entry = iterator.next();
            FusionReactorBlockEntity reactor = entry.getValue();
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
