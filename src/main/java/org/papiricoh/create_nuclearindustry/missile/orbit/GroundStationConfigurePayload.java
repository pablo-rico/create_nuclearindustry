package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** Server validated station code edit. */
public record GroundStationConfigurePayload(BlockPos pos, String code) implements CustomPacketPayload {
    public static final Type<GroundStationConfigurePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ground_station_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GroundStationConfigurePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GroundStationConfigurePayload::pos,
            ByteBufCodecs.stringUtf8(16), GroundStationConfigurePayload::code, GroundStationConfigurePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static boolean apply(ServerPlayer player, GroundStationConfigurePayload payload) {
        if (!(player.containerMenu instanceof GroundStationMenu menu) || !menu.pos.equals(payload.pos)
                || !menu.stillValid(player) || !ServerPayloadGuard.check(player, payload.pos)
                || !(player.level().getBlockEntity(payload.pos) instanceof GroundStationBlockEntity station)) return false;
        station.setCode(payload.code);
        return true;
    }
}
