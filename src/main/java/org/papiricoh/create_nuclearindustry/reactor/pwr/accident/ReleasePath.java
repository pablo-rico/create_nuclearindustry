package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

/**
 * Where radioactivity from a damaged core goes. A sealed containment keeps it; a filtered vent
 * lets out what passes its filters (the activity over {@link PwrConfig#ventDecontamination}); a
 * burst containment, a leaking building or no containment at all lets everything out as fallout.
 * Activity is measured so that {@link #CORE_REFERENCE} is the volatile inventory of a 16-rod core,
 * which falls out as the old meltdown did: 20 rad/s at the release point over 48 blocks.
 */
public enum ReleasePath {
    CONTAINED,
    FILTERED,
    UNFILTERED;

    /** Airborne activity of a whole 16-rod core. */
    public static final double CORE_REFERENCE = 100.0;
    /** Activity each melted fuel rod releases. */
    public static final double ACTIVITY_PER_ROD = CORE_REFERENCE / 16.0;
    private static final float REFERENCE_INTENSITY = 20.0f;
    private static final int REFERENCE_RADIUS = 48;
    private static final double SMALLEST = 1.0e-4;

    /** Lays the fallout of {@code activity} released at {@code at} along this path. */
    public void release(ServerLevel level, BlockPos at, double activity) {
        if (this == CONTAINED || !(activity > SMALLEST)) {
            return;
        }
        double released = this == FILTERED ? activity / Math.max(1.0, PwrConfig.ventDecontamination) : activity;
        if (released <= SMALLEST) {
            return;
        }
        double share = released / CORE_REFERENCE;
        int radius = this == FILTERED
                ? clamp((int) Math.round(6 + 10 * Math.sqrt(share)), 4, 16)
                : clamp((int) Math.round(REFERENCE_RADIUS * Math.sqrt(share)), 8, 64);
        float intensity = (float) Math.min(2.0 * REFERENCE_INTENSITY, REFERENCE_INTENSITY * share);
        Contamination.fallout(level, at, radius, intensity);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
