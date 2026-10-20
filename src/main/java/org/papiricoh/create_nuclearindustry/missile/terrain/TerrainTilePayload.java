package org.papiricoh.create_nuclearindustry.missile.terrain;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Server → player: one terrain tile of the player's dimension; {@code data} as {@link TerrainTile} lays it out. */
public record TerrainTilePayload(long key, byte[] data) implements CustomPacketPayload {
    public static final Type<TerrainTilePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "terrain_tile"));
    public static final StreamCodec<ByteBuf, TerrainTilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TerrainTilePayload::key,
            ByteBufCodecs.byteArray(TerrainTile.BYTES), TerrainTilePayload::data,
            TerrainTilePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
