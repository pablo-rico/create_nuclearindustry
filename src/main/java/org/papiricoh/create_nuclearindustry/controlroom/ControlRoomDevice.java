package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** A monitor, panel or alarm that a Control Room Linker can point at a machine. */
public interface ControlRoomDevice {

    @Nullable
    GlobalPos getTarget();

    /** Links the device to {@code target}, or unlinks it when null. */
    void setTarget(@Nullable GlobalPos target);

    static void writeTarget(CompoundTag tag, @Nullable GlobalPos target) {
        if (target != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(encoded -> tag.put("Target", encoded));
        }
    }

    @Nullable
    static GlobalPos readTarget(CompoundTag tag) {
        if (!tag.contains("Target", Tag.TAG_COMPOUND)) {
            return null;
        }
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Target")).result().orElse(null);
    }
}
