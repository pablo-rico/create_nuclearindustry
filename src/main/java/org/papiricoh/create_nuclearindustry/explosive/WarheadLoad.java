package org.papiricoh.create_nuclearindustry.explosive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The fissile load a warhead carries, in the units of {@link WarheadStats}: what a nuclear cannon shell
 * goes off with. Each weapons-grade core is worth {@link #UNITS_PER_CORE}, the metal cast into it, so six
 * make a full warhead.
 *
 * @param units      warhead units
 * @param enrichment their unit-weighted enrichment, %
 */
public record WarheadLoad(int units, float enrichment) {
    public static final int UNITS_PER_CORE = 8;
    public static final int MAX_CORES = WarheadStats.MAX_URANIUM_UNITS / UNITS_PER_CORE;
    /** A shell from before loads were counted: a full warhead, as such shells always went off. */
    public static final WarheadLoad FULL = new WarheadLoad(WarheadStats.MAX_URANIUM_UNITS, WarheadStats.REQUIRED_ENRICHMENT);

    public static final Codec<WarheadLoad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("units").forGetter(WarheadLoad::units),
            Codec.FLOAT.fieldOf("enrichment").forGetter(WarheadLoad::enrichment)).apply(instance, WarheadLoad::new));
    public static final StreamCodec<ByteBuf, WarheadLoad> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarheadLoad::units,
            ByteBufCodecs.FLOAT, WarheadLoad::enrichment,
            WarheadLoad::new);

    /** A load of {@code cores} weapons-grade cores. */
    public static WarheadLoad ofCores(int cores) {
        return new WarheadLoad(Math.max(0, Math.min(MAX_CORES, cores)) * UNITS_PER_CORE, WarheadStats.REQUIRED_ENRICHMENT);
    }

    /** Cores it holds, rounded up: shells from when a core was worth 12 units keep their units. */
    public int cores() {
        return (units + UNITS_PER_CORE - 1) / UNITS_PER_CORE;
    }

    public void write(CompoundTag tag) {
        tag.putInt("WarheadUnits", units);
        tag.putFloat("WarheadEnrichment", enrichment);
    }

    /** The load saved in {@code tag}, or {@link #FULL} for one saved before loads were counted. */
    public static WarheadLoad read(CompoundTag tag) {
        return tag.contains("WarheadUnits") ? new WarheadLoad(tag.getInt("WarheadUnits"), tag.getFloat("WarheadEnrichment")) : FULL;
    }
}
