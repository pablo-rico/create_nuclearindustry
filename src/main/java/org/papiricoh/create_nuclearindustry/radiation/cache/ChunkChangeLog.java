package org.papiricoh.create_nuclearindustry.radiation.cache;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

/**
 * Which chunks of a level had blocks change, and in what order: each change gets a sequence
 * number, so a cached result taken at sequence {@code s} is stale once any chunk it depends on
 * has a stamp above {@code s}. Repeated changes to a chunk that is already newer than every
 * cached result cost a lookup and nothing more. No game classes, so it can be unit tested.
 */
public final class ChunkChangeLog {
    /** Last change's sequence number per packed chunk position. */
    private final Long2LongOpenHashMap stamps = new Long2LongOpenHashMap();
    /** Game time of each chunk's last change, so old stamps can be forgotten. */
    private final Long2LongOpenHashMap times = new Long2LongOpenHashMap();
    private long sequence;
    /** Highest sequence handed out to a cached result. */
    private long issued;

    /** Records a change in chunk ({@code chunkX}, {@code chunkZ}) at {@code gameTime}. */
    public void mark(int chunkX, int chunkZ, long gameTime) {
        long key = pack(chunkX, chunkZ);
        times.put(key, gameTime);
        if (stamps.get(key) > issued) {
            // Every cached result is already older than this chunk's last change.
            return;
        }
        stamps.put(key, ++sequence);
    }

    /** The sequence a result computed now depends on; it stays valid until a later change. */
    public long issue() {
        issued = sequence;
        return sequence;
    }

    /** Whether any chunk in the rectangle changed after {@code since} was issued. */
    public boolean changedSince(long since, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        if (sequence <= since) {
            return false;
        }
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (stamps.get(pack(x, z)) > since) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Forgets chunks that last changed before {@code gameTime}: once every result cached before
     * then has expired, their stamps can no longer make one stale.
     */
    public void forgetBefore(long gameTime) {
        var iterator = times.long2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            Long2LongMap.Entry entry = iterator.next();
            if (entry.getLongValue() < gameTime) {
                stamps.remove(entry.getLongKey());
                iterator.remove();
            }
        }
    }

    /** Chunks with a remembered change. */
    public int size() {
        return stamps.size();
    }

    public long sequence() {
        return sequence;
    }

    static long pack(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }
}
