package org.papiricoh.create_nuclearindustry.integration.sable;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Optional Sable / Create Aeronautics support. Nothing here touches Sable's classes unless it is
 * installed, and a Sable whose API does not match only turns the support off: the rest of the mod
 * then sees no ships through {@link ShipSpace}.
 */
public final class SableCompat {
    public static final String MODID = "sable";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean active;

    private SableCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** True once {@link ShipSpace} sees Sable's ships. */
    public static boolean isActive() {
        return active;
    }

    public static void init(IEventBus modEventBus) {
        // Registered either way, so a client and a server agree on the channels whatever they run.
        ShipSeverityPayload.register(modEventBus);
        if (!isLoaded()) {
            return;
        }
        try {
            SableShips.install(modEventBus);
            active = true;
            LOGGER.info("Sable found: explosions, contamination and control rooms reach its ships");
        } catch (LinkageError | RuntimeException e) {
            LOGGER.warn("Sable found, but not an API this was built for; its ships are left alone", e);
        }
    }
}
