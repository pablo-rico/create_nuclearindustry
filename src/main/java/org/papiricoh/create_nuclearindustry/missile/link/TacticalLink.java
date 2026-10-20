package org.papiricoh.create_nuclearindustry.missile.link;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a Tactical Linker holds: a missile radar, a launch controller, an interceptor battery or a ground station, and which, so its tooltip can name
 * it without the chunk loaded.
 */
public record TacticalLink(GlobalPos target, Kind kind) {
    /** Saved by name, sent by ordinal: add new kinds at the end, never rename one. */
    public enum Kind implements StringRepresentable {
        RADAR, LAUNCH_CONTROLLER, INTERCEPTOR_BATTERY, GROUND_STATION;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<TacticalLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("target").forGetter(TacticalLink::target),
            Kind.CODEC.fieldOf("kind").forGetter(TacticalLink::kind)).apply(instance, TacticalLink::new));
    public static final StreamCodec<ByteBuf, TacticalLink> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, TacticalLink::target,
            ByteBufCodecs.idMapper(i -> Kind.values()[Math.floorMod(i, Kind.values().length)], Kind::ordinal), TacticalLink::kind,
            TacticalLink::new);
}
