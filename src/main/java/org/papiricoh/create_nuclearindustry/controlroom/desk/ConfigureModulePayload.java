package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** Client to server: the new setup of one control module, from its configuration screen. */
public record ConfigureModulePayload(BlockPos pos, int slot, CompoundTag config) implements CustomPacketPayload {
    public static final Type<ConfigureModulePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "configure_module"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureModulePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureModulePayload::pos,
            ByteBufCodecs.VAR_INT, ConfigureModulePayload::slot,
            ByteBufCodecs.COMPOUND_TAG, ConfigureModulePayload::config,
            ConfigureModulePayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToServer(TYPE, STREAM_CODEC, ConfigureModulePayload::handle));
    }

    private static void handle(ConfigureModulePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(context.player(), payload));
    }

    /**
     * Applies the setup for {@code player}, who must pass {@link ServerPayloadGuard} for a desk with a
     * module in that slot. The module sanitises what it takes. Returns whether it was applied.
     */
    public static boolean apply(Player player, ConfigureModulePayload payload) {
        if (!ServerPayloadGuard.check(player, payload.pos())) {
            return false;
        }
        return player.level().getBlockEntity(payload.pos()) instanceof ControlDeskBlockEntity desk
                && desk.configure(payload.slot(), payload.config());
    }
}
