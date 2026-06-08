package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.util.StringRepresentable;

public enum ReactorFluidPortMode implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output");

    private final String serializedName;

    ReactorFluidPortMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public ReactorFluidPortMode next() {
        return this == INPUT ? OUTPUT : INPUT;
    }

    public String displayName() {
        return this == INPUT ? "Input" : "Output";
    }
}
