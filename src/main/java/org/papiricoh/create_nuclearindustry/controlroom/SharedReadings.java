package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Samples of linked machines shared within a tick: an annunciator with six windows on one reactor,
 * the alarms around it and the screens above all read the same machine in the same tick, and the
 * first of them to ask pays for the sample. Only the server's game thread uses it. A link with no
 * signal (out of range, unloaded, another dimension) depends on where the device stands, so it is
 * never shared; it costs nothing to work out anyway.
 */
public final class SharedReadings {
    private static final Map<Level, Tick> LEVELS = new WeakHashMap<>();
    private static long hits;
    private static long misses;

    private SharedReadings() {}

    /** What {@code target} reads now, as seen from {@code from}: {@link MonitorReading#sample} of the resolved link. */
    public static MonitorReading sample(Level level, BlockPos from, @Nullable GlobalPos target) {
        LinkedTarget.Resolved resolved = LinkedTarget.resolve(level, from, target);
        if (target == null || level.isClientSide || resolved instanceof LinkedTarget.NoSignal) {
            return MonitorReading.sample(resolved);
        }
        long now = level.getGameTime();
        Tick tick = LEVELS.get(level);
        if (tick == null || tick.time != now) {
            tick = new Tick(now);
            LEVELS.put(level, tick);
        }
        MonitorReading cached = tick.readings.get(target);
        if (cached != null) {
            hits++;
            return cached;
        }
        misses++;
        MonitorReading reading = MonitorReading.sample(resolved);
        tick.readings.put(target, reading);
        return reading;
    }

    /**
     * Whether a device checking every {@code interval} ticks checks this tick. The phase comes from the
     * machine it watches (its own position when unlinked): devices on one machine check together and
     * share its sample, devices on different machines spread over the interval instead of all landing
     * on the same tick.
     */
    public static boolean due(Level level, BlockPos from, @Nullable GlobalPos target, int interval) {
        int period = Math.max(1, interval);
        return Math.floorMod(level.getGameTime() + phase(target, from), period) == 0;
    }

    /** A well-mixed number for the watched machine's position (or {@code from}'s), the same on every call. */
    public static long phase(@Nullable GlobalPos target, BlockPos from) {
        long key = target != null ? target.pos().asLong() : from.asLong();
        // SplitMix64's finaliser: neighbouring positions get unrelated phases.
        key = (key ^ (key >>> 30)) * 0xBF58476D1CE4E5B9L;
        key = (key ^ (key >>> 27)) * 0x94D049BB133111EBL;
        return (key ^ (key >>> 31)) & Long.MAX_VALUE;
    }

    /** Samples served from another device's sample this tick, for tests. */
    public static long hits() {
        return hits;
    }

    /** Samples taken, for tests. */
    public static long misses() {
        return misses;
    }

    private static final class Tick {
        final long time;
        final Map<GlobalPos, MonitorReading> readings = new HashMap<>();

        Tick(long time) {
            this.time = time;
        }
    }
}
