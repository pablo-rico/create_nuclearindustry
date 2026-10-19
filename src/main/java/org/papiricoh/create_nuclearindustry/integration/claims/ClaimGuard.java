package org.papiricoh.create_nuclearindustry.integration.claims;

import net.minecraft.server.level.ServerLevel;

/**
 * What a land-claim mod says about a chunk. Each supported mod has its own implementation, the only
 * classes that touch its API; the rest of the mod asks {@link ClaimGuards}.
 */
public interface ClaimGuard {
    /** Whether a claim keeps explosions from changing the blocks of chunk ({@code chunkX}, {@code chunkZ}). */
    boolean protectsFromExplosions(ServerLevel level, int chunkX, int chunkZ);
}
