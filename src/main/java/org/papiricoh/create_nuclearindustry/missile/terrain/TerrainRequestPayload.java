package org.papiricoh.create_nuclearindustry.missile.terrain;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;

/**
 * Player → server: the terrain tiles ({@link TerrainTile} keys, at most {@link #MAX}) a scope or table at {@code centre} wants
 * drawn. The server answers only if that block vouches for the player ({@link TerrainViewer}).
 */
public record TerrainRequestPayload(BlockPos centre, List<Long> keys) implements CustomPacketPayload {
    public static final int MAX = 16;
    public static final Type<TerrainRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "terrain_request"));
    public static final StreamCodec<ByteBuf, TerrainRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerrainRequestPayload::centre,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(MAX)), TerrainRequestPayload::keys,
            TerrainRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registers both terrain payloads; the client handler is the client side's {@code ClientTerrain}. */
    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> {
            var registrar = e.registrar("1");
            registrar.playToServer(TYPE, STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) TerrainSurvey.request(player, p.centre(), p.keys());
            }));
            registrar.playToClient(TerrainTilePayload.TYPE, TerrainTilePayload.STREAM_CODEC,
                    (p, ctx) -> ctx.enqueueWork(() -> ClientTerrainHook.handle(p)));
        });
    }
}
