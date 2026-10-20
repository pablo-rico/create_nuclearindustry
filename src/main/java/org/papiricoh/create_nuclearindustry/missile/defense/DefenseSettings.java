package org.papiricoh.create_nuclearindustry.missile.defense;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;

import java.util.Locale;

/**
 * How a battery or a command table defends: whether it engages on its own, how many interceptors it sends at a missile and the
 * zone it defends (a missile is engaged only if it is predicted to come down in it; radius 0 is the launcher's default).
 */
public record DefenseSettings(boolean auto, Doctrine doctrine, int zoneX, int zoneZ, int zoneRadius) {
    public DefenseSettings {
        zoneRadius = Math.max(0, Math.min(RadarBlockEntity.MAX_ZONE, zoneRadius));
    }

    /** Off, one at a time, the default zone round {@code pos}. */
    public static DefenseSettings defaults(BlockPos pos) {
        return new DefenseSettings(false, Doctrine.SHOOT_LOOK_SHOOT, pos.getX(), pos.getZ(), 0);
    }

    public DefenseSettings withAuto(boolean auto) {
        return new DefenseSettings(auto, doctrine, zoneX, zoneZ, zoneRadius);
    }

    public DefenseSettings withDoctrine(Doctrine doctrine) {
        return new DefenseSettings(auto, doctrine, zoneX, zoneZ, zoneRadius);
    }

    public DefenseSettings withZone(int x, int z, int radius) {
        return new DefenseSettings(auto, doctrine, x, z, radius);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Auto", auto);
        tag.putString("Doctrine", doctrine.name().toLowerCase(Locale.ROOT));
        tag.putInt("ZoneX", zoneX);
        tag.putInt("ZoneZ", zoneZ);
        tag.putInt("ZoneRadius", zoneRadius);
        return tag;
    }

    /** The settings saved in {@code tag}, or {@code fallback} if there are none. */
    public static DefenseSettings load(CompoundTag tag, DefenseSettings fallback) {
        if (!tag.contains("Doctrine")) return fallback;
        return new DefenseSettings(tag.getBoolean("Auto"), Doctrine.byName(tag.getString("Doctrine")), tag.getInt("ZoneX"),
                tag.getInt("ZoneZ"), tag.getInt("ZoneRadius"));
    }

    public static final StreamCodec<ByteBuf, DefenseSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DefenseSettings::auto,
            ByteBufCodecs.VAR_INT.map(Doctrine::byOrdinal, Doctrine::ordinal), DefenseSettings::doctrine,
            ByteBufCodecs.VAR_INT, DefenseSettings::zoneX,
            ByteBufCodecs.VAR_INT, DefenseSettings::zoneZ,
            ByteBufCodecs.VAR_INT, DefenseSettings::zoneRadius,
            DefenseSettings::new);
}
