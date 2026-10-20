package org.papiricoh.create_nuclearindustry.missile.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * The orders given in a level and not yet launched, so batteries and command tables do not all fire at the same missile. Not
 * saved: an order left over a reload is simply given again.
 */
public final class EngagementBoard {
    private EngagementBoard() {
    }

    /** Notes that the launcher at {@code shooter} is to fire at {@code target}, until game tick {@code expires}. */
    public static void reserve(ServerLevel level, UUID target, BlockPos shooter, long expires) {
    }

    /** Drops what the launcher at {@code shooter} was to fire at {@code target}: launched or given up. */
    public static void release(ServerLevel level, UUID target, BlockPos shooter) {
    }

    /** Interceptors ordered at {@code target} and not yet launched. */
    public static int reserved(ServerLevel level, UUID target) {
        return 0;
    }
}
