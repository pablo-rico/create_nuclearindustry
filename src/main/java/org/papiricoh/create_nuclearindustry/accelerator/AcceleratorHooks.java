package org.papiricoh.create_nuclearindustry.accelerator;

import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Seams other parts of the accelerator area hook into without the controller knowing them: client
 * effects, interlocks and the loss hot spots. Each starts as a no-op and is chained by whoever
 * needs it at setup.
 */
public final class AcceleratorHooks {
    /** Every client tick of a controller (sounds, effects). */
    public static Consumer<AcceleratorControllerBlockEntity> clientTick = controller -> {};
    /** A controller found, lost or changed its structure (server). */
    public static Consumer<AcceleratorControllerBlockEntity> onStructureChanged = controller -> {};
    /** A controller was removed or unloaded (server). */
    public static Consumer<AcceleratorControllerBlockEntity> onRemoved = controller -> {};
    /** The beam tripped (server). */
    public static BiConsumer<AcceleratorControllerBlockEntity, BeamTripCause> onTrip = (controller, cause) -> {};
    /** True refuses a start that the sequence would accept (server). */
    public static Predicate<AcceleratorControllerBlockEntity> startVeto = controller -> false;

    private AcceleratorHooks() {}
}
