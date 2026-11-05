package org.papiricoh.create_nuclearindustry.dyson.swarm;

import java.util.Arrays;

/**
 * The sails of one orbit, counted by when they wear out rather than one by one: each cohort is a number of sails ending on the
 * same {@link #BUCKET}-tick mark, oldest first, so a swarm of thousands is a few hundred numbers and wears out in order. Free of
 * game classes, for JUnit.
 */
public final class SailCohorts {
    /** Sails launched within a minute of each other wear out together. */
    public static final long BUCKET = 1200;

    private long[] expires = new long[8];
    private int[] counts = new int[8];
    private int size;
    private long total;

    /** Adds {@code count} sails that wear out at {@code expiresAt} (rounded up to the next {@link #BUCKET} mark). */
    public void add(long expiresAt, int count) {
        if (count <= 0) return;
        long mark = Math.floorDiv(expiresAt + BUCKET - 1, BUCKET) * BUCKET;
        int at = size;
        while (at > 0 && expires[at - 1] > mark) at--;
        total += count;
        if (at > 0 && expires[at - 1] == mark) {
            counts[at - 1] += count;
            return;
        }
        if (size == expires.length) {
            expires = Arrays.copyOf(expires, size * 2);
            counts = Arrays.copyOf(counts, size * 2);
        }
        System.arraycopy(expires, at, expires, at + 1, size - at);
        System.arraycopy(counts, at, counts, at + 1, size - at);
        expires[at] = mark;
        counts[at] = count;
        size++;
    }

    /** Takes away every sail worn out by {@code now}; returns how many. */
    public long expire(long now) {
        int gone = 0;
        long sails = 0;
        while (gone < size && expires[gone] <= now) sails += counts[gone++];
        if (gone > 0) {
            System.arraycopy(expires, gone, expires, 0, size - gone);
            System.arraycopy(counts, gone, counts, 0, size - gone);
            size -= gone;
            total -= sails;
        }
        return sails;
    }

    /** Takes away {@code count} sails, the oldest first (wiped by a command, or past a lowered cap); returns how many went. */
    public long removeOldest(long count) {
        long left = Math.max(0, count);
        while (left > 0 && size > 0) {
            int take = (int) Math.min(left, counts[0]);
            counts[0] -= take;
            total -= take;
            left -= take;
            if (counts[0] == 0) {
                System.arraycopy(expires, 1, expires, 0, size - 1);
                System.arraycopy(counts, 1, counts, 0, size - 1);
                size--;
            }
        }
        return Math.max(0, count) - left;
    }

    public long total() {
        return total;
    }

    /** Sails that wear out before {@code time}. */
    public long expiringBefore(long time) {
        long sails = 0;
        for (int i = 0; i < size && expires[i] < time; i++) sails += counts[i];
        return sails;
    }

    /** When the next sails wear out, or -1 with none left. */
    public long nextExpiry() {
        return size == 0 ? -1 : expires[0];
    }

    public void clear() {
        size = 0;
        total = 0;
    }

    /** The cohorts' wear-out marks, oldest first, for saving. */
    public long[] marks() {
        return Arrays.copyOf(expires, size);
    }

    /** The cohorts' sail counts, in {@link #marks} order. */
    public int[] counts() {
        return Arrays.copyOf(counts, size);
    }

    /** Cohorts as {@link #marks} and {@link #counts} saved them; mismatched or bad entries are dropped. */
    public static SailCohorts of(long[] marks, int[] counts) {
        SailCohorts cohorts = new SailCohorts();
        for (int i = 0; i < Math.min(marks.length, counts.length); i++) cohorts.add(marks[i], counts[i]);
        return cohorts;
    }
}
