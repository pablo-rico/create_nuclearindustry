package org.papiricoh.create_nuclearindustry.integration.claims;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.server.level.ServerLevel;

/** FTB Chunks: a claimed chunk is protected unless its team lets explosions damage terrain. */
final class FtbChunksClaimGuard implements ClaimGuard {
    FtbChunksClaimGuard() {
        // Fails here, at install, if the API is not the one this was built for.
        FTBChunksAPI.class.getName();
    }

    @Override
    public boolean protectsFromExplosions(ServerLevel level, int chunkX, int chunkZ) {
        FTBChunksAPI.API api = FTBChunksAPI.api();
        if (api == null || !api.isManagerLoaded()) {
            return false;
        }
        ClaimedChunk claim = api.getManager().getChunk(new ChunkDimPos(level.dimension(), chunkX, chunkZ));
        return claim != null && !claim.getTeamData().canExplosionsDamageTerrain();
    }
}
