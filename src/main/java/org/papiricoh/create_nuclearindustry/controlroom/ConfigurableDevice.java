package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.nbt.CompoundTag;

/** A control room instrument set up from a screen, through a {@link ConfigureDevicePayload}. */
public interface ConfigurableDevice {

    /**
     * Applies a setup sent by a client for {@code part} (a window, a pen...). The tag comes straight
     * from the network: the device takes only what it understands and sanitises every value.
     * Returns whether it was applied.
     */
    boolean configure(int part, CompoundTag config);
}
