package org.papiricoh.create_nuclearindustry.propulsion;

import org.papiricoh.create_nuclearindustry.propulsion.blockentity.RocketEngineBlockEntity;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.ArrayList;

/**
 * The nuclear engines loaded now, server side, for whatever pushes the ship they are on: Sable's
 * physics step asks each for its thrust (see {@code integration.sable.SableShips}).
 */
public final class RocketEngines {
    private static final Set<RocketEngineBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private RocketEngines() {}

    public static synchronized void add(RocketEngineBlockEntity engine) {
        LOADED.add(engine);
    }

    public static synchronized void remove(RocketEngineBlockEntity engine) {
        LOADED.remove(engine);
    }

    /** The engines firing now. */
    public static synchronized List<RocketEngineBlockEntity> firing() {
        List<RocketEngineBlockEntity> firing = new ArrayList<>();
        for (RocketEngineBlockEntity engine : LOADED) {
            // A copy left behind by a ship's assembly is no longer the block entity at its place.
            if (!engine.isRemoved() && engine.thrust() > 0 && engine.getLevel() != null
                    && engine.getLevel().getBlockEntity(engine.getBlockPos()) == engine) {
                firing.add(engine);
            }
        }
        return firing;
    }
}
