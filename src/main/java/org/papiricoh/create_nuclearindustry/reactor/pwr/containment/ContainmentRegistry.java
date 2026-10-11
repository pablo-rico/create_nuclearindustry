package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The loaded containment monitors of each level, so a reactor or a pool of corium can find the
 * building around it. A level holds a handful of containments at most, so lookups just walk them.
 * The monitors hold their level, so the weak keys alone would never let it go: a level's entry is
 * dropped when it unloads, and all of them when the server stops.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public final class ContainmentRegistry {
    private static final Map<Level, Set<ContainmentMonitorBlockEntity>> LEVELS = new WeakHashMap<>();

    private ContainmentRegistry() {}

    static void register(ContainmentMonitorBlockEntity monitor) {
        Level level = monitor.getLevel();
        if (level != null && !level.isClientSide) {
            LEVELS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(monitor);
        }
    }

    static void unregister(ContainmentMonitorBlockEntity monitor) {
        Level level = monitor.getLevel();
        Set<ContainmentMonitorBlockEntity> monitors = level == null ? null : LEVELS.get(level);
        if (monitors != null) {
            monitors.remove(monitor);
        }
    }

    /** Levels with registered monitors; for tests. */
    public static boolean tracks(Level level) {
        return LEVELS.containsKey(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LEVELS.clear();
    }

    /** The sealed containment a structure occupying {@code box} stands in, or null. */
    @Nullable
    public static ContainmentMonitorBlockEntity enclosing(Level level, BoundingBox box) {
        Set<ContainmentMonitorBlockEntity> monitors = LEVELS.get(level);
        if (monitors == null) {
            return null;
        }
        for (ContainmentMonitorBlockEntity monitor : monitors) {
            if (monitor.isLive() && monitor.encloses(box)) {
                return monitor;
            }
        }
        return null;
    }

    /** The sealed containment whose air or walls include {@code pos}, or null. */
    @Nullable
    public static ContainmentMonitorBlockEntity at(Level level, BlockPos pos) {
        Set<ContainmentMonitorBlockEntity> monitors = LEVELS.get(level);
        if (monitors == null) {
            return null;
        }
        for (ContainmentMonitorBlockEntity monitor : monitors) {
            if (monitor.isLive() && monitor.covers(pos)) {
                return monitor;
            }
        }
        return null;
    }
}
