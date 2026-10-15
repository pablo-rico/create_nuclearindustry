package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The loaded seismic detectors of each level, so a detonation reaches every one of them in its
 * dimension without a block scan. Detectors join on load and leave on unload or removal. The
 * detectors hold their level, so a level's entry is dropped when it unloads, and all of them when
 * the server stops.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public final class SeismicNetwork {
    private static final Map<Level, Set<SeismicDetectorBlockEntity>> DETECTORS = new WeakHashMap<>();

    private SeismicNetwork() {}

    static void add(SeismicDetectorBlockEntity detector) {
        Level level = detector.getLevel();
        if (level != null && !level.isClientSide) {
            DETECTORS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(detector);
        }
    }

    static void remove(SeismicDetectorBlockEntity detector) {
        Level level = detector.getLevel();
        if (level != null) {
            Set<SeismicDetectorBlockEntity> detectors = DETECTORS.get(level);
            if (detectors != null) {
                detectors.remove(detector);
            }
        }
    }

    /** Levels with registered detectors; for tests. */
    public static boolean tracks(Level level) {
        return DETECTORS.containsKey(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            DETECTORS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DETECTORS.clear();
    }

    /** Listener on the NeoForge event bus: every detector in the blast's dimension records it. */
    public static void onDetonation(NuclearDetonationEvent event) {
        Set<SeismicDetectorBlockEntity> detectors = DETECTORS.get(event.getLevel());
        if (detectors == null) {
            return;
        }
        for (SeismicDetectorBlockEntity detector : new ArrayList<>(detectors)) {
            if (!detector.isRemoved()) {
                detector.record(event);
            }
        }
    }

    /** An earthquake: every detector in its dimension records it, as a blast of {@code power}. */
    public static void onEarthquake(Level level, Vec3 epicentre, float power) {
        Set<SeismicDetectorBlockEntity> detectors = DETECTORS.get(level);
        if (detectors == null) {
            return;
        }
        for (SeismicDetectorBlockEntity detector : new ArrayList<>(detectors)) {
            if (!detector.isRemoved()) {
                detector.record(epicentre, power, false);
            }
        }
    }
}
