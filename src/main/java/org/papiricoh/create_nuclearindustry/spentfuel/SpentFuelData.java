package org.papiricoh.create_nuclearindustry.spentfuel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The cooling history of a spent assembly, kept in its {@code spent_fuel} component.
 * The day is whole, so assemblies discharged on the same day stack.
 *
 * @param dischargeDay game time / 24000 when it left the core, or {@link #UNDATED}
 * @param grade        how far it has cooled, last time someone looked
 */
public record SpentFuelData(long dischargeDay, CoolingGrade grade) {
    /** No known discharge day: an example in a recipe viewer. It is dated the first time it is refreshed. */
    public static final long UNDATED = Long.MIN_VALUE;

    public static final Codec<CoolingGrade> GRADE_CODEC = Codec.STRING.comapFlatMap(id -> {
        CoolingGrade grade = CoolingGrade.fromId(id);
        return grade != null ? DataResult.success(grade) : DataResult.error(() -> "Unknown cooling grade " + id);
    }, CoolingGrade::id);
    public static final StreamCodec<ByteBuf, CoolingGrade> GRADE_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> CoolingGrade.values()[Math.floorMod(i, CoolingGrade.values().length)], CoolingGrade::ordinal);

    public static final Codec<SpentFuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("discharge_day").forGetter(SpentFuelData::dischargeDay),
            GRADE_CODEC.fieldOf("grade").forGetter(SpentFuelData::grade)
    ).apply(instance, SpentFuelData::new));
    public static final StreamCodec<ByteBuf, SpentFuelData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SpentFuelData::dischargeDay,
            GRADE_STREAM_CODEC, SpentFuelData::grade,
            SpentFuelData::new);

    public static SpentFuelData undated(CoolingGrade grade) {
        return new SpentFuelData(UNDATED, grade);
    }

    public boolean dated() {
        return dischargeDay != UNDATED;
    }

    public SpentFuelData withGrade(CoolingGrade grade) {
        return new SpentFuelData(dischargeDay, grade);
    }
}
