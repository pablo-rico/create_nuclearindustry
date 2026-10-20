package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of what a radar's open screen shows, sent once a second: up to {@link #PAGE} tracks, with a track's whole plotted
 * history the first time that screen hears of it and only its newest point after. A screen swaps in the new picture once the
 * last page arrives.
 */
public record RadarPayload(int menu, BlockPos pos, long sample, int status, boolean first, boolean last, float gravity, int sensors,
                           List<RadarTrack.Snapshot> tracks) implements CustomPacketPayload {
    public static final int PAGE = 64;
    public static final Type<RadarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "radar_tracks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarPayload> CODEC = new StreamCodec<>() {
        @Override
        public RadarPayload decode(RegistryFriendlyByteBuf b) {
            int menu = b.readVarInt();
            BlockPos pos = b.readBlockPos();
            long sample = b.readLong();
            int status = b.readVarInt();
            boolean first = b.readBoolean(), last = b.readBoolean();
            float gravity = b.readFloat();
            int sensors = b.readVarInt();
            int size = b.readVarInt();
            if (size < 0 || size > PAGE) throw new IllegalArgumentException("Radar page too large");
            List<RadarTrack.Snapshot> tracks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) tracks.add(RadarTrack.Snapshot.read(b));
            return new RadarPayload(menu, pos, sample, status, first, last, gravity, sensors, List.copyOf(tracks));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf b, RadarPayload p) {
            b.writeVarInt(p.menu);
            b.writeBlockPos(p.pos);
            b.writeLong(p.sample);
            b.writeVarInt(p.status);
            b.writeBoolean(p.first);
            b.writeBoolean(p.last);
            b.writeFloat(p.gravity);
            b.writeVarInt(p.sensors);
            b.writeVarInt(p.tracks.size());
            for (RadarTrack.Snapshot track : p.tracks) track.write(b);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("2").playToClient(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof RadarMenu m && m.containerId == p.menu && m.pos.equals(p.pos)) m.accept(p);
        })));
    }
}
