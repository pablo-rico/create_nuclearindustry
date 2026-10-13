package org.papiricoh.create_nuclearindustry.controlroom.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a client does with a chart recorder's updates: it appends the samples an update brings to its
 * own copy, and ends up with the same trace as the server without being sent all of it again.
 */
class ChartBufferAppendTest {

    @Test
    void appendingTheNewSamplesGivesTheServersTrace() {
        ChartBuffer server = new ChartBuffer();
        ChartBuffer client = new ChartBuffer();
        for (int i = 0; i < 10; i++) {
            server.push(i / 10f, 1 - i / 10f, 0.5f);
        }
        client.load(server.bytes(0), server.bytes(1), server.bytes(2));
        for (int i = 0; i < 4; i++) {
            server.push(i / 4f, 0.25f, 1f);
        }
        client.append(server.newest(0, 4), server.newest(1, 4), server.newest(2, 4));
        assertEquals(server.size(), client.size());
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            assertArrayEquals(server.bytes(pen), client.bytes(pen), "pen " + pen);
        }
    }

    @Test
    void appendingPastCapacityDropsTheOldest() {
        ChartBuffer server = new ChartBuffer();
        ChartBuffer client = new ChartBuffer();
        for (int i = 0; i < ChartBuffer.CAPACITY - 2; i++) {
            server.push((i % 7) / 7f);
        }
        client.load(server.bytes(0), server.bytes(1), server.bytes(2));
        for (int i = 0; i < 5; i++) {
            server.push((i % 3) / 3f);
        }
        client.append(server.newest(0, 5), server.newest(1, 5), server.newest(2, 5));
        assertEquals(ChartBuffer.CAPACITY, client.size());
        assertArrayEquals(server.bytes(0), client.bytes(0));
    }

    @Test
    void newestIsCutToWhatTheChartHolds() {
        ChartBuffer chart = new ChartBuffer();
        chart.push(0.2f);
        chart.push(0.4f);
        assertEquals(2, chart.newest(0, 60).length);
        assertEquals(1, chart.newest(0, 1).length);
        assertEquals(ChartBuffer.quantize(0.4f), chart.newest(0, 1)[0]);
        assertEquals(0, chart.newest(0, 0).length);
    }

    @Test
    void seriesOfDifferentLengthsAppendTheNewestOfTheShortest() {
        ChartBuffer chart = new ChartBuffer();
        chart.append(new byte[]{1, 2, 3}, new byte[]{9}, null);
        assertEquals(1, chart.size());
        assertArrayEquals(new byte[]{3}, chart.bytes(0));
        assertArrayEquals(new byte[]{9}, chart.bytes(1));
        assertArrayEquals(new byte[]{0}, chart.bytes(2));
    }
}
