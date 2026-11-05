package org.papiricoh.create_nuclearindustry.dyson.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.ClientSwarm;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;

/**
 * How much the swarm dims the vanilla sun's disc: down to {@code 1 - MAX_DIM} as it fills ({@link ClientSwarm#coverage}). Only in
 * the Overworld's sky, with {@link ClientConfig#dysonDimSun} on, and never under a shader pack, which draws its own sun.
 */
public final class SunDimming {
    /** The most of the sun's light the swarm ever hides. */
    public static final float MAX_DIM = 0.35f;

    private SunDimming() {
    }

    /** What to scale the sun quad's alpha by in {@code level}: 1 for no change. */
    public static float factor(@Nullable ClientLevel level) {
        if (!ClientConfig.dysonDimSun || level == null || !ClientSwarm.shows(level) || RocketRenderTypes.shaderPackInUse()) return 1;
        return 1 - MAX_DIM * ClientSwarm.coverage();
    }
}
