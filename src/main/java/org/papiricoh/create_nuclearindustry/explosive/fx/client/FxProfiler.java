package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Times the explosion effects' drawing when the game is started with {@code -Dcreate_nuclearindustry.fxProfile=true}: every five
 * seconds it logs the average milliseconds a frame spent in it, how many frames that was and how many clouds were up. Costs
 * nothing when off.
 */
final class FxProfiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final boolean ON = Boolean.getBoolean("create_nuclearindustry.fxProfile");
    private static final long WINDOW = 5_000_000_000L;

    private static long started;
    private static long spent;
    private static long frames;
    private static long windowStart;
    private static long lastFrame = -1;

    private FxProfiler() {}

    static void begin() {
        if (ON) started = System.nanoTime();
    }

    /** Ends a timed stretch of frame {@code frame}. */
    static void end(long frame) {
        if (!ON) return;
        long now = System.nanoTime();
        spent += now - started;
        if (frame != lastFrame) {
            lastFrame = frame;
            frames++;
        }
        if (windowStart == 0) windowStart = now;
        if (now - windowStart >= WINDOW && frames > 0) {
            LOGGER.info("Explosion FX: {} ms a frame over {} frames ({} clouds, {} blasts)", String.format("%.3f", spent / 1e6 / frames), frames,
                    NuclearFx.nukes().size(), ConventionalFx.active());
            spent = 0;
            frames = 0;
            windowStart = now;
        }
    }
}
