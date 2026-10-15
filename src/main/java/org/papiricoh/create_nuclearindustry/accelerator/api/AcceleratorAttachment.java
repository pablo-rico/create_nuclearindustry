package org.papiricoh.create_nuclearindustry.accelerator.api;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity next to the beamline that serves the accelerator: a klystron, a magnet power
 * supply, a vacuum pump. The controller finds them while it walks the beamline, so they need no
 * link of their own.
 */
public interface AcceleratorAttachment {

    /** Adds this part's share to the supply; called on the server every few controller ticks. */
    void contribute(AcceleratorSupply.Builder supply);

    /**
     * Hears what the accelerator is doing: called on the server every few ticks while formed, and once
     * with {@link BeamReadings#NONE} when it breaks.
     */
    default void observe(BeamReadings readings) {}

    /**
     * Tells the attachment which controller it serves, each time the controller looks its parts up,
     * and {@code null} once the structure breaks; attachments that proxy the controller's own tanks
     * (the cryogenic feed) need it, the rest ignore it.
     */
    default void bind(@Nullable BlockPos controller) {}
}
