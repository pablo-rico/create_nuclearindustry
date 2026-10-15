package org.papiricoh.create_nuclearindustry.antimatter.trap;

import net.minecraft.util.StringRepresentable;

/** Which of a large Penning trap's three layers a block is: the bottom one holds the master. */
public enum TrapLevel implements StringRepresentable {
    BOTTOM,
    MIDDLE,
    TOP;

    /** Blocks above the bottom layer. */
    public int dy() {
        return ordinal();
    }

    public static TrapLevel of(int dy) {
        TrapLevel[] values = values();
        return dy >= 0 && dy < values.length ? values[dy] : BOTTOM;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
