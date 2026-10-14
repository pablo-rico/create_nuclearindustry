package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * Deterministic noise for the blast's choices (which block wrecks, which ember lands), so a crater dug
 * in a chunk that loaded late looks as it would have, and tests can count on it. No game classes.
 */
public final class BlastHash {
    private BlastHash() {}

    /** A value in [0, 1) for a column and a purpose. */
    public static double unit(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** As {@link #unit(int, int, int)} for a block, with its height. */
    public static double unit(int x, int y, int z, int salt) {
        return unit(x * 31 + y, z * 17 - y, salt);
    }
}
