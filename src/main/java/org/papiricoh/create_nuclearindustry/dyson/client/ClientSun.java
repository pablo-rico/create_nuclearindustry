package org.papiricoh.create_nuclearindustry.dyson.client;

import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SunPath;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;

/**
 * Where this client sees the sun: the game's path, tipped by the shader pack's {@code sunPathRotation} when one is on, so the
 * swarm, the beams and the machines' aim meet the sun the pack draws rather than the one it hides. Gameplay keeps the game's sun
 * ({@link SunPath}); only the drawing follows the pack.
 */
public final class ClientSun {
    private ClientSun() {
    }

    /** The unit direction to the sun as drawn in {@code level}, {@code partial} ticks into this one, into {@code out}. */
    public static void direction(Level level, float partial, double[] out) {
        SunPath.direction(level.getTimeOfDay(partial), RocketRenderTypes.sunPathRotation(), out);
    }
}
