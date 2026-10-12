package org.papiricoh.create_nuclearindustry.reactor.control;

import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;

/**
 * Temperature trip for the SCRAM signal. Once tripped it stays latched until reset, and a reset
 * is only accepted once the core has cooled {@link #RESET_MARGIN} below the threshold.
 * It never moves rods itself: it only raises the reactor's SCRAM request. The default sits just
 * under the cladding failure temperature (1500 °C), so an untouched reactor trips before it damages itself.
 */
public final class AutoScram {
    public static final int DEFAULT_THRESHOLD = 1400;
    public static final int MIN_THRESHOLD = 500;
    public static final int MAX_THRESHOLD = 3900;
    public static final int RESET_MARGIN = 200;
    /** The default before the cladding failed at 1500 C: saves holding exactly this are moved to the new default. */
    private static final int OLD_DEFAULT_THRESHOLD = 2800;
    private static final int SAVE_VERSION = 2;

    private boolean enabled = true;
    private int threshold = defaultThreshold();
    private boolean tripped;

    /** The default threshold: 1400 C, or 100 C under a cladding failure temperature set lower in the config. */
    public static int defaultThreshold() {
        int underDamage = (int) Math.round(FissionConfig.warningTemperature()) - 100;
        return Math.max(MIN_THRESHOLD, Math.min(DEFAULT_THRESHOLD, underDamage));
    }

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
        tag.putInt("autoScramVersion", SAVE_VERSION);
        tag.putBoolean("autoScramEnabled", enabled);
        tag.putInt("autoScramThreshold", threshold);
        tag.putBoolean("autoScramTripped", tripped);
    }

    /**
     * Older saves have none of these keys and get the defaults. A reactor saved before the cladding
     * failed at 1500 C, still on the old 2800 C default (where the fuel now melts), takes the new default;
     * a threshold its operator set is kept.
     */
    public void load(CompoundTag tag) {
        enabled = !tag.contains("autoScramEnabled") || tag.getBoolean("autoScramEnabled");
        threshold = tag.contains("autoScramThreshold") ? tag.getInt("autoScramThreshold") : defaultThreshold();
        if (tag.getInt("autoScramVersion") < SAVE_VERSION && threshold == OLD_DEFAULT_THRESHOLD) {
            threshold = defaultThreshold();
        }
        threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
        tripped = tag.getBoolean("autoScramTripped");
    }
}
