package org.papiricoh.create_nuclearindustry.accelerator.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * A machine's Forge Energy buffer: other mods' cables push energy in, nothing can pull it out, and
 * the machine spends it itself with {@link #take}. The capacity follows the machine's demand.
 */
public class FeBuffer extends EnergyStorage {
    private final Runnable onChange;

    public FeBuffer(int capacity, Runnable onChange) {
        super(Math.max(1, capacity), Math.max(1, capacity), 0);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(Math.max(0, toReceive), simulate);
        if (received > 0 && !simulate) {
            onChange.run();
        }
        return received;
    }

    /** Spends up to {@code amount} FE and returns what was there. */
    public int take(int amount) {
        int taken = Math.min(energy, Math.max(0, amount));
        if (taken > 0) {
            energy -= taken;
            onChange.run();
        }
        return taken;
    }

    /** Resizes to {@code newCapacity}, dropping whatever no longer fits. */
    public void resize(int newCapacity) {
        capacity = Math.max(1, newCapacity);
        maxReceive = capacity;
        energy = Math.min(energy, capacity);
    }

    public void set(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
