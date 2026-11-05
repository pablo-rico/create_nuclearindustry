package org.papiricoh.create_nuclearindustry.dyson.client;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmSyncPayload;

/**
 * The Dyson swarm as last sent to this client. Its sails' places come from {@code SwarmGeometry} and the level's game time; only the
 * Overworld's sky shows it.
 */
public final class ClientSwarm {
    /** Sails at which the sun is dimmed the most it ever is. */
    public static final long DIM_FULL = 10000;
    private static SwarmSyncPayload swarm = SwarmSyncPayload.EMPTY;

    private ClientSwarm() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    public static void accept(SwarmSyncPayload payload) {
        swarm = payload;
    }

    public static SwarmSyncPayload get() {
        return swarm;
    }

    /** Whether {@code level}'s sky is the swarm's. */
    public static boolean shows(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    /** How much of the sun the swarm covers, 0 to 1 (by sails, full at {@link #DIM_FULL}), for dimming it. */
    public static float coverage() {
        return (float) Math.min(1.0, swarm.totalSails() / (double) DIM_FULL);
    }

    public static void clear() {
        swarm = SwarmSyncPayload.EMPTY;
    }
}
