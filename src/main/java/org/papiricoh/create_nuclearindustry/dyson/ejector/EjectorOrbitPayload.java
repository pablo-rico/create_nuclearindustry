package org.papiricoh.create_nuclearindustry.dyson.ejector;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** Picks the orbit an ejector launches into, from its screen. */
public record EjectorOrbitPayload(BlockPos pos, int orbit) implements CustomPacketPayload {
    public static final Type<EjectorOrbitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ejector_orbit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EjectorOrbitPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EjectorOrbitPayload::pos, ByteBufCodecs.VAR_INT, EjectorOrbitPayload::orbit, EjectorOrbitPayload::new);

    /** Server: sets it if the player may use the ejector and the orbit exists. */
    public static void apply(ServerPlayer player, EjectorOrbitPayload payload) {
        if (!OrbitShape.validIndex(payload.orbit) || !ServerPayloadGuard.check(player, payload.pos)) return;
        if (player.level().getBlockEntity(payload.pos) instanceof EmRailEjectorBlockEntity ejector) ejector.setOrbit(payload.orbit);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
