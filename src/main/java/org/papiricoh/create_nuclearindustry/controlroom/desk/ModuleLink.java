package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A control module transmitting on a Create Redstone Link frequency. A block entity can only hold
 * one of Create's link behaviours, so each module gets one of these instead, registered with the
 * network the way Create's Linked Controller registers its buttons.
 */
public final class ModuleLink implements IRedstoneLinkable {
    private final ControlDeskBlockEntity desk;
    private final Couple<Frequency> key;
    private int strength;

    public ModuleLink(ControlDeskBlockEntity desk, ItemStack first, ItemStack last, int strength) {
        this.desk = desk;
        this.key = keyOf(first, last);
        this.strength = strength;
    }

    public static Couple<Frequency> keyOf(ItemStack first, ItemStack last) {
        return Couple.create(Frequency.of(first), Frequency.of(last));
    }

    public boolean sameKey(ItemStack first, ItemStack last) {
        return key.equals(keyOf(first, last));
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
    public int getTransmittedStrength() {
        return isAlive() ? strength : 0;
    }

    @Override
    public void setReceivedStrength(int power) {
    }

    @Override
    public boolean isListening() {
        return false;
    }

    @Override
    public boolean isAlive() {
        return !desk.isRemoved() && desk.getLevel() != null && desk.getLevel().getBlockEntity(desk.getBlockPos()) == desk;
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return key;
    }

    @Override
    public BlockPos getLocation() {
        return desk.getBlockPos();
    }
}
