package org.papiricoh.create_nuclearindustry.integration.dh;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Optional Distant Horizons support, client side: a nuclear cloud stays in sight over its distant terrain
 * ({@link DhFarCloudPass}). Nothing here touches Distant Horizons' classes unless it is installed, and a
 * version whose API does not match only turns the support off.
 */
public final class DistantHorizonsCompat {
    public static final String MODID = "distanthorizons";
    private static final Logger LOGGER = LogUtils.getLogger();

    private DistantHorizonsCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** Client: hooks the far cloud into Distant Horizons' rendering if it is there. */
    public static void initClient(IEventBus modEventBus) {
        if (!isLoaded()) {
            return;
        }
        try {
            DhFarCloudPass.register(modEventBus);
            LOGGER.info("Distant Horizons found: nuclear clouds draw over its distant terrain");
        } catch (LinkageError | RuntimeException e) {
            LOGGER.warn("Distant Horizons found, but not an API this was built for; nuclear clouds stay within the vanilla view", e);
        }
    }
}
