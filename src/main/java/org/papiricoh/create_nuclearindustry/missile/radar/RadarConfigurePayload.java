package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * What an operator changes on a radar's screen: its network's link code, its defended zone or an acknowledged alarm. Checked on
 * the server: the sender must have that radar's screen open and stand close enough, the code is sanitised, and the zone is kept
 * within {@link #MAX_ZONE_OFFSET} blocks of the radar and {@link RadarBlockEntity#MAX_ZONE} blocks across.
 */
public record RadarConfigurePayload(BlockPos pos, int action, String code, int x, int z, int radius) implements CustomPacketPayload {
    public static final int SET_CODE = 0, SET_ZONE = 1, ACKNOWLEDGE = 2;
    /** Furthest a defended zone's centre may lie from its radar. */
    public static final int MAX_ZONE_OFFSET = 100_000;

    public static final Type<RadarConfigurePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "radar_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarConfigurePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RadarConfigurePayload::pos,
            ByteBufCodecs.VAR_INT, RadarConfigurePayload::action,
            ByteBufCodecs.stringUtf8(16), RadarConfigurePayload::code,
            ByteBufCodecs.VAR_INT, RadarConfigurePayload::x,
            ByteBufCodecs.VAR_INT, RadarConfigurePayload::z,
            ByteBufCodecs.VAR_INT, RadarConfigurePayload::radius,
            RadarConfigurePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToServer(TYPE, CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer player) apply(player, p);
                })));
    }

    /** Applies {@code p} for {@code player} if they may; public for tests. Returns whether anything changed. */
    public static boolean apply(ServerPlayer player, RadarConfigurePayload p) {
        if (!(player.containerMenu instanceof RadarMenu menu) || !menu.pos.equals(p.pos) || !menu.stillValid(player)
                || !ServerPayloadGuard.check(player, p.pos) || !(player.level().getBlockEntity(p.pos) instanceof RadarBlockEntity radar)) return false;
        switch (p.action) {
            case SET_CODE -> radar.setCode(p.code);
            case SET_ZONE -> {
                int x = clamp(p.x, p.pos.getX() - MAX_ZONE_OFFSET, p.pos.getX() + MAX_ZONE_OFFSET);
                int z = clamp(p.z, p.pos.getZ() - MAX_ZONE_OFFSET, p.pos.getZ() + MAX_ZONE_OFFSET);
                radar.setZone(x, z, clamp(p.radius, 0, RadarBlockEntity.MAX_ZONE));
            }
            case ACKNOWLEDGE -> radar.acknowledge();
            default -> {
                return false;
            }
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
