package org.papiricoh.create_nuclearindustry.radiation.contamination;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Ground contamination of one chunk, in rad/s at ground level, as it was at {@code lastUpdate}
 * (game time). It decays lazily: nothing ticks, the current value is worked out when read.
 * {@code epoch} and {@code lastEvent} say which global clear and which fallout events this
 * chunk has already seen, so chunks that were unloaded catch up when they load.
 */
public record ChunkContamination(float level, long lastUpdate, int epoch, int lastEvent) {
    /** Lower bound of severities 1 to 7, in rad/s: roughly logarithmic, so dust thickens visibly. */
    private static final float[] SEVERITY_THRESHOLDS = {0.05f, 0.2f, 0.5f, 1, 2, 5, 10};
    public static final int MAX_SEVERITY = SEVERITY_THRESHOLDS.length;

    public static final ChunkContamination EMPTY = new ChunkContamination(0, 0, 0, 0);

    public static final Codec<ChunkContamination> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("level").forGetter(ChunkContamination::level),
            Codec.LONG.fieldOf("updated").forGetter(ChunkContamination::lastUpdate),
            Codec.INT.optionalFieldOf("epoch", 0).forGetter(ChunkContamination::epoch),
            Codec.INT.optionalFieldOf("event", 0).forGetter(ChunkContamination::lastEvent)
    ).apply(i, ChunkContamination::new));

    /** The value at {@code gameTime}, after decaying with the given half-life (0 = no decay). */
    public float levelAt(long gameTime, long halfLifeTicks) {
        return decay(level, gameTime - lastUpdate, halfLifeTicks);
    }

    public static float decay(float level, long elapsed, long halfLifeTicks) {
        if (halfLifeTicks <= 0 || elapsed <= 0 || level <= 0) {
            return level;
        }
        return (float) (level * Math.pow(0.5, (double) elapsed / halfLifeTicks));
    }

    /** 0 when clean, up to {@link #MAX_SEVERITY}: what clients are told about a chunk. */
    public static int severity(float level) {
        int severity = 0;
        while (severity < SEVERITY_THRESHOLDS.length && level >= SEVERITY_THRESHOLDS[severity]) {
            severity++;
        }
        return severity;
    }

    public ChunkContamination with(float newLevel, long gameTime) {
        return new ChunkContamination(newLevel, gameTime, epoch, lastEvent);
    }

    public ChunkContamination withEpoch(int newEpoch) {
        return new ChunkContamination(level, lastUpdate, newEpoch, lastEvent);
    }

    public ChunkContamination withLastEvent(int event) {
        return new ChunkContamination(level, lastUpdate, epoch, event);
    }
}
