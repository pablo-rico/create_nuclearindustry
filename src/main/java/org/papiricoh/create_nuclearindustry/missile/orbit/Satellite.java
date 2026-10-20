package org.papiricoh.create_nuclearindustry.missile.orbit;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One satellite in orbit: what it does, where it flies and whose it is. Its network is a link code ({@code code}, four characters
 * like a radar's): it works for the radars, ground stations and command tables on that code, and is foreign to every other.
 *
 * @param designation a short name shown on tables, e.g. {@code EW-3}
 * @param launchedAt  the game tick it reached orbit
 */
public record Satellite(UUID id, String designation, String code, SatelliteKind kind, Orbit orbit, @Nullable UUID owner, long launchedAt) {
    public static final int MAX_DESIGNATION = 12;

    public Satellite withOrbit(Orbit next) {
        return new Satellite(id, designation, code, kind, next, owner, launchedAt);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Designation", designation);
        tag.putString("Code", code);
        tag.putString("Kind", kind.name().toLowerCase(java.util.Locale.ROOT));
        tag.put("Orbit", orbit.save());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("LaunchedAt", launchedAt);
        return tag;
    }

    public static Satellite load(CompoundTag tag) {
        return new Satellite(tag.getUUID("Id"), tag.getString("Designation"), tag.getString("Code"), SatelliteKind.byName(tag.getString("Kind")),
                Orbit.load(tag.getCompound("Orbit")), tag.hasUUID("Owner") ? tag.getUUID("Owner") : null, tag.getLong("LaunchedAt"));
    }

    /** The client's copy: everything but its owner. */
    public void write(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        b.writeUUID(id);
        b.writeUtf(designation, MAX_DESIGNATION);
        b.writeUtf(code, 8);
        b.writeByte(kind.ordinal());
        orbit.write(b);
        b.writeLong(launchedAt);
    }

    public static Satellite read(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        return new Satellite(b.readUUID(), b.readUtf(MAX_DESIGNATION), b.readUtf(8), SatelliteKind.byOrdinal(b.readByte()), Orbit.read(b), null,
                b.readLong());
    }
}
