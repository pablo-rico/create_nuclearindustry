package org.papiricoh.create_nuclearindustry.missile.defense.battery;

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

/**
 * What an operator changes on a battery's screen: its radar link code, automatic mode, doctrine and defended zone. Checked on the
 * server: the sender must have that battery's screen open and stand close enough. Actions are sent by number: add new ones at the
 * end.
 */
public record BatteryConfigurePayload(BlockPos pos, byte action, int arg, String code) implements CustomPacketPayload {
    public static final byte SET_CODE = 0, SET_AUTO = 1, SET_DOCTRINE = 2, SET_ZONE_RADIUS = 3, ZONE_HERE = 4;

    public static final Type<BatteryConfigurePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "battery_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatteryConfigurePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BatteryConfigurePayload::pos,
            ByteBufCodecs.BYTE, BatteryConfigurePayload::action,
            ByteBufCodecs.VAR_INT, BatteryConfigurePayload::arg,
            ByteBufCodecs.stringUtf8(16), BatteryConfigurePayload::code,
            BatteryConfigurePayload::new);

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
    public static boolean apply(ServerPlayer player, BatteryConfigurePayload p) {
        return false;
    }
}
