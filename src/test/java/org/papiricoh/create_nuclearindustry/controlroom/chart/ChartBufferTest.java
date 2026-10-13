package org.papiricoh.create_nuclearindustry.controlroom.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartBufferTest {
    private static final float STEP = 1.0f / ChartBuffer.LEVELS;

    @Test
    void samplesComeBackOldestFirstPerPen() {
        ChartBuffer chart = new ChartBuffer();
        chart.push(0.0f, 0.5f, 1.0f);
        chart.push(0.25f, 0.75f, 0.1f);
        assertEquals(2, chart.size());
        assertEquals(0.0f, chart.get(0, 0), STEP);
        assertEquals(0.25f, chart.get(0, 1), STEP);
        assertEquals(0.5f, chart.get(1, 0), STEP);
        assertEquals(1.0f, chart.get(2, 0), STEP);
        assertEquals(0.1f, chart.latest(2), STEP);
    }

    @Test
    void aFullChartDropsTheOldestSample() {
        ChartBuffer chart = new ChartBuffer();
        for (int i = 0; i < ChartBuffer.CAPACITY + 10; i++) {
            chart.push((i % 100) / 100.0f);
        }
        assertEquals(ChartBuffer.CAPACITY, chart.size());
        // The oldest kept sample is number 10.
        assertEquals(0.10f, chart.get(0, 0), STEP);
        assertEquals(((ChartBuffer.CAPACITY + 9) % 100) / 100.0f, chart.latest(0), STEP);
    }

    @Test
    void valuesAreClampedAndQuantizedToAByte() {
        assertEquals(0, ChartBuffer.quantize(-3.0f) & 0xFF);
        assertEquals(255, ChartBuffer.quantize(7.0f) & 0xFF);
        assertEquals(0, ChartBuffer.quantize(Float.NaN) & 0xFF);
        assertEquals(128, ChartBuffer.quantize(0.5f) & 0xFF);
        assertEquals(1.0f, ChartBuffer.dequantize(ChartBuffer.quantize(1.0f)), 1e-6f);
    }

    @Test
    void missingPensRecordZero() {
        ChartBuffer chart = new ChartBuffer();
        chart.push(0.8f);
        assertEquals(0.0f, chart.get(1, 0));
        assertEquals(0.0f, chart.get(2, 0));
        assertEquals(0.0f, chart.get(5, 0), "an unknown pen reads 0");
        assertEquals(0.0f, chart.get(0, 3), "past the end reads 0");
    }

    @Test
    void bytesAndLoadRoundTripAfterWrapping() {
        ChartBuffer chart = new ChartBuffer();
        for (int i = 0; i < 300; i++) {
            chart.push(i / 300.0f, 1 - i / 300.0f, 0.5f);
        }
        ChartBuffer copy = new ChartBuffer();
        copy.load(chart.bytes(0), chart.bytes(1), chart.bytes(2));
        assertEquals(chart.size(), copy.size());
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            assertArrayEquals(chart.bytes(pen), copy.bytes(pen));
        }
    }

    @Test
    void loadKeepsTheNewestSamplesOfTheShortestSeries() {
        ChartBuffer chart = new ChartBuffer();
        chart.load(new byte[]{1, 2, 3, 4}, new byte[]{9, 8});
        assertEquals(2, chart.size());
        assertArrayEquals(new byte[]{3, 4}, chart.bytes(0));
        assertArrayEquals(new byte[]{9, 8}, chart.bytes(1));
        assertArrayEquals(new byte[]{0, 0}, chart.bytes(2), "a pen with no series reads 0");
        chart.load();
        assertTrue(chart.isEmpty());
        byte[] tooLong = new byte[ChartBuffer.CAPACITY + 5];
        tooLong[tooLong.length - 1] = 42;
        chart.load(tooLong);
        assertEquals(ChartBuffer.CAPACITY, chart.size());
        assertEquals(42, chart.bytes(0)[ChartBuffer.CAPACITY - 1]);
    }
}
