package org.papiricoh.create_nuclearindustry.missile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Compatibility shim. Missile alerts require an operational radar, regardless of old configuration. */
@Deprecated
public final class LaunchWarnings {
    public static final int MIN_RADIUS = 128;
    private LaunchWarnings() {}
    public static void announce(ServerLevel level, Vec3 aim, int threatRadius, int flightTicks) {
        // Intentionally silent: legacy launchBroadcast settings cannot bypass radar detection.
    }
}
