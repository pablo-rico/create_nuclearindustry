package org.papiricoh.create_nuclearindustry.accelerator.gui;

import net.minecraft.core.BlockPos;
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
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * Client to server: a button on the accelerator control screen. The server checks the player may use
 * the controller ({@link ServerPayloadGuard}), and the controller clamps every setpoint itself.
 *
 * @param action what was pressed, one of the constants here
 * @param value  the new setpoint for the setpoint actions, 0..1; ignored otherwise
 */
public record AcceleratorControlPayload(BlockPos pos, int action, double value) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int ENERGY = 2;
    public static final int CURRENT = 3;

    public static final Type<AcceleratorControlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "accelerator_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AcceleratorControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AcceleratorControlPayload::pos,
            ByteBufCodecs.VAR_INT, AcceleratorControlPayload::action,
            ByteBufCodecs.DOUBLE, AcceleratorControlPayload::value,
            AcceleratorControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToServer(TYPE, STREAM_CODEC, AcceleratorControlPayload::handle));
    }

    private static void handle(AcceleratorControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(context.player(), payload));
    }

    /** Applies the action for {@code player}, who must pass {@link ServerPayloadGuard}. Returns whether it was applied. */
    public static boolean apply(Player player, AcceleratorControlPayload payload) {
        BlockPos pos = payload.pos();
        if (!ServerPayloadGuard.check(player, pos)
                || !(player.level().getBlockEntity(pos) instanceof AcceleratorControllerBlockEntity controller)) {
            return false;
        }
        double value = payload.value();
        if (!Double.isFinite(value)) {
            return false;
        }
        controller.setOperator(player);
        switch (payload.action()) {
            case START -> controller.requestStart();
            case STOP -> controller.requestStop();
            case ENERGY -> controller.setEnergySetpoint(value);
            case CURRENT -> controller.setCurrentSetpoint(value);
            default -> {
                return false;
            }
        }
        return true;
    }
}
