package org.papiricoh.create_nuclearindustry.dyson;

import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlockEntity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Where the swarm's machines tell the client's effects what happened, without the common side loading client classes: no-ops
 * until {@code DysonClient} sets them.
 */
public final class DysonFxHooks {
    /** An ejector on this client launched a sail into the given orbit, this tick. */
    public static BiConsumer<EmRailEjectorBlockEntity, Integer> ejectorFired = (ejector, orbit) -> {
    };
    /** A receiver on this client started taking power after taking none. */
    public static Consumer<RayReceiverBlockEntity> receiverLit = receiver -> {
    };

    private DysonFxHooks() {
    }
}
