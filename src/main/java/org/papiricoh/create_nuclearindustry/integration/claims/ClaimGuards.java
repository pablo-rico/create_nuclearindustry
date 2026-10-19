package org.papiricoh.create_nuclearindustry.integration.claims;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Optional support for land-claim mods (Open Parties and Claims, FTB Chunks). A guard is installed
 * only for a mod that is present, and one whose API does not match, at install or later, is dropped
 * with a warning: claims then simply stop being respected, nothing breaks.
 */
public final class ClaimGuards {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<ClaimGuard> GUARDS = new CopyOnWriteArrayList<>();

    private ClaimGuards() {}

    public static void init() {
        // Lambdas, so a guard's class (and the mod API it imports) is only loaded once its mod is known to be there.
        tryInstall("openpartiesandclaims", "Open Parties and Claims", () -> new OpenPacClaimGuard());
        tryInstall("ftbchunks", "FTB Chunks", () -> new FtbChunksClaimGuard());
    }

    private static void tryInstall(String modId, String name, Supplier<ClaimGuard> guard) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            GUARDS.add(guard.get());
            LOGGER.info("{} found: nuclear detonations leave its protected claims alone", name);
        } catch (LinkageError | RuntimeException e) {
            LOGGER.warn("{} found, but not an API this was built for; its claims are not respected", name, e);
        }
    }

    /**
     * Whether a nuclear detonation must leave the blocks of chunk ({@code chunkX}, {@code chunkZ}) alone:
     * {@code explosive.respectClaims} is on and some claim mod protects it from explosions.
     */
    public static boolean protectsFromExplosions(ServerLevel level, int chunkX, int chunkZ) {
        if (!ExplosiveConfig.respectClaims || GUARDS.isEmpty()) {
            return false;
        }
        for (ClaimGuard guard : GUARDS) {
            try {
                if (guard.protectsFromExplosions(level, chunkX, chunkZ)) {
                    return true;
                }
            } catch (LinkageError | RuntimeException e) {
                GUARDS.remove(guard);
                LOGGER.warn("A claim mod's API failed; its claims are no longer respected", e);
            }
        }
        return false;
    }

    /** Adds {@code guard}, for tests; take it out again with {@link #remove}. */
    public static void add(ClaimGuard guard) {
        GUARDS.add(guard);
    }

    public static void remove(ClaimGuard guard) {
        GUARDS.remove(guard);
    }
}
