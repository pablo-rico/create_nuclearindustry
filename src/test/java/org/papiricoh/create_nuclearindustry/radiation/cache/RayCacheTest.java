package org.papiricoh.create_nuclearindustry.radiation.cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RayCacheTest {
    private record Key(long source, long body) {}

    private static double cast(RayCache cache, ChunkChangeLog log, Object key, long now, int ttl, AtomicInteger casts, double value) {
        return cache.get(key, log, now, ttl, 0.5, 0.5, 40.5, 0.5, () -> {
            casts.incrementAndGet();
            return value;
        });
    }

    /** Expiry counts from the last use: a ray read again within its lifetime lives on. */
    @Test
    void aRayIsReusedUntilItGoesUnusedForItsLifetime() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        Key key = new Key(1, 2);
        assertEquals(0.5, cast(cache, log, key, 100, 20, casts, 0.5));
        assertEquals(0.5, cast(cache, log, key, 120, 20, casts, 0.9), "still cached 20 ticks after its last use");
        assertEquals(0.5, cast(cache, log, key, 140, 20, casts, 0.9), "and again, 40 ticks after it was cast");
        assertEquals(1, casts.get());
        assertEquals(0.9, cast(cache, log, key, 161, 20, casts, 0.9), "unused for more than 20 ticks: cast again");
        assertEquals(2, casts.get());
        assertEquals(2, cache.hits());
        assertEquals(2, cache.misses());
    }

    /** The once-a-second player update with the default lifetime: one cast, then hits for as long as nothing changes. */
    @Test
    void aRayReadEverySecondIsCastOnce() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        Key key = new Key(1, 2);
        for (long tick = 0; tick <= 20 * 30; tick += 20) {
            cast(cache, log, key, tick, 100, casts, 0.5);
            cache.prune(tick, 100, 0);
        }
        assertEquals(1, casts.get(), "thirty seconds of updates cast the ray once");
        assertEquals(30, cache.hits());
    }

    @Test
    void aRayInUseIsCastAgainOnceItReachesItsMaximumAge() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        Key key = new Key(1, 2);
        for (long tick = 0; tick < 200; tick += 20) {
            cache.get(key, log, tick, 100, 100, 0.5, 0.5, 40.5, 0.5, () -> {
                casts.incrementAndGet();
                return 0.5;
            });
        }
        assertEquals(2, casts.get(), "cast at 0 and again at 100, though read every 20 ticks");
    }

    @Test
    void eitherEndMovingIsAnotherRay() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        cast(cache, log, new Key(1, 2), 0, 20, casts, 0.5);
        cast(cache, log, new Key(1, 3), 0, 20, casts, 0.5);
        cast(cache, log, new Key(4, 2), 0, 20, casts, 0.5);
        cast(cache, log, new Key(1, 2), 0, 20, casts, 0.5);
        assertEquals(3, casts.get());
        assertEquals(3, cache.size());
    }

    @Test
    void aBlockChangeAlongTheRayMakesItStale() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        Key key = new Key(1, 2);
        cast(cache, log, key, 0, 100, casts, 0.5);
        log.mark(0, 5, 1);
        assertEquals(0.5, cast(cache, log, key, 2, 100, casts, 0.1), "a change in a chunk off the ray does not count");
        // The ray runs along z = 0.5 from x = 0.5 to x = 40.5: chunks 0, 1 and 2.
        log.mark(2, 0, 3);
        assertEquals(0.1, cast(cache, log, key, 4, 100, casts, 0.1), "one in a chunk it crosses does");
        assertEquals(2, casts.get());
    }

    @Test
    void noTimeToLiveMeansNoCache() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            cast(cache, log, new Key(1, 2), 0, 0, casts, 0.5);
        }
        assertEquals(3, casts.get());
        assertEquals(0, cache.size());
    }

    @Test
    void expiredRaysArePruned() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        cast(cache, log, new Key(1, 2), 0, 10, casts, 0.5);
        cast(cache, log, new Key(3, 4), 5, 10, casts, 0.5);
        cache.prune(12, 10, 0);
        assertEquals(1, cache.size(), "the first has gone unused for more than 10 ticks, the second not");
        cache.prune(16, 10, 0);
        assertEquals(0, cache.size());
    }
}
