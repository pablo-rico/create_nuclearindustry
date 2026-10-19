package org.papiricoh.create_nuclearindustry.missile;

import java.util.List;
import java.util.Locale;

/**
 * The limits a server puts on missile launches ({@code missile.*} in {@link MissileConfig}), worked out
 * without the game so they can be tested on their own.
 */
public final class LaunchRules {
    /** Why a launch pad will not launch. Synced by ordinal: new reasons go at the end. */
    public enum Refusal {
        NONE,
        /** Not in one of {@code missile.allowedDimensions}. */
        DIMENSION,
        /** Further than {@code missile.maxRange}. */
        OUT_OF_RANGE,
        /** Within {@code missile.protectedSpawnRadius} of the world spawn. */
        SPAWN_PROTECTED,
        /** Its player lacks {@code missile.launchPermissionLevel}. */
        NO_PERMISSION,
        /** Its player launched less than {@code missile.launchCooldownSeconds} ago. */
        COOLDOWN;

        private static final Refusal[] VALUES = values();

        public static Refusal byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
        }

        /** The lang key that says why. */
        public String key() {
            return "gui.create_nuclearindustry.launch_pad.refused." + name().toLowerCase(Locale.ROOT);
        }
    }

    private LaunchRules() {}

    /** Whether launch pads work in {@code dimension}: every one does while {@code allowed} is empty. */
    public static boolean dimensionAllowed(String dimension, List<String> allowed) {
        return allowed.isEmpty() || allowed.contains(dimension);
    }

    /** Whether a target is further (horizontally) than {@code maxRange}; 0 sets no limit. */
    public static boolean outOfRange(double fromX, double fromZ, double toX, double toZ, double maxRange) {
        return maxRange > 0 && Math.hypot(toX - fromX, toZ - fromZ) > maxRange;
    }

    /** Whether a point lies within {@code radius} blocks (horizontally) of the spawn; 0 protects nothing. */
    public static boolean nearSpawn(double x, double z, double spawnX, double spawnZ, int radius) {
        double dx = x - spawnX;
        double dz = z - spawnZ;
        return radius > 0 && dx * dx + dz * dz <= (double) radius * radius;
    }

    /**
     * Ticks still to wait after a launch at game time {@code lastLaunch} (negative for none) before the
     * next, at game time {@code now}, with a cooldown of {@code cooldownSeconds}.
     */
    public static long cooldownTicksLeft(long lastLaunch, long now, int cooldownSeconds) {
        if (lastLaunch < 0 || cooldownSeconds <= 0) {
            return 0;
        }
        return Math.max(0, lastLaunch + cooldownSeconds * 20L - now);
    }

    /**
     * How far from where a missile comes down players are warned of it with
     * {@link LaunchBroadcast#IMPACT_AREA}: twice the reach of its warning to sirens, and never under
     * {@code floor} blocks.
     */
    public static int warningRadius(int threatRadius, int floor) {
        return Math.max(2 * threatRadius, floor);
    }
}
