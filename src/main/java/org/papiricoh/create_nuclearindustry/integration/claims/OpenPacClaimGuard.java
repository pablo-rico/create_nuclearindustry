package org.papiricoh.create_nuclearindustry.integration.claims;

import net.minecraft.server.level.ServerLevel;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

/** Open Parties and Claims: a claimed chunk is protected unless its owner lets explosions through. */
final class OpenPacClaimGuard implements ClaimGuard {
    OpenPacClaimGuard() {
        // Fails here, at install, if the API is not the one this was built for.
        OpenPACServerAPI.class.getName();
    }

    @Override
    public boolean protectsFromExplosions(ServerLevel level, int chunkX, int chunkZ) {
        OpenPACServerAPI api = OpenPACServerAPI.get(level.getServer());
        IPlayerChunkClaimAPI claim = api.getServerClaimsManager().get(level.dimension().location(), chunkX, chunkZ);
        if (claim == null) {
            return false;
        }
        Boolean protect = api.getChunkProtection().getClaimConfig(claim)
                .getEffective(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS_BLOCKS_FROM_EXPLOSIONS);
        return protect == null || protect;
    }
}
