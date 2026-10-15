package org.papiricoh.create_nuclearindustry.wastetransport.cask;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;

import java.util.function.LongSupplier;

/**
 * The slots one block of a Shielded Waste Cask adds. Only {@code waste_cask_cargo} goes in, and
 * never Hot spent fuel; {@code clock} dates spent fuel (below 0: use its stored grade).
 */
public class WasteCaskInventory extends ItemStackHandler {
    private final LongSupplier clock;
    private Runnable onChange;

    public WasteCaskInventory(int slots, LongSupplier clock, Runnable onChange) {
        super(slots);
        this.clock = clock;
        this.onChange = onChange;
    }

    /** A detached copy of {@code source} (a contraption's cargo), with the same filter. */
    public static WasteCaskInventory copyOf(IItemHandler source) {
        WasteCaskInventory copy = new WasteCaskInventory(source.getSlots(), () -> -1L, () -> {});
        for (int slot = 0; slot < source.getSlots(); slot++) {
            copy.setStackInSlot(slot, source.getStackInSlot(slot).copy());
        }
        return copy;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return WasteCargo.acceptsItem(stack, clock.getAsLong());
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }
}
