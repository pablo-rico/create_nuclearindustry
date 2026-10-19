package org.papiricoh.create_nuclearindustry.missile.flight;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Writes a {@link RocketShape} to a save or a packet; reading clamps it again, so neither can hold more than a shape allows. */
public final class RocketShapeCodec {

    public static final StreamCodec<ByteBuf, RocketShape> STREAM_CODEC = StreamCodec.of(RocketShapeCodec::write, RocketShapeCodec::read);

    private RocketShapeCodec() {
    }

    private static void write(ByteBuf buf, RocketShape shape) {
        buf.writeByte(shape.height());
        buf.writeFloat(shape.radius());
        buf.writeByte(shape.payloadBottom());
        buf.writeByte(shape.stages().size());
        for (RocketShape.Stage stage : shape.stages()) {
            buf.writeByte(stage.bottom());
            buf.writeByte(stage.top());
            buf.writeByte(stage.propellant().ordinal());
            buf.writeByte(stage.nozzles().size());
            for (RocketShape.Nozzle n : stage.nozzles()) {
                buf.writeFloat(n.x());
                buf.writeFloat(n.y());
                buf.writeFloat(n.z());
            }
        }
    }

    private static RocketShape read(ByteBuf buf) {
        int height = buf.readUnsignedByte();
        float radius = buf.readFloat();
        int payload = buf.readUnsignedByte();
        int count = Math.min(RocketShape.MAX_STAGES, buf.readUnsignedByte());
        List<RocketShape.Stage> stages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int bottom = buf.readUnsignedByte();
            int top = buf.readUnsignedByte();
            RocketShape.Propellant propellant = RocketShape.Propellant.byId(buf.readUnsignedByte());
            int nozzles = Math.min(RocketShape.MAX_NOZZLES, buf.readUnsignedByte());
            List<RocketShape.Nozzle> list = new ArrayList<>();
            for (int j = 0; j < nozzles; j++) list.add(new RocketShape.Nozzle(buf.readFloat(), buf.readFloat(), buf.readFloat()));
            stages.add(new RocketShape.Stage(bottom, top, list, propellant));
        }
        return new RocketShape(height, radius, stages, payload);
    }

    public static CompoundTag save(RocketShape shape) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Height", shape.height());
        tag.putFloat("Radius", shape.radius());
        tag.putInt("Payload", shape.payloadBottom());
        ListTag stages = new ListTag();
        for (RocketShape.Stage stage : shape.stages()) {
            CompoundTag s = new CompoundTag();
            s.putInt("Bottom", stage.bottom());
            s.putInt("Top", stage.top());
            s.putInt("Propellant", stage.propellant().ordinal());
            ListTag nozzles = new ListTag();
            for (RocketShape.Nozzle n : stage.nozzles()) {
                CompoundTag t = new CompoundTag();
                t.putFloat("X", n.x());
                t.putFloat("Y", n.y());
                t.putFloat("Z", n.z());
                nozzles.add(t);
            }
            s.put("Nozzles", nozzles);
            stages.add(s);
        }
        tag.put("Stages", stages);
        return tag;
    }

    public static RocketShape load(CompoundTag tag) {
        List<RocketShape.Stage> stages = new ArrayList<>();
        for (Tag t : tag.getList("Stages", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) t;
            List<RocketShape.Nozzle> nozzles = new ArrayList<>();
            for (Tag n : s.getList("Nozzles", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) n;
                nozzles.add(new RocketShape.Nozzle(c.getFloat("X"), c.getFloat("Y"), c.getFloat("Z")));
            }
            stages.add(new RocketShape.Stage(s.getInt("Bottom"), s.getInt("Top"), nozzles, RocketShape.Propellant.byId(s.getInt("Propellant"))));
        }
        return new RocketShape(tag.getInt("Height"), tag.getFloat("Radius"), stages, tag.getInt("Payload"));
    }
}
