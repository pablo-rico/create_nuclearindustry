package org.papiricoh.create_nuclearindustry.controlroom.link;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Advanced Redstone Links and control modules sharing a code, per level, the way Create's Redstone
 * Link network shares item frequencies: every listener gets the strongest signal sent on its code by
 * a transmitter within Create's link range of it. Server side only.
 */
public final class AdvancedLinkNetwork {
    private static final Map<LevelAccessor, Map<String, Set<CodeLinkable>>> NETWORKS = new WeakHashMap<>();

    private AdvancedLinkNetwork() {}

    private static Map<String, Set<CodeLinkable>> networksIn(LevelAccessor level) {
        return NETWORKS.computeIfAbsent(level, l -> new HashMap<>());
    }

    public static void add(LevelAccessor level, CodeLinkable link) {
        if (level.isClientSide() || !LinkCode.complete(link.code())) {
            return;
        }
        networksIn(level).computeIfAbsent(link.code(), code -> new HashSet<>()).add(link);
        update(level, link.code());
    }

    public static void remove(LevelAccessor level, CodeLinkable link) {
        if (level.isClientSide()) {
            return;
        }
        Map<String, Set<CodeLinkable>> networks = networksIn(level);
        Set<CodeLinkable> network = networks.get(link.code());
        if (network == null || !network.remove(link)) {
            // Its code may have changed since it joined: look for it everywhere.
            for (Map.Entry<String, Set<CodeLinkable>> entry : networks.entrySet()) {
                if (entry.getValue().remove(link)) {
                    update(level, entry.getKey());
                    break;
                }
            }
        } else {
            update(level, link.code());
        }
        if (link.isListening()) {
            link.setReceivedStrength(0);
        }
    }

    /** Works out what every listener on {@code code} receives. */
    public static void update(LevelAccessor level, String code) {
        if (level.isClientSide()) {
            return;
        }
        Set<CodeLinkable> network = networksIn(level).get(code);
        if (network == null) {
            return;
        }
        network.removeIf(link -> !link.isAlive());
        if (network.isEmpty()) {
            networksIn(level).remove(code);
            return;
        }
        int range = AllConfigs.server().logistics.linkRange.get();
        for (CodeLinkable listener : network) {
            if (!listener.isListening()) {
                continue;
            }
            int strength = 0;
            for (CodeLinkable transmitter : network) {
                if (!transmitter.isListening() && transmitter != listener
                        && inRange(level, transmitter.location(), listener.location(), range)) {
                    strength = Math.max(strength, transmitter.transmittedStrength());
                }
            }
            listener.setReceivedStrength(strength);
        }
    }

    /** Links on a ship reach as far as their real distance allows. */
    private static boolean inRange(LevelAccessor level, net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b, int range) {
        return level instanceof net.minecraft.world.level.Level real
                ? org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.closerThan(real, a, b, range)
                : a.closerThan(b, range);
    }

    /** How many links are on {@code code} in {@code level}, for tests. */
    public static int size(LevelAccessor level, String code) {
        Set<CodeLinkable> network = networksIn(level).get(code);
        return network == null ? 0 : network.size();
    }

    /** Forgets an unloaded level's networks. */
    public static void onLevelUnload(LevelEvent.Unload event) {
        NETWORKS.remove(event.getLevel());
    }
}
