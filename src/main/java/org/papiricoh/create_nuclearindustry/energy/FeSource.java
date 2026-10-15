package org.papiricoh.create_nuclearindustry.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.papiricoh.create_nuclearindustry.accelerator.physics.FeDrive;

/**
 * The other half of {@link org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer}: a
 * generator's Forge Energy store that nothing can push into, which cables may pull from and which
 * pushes its energy into the blocks around it with {@link #pushToNeighbours}. The generator fills it
 * with {@link #generate}.
 */
public class FeSource extends EnergyStorage {
    private final Runnable onChange;

    public FeSource(int capacity, int maxExtract, Runnable onChange) {
        super(Math.max(1, capacity), 0, Math.max(0, maxExtract));
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(Math.max(0, toExtract), simulate);
        if (extracted > 0 && !simulate) {
            onChange.run();
        }
        return extracted;
    }

    /** Adds what the generator made this tick, up to the capacity; returns what fitted. */
    public int generate(int amount) {
        int added = Math.min(capacity - energy, Math.max(0, amount));
        if (added > 0) {
            energy += added;
            onChange.run();
        }
        return added;
    }

    /** Pushes up to {@code perSide} FE into each neighbour of {@code pos}; returns what left in all. */
    public int pushToNeighbours(Level level, BlockPos pos, int perSide) {
        int sent = 0;
        for (Direction side : Direction.values()) {
            int offer = Math.min(energy, Math.max(0, perSide));
            if (offer <= 0) {
                break;
            }
            int taken = push(level, pos, side, offer);
            if (taken > 0) {
                energy -= taken;
                sent += taken;
            }
        }
        if (sent > 0) {
            onChange.run();
        }
        return sent;
    }

    /**
     * Pushes everything it holds into the neighbours of {@code pos} that take Forge Energy, split
     * fairly ({@link FeDrive#fairShare}) so the first side does not starve the others; returns what
     * left in all.
     */
    public int pushFairly(Level level, BlockPos pos) {
        if (energy <= 0) {
            return 0;
        }
        Direction[] sides = Direction.values();
        IEnergyStorage[] targets = new IEnergyStorage[sides.length];
        int[] accepts = new int[sides.length];
        for (int i = 0; i < sides.length; i++) {
            targets[i] = receiver(level, pos, sides[i]);
            accepts[i] = targets[i] == null ? 0 : Math.max(0, targets[i].receiveEnergy(energy, true));
        }
        int[] shares = FeDrive.fairShare(energy, accepts);
        int sent = 0;
        for (int i = 0; i < sides.length; i++) {
            if (shares[i] > 0 && targets[i] != null) {
                sent += Math.max(0, Math.min(shares[i], targets[i].receiveEnergy(shares[i], false)));
            }
        }
        if (sent > 0) {
            energy = Math.max(0, energy - sent);
            onChange.run();
        }
        return sent;
    }

    /** Room left, FE. */
    public int space() {
        return Math.max(0, capacity - energy);
    }

    /** Resizes to {@code newCapacity}, dropping whatever no longer fits; it also gives that much per tick at most. */
    public void resize(int newCapacity) {
        capacity = Math.max(1, newCapacity);
        maxExtract = capacity;
        energy = Math.min(energy, capacity);
    }

    public void set(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    /**
     * Offers {@code amount} FE to the block on {@code side} of {@code pos} and returns what it took.
     * Shared by every generator, including those with an endless store.
     */
    public static int push(Level level, BlockPos pos, Direction side, int amount) {
        if (amount <= 0) {
            return 0;
        }
        IEnergyStorage storage = receiver(level, pos, side);
        return storage == null ? 0 : Math.max(0, storage.receiveEnergy(amount, false));
    }

    /** What takes Forge Energy on {@code side} of {@code pos}, or null. */
    @org.jetbrains.annotations.Nullable
    private static IEnergyStorage receiver(Level level, BlockPos pos, Direction side) {
        BlockPos target = pos.relative(side);
        if (!level.isLoaded(target)) {
            return null;
        }
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side.getOpposite());
        return storage != null && storage.canReceive() ? storage : null;
    }
}
