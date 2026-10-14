package org.papiricoh.create_nuclearindustry.controlroom.desk;

import java.util.Arrays;

/**
 * The trace a trend recorder module keeps: its last {@link #CAPACITY} samples of a reading, oldest
 * first, as fractions of full scale quantized to a byte. A sample taken while the module had nothing to
 * read is a gap, drawn as a break in the line. No game classes.
 */
public final class TrendRing {
    public static final int CAPACITY = 48;
    /** Seconds between samples for each speed setting: about a minute, four and sixteen minutes on the screen. */
    public static final int[] SECONDS = {1, 5, 20};
    /** A stored gap; values run 0 to {@link #LEVELS}. */
    public static final byte GAP = (byte) 0xFF;
    public static final int LEVELS = 254;

    private final byte[] samples = new byte[CAPACITY];
    /** Index of the oldest sample. */
    private int start;
    private int size;

    public int size() {
        return size;
    }

    /** Adds a sample, a fraction of full scale, or NaN for a gap; a full ring drops its oldest. */
    public void push(float fraction) {
        byte value = quantize(fraction);
        if (size < CAPACITY) {
            samples[(start + size) % CAPACITY] = value;
            size++;
        } else {
            samples[start] = value;
            start = (start + 1) % CAPACITY;
        }
    }

    /** The samples, oldest first. */
    public byte[] ordered() {
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[i] = samples[(start + i) % CAPACITY];
        }
        return out;
    }

    /** Starts again from samples saved by {@link #ordered}, keeping the newest that fit. */
    public void load(byte[] ordered) {
        Arrays.fill(samples, (byte) 0);
        start = 0;
        size = 0;
        int from = Math.max(0, ordered.length - CAPACITY);
        for (int i = from; i < ordered.length; i++) {
            samples[size++] = ordered[i];
        }
    }

    public void clear() {
        start = 0;
        size = 0;
    }

    public static byte quantize(float fraction) {
        if (Float.isNaN(fraction)) {
            return GAP;
        }
        return (byte) Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * LEVELS);
    }

    /** A stored sample as a fraction of full scale, or NaN for a gap. */
    public static float value(byte stored) {
        return stored == GAP ? Float.NaN : (stored & 0xFF) / (float) LEVELS;
    }

    /** Seconds between samples for a speed setting, clamped to the known speeds. */
    public static int secondsFor(int setting) {
        return SECONDS[Math.max(0, Math.min(SECONDS.length - 1, setting))];
    }
}
