package org.papiricoh.create_nuclearindustry.isotope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The decay clock of a radioisotope item, kept in its {@code isotope} component. The activity is
 * worked out from {@code createdTick} whenever the item is looked at, like spent fuel's cooling,
 * so nothing ticks in a chest. {@code createdTick} is rounded to a quantum of the half-life, so
 * items made close together stack.
 *
 * @param createdTick     game time the item had {@code initialActivity}, or {@link #UNDATED}
 * @param initialActivity relative activity at {@code createdTick} (1 = fresh, nominal strength)
 * @param percent         activity in percent when last refreshed, for tooltips and the client
 */
public record IsotopeData(long createdTick, float initialActivity, int percent) {
    /** Not dated yet (made by a recipe, or shown by a recipe viewer): counts as fresh, dated on its first refresh. */
    public static final long UNDATED = Long.MIN_VALUE;
    public static final IsotopeData FRESH = new IsotopeData(UNDATED, 1.0f, 100);

    public static final Codec<IsotopeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("created").forGetter(IsotopeData::createdTick),
            Codec.FLOAT.fieldOf("initial").forGetter(IsotopeData::initialActivity),
            Codec.INT.optionalFieldOf("percent", 100).forGetter(IsotopeData::percent)
    ).apply(instance, IsotopeData::new));
    public static final StreamCodec<ByteBuf, IsotopeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, IsotopeData::createdTick,
            ByteBufCodecs.FLOAT, IsotopeData::initialActivity,
            ByteBufCodecs.VAR_INT, IsotopeData::percent,
            IsotopeData::new);

    public boolean dated() {
        return createdTick != UNDATED;
    }

    public IsotopeData withPercent(int percent) {
        return new IsotopeData(createdTick, initialActivity, percent);
    }
}
