package org.papiricoh.create_nuclearindustry.integration.sable.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client side: how contaminated each ship in sight is, as its last {@code ShipSeverityPayload} said. */
public final class ShipSeverities {
    /** Ticks a severity holds without news: a ship that went out of range stops showing dust. */
    private static final long STALE = 300;
    private static final Map<UUID, Entry> SEVERITIES = new HashMap<>();

    private record Entry(int severity, long time) {}

    private ShipSeverities() {}

    public static void set(UUID ship, int severity) {
        if (severity <= 0) {
            SEVERITIES.remove(ship);
        } else {
            SEVERITIES.put(ship, new Entry(severity, now()));
        }
    }

    public static int severity(UUID ship) {
        Entry entry = SEVERITIES.get(ship);
        if (entry == null) {
            return 0;
        }
        if (now() - entry.time() > STALE) {
            SEVERITIES.remove(ship);
            return 0;
        }
        return entry.severity();
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }
}
