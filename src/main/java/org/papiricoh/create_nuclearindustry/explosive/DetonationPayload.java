package org.papiricoh.create_nuclearindustry.explosive;

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
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;

/**
 * Server to client: a nuclear detonation went off, for its flash, fireball, mushroom cloud and shock wave.
 * Sent to the players of the level within {@code explosive.visualRange} blocks. The client draws it all
 * from these few numbers and the seed, so every player sees the same cloud.
 *
 * @param centre        where it went off
 * @param scale         the warhead's destruction radius, which sizes the cloud
 * @param fireball      the fireball's radius
 * @param burstHeight   blocks above the ground
 * @param airburst      whether it went off above its fireball
 * @param yieldKg       TNT equivalent, for how hard the shock wave shakes a player
 * @param waveSpeed     blocks per tick the shock wave travels
 * @param waveRadius    how far it travels
 * @param thermalRadius how far the flash burns
 * @param seed          picks the cloud's puffs
 * @param style         how it looks: a fission or thermonuclear fireball and cloud, or a dirty bomb's small
 *                      conventional flash and no cloud
 */
public record DetonationPayload(Vec3 centre, float scale, float fireball, float burstHeight, boolean airburst, float yieldKg,
                                float waveSpeed, float waveRadius, float thermalRadius, long seed, DetonationStyle style)
        implements CustomPacketPayload {
    public static final Type<DetonationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "detonation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetonationPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.centre.x);
                buf.writeDouble(p.centre.y);
                buf.writeDouble(p.centre.z);
                buf.writeFloat(p.scale);
                buf.writeFloat(p.fireball);
                buf.writeFloat(p.burstHeight);
                buf.writeBoolean(p.airburst);
                buf.writeFloat(p.yieldKg);
                buf.writeFloat(p.waveSpeed);
                buf.writeFloat(p.waveRadius);
                buf.writeFloat(p.thermalRadius);
                buf.writeLong(p.seed);
                buf.writeByte(p.style.id());
            },
            buf -> new DetonationPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readLong(),
                    DetonationStyle.byId(buf.readByte())));

    /** A dirty bomb: a small conventional flash and no cloud. */
    public boolean dirty() {
        return style == DetonationStyle.DIRTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("2")
                .playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                        () -> org.papiricoh.create_nuclearindustry.explosive.client.NuclearExplosionEffects.add(payload))));
    }

    /** The payload of a nuclear detonation of {@code profile} at {@code centre}. */
    public static DetonationPayload of(Vec3 centre, DetonationProfile profile, double waveRadius, long seed, DetonationStyle style) {
        return new DetonationPayload(centre, profile.horizontalRadius(), (float) profile.fireballRadius(), (float) profile.burstHeight(),
                profile.airburst(), (float) profile.yieldKg(), (float) CivilDefenseConfig.blastWaveSpeed, (float) waveRadius,
                (float) profile.thermalRadius(), seed, style);
    }

    /** A dirty bomb's conventional blast of {@code power} at {@code centre}. */
    public static DetonationPayload dirty(Vec3 centre, float power, long seed) {
        return new DetonationPayload(centre, 0, Math.max(2, power * 0.4f), 0, false,
                (float) BlastWaveModel.yield(power, CivilDefenseConfig.blastWaveYieldPerPower), (float) CivilDefenseConfig.blastWaveSpeed,
                0, 0, seed, DetonationStyle.DIRTY);
    }

    /** Sends it to the players of the level close enough to see it. */
    public void send(ServerLevel level) {
        double range = ExplosiveConfig.visualRange;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(centre) <= range * range) {
                PacketDistributor.sendToPlayer(player, this);
            }
        }
    }
}
