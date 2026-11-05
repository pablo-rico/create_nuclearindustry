package org.papiricoh.create_nuclearindustry.dyson.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** Opens, closes or reshapes one of the swarm's orbits, from a terminal's screen. */
public record ConfigureSwarmOrbitPayload(BlockPos pos, int orbit, boolean enabled, float radius, float inclination, float node)
        implements CustomPacketPayload {
    public static final Type<ConfigureSwarmOrbitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "configure_swarm_orbit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureSwarmOrbitPayload> CODEC = StreamCodec.of(ConfigureSwarmOrbitPayload::write, ConfigureSwarmOrbitPayload::read);

    private static void write(RegistryFriendlyByteBuf b, ConfigureSwarmOrbitPayload p) {
        b.writeBlockPos(p.pos);
        b.writeVarInt(p.orbit);
        b.writeBoolean(p.enabled);
        b.writeFloat(p.radius);
        b.writeFloat(p.inclination);
        b.writeFloat(p.node);
    }

    private static ConfigureSwarmOrbitPayload read(RegistryFriendlyByteBuf b) {
        return new ConfigureSwarmOrbitPayload(b.readBlockPos(), b.readVarInt(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat());
    }

    /** Whether the values are ones a terminal can send. */
    public boolean valid() {
        return OrbitShape.validIndex(orbit) && OrbitShape.valid(radius, inclination, node);
    }

    /** Server: applies it if the player may use the terminal at {@code pos} and the values are in range; returns whether it did. */
    public static boolean apply(ServerPlayer player, ConfigureSwarmOrbitPayload payload) {
        if (!payload.valid() || !ServerPayloadGuard.check(player, payload.pos)
                || !(player.level().getBlockEntity(payload.pos) instanceof SwarmTerminalBlockEntity)) return false;
        DysonSwarm.get(player.serverLevel()).setShape(payload.orbit,
                new OrbitShape(payload.enabled, payload.radius, payload.inclination, payload.node));
        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
