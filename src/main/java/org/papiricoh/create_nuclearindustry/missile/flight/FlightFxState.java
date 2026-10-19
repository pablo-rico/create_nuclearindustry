package org.papiricoh.create_nuclearindustry.missile.flight;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * What a client is told of one flight to draw it: where it is and how it moves, its phase and stage, a few flags and the way
 * it was launched. Its shape comes only the first time a player hears of it. Never its target.
 */
public record FlightFxState(UUID id, double x, double y, double z, float vx, float vy, float vz, byte phase, byte stage,
                            byte flags, float heading, @Nullable RocketShape shape) {
    /** Its engines are burning. */
    public static final int BURNING = 1;
    /** Still climbing straight out of its silo. */
    public static final int RISING = 1 << 1;
    /** It carries a nuclear warhead: its impact hands over to the detonation's own effects. */
    public static final int NUCLEAR_WARHEAD = 1 << 2;
    /** The server shows a puppet entity of it near this player, which draws the rocket itself. */
    public static final int PUPPET = 1 << 3;
    /** Its engines are done: only the warhead is still attached. */
    public static final int BURNED_OUT = 1 << 4;

    public static final StreamCodec<ByteBuf, FlightFxState> STREAM_CODEC = StreamCodec.of(FlightFxState::write, FlightFxState::read);
    private static final StreamCodec<ByteBuf, Optional<RocketShape>> SHAPE = ByteBufCodecs.optional(RocketShapeCodec.STREAM_CODEC);

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    private static void write(ByteBuf buf, FlightFxState s) {
        UUIDUtil.STREAM_CODEC.encode(buf, s.id);
        buf.writeDouble(s.x);
        buf.writeDouble(s.y);
        buf.writeDouble(s.z);
        buf.writeFloat(s.vx);
        buf.writeFloat(s.vy);
        buf.writeFloat(s.vz);
        buf.writeByte(s.phase);
        buf.writeByte(s.stage);
        buf.writeByte(s.flags);
        buf.writeFloat(s.heading);
        SHAPE.encode(buf, Optional.ofNullable(s.shape));
    }

    private static FlightFxState read(ByteBuf buf) {
        return new FlightFxState(UUIDUtil.STREAM_CODEC.decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readFloat(), SHAPE.decode(buf).orElse(null));
    }
}
