package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

/**
 * What a module's setup screen needs to know about its console: the kind of machine it is linked to,
 * that machine's status, whether a reactor has a pressurizer (some functions need one) and the latest
 * sample, so the screen can offer only what works there and show live values next to each option.
 * Sent when the screen opens, then refreshed with the desk's sync while someone has it open.
 */
public record ConsoleSummary(TargetKind kind, String status, @Nullable GlobalPos target, boolean pressurizer,
                             MonitorReading sample) {
    /** Unlinked, nothing sampled. */
    public static final ConsoleSummary NONE =
            new ConsoleSummary(TargetKind.NONE, LinkedTarget.NO_LINK, null, false, MonitorReading.NONE);
    /** Ceiling on the values read from the network: the largest sample (a reactor's) has 35. */
    public static final int MAX_VALUES = 64;
    private static final int MAX_STATUS = 256;

    /** The console {@code desk} belongs to, as it stands now. Server side. */
    public static ConsoleSummary of(ControlDeskBlockEntity desk) {
        MonitorReading sample = desk.consoleSample();
        LinkedTarget.Resolved resolved = desk.consoleTarget();
        boolean pressurizer = resolved instanceof LinkedTarget.Reactor reactor && reactor.reactor().hasPressurizer();
        return new ConsoleSummary(sample.kind(), sample.status(), desk.getTarget(), pressurizer, sample);
    }

    /** Whether the console is linked to a machine it can read (unlinked and out-of-reach consoles are not). */
    public boolean linked() {
        return kind != TargetKind.NONE && kind != TargetKind.NO_SIGNAL;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(kind.ordinal());
        buf.writeUtf(status.length() > MAX_STATUS ? status.substring(0, MAX_STATUS) : status, MAX_STATUS);
        buf.writeBoolean(target != null);
        if (target != null) {
            GlobalPos.STREAM_CODEC.encode(buf, target);
        }
        buf.writeBoolean(pressurizer);
        float[] values = sample.values();
        int count = Math.min(values.length, MAX_VALUES);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeFloat(values[i]);
        }
        buf.writeFloat(sample.seriesA());
        buf.writeFloat(sample.seriesB());
    }

    public static ConsoleSummary read(FriendlyByteBuf buf) {
        TargetKind[] kinds = TargetKind.values();
        TargetKind kind = kinds[Mth.clamp(buf.readVarInt(), 0, kinds.length - 1)];
        String status = buf.readUtf(MAX_STATUS);
        GlobalPos target = buf.readBoolean() ? GlobalPos.STREAM_CODEC.decode(buf) : null;
        boolean pressurizer = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_VALUES) {
            throw new IllegalArgumentException("Console sample with " + count + " values");
        }
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            values[i] = buf.readFloat();
        }
        float seriesA = buf.readFloat();
        float seriesB = buf.readFloat();
        return new ConsoleSummary(kind, status, target, pressurizer, new MonitorReading(kind, status, values, seriesA, seriesB));
    }

    /** For the desk's sync packet: the sample with its kind and status, plus the link and pressurizer. */
    public CompoundTag write() {
        CompoundTag tag = sample.write();
        tag.putString("Kind", kind.name());
        tag.putString("Status", status);
        tag.putBoolean("Pressurizer", pressurizer);
        ControlRoomDevice.writeTarget(tag, target);
        return tag;
    }

    public static ConsoleSummary read(CompoundTag tag) {
        MonitorReading sample = MonitorReading.read(tag);
        if (sample.values().length > MAX_VALUES) {
            sample = new MonitorReading(sample.kind(), sample.status(), java.util.Arrays.copyOf(sample.values(), MAX_VALUES),
                    sample.seriesA(), sample.seriesB());
        }
        return new ConsoleSummary(sample.kind(), sample.status(), ControlRoomDevice.readTarget(tag),
                tag.getBoolean("Pressurizer"), sample);
    }
}
