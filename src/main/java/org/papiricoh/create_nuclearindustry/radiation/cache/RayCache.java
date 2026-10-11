package org.papiricoh.create_nuclearindustry.radiation.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Results of rays (the fraction of radiation that crosses the blocks between a source and a
 * body), reused for a few ticks. A result keyed by where the source and the body are is valid
 * until it expires or a block changes in a chunk the ray crosses, per a {@link ChunkChangeLog}.
 * Keys say where both ends are, so either moving asks for a new ray. No game classes, so it can
 * be unit tested.
 */
public final class RayCache {
    /** Computes a ray that is not cached. */
    @FunctionalInterface
    public interface Ray {
        double cast();
    }

    private record Entry(double value, long expires, long since, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {}

    private final Map<Object, Entry> entries = new HashMap<>();
    private int hits;
    private int misses;

    /**
     * The cached value for {@code key} if still good at {@code now}, otherwise {@code ray}'s,
     * remembered for {@code ttl} ticks. The ray crosses blocks from ({@code fromX}, {@code fromZ})
     * to ({@code toX}, {@code toZ}); a change in any chunk between them makes it stale. A
     * {@code ttl} of 0 or less casts every time.
     */
    public double get(Object key, ChunkChangeLog changes, long now, int ttl,
                      double fromX, double fromZ, double toX, double toZ, Ray ray) {
        if (ttl <= 0) {
            misses++;
            return ray.cast();
        }
        Entry entry = entries.get(key);
        if (entry != null && now < entry.expires
                && !changes.changedSince(entry.since, entry.minChunkX, entry.minChunkZ, entry.maxChunkX, entry.maxChunkZ)) {
            hits++;
            return entry.value;
        }
        misses++;
        long since = changes.issue();
        double value = ray.cast();
        entries.put(key, new Entry(value, now + ttl, since,
                chunk(Math.min(fromX, toX)), chunk(Math.min(fromZ, toZ)), chunk(Math.max(fromX, toX)), chunk(Math.max(fromZ, toZ))));
        return value;
    }

    /** Drops results that have expired by {@code now}. */
    public void prune(long now) {
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            if (now >= iterator.next().expires) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    /** Lookups answered from the cache since the counters were last reset. */
    public int hits() {
        return hits;
    }

    /** Lookups that cast a ray since the counters were last reset. */
    public int misses() {
        return misses;
    }

    public void resetCounters() {
        hits = 0;
        misses = 0;
    }

    private static int chunk(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }
}
