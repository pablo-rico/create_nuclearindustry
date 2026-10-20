package org.papiricoh.create_nuclearindustry.missile.holo;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Server → players near a command table: a nuclear detonation at ({@code x, z}) reaching {@code radius}, for the tables to flash. */
public record HoloDetonationPayload(double x, double y, double z, int radius) implements CustomPacketPayload {
    public static final Type<HoloDetonationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "holo_detonation"));
    public static final StreamCodec<ByteBuf, HoloDetonationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, HoloDetonationPayload::x,
            ByteBufCodecs.DOUBLE, HoloDetonationPayload::y,
            ByteBufCodecs.DOUBLE, HoloDetonationPayload::z,
            ByteBufCodecs.VAR_INT, HoloDetonationPayload::radius,
            HoloDetonationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
