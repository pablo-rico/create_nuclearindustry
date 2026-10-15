package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import java.util.function.Consumer;

/**
 * Client-side work the Containment Monitor hands off each client tick (the hydrogen haze), set by
 * {@link org.papiricoh.create_nuclearindustry.reactor.pwr.client.PwrClient} so the block entity
 * never touches client classes.
 */
public final class ContainmentClientHooks {
    /** Runs every client tick for each loaded Containment Monitor. */
    public static Consumer<ContainmentMonitorBlockEntity> monitorClientTick = monitor -> {};

    private ContainmentClientHooks() {}
}
