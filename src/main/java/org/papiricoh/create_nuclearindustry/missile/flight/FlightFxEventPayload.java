package org.papiricoh.create_nuclearindustry.missile.flight;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.UUID;

/** One {@link FlightFxEvent} of flight {@code id}, sent the moment it happens with where and how fast. */
public record FlightFxEventPayload(UUID id, byte event, byte stage, byte flags, double x, double y, double z, float vx, float vy,
                                   float vz) implements CustomPacketPayload {
    public static final Type<FlightFxEventPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "flight_fx_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightFxEventPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> write(buf, p), FlightFxEventPayload::read);

    private static void write(ByteBuf buf, FlightFxEventPayload p) {
        UUIDUtil.STREAM_CODEC.encode(buf, p.id);
        buf.writeByte(p.event);
        buf.writeByte(p.stage);
        buf.writeByte(p.flags);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.vx);
        buf.writeFloat(p.vy);
        buf.writeFloat(p.vz);
    }

    private static FlightFxEventPayload read(ByteBuf buf) {
        return new FlightFxEventPayload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readByte(), buf.readByte(), buf.readByte(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public FlightFxEvent fxEvent() {
        return FlightFxEvent.byId(event);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
