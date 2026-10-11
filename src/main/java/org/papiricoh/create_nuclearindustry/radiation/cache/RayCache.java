package org.papiricoh.create_nuclearindustry.radiation.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Results of rays (the fraction of radiation that crosses the blocks between a source and a
 * body), reused while they are in use. A result keyed by where the source and the body are is
 * valid until nobody has asked for it for more than {@code ttl} ticks, it is older than {@code maxAge}
 * ticks, or a block changes in a chunk the ray crosses, per a {@link ChunkChangeLog}. Keys say
 * where both ends are, so either moving asks for a new ray.
 * <p>
 * Expiry counts from the last use, not from the cast: players are updated once a second, so a
 * lifetime counted from the cast had to be longer than that interval to ever be hit, and every
 * standing player recast every ray every second. The age cap bounds how long a change that
 * fires no block event (a block set without neighbour updates) can go unnoticed. No game
 * classes, so it can be unit tested.
 */
public final class RayCache {
    /** Computes a ray that is not cached. */
    @FunctionalInterface
    public interface Ray {
        double cast();
    }

    private static final class Entry {
        final double value;
        final long cast;
        final long since;
        final int minChunkX, minChunkZ, maxChunkX, maxChunkZ;
        long lastUsed;

        Entry(double value, long cast, long since, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            this.value = value;
            this.cast = cast;
            this.lastUsed = cast;
            this.since = since;
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.maxChunkX = maxChunkX;
            this.maxChunkZ = maxChunkZ;
        }

        boolean expired(long now, int ttl, int maxAge) {
            return now - lastUsed > ttl || (maxAge > 0 && now - cast >= maxAge) || now < cast;
        }
    }

    private final Map<Object, Entry> entries = new HashMap<>();
    private int hits;
    private int misses;

    /** As {@link #get(Object, ChunkChangeLog, long, int, int, double, double, double, double, Ray)} without an age cap. */
    public double get(Object key, ChunkChangeLog changes, long now, int ttl,
                      double fromX, double fromZ, double toX, double toZ, Ray ray) {
        return get(key, changes, now, ttl, 0, fromX, fromZ, toX, toZ, ray);
    }

    /**
     * The cached value for {@code key} if still good at {@code now}, otherwise {@code ray}'s. A
     * value stays good while it is asked for again within {@code ttl} ticks of its last use, for at most
     * {@code maxAge} ticks after it was cast (0: no cap). The ray crosses blocks from
     * ({@code fromX}, {@code fromZ}) to ({@code toX}, {@code toZ}); a change in any chunk between
     * them makes it stale. A {@code ttl} of 0 or less casts every time.
     */
    public double get(Object key, ChunkChangeLog changes, long now, int ttl, int maxAge,
                      double fromX, double fromZ, double toX, double toZ, Ray ray) {
        if (ttl <= 0) {
            misses++;
            return ray.cast();
        }
        Entry entry = entries.get(key);
        if (entry != null && !entry.expired(now, ttl, maxAge)
                && !changes.changedSince(entry.since, entry.minChunkX, entry.minChunkZ, entry.maxChunkX, entry.maxChunkZ)) {
            hits++;
            entry.lastUsed = now;
            return entry.value;
        }
        misses++;
        long since = changes.issue();
        double value = ray.cast();
        entries.put(key, new Entry(value, now, since,
                chunk(Math.min(fromX, toX)), chunk(Math.min(fromZ, toZ)), chunk(Math.max(fromX, toX)), chunk(Math.max(fromZ, toZ))));
        return value;
    }

    /** Drops results nobody has used for more than {@code ttl} ticks by {@code now}, or older than {@code maxAge} (0: no cap). */
    public void prune(long now, int ttl, int maxAge) {
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expired(now, ttl, maxAge)) {
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
