package org.papiricoh.create_nuclearindustry.dyson.swarm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The swarm as a player sees it: each orbit's shape, its sails and how many of those wear out within a day, and what the swarm
 * beams down (the pool, the sun's share, how many receivers split it and what they took). The client moves the sails itself
 * ({@link SwarmGeometry} is deterministic), so this goes out only when something changes and every few seconds as a refresh. Sent
 * to every player wherever they are: only the Overworld's sky draws it, but terminals in any level read it.
 */
public record SwarmSyncPayload(OrbitShape[] shapes, long[] sails, long[] expiringToday, float pool, float sunFactor,
                               int receivers, int delivered) implements CustomPacketPayload {
    public static final Type<SwarmSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "dyson_swarm_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwarmSyncPayload> CODEC = StreamCodec.of(SwarmSyncPayload::write, SwarmSyncPayload::read);
    /** A world with no swarm yet. */
    public static final SwarmSyncPayload EMPTY = empty();

    public SwarmSyncPayload {
        if (shapes.length != OrbitShape.COUNT || sails.length != OrbitShape.COUNT || expiringToday.length != OrbitShape.COUNT)
            throw new IllegalArgumentException("A swarm has " + OrbitShape.COUNT + " orbits");
    }

    private static SwarmSyncPayload empty() {
        OrbitShape[] shapes = new OrbitShape[OrbitShape.COUNT];
        for (int i = 0; i < shapes.length; i++) shapes[i] = OrbitShape.defaults(i);
        return new SwarmSyncPayload(shapes, new long[OrbitShape.COUNT], new long[OrbitShape.COUNT], 0, 0, 0, 0);
    }

    public long totalSails() {
        long total = 0;
        for (long s : sails) total += s;
        return total;
    }

    private static void write(RegistryFriendlyByteBuf b, SwarmSyncPayload p) {
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            OrbitShape shape = p.shapes[i];
            b.writeBoolean(shape.enabled());
            b.writeFloat(shape.radius());
            b.writeFloat(shape.inclination());
            b.writeFloat(shape.node());
            b.writeVarLong(p.sails[i]);
            b.writeVarLong(p.expiringToday[i]);
        }
        b.writeFloat(p.pool);
        b.writeFloat(p.sunFactor);
        b.writeVarInt(p.receivers);
        b.writeVarInt(p.delivered);
    }

    private static SwarmSyncPayload read(RegistryFriendlyByteBuf b) {
        OrbitShape[] shapes = new OrbitShape[OrbitShape.COUNT];
        long[] sails = new long[OrbitShape.COUNT], expiring = new long[OrbitShape.COUNT];
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            shapes[i] = new OrbitShape(b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat());
            sails[i] = Math.max(0, b.readVarLong());
            expiring[i] = Math.max(0, b.readVarLong());
        }
        return new SwarmSyncPayload(shapes, sails, expiring, b.readFloat(), b.readFloat(), Math.max(0, b.readVarInt()), Math.max(0, b.readVarInt()));
    }

    /** Tells every player on {@code server}. */
    public static void send(MinecraftServer server, SwarmSyncPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, payload);
    }

    public static void send(ServerPlayer player, SwarmSyncPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
