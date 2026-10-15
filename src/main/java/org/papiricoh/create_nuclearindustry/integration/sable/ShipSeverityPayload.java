package org.papiricoh.create_nuclearindustry.integration.sable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.UUID;

/**
 * Server to client: how contaminated a ship is (0-7), for the dust over its deck. A ship's chunks are
 * sent by Sable's own tracking, which does not carry the ground's synced severity, so ships send theirs
 * here, to the players near them, every few seconds.
 */
public record ShipSeverityPayload(UUID ship, byte severity) implements CustomPacketPayload {
    public static final Type<ShipSeverityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ship_severity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipSeverityPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipSeverityPayload::ship, ByteBufCodecs.BYTE, ShipSeverityPayload::severity, ShipSeverityPayload::new);
    /** How far from a ship its dust is worth telling about, blocks. */
    private static final double RANGE = 128.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                        () -> org.papiricoh.create_nuclearindustry.integration.sable.client.ShipSeverities.set(payload.ship(), payload.severity()))));
    }

    /** Tells the players near the ship. */
    static void send(ServerLevel level, ShipSpace.Ship ship, int severity) {
        Vec3 centre = ship.worldBounds().getCenter();
        ShipSeverityPayload payload = new ShipSeverityPayload(ship.id(), (byte) severity);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centre) <= RANGE * RANGE) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
