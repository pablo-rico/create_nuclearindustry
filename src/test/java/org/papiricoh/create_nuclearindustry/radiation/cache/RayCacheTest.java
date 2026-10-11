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

    @Test
    void aRayIsReusedUntilItExpires() {
        RayCache cache = new RayCache();
        ChunkChangeLog log = new ChunkChangeLog();
        AtomicInteger casts = new AtomicInteger();
        Key key = new Key(1, 2);
        assertEquals(0.5, cast(cache, log, key, 100, 20, casts, 0.5));
        assertEquals(0.5, cast(cache, log, key, 119, 20, casts, 0.9), "still cached at the last tick");
        assertEquals(1, casts.get());
        assertEquals(0.9, cast(cache, log, key, 120, 20, casts, 0.9), "expired: cast again");
        assertEquals(2, casts.get());
        assertEquals(1, cache.hits());
        assertEquals(2, cache.misses());
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
        cache.prune(12);
        assertEquals(1, cache.size(), "the first has expired, the second not");
        cache.prune(15);
        assertEquals(0, cache.size());
    }
}
