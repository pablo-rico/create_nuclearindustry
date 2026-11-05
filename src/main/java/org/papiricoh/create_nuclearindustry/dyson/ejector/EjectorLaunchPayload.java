package org.papiricoh.create_nuclearindustry.dyson.ejector;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

/**
 * An ejector launched a sail. Sent to everyone within {@link #RANGE} blocks rather than as a block event (which reaches only 64), so
 * the streak climbing to the sun can be seen from far off.
 */
public record EjectorLaunchPayload(BlockPos pos, int orbit) implements CustomPacketPayload {
    public static final double RANGE = 512;
    public static final Type<EjectorLaunchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ejector_launch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EjectorLaunchPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EjectorLaunchPayload::pos, ByteBufCodecs.VAR_INT, EjectorLaunchPayload::orbit, EjectorLaunchPayload::new);

    public static void send(ServerLevel level, BlockPos pos, int orbit) {
        var at = ShipSpace.toWorld(level, pos);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE, new EjectorLaunchPayload(pos, orbit));
    }

    /** Client: plays the launch at the ejector, if this client has it loaded. */
    public static void handle(Level level, EjectorLaunchPayload payload) {
        if (level != null && level.isLoaded(payload.pos) && level.getBlockEntity(payload.pos) instanceof EmRailEjectorBlockEntity ejector)
            ejector.launched(payload.orbit);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
