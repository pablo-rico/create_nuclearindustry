package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;

/** The flights a player is in range of, sampled at {@code gameTime}, with the gravity they fall at to fly them on between packets. */
public record FlightFxStatePayload(long gameTime, float gravity, List<FlightFxState> states) implements CustomPacketPayload {
    public static final int MAX_FLIGHTS = 64;
    public static final Type<FlightFxStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "flight_fx_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightFxStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, FlightFxStatePayload::gameTime,
            ByteBufCodecs.FLOAT, FlightFxStatePayload::gravity,
            FlightFxState.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FLIGHTS)), FlightFxStatePayload::states,
            FlightFxStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
