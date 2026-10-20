package org.papiricoh.create_nuclearindustry.missile.terrain;

import java.util.function.Consumer;

/** Where a terrain tile goes on a client; set by the client side, so the common payload code never loads client classes. */
public final class ClientTerrainHook {
    private static Consumer<TerrainTilePayload> handler = payload -> {
    };

    private ClientTerrainHook() {
    }

    public static void install(Consumer<TerrainTilePayload> handler) {
        ClientTerrainHook.handler = handler;
    }

    static void handle(TerrainTilePayload payload) {
        handler.accept(payload);
    }
}
