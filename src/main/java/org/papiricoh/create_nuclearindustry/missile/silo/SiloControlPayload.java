package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * A player at the launch controller's screen: set the target ({@code arg} bit 0 for an airburst), arm, disarm, or pick the
 * section at column index {@code arg} to load. Action ids are append only.
 */
public record SiloControlPayload(BlockPos pos, int action, BlockPos target, int arg) implements CustomPacketPayload {
    public static final int CONFIGURE = 0, ARM = 1, DISARM = 2, SELECT = 3;
    public static final Type<SiloControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("create_nuclearindustry", "silo_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiloControlPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SiloControlPayload::pos, ByteBufCodecs.VAR_INT, SiloControlPayload::action,
            BlockPos.STREAM_CODEC, SiloControlPayload::target, ByteBufCodecs.VAR_INT, SiloControlPayload::arg, SiloControlPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player.containerMenu instanceof SiloMenu menu) || !menu.pos.equals(p.pos) || !menu.stillValid(player)
                    || !ServerPayloadGuard.check(player, p.pos) || !(player.level().getBlockEntity(p.pos) instanceof SiloControllerEntity c)) return;
            switch (p.action) {
                case CONFIGURE -> c.configure(player, p.target, (p.arg & 1) != 0);
                case ARM -> c.arm(player);
                case DISARM -> { c.disarm(); c.sendData(); }
                case SELECT -> menu.select(p.arg);
                default -> { }
            }
        })));
    }
}
