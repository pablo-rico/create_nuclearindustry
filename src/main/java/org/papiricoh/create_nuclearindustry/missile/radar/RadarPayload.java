package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import java.util.*;

public record RadarPayload(int menu, BlockPos pos, long sample, int status, boolean first, List<RadarContacts.Contact> contacts) implements CustomPacketPayload {
    public static final Type<RadarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "radar_contacts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarPayload> CODEC = new StreamCodec<>() {
        public RadarPayload decode(RegistryFriendlyByteBuf b) {
            int menu = b.readVarInt(); BlockPos pos = b.readBlockPos(); long sample = b.readLong(); int status = b.readVarInt(); boolean first = b.readBoolean();
            int size = b.readVarInt(); if (size < 0 || size > 128) throw new IllegalArgumentException("Radar packet too large");
            List<RadarContacts.Contact> contacts = new ArrayList<>();
            for (int i = 0; i < size; i++) contacts.add(new RadarContacts.Contact(b.readUUID(), b.readBoolean() ? b.readBlockPos() : null,
                    readVec(b), readVec(b), readVec(b), b.readVarInt()));
            return new RadarPayload(menu, pos, sample, status, first, List.copyOf(contacts));
        }
        public void encode(RegistryFriendlyByteBuf b, RadarPayload p) {
            b.writeVarInt(p.menu); b.writeBlockPos(p.pos); b.writeLong(p.sample); b.writeVarInt(p.status); b.writeBoolean(p.first); b.writeVarInt(p.contacts.size());
            for (var c : p.contacts) { b.writeUUID(c.id()); b.writeBoolean(c.origin() != null); if (c.origin() != null) b.writeBlockPos(c.origin());
                writeVec(b, c.position()); writeVec(b, c.velocity()); writeVec(b, c.impact()); b.writeVarInt(c.ticks()); }
        }
    };
    private static Vec3 readVec(RegistryFriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    private static void writeVec(RegistryFriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToClient(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof RadarMenu m && m.containerId == p.menu && m.pos.equals(p.pos)) {
                if (p.first) m.contacts = new ArrayList<>();
                m.contacts.addAll(p.contacts); m.status = p.status;
            }
        })));
    }
}
