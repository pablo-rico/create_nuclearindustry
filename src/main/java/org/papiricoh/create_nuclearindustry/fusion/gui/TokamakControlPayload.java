package org.papiricoh.create_nuclearindustry.fusion.gui;

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
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

/**
 * Client to server: a button on the tokamak control screen. The server checks the player may use the
 * tokamak controller ({@link ServerPayloadGuard}), and the controller clamps every setpoint itself.
 *
 * @param action what was pressed, one of the constants here
 * @param value  the new setpoint for the setpoint actions; ignored otherwise
 */
public record TokamakControlPayload(BlockPos pos, int action, double value) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int SHUTDOWN = 1;
    public static final int FUEL_RATE = 2;
    public static final int DT_RATIO = 3;
    public static final int HEATING_LEVEL = 4;
    public static final int HEATING_TOGGLE = 5;

    public static final Type<TokamakControlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "tokamak_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TokamakControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TokamakControlPayload::pos,
            ByteBufCodecs.VAR_INT, TokamakControlPayload::action,
            ByteBufCodecs.DOUBLE, TokamakControlPayload::value,
            TokamakControlPayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToServer(TYPE, STREAM_CODEC, TokamakControlPayload::handle));
    }

    private static void handle(TokamakControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(context.player(), payload));
    }

    /**
     * Applies the action for {@code player}, who must pass {@link ServerPayloadGuard} for a loaded
     * tokamak controller. Returns whether it was applied.
     */
    public static boolean apply(Player player, TokamakControlPayload payload) {
        BlockPos pos = payload.pos();
        if (!ServerPayloadGuard.check(player, pos)
                || !(player.level().getBlockEntity(pos) instanceof FusionReactorBlockEntity tokamak)) {
            return false;
        }
        double value = payload.value();
        if (!Double.isFinite(value)) {
            return false;
        }
        tokamak.setOperator(player);
        switch (payload.action()) {
            case START -> tokamak.requestStart();
            case SHUTDOWN -> tokamak.requestShutdown();
            case FUEL_RATE -> tokamak.setFuelRate(value);
            case DT_RATIO -> tokamak.setDtRatio(value);
            case HEATING_LEVEL -> tokamak.setHeating(tokamak.isHeatingEnabled(), value);
            case HEATING_TOGGLE -> tokamak.setHeating(!tokamak.isHeatingEnabled(), tokamak.getHeatingLevel());
            default -> {
                return false;
            }
        }
        return true;
    }
}
