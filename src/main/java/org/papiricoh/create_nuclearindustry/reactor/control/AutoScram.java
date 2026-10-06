package org.papiricoh.create_nuclearindustry.reactor.control;

import net.minecraft.nbt.CompoundTag;

/**
 * Temperature trip for the SCRAM signal. Once tripped it stays latched until reset, and a reset
 * is only accepted once the core has cooled {@link #RESET_MARGIN} below the threshold.
 * It never moves rods itself: it only raises the reactor's SCRAM request.
 */
public final class AutoScram {
    public static final int DEFAULT_THRESHOLD = 2800;
    public static final int MIN_THRESHOLD = 500;
    public static final int MAX_THRESHOLD = 3900;
    public static final int RESET_MARGIN = 200;

    private boolean enabled = true;
    private int threshold = DEFAULT_THRESHOLD;
    private boolean tripped;

    /** Trips when enabled and the core reaches the threshold. Returns true only on the tick it trips. */
    public boolean check(double coreTemperature) {
        if (!enabled || tripped || coreTemperature < threshold) {
            return false;
        }
        tripped = true;
        return true;
    }

    public boolean canReset(double coreTemperature) {
        return !tripped || coreTemperature < threshold - RESET_MARGIN;
    }

    /** Clears the trip if the core is cool enough. Returns whether the trip is now clear. */
    public boolean reset(double coreTemperature) {
        if (!canReset(coreTemperature)) {
            return false;
        }
        tripped = false;
        return true;
    }

    public void adjust(int delta) {
        threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold + delta));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getThreshold() {
        return threshold;
    }

    public boolean isTripped() {
        return tripped;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("autoScramEnabled", enabled);
        tag.putInt("autoScramThreshold", threshold);
        tag.putBoolean("autoScramTripped", tripped);
    }

    /** Older saves have none of these keys and get the defaults: enabled at 2800 C. */
    public void load(CompoundTag tag) {
        enabled = !tag.contains("autoScramEnabled") || tag.getBoolean("autoScramEnabled");
        threshold = tag.contains("autoScramThreshold") ? tag.getInt("autoScramThreshold") : DEFAULT_THRESHOLD;
        threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
        tripped = tag.getBoolean("autoScramTripped");
    }
}
