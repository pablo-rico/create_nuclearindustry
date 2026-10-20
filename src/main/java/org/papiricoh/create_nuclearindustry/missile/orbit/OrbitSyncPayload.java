package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.ArrayList;
import java.util.List;

/**
 * Every satellite over the player's level, orbits only: the client flies them on its own ({@link Orbit} is deterministic), so this
 * is sent only when one is added or removed, on joining or changing level, and every {@link OrbitConfig#syncSeconds} as a refresh.
 * Bounded by {@link #MAX}.
 */
public record OrbitSyncPayload(List<Satellite> satellites) implements CustomPacketPayload {
    public static final int MAX = 128;
    public static final Type<OrbitSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "orbit_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrbitSyncPayload> CODEC = StreamCodec.of(OrbitSyncPayload::write, OrbitSyncPayload::read);

    public OrbitSyncPayload {
        satellites = List.copyOf(satellites.size() > MAX ? satellites.subList(0, MAX) : satellites);
    }

    private static void write(RegistryFriendlyByteBuf b, OrbitSyncPayload p) {
        b.writeVarInt(p.satellites.size());
        for (Satellite s : p.satellites) s.write(b);
    }

    private static OrbitSyncPayload read(RegistryFriendlyByteBuf b) {
        int count = b.readVarInt();
        if (count < 0 || count > MAX) throw new IllegalArgumentException("Too many satellites");
        List<Satellite> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(Satellite.read(b));
        return new OrbitSyncPayload(list);
    }

    /** Tells every player in {@code level} its satellites. */
    public static void send(ServerLevel level, List<Satellite> satellites) {
        OrbitSyncPayload payload = new OrbitSyncPayload(satellites);
        for (ServerPlayer player : level.players()) PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
