package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;

/**
 * Server to client: a non-nuclear warhead, bomblet or dirty bomb went off, for its fireball, smoke, debris, shock ring and
 * sound. The client draws it all from these numbers and the seed, so every player sees the same blast.
 *
 * @param centre         where it went off
 * @param kind           what it was made of
 * @param seed           picks the debris and puffs
 * @param startTick      the server's game time when it went off, so a player told late sees it at its true age
 * @param chargeKg       TNT equivalent of the charge, kg, which sizes the sound and the smoke
 * @param burstHeight    blocks above the ground
 * @param airburst       whether it went off in the air, clear of the ground
 * @param fireballRadius blocks
 * @param craterRadius   the bowl dug at ground level, 0 for an airburst
 * @param waveRadius     how far its shock wave travels
 * @param waveSpeed      blocks per tick the shock wave travels
 * @param yieldKg        the shock wave's TNT equivalent, kg, for how hard it shakes a player
 * @param fireRadius     how far it sets fires (a thermobaric charge's burning cloud), 0 for none
 */
public record ConventionalBlastPayload(Vec3 centre, ConventionalKind kind, long seed, long startTick, float chargeKg,
                                       float burstHeight, boolean airburst, float fireballRadius, float craterRadius,
                                       float waveRadius, float waveSpeed, float yieldKg, float fireRadius)
        implements CustomPacketPayload {
    public static final Type<ConventionalBlastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "conventional_blast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConventionalBlastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.centre.x);
                buf.writeDouble(p.centre.y);
                buf.writeDouble(p.centre.z);
                buf.writeByte(p.kind.id());
                buf.writeLong(p.seed);
                buf.writeLong(p.startTick);
                buf.writeFloat(p.chargeKg);
                buf.writeFloat(p.burstHeight);
                buf.writeBoolean(p.airburst);
                buf.writeFloat(p.fireballRadius);
                buf.writeFloat(p.craterRadius);
                buf.writeFloat(p.waveRadius);
                buf.writeFloat(p.waveSpeed);
                buf.writeFloat(p.yieldKg);
                buf.writeFloat(p.fireRadius);
            },
            buf -> new ConventionalBlastPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    ConventionalKind.byId(buf.readByte()), buf.readLong(), buf.readLong(), buf.readFloat(), buf.readFloat(),
                    buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                        () -> org.papiricoh.create_nuclearindustry.explosive.fx.client.ExplosionFxClient.onConventional(payload))));
    }

    /** Sends it to the players of the level close enough to see or hear it. */
    public void send(ServerLevel level) {
        double range = ExplosiveConfig.broadcastRange();
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(centre) <= range * range) {
                PacketDistributor.sendToPlayer(player, this);
            }
        }
    }
}
