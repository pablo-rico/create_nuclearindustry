package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.function.Consumer;

/** Server → a player with the launch controller at {@code pos} open: the cutaway its screen draws. */
public record SiloDiagramPayload(BlockPos pos, LaunchDiagram diagram) implements CustomPacketPayload {
    public static final Type<SiloDiagramPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "silo_diagram"));
    public static final StreamCodec<FriendlyByteBuf, SiloDiagramPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.pos); p.diagram.write(buf); },
            buf -> new SiloDiagramPayload(buf.readBlockPos(), LaunchDiagram.read(buf)));

    /** Where the client takes it; the screen's code sets this, the server never calls it. */
    public static Consumer<SiloDiagramPayload> client = p -> {
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1")
                .playToClient(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> client.accept(p))));
    }
}
