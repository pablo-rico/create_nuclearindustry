package org.papiricoh.create_nuclearindustry.coolingtower;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** What the client needs of a cooling tower: its shape for the plume, and its load. */
public record CoolingTowerDisplay(boolean formed, BlockPos center, int baseRadius, int height, int currentUse, int capacity,
                                  int fans, String message) {
    public static final CoolingTowerDisplay EMPTY = new CoolingTowerDisplay(false, BlockPos.ZERO, 0, 0, 0, 0, 0, "Not formed");

    public float load() {
        return capacity <= 0 ? 0 : Math.min(1, currentUse / (float) capacity);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Formed", formed);
        tag.putLong("Center", center.asLong());
        tag.putInt("BaseRadius", baseRadius);
        tag.putInt("Height", height);
        tag.putInt("Use", currentUse);
        tag.putInt("Capacity", capacity);
        tag.putInt("Fans", fans);
        tag.putString("Message", message);
        return tag;
    }

    public static CoolingTowerDisplay read(CompoundTag tag) {
        if (!tag.contains("Formed")) {
            return EMPTY;
        }
        return new CoolingTowerDisplay(tag.getBoolean("Formed"), BlockPos.of(tag.getLong("Center")), tag.getInt("BaseRadius"),
                tag.getInt("Height"), tag.getInt("Use"), tag.getInt("Capacity"), tag.getInt("Fans"), tag.getString("Message"));
    }
}
