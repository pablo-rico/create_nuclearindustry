package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

public record SiloControlPayload(BlockPos pos, int action, BlockPos target) implements CustomPacketPayload {
    public static final Type<SiloControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("create_nuclearindustry", "silo_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiloControlPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SiloControlPayload::pos, ByteBufCodecs.VAR_INT, SiloControlPayload::action,
            BlockPos.STREAM_CODEC, SiloControlPayload::target, SiloControlPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player.containerMenu instanceof SiloMenu menu) || !menu.pos.equals(p.pos) || !menu.stillValid(player)
                    || !ServerPayloadGuard.check(player, p.pos) || !(player.level().getBlockEntity(p.pos) instanceof SiloControllerEntity c)) return;
            switch (p.action) {
                case 0 -> c.configure(player, p.target, false);
                case 1 -> c.arm(player);
                case 2 -> { c.disarm(); c.sendData(); }
                default -> { }
            }
        })));
    }
}
