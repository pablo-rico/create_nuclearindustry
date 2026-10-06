package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.util.StringRepresentable;

/** What a Reactor Redstone Port puts out. Every mode is an output: control rods only move physically. */
public enum RedstonePortMode implements StringRepresentable {
    TEMPERATURE("temperature"),
    POWER("power"),
    FUEL("fuel"),
    COOLANT("coolant"),
    STEAM("steam"),
    SCRAM("scram");

    private final String name;

    RedstonePortMode(String name) {
        this.name = name;
    }

    public RedstonePortMode next() {
        RedstonePortMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
