package org.papiricoh.create_nuclearindustry.radiation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoseReadingTest {

    @Test
    void roundTripsThroughTheCodec() {
        DoseReading reading = new DoseReading(123, 456, 78901, (byte) 2, (byte) 76);
        ByteBuf buf = Unpooled.buffer();
        DoseReading.STREAM_CODEC.encode(buf, reading);
        assertTrue(buf.readableBytes() <= 9, "a reading should take a handful of bytes, took " + buf.readableBytes());
        assertEquals(reading, DoseReading.STREAM_CODEC.decode(buf));
    }

    @Test
    void roundsToWhatTheHudShows() {
        assertEquals(DoseReading.of(0.501f, 10.4f, 20.3f), DoseReading.of(0.499f, 10.2f, 20.9f));
        assertEquals(50, DoseReading.of(0.5f, 0, 0).rateCenti());
    }
}
