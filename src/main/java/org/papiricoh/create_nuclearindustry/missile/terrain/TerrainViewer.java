package org.papiricoh.create_nuclearindustry.missile.terrain;

import net.minecraft.server.level.ServerPlayer;

/**
 * A block entity that draws terrain for a player: a radar whose screen they have open, a command table they stand at. The server
 * only surveys land for a request a viewer vouches for, centred where the viewer is.
 */
public interface TerrainViewer {
    /** Whether {@code player} may ask for terrain round this viewer now. */
    boolean showsTerrainTo(ServerPlayer player);
}
