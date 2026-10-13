package org.papiricoh.create_nuclearindustry.radiation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.papiricoh.create_nuclearindustry.Config;

/**
 * What the dose HUD shows, rounded so that it only changes (and is only sent) when the
 * player could see the difference: rate to 0.01 rad/s, doses to whole rad. The stage and the
 * bar are worked out here, on the server, so the client needs none of the server's thresholds.
 */
public record DoseReading(int rateCenti, int dose, int total, byte stage, byte bar) {
    public static final DoseReading NONE = new DoseReading(0, 0, 0, (byte) 0, (byte) 0);

    public static final StreamCodec<ByteBuf, DoseReading> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DoseReading::rateCenti,
            ByteBufCodecs.VAR_INT, DoseReading::dose,
            ByteBufCodecs.VAR_INT, DoseReading::total,
            ByteBufCodecs.BYTE, DoseReading::stage,
            ByteBufCodecs.BYTE, DoseReading::bar,
            DoseReading::new);

    public static DoseReading of(float rate, float dose, float total) {
        int stage = 0;
        if (dose >= Config.radiationSicknessThreshold) {
            stage = 3;
        } else if (dose >= Config.radiationWeaknessThreshold) {
            stage = 2;
        } else if (dose >= Config.radiationNauseaThreshold) {
            stage = 1;
        }
        double sickness = Config.radiationSicknessThreshold;
        int bar = sickness <= 0 ? 100 : (int) Math.min(100, Math.floor(dose / sickness * 100));
        return new DoseReading(Math.round(rate * 100), (int) Math.floor(dose), (int) Math.floor(total), (byte) stage, (byte) bar);
    }

    /** The HUD shows only while there is something to warn about; the lifetime total alone does not count. */
    public boolean visible() {
        return rateCenti > 0 || dose > 0;
    }

    public float rate() {
        return rateCenti / 100f;
    }
}
