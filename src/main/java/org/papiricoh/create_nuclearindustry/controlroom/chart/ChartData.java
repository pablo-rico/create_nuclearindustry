package org.papiricoh.create_nuclearindustry.controlroom.chart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A torn-off chart: what the recorder watched, each pen's reading and its trace (one byte per
 * sample, see {@link ChartBuffer}), the paper speed and the game time of the last sample.
 *
 * @param kind           {@link TargetKind} name of what the recorder was linked to
 * @param pens           {@link GaugeReading} name of each pen, empty for a pen that was off
 * @param series         each pen's samples, oldest first
 * @param ticksPerSample the paper speed when the chart was torn off
 * @param endTime        game time of the newest sample
 */
public record ChartData(String kind, List<String> pens, List<byte[]> series, int ticksPerSample, long endTime) {
    private static final Codec<byte[]> BYTES = Codec.BYTE_BUFFER.xmap(buffer -> {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }, ByteBuffer::wrap);
    public static final Codec<ChartData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kind").forGetter(ChartData::kind),
            Codec.STRING.listOf().fieldOf("pens").forGetter(ChartData::pens),
            BYTES.listOf().fieldOf("series").forGetter(ChartData::series),
            Codec.INT.fieldOf("ticks_per_sample").forGetter(ChartData::ticksPerSample),
            Codec.LONG.fieldOf("end_time").forGetter(ChartData::endTime)
    ).apply(instance, ChartData::new));
    public static final StreamCodec<ByteBuf, ChartData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChartData::kind,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(ChartBuffer.MAX_PENS)), ChartData::pens,
            ByteBufCodecs.BYTE_ARRAY.apply(ByteBufCodecs.list(ChartBuffer.MAX_PENS)), ChartData::series,
            ByteBufCodecs.VAR_INT, ChartData::ticksPerSample,
            ByteBufCodecs.VAR_LONG, ChartData::endTime,
            ChartData::new);

    public ChartData {
        pens = List.copyOf(pens);
        series = List.copyOf(series);
    }

    /** The chart as it stands, for {@code pens} (null for a pen that is off). */
    public static ChartData of(TargetKind kind, GaugeReading[] pens, ChartBuffer chart, int ticksPerSample, long endTime) {
        List<String> names = new ArrayList<>();
        List<byte[]> series = new ArrayList<>();
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            names.add(pen < pens.length && pens[pen] != null ? pens[pen].name() : "");
            series.add(chart.bytes(pen));
        }
        return new ChartData(kind.name(), names, series, ticksPerSample, endTime);
    }

    /** The trace, ready to read sample by sample. */
    public ChartBuffer buffer() {
        ChartBuffer chart = new ChartBuffer();
        chart.load(series.toArray(byte[][]::new));
        return chart;
    }

    /** Pen {@code index}'s reading, or null when it was off. */
    @Nullable
    public GaugeReading pen(int index) {
        return index >= 0 && index < pens.size() ? GaugeReading.parse(pens.get(index)) : null;
    }

    public int samples() {
        return buffer().size();
    }

    /** Ticks the chart covers. */
    public long spanTicks() {
        return (long) samples() * Math.max(1, ticksPerSample);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChartData data) || !kind.equals(data.kind) || !pens.equals(data.pens)
                || ticksPerSample != data.ticksPerSample || endTime != data.endTime || series.size() != data.series.size()) {
            return false;
        }
        for (int i = 0; i < series.size(); i++) {
            if (!Arrays.equals(series.get(i), data.series.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 31 * kind.hashCode() + pens.hashCode();
        for (byte[] values : series) {
            hash = 31 * hash + Arrays.hashCode(values);
        }
        return 31 * (31 * hash + ticksPerSample) + Long.hashCode(endTime);
    }
}
