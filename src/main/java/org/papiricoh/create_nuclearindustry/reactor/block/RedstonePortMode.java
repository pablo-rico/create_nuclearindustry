package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.util.StringRepresentable;

/**
 * What a Reactor Redstone Port puts out. Every mode is an output: control rods only move physically.
 * ROD_INSERT and ROD_WITHDRAW turn on while the rods sit below or above the target a control panel set,
 * to drive the clutch and gearshift of the pistons that move them.
 */
public enum RedstonePortMode implements StringRepresentable {
    TEMPERATURE("temperature"),
    POWER("power"),
    FUEL("fuel"),
    COOLANT("coolant"),
    STEAM("steam"),
    SCRAM("scram"),
    ROD_INSERT("rod_insert"),
    ROD_WITHDRAW("rod_withdraw");

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
