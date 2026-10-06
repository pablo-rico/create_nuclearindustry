package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** The radiation emitters loaded in each level. Server side only. */
public final class RadiationEmitters {
    private static final Map<Level, Set<RadiationEmitter>> EMITTERS = new WeakHashMap<>();

    private RadiationEmitters() {}

    public static void register(RadiationEmitter emitter) {
        Level level = emitter.getLevel();
        if (level != null && !level.isClientSide) {
            EMITTERS.computeIfAbsent(level, key -> Collections.newSetFromMap(new java.util.IdentityHashMap<>())).add(emitter);
        }
    }

    public static void unregister(RadiationEmitter emitter) {
        Level level = emitter.getLevel();
        if (level != null) {
            Set<RadiationEmitter> set = EMITTERS.get(level);
            if (set != null) {
                set.remove(emitter);
            }
        }
    }

    /** Active emitters within {@code range} of {@code at}, nearest first, at most {@code limit}. */
    public static List<RadiationEmitter> near(Level level, Vec3 at, double range, int limit) {
        Set<RadiationEmitter> set = EMITTERS.get(level);
        if (set == null || set.isEmpty()) {
            return List.of();
        }
        List<RadiationEmitter> found = new ArrayList<>();
        double rangeSqr = range * range;
        set.removeIf(RadiationEmitter::isRemoved);
        for (RadiationEmitter emitter : set) {
            if (emitter.radiationStrength() > 0 && emitter.radiationOrigin().distanceToSqr(at) <= rangeSqr) {
                found.add(emitter);
            }
        }
        found.sort(Comparator.comparingDouble(emitter -> emitter.radiationOrigin().distanceToSqr(at)));
        return found.size() > limit ? found.subList(0, limit) : found;
    }
}
