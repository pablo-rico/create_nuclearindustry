package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A satellite ready for orbit: goes in a rocket's satellite fairing. */
public class SatelliteItem extends Item {
    private final SatelliteKind kind;

    public SatelliteItem(SatelliteKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public SatelliteKind kind() {
        return kind;
    }

    /** What {@code stack} would put in orbit, or null if it is not a satellite. */
    @Nullable
    public static SatelliteKind kindOf(ItemStack stack) {
        return stack.getItem() instanceof SatelliteItem satellite ? satellite.kind : null;
    }
}
