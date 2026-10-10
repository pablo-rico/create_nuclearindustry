package org.papiricoh.create_nuclearindustry.spentfuel.block;

import net.minecraft.util.StringRepresentable;

/** What the top of a pool rack shows: empty cells, assembly heads, or the blue Cherenkov glow of fresh fuel under water. */
public enum RackLoad implements StringRepresentable {
    EMPTY("empty"),
    LOADED("loaded"),
    GLOWING("glowing");

    private final String name;

    RackLoad(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
