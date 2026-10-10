package org.papiricoh.create_nuclearindustry.controlroom;

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

/** Client to server: the new setup of one part of a {@link ConfigurableDevice}, from its screen. */
public record ConfigureDevicePayload(BlockPos pos, int part, CompoundTag config) implements CustomPacketPayload {
    public static final Type<ConfigureDevicePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "configure_device"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureDevicePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureDevicePayload::pos,
            ByteBufCodecs.VAR_INT, ConfigureDevicePayload::part,
            ByteBufCodecs.COMPOUND_TAG, ConfigureDevicePayload::config,
            ConfigureDevicePayload::new);
    private static final double REACH = 8.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToServer(TYPE, STREAM_CODEC, ConfigureDevicePayload::handle));
    }

    private static void handle(ConfigureDevicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(context.player(), payload));
    }

    /**
     * Applies the setup for {@code player}, who must be within reach of a loaded device. The device
     * sanitises what it takes. Returns whether it was applied.
     */
    public static boolean apply(Player player, ConfigureDevicePayload payload) {
        if (player.isSpectator() || player.distanceToSqr(payload.pos().getCenter()) > REACH * REACH
                || !player.level().isLoaded(payload.pos())) {
            return false;
        }
        return player.level().getBlockEntity(payload.pos()) instanceof ConfigurableDevice device
                && device.configure(payload.part(), payload.config());
    }
}
