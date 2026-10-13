package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import java.util.function.Consumer;

/**
 * Client-side work the cryogenic plant controller hands off each tick (compressor hum, frost on
 * the casing), set by the client setup so the block entity never touches client classes.
 */
public final class CryoplantClientHooks {
    /** Runs every client tick for each loaded plant controller. */
    public static Consumer<CryoplantControllerBlockEntity> clientTick = controller -> {};

    private CryoplantClientHooks() {}
}
