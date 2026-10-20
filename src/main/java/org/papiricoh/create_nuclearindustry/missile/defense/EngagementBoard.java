package org.papiricoh.create_nuclearindustry.missile.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * The orders given in a level and not yet launched, so batteries and command tables do not all fire at the same missile. Not
 * saved: an order left over a reload is simply given again.
 */
public final class EngagementBoard {
    private record Key(UUID target, BlockPos shooter) {}
    private static final java.util.Map<ServerLevel, java.util.Map<Key, java.util.List<Long>>> LEVELS = new java.util.WeakHashMap<>();

    private EngagementBoard() {
    }

    /** Notes that the launcher at {@code shooter} is to fire at {@code target}, until game tick {@code expires}. */
    public static void reserve(ServerLevel level, UUID target, BlockPos shooter, long expires) {
        LEVELS.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
                .computeIfAbsent(new Key(target, shooter.immutable()), ignored -> new java.util.ArrayList<>()).add(expires);
    }

    /** Drops what the launcher at {@code shooter} was to fire at {@code target}: launched or given up. */
    public static void release(ServerLevel level, UUID target, BlockPos shooter) {
        var board = LEVELS.get(level);
        if (board == null) return;
        Key key = new Key(target, shooter);
        var orders = board.get(key);
        if (orders != null && !orders.isEmpty()) {
            orders.remove(0);
            if (orders.isEmpty()) board.remove(key);
        }
    }

    /** Interceptors ordered at {@code target} and not yet launched. */
    public static int reserved(ServerLevel level, UUID target) {
        var board = LEVELS.get(level);
        if (board == null) return 0;
        board.values().forEach(orders -> orders.removeIf(expiry -> expiry <= level.getGameTime()));
        board.values().removeIf(java.util.List::isEmpty);
        return board.entrySet().stream().filter(entry -> entry.getKey().target().equals(target))
                .mapToInt(entry -> entry.getValue().size()).sum();
    }
}
