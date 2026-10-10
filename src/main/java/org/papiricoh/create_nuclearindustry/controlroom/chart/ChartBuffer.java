package org.papiricoh.create_nuclearindustry.controlroom.chart;

import java.util.Arrays;

/**
 * The trace a chart recorder has drawn: up to {@link #CAPACITY} samples of each pen, oldest first,
 * as 0-1 fractions of full scale. A full chart drops its oldest sample for every new one, like paper
 * running off the end. Values are kept quantized to a byte, the resolution a chart roll stores, so
 * what the block draws and what a torn-off roll shows are the same trace.
 */
public final class ChartBuffer {
    public static final int CAPACITY = 240;
    public static final int MAX_PENS = 3;
    /** Steps between empty and full scale in a stored sample. */
    public static final int LEVELS = 255;

    private final byte[][] pens = new byte[MAX_PENS][CAPACITY];
    /** Index of the oldest sample in the ring. */
    private int start;
    private int size;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        start = 0;
        size = 0;
    }

    /** Adds one sample for every pen; pens past {@code values.length} record 0. */
    public void push(float... values) {
        int index;
        if (size < CAPACITY) {
            index = (start + size) % CAPACITY;
            size++;
        } else {
            index = start;
            start = (start + 1) % CAPACITY;
        }
        for (int pen = 0; pen < MAX_PENS; pen++) {
            pens[pen][index] = quantize(pen < values.length ? values[pen] : 0.0f);
        }
    }

    /** Sample {@code index} (0 = oldest) of {@code pen}, as a 0-1 fraction. */
    public float get(int pen, int index) {
        if (pen < 0 || pen >= MAX_PENS || index < 0 || index >= size) {
            return 0.0f;
        }
        return dequantize(pens[pen][(start + index) % CAPACITY]);
    }

    /** The newest sample of {@code pen}, or 0 on an empty chart. */
    public float latest(int pen) {
        return size == 0 ? 0.0f : get(pen, size - 1);
    }

    /** {@code pen}'s samples, oldest first, one byte each. */
    public byte[] bytes(int pen) {
        byte[] out = new byte[size];
        if (pen < 0 || pen >= MAX_PENS) {
            return out;
        }
        for (int i = 0; i < size; i++) {
            out[i] = pens[pen][(start + i) % CAPACITY];
        }
        return out;
    }

    /**
     * Replaces the chart with stored series, oldest first. Series of different lengths are cut to the
     * newest samples of the shortest, only the newest {@link #CAPACITY} are kept, and pens with no
     * series read 0.
     */
    public void load(byte[]... series) {
        clear();
        int length = CAPACITY;
        boolean any = false;
        for (int pen = 0; pen < Math.min(MAX_PENS, series.length); pen++) {
            if (series[pen] != null) {
                length = Math.min(length, series[pen].length);
                any = true;
            }
        }
        if (!any) {
            length = 0;
        }
        for (int pen = 0; pen < MAX_PENS; pen++) {
            Arrays.fill(pens[pen], (byte) 0);
            if (pen < series.length && series[pen] != null) {
                int skip = series[pen].length - length;
                System.arraycopy(series[pen], skip, pens[pen], 0, length);
            }
        }
        size = length;
    }

    public static byte quantize(float fraction) {
        float clamped = Float.isNaN(fraction) ? 0.0f : Math.max(0.0f, Math.min(1.0f, fraction));
        return (byte) Math.round(clamped * LEVELS);
    }

    public static float dequantize(byte value) {
        return (value & 0xFF) / (float) LEVELS;
    }
}
