package org.papiricoh.create_nuclearindustry.civildefense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What air raid sirens listen to: missiles in flight, bomb countdowns and launch countdowns, each
 * reported by its source while it is pending and withdrawn when it is cancelled or goes off. Kept in
 * memory only: after a reload every source reports itself again on its next tick. Other areas
 * (a meltdown, a containment breach) {@link #raise} a hazard for a while.
 */
public final class ThreatRegistry {
    /** How long a reported threat outlives its impact time if its source stops reporting (unloaded, say). */
    public static final int REPORT_GRACE_TICKS = 40;

    private static final Map<Level, ThreatBoard> BOARDS = new WeakHashMap<>();

    private ThreatRegistry() {}

    /**
     * Reports, or refreshes, the threat of {@code source}: an impact at {@code target} in
     * {@code ticksToImpact} ticks reaching {@code radius} blocks around it.
     */
    public static void report(Level level, Object source, BlockPos target, int radius, int ticksToImpact) {
        if (level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        int ticks = Math.max(0, ticksToImpact);
        board(level).put(source, new ThreatBoard.Threat(target.getX(), target.getY(), target.getZ(), radius,
                now + ticks, now + ticks + REPORT_GRACE_TICKS));
    }

    /** The threat of {@code source} is over: cancelled, launched as something else, or gone off. */
    public static void withdraw(Level level, Object source) {
        if (level.isClientSide) {
            return;
        }
        ThreatBoard board = BOARDS.get(level);
        if (board != null) {
            board.remove(source);
        }
    }

    /** A hazard under way at {@code pos} for the next {@code ticks} ticks, for anything that is not a countdown. */
    public static void raise(Level level, BlockPos pos, int radius, int ticks) {
        if (level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        board(level).put(new Object(), new ThreatBoard.Threat(pos.getX(), pos.getY(), pos.getZ(), radius,
                now, now + Math.max(0, ticks)));
    }

    @Nullable
    public static ThreatBoard.Threat find(Level level, Object source) {
        ThreatBoard board = BOARDS.get(level);
        if (board == null) {
            return null;
        }
        board.prune(level.getGameTime());
        return board.get(source);
    }

    /** The threat that lands first among those in reach of a siren at {@code listener}, or {@code null}. */
    @Nullable
    public static ThreatBoard.Threat threatening(Level level, BlockPos listener, int listenRadius) {
        ThreatBoard board = BOARDS.get(level);
        if (board == null) {
            return null;
        }
        return board.soonestReaching(listener.getX(), listener.getZ(), listenRadius, level.getGameTime());
    }

    /** Forgets every threat of a level; for tests. */
    public static void clear(Level level) {
        ThreatBoard board = BOARDS.get(level);
        if (board != null) {
            board.clear();
        }
    }

    private static ThreatBoard board(Level level) {
        return BOARDS.computeIfAbsent(level, ignored -> new ThreatBoard());
    }
}
