package org.papiricoh.create_nuclearindustry.antimatter.trap;

import net.minecraft.util.StringRepresentable;

/**
 * Which column of a large Penning trap's 2x2 footprint a block is: north-west is the smallest x and
 * z, where the master stands.
 */
public enum TrapCorner implements StringRepresentable {
    NW(0, 0),
    NE(1, 0),
    SW(0, 1),
    SE(1, 1);

    private final int dx;
    private final int dz;

    TrapCorner(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** Blocks east of the north-west column. */
    public int dx() {
        return dx;
    }

    /** Blocks south of the north-west column. */
    public int dz() {
        return dz;
    }

    public static TrapCorner of(int dx, int dz) {
        for (TrapCorner corner : values()) {
            if (corner.dx == dx && corner.dz == dz) {
                return corner;
            }
        }
        return NW;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
