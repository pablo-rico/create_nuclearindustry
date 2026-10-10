package org.papiricoh.create_nuclearindustry.fusion.api;

import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

import java.util.function.Consumer;

/**
 * Client-side work the controller hands off each tick (plasma sounds, frost and steam particles),
 * set by the client setup so the block entity never touches client classes.
 */
public final class TokamakClientHooks {
    /** Runs every client tick for each loaded tokamak controller. */
    public static Consumer<FusionReactorBlockEntity> clientTick = controller -> {};

    private TokamakClientHooks() {}
}
