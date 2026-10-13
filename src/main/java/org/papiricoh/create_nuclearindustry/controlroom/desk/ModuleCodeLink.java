package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.controlroom.link.CodeLinkable;

/** A control module sending its signal on an Advanced Redstone Link code, as {@link ModuleLink} does on a Create frequency. */
public final class ModuleCodeLink implements CodeLinkable {
    private final ControlDeskBlockEntity desk;
    private final String code;
    private int strength;

    public ModuleCodeLink(ControlDeskBlockEntity desk, String code, int strength) {
        this.desk = desk;
        this.code = code;
        this.strength = strength;
    }

    /** Sets the strength sent; true when it changed. */
    public boolean setStrength(int strength) {
        if (this.strength == strength) {
            return false;
        }
        this.strength = strength;
        return true;
    }

    @Override
    public int transmittedStrength() {
        return isAlive() ? strength : 0;
    }

    @Override
    public void setReceivedStrength(int strength) {
    }

    @Override
    public boolean isListening() {
        return false;
    }

    /** Only the desk's flag: the network may ask while chunks unload, when looking the block up would wait for them. */
    @Override
    public boolean isAlive() {
        return !desk.isRemoved();
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public BlockPos location() {
        return desk.getBlockPos();
    }
}
