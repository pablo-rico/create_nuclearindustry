package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The loaded seismic detectors of each level, so a detonation reaches every one of them in its
 * dimension without a block scan. Detectors join on load and leave on unload or removal.
 */
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
}
