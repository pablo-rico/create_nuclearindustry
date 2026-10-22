package org.papiricoh.create_nuclearindustry.radiation;

/**
 * What the last radiation update of a player (or reading of a monitored spot) cost: blocks looked
 * at, chunk sections scanned or skipped, rays cast or reused, emitter chunks and fallout sources
 * searched. Counters for tests and benchmarks,
 * which read them straight after {@link RadiationHandler#update} or
 * {@link RadiationHandler#exposureRate}; server thread only.
 */
public final class RadiationStats {
    private static RadiationStats current = new RadiationStats();

    /** Block states read by the scan for radioactive blocks. */
    public int blockLookups;
    /** Chunk sections around the player that might hold a radioactive block and were read. */
    public int sectionsScanned;
    /** Chunk sections skipped because their palette holds nothing radioactive. */
    public int sectionsSkipped;
    /** Rays walked through the world. */
    public int raysCast;
    /** Rays answered from the player's cache. */
    public int raysReused;
    /** Chunks of the emitter index searched. */
    public int emitterChunks;
    /** Fallout sources looked at (those in the chunks around the body). */
    public int falloutSources;
    /** Whole readings of a monitored spot answered from the last one, without any scan or ray. */
    public int spotReadingsReused;
    /** A player's blocks and fallout answered from their last update, without a scan or their rays. */
    public int surroundingsReused;

    private RadiationStats() {}

    /** Starts counting a new update. */
    static RadiationStats begin() {
        current = new RadiationStats();
        return current;
    }

    static RadiationStats current() {
        return current;
    }

    /** The counters of the last update. */
    public static RadiationStats last() {
        return current;
    }

    @Override
    public String toString() {
        return "blocks " + blockLookups + ", sections " + sectionsScanned + " read / " + sectionsSkipped + " skipped, rays "
                + raysCast + " cast / " + raysReused + " reused, emitter chunks " + emitterChunks + ", fallout sources " + falloutSources
                + (spotReadingsReused > 0 ? ", spot reading reused" : "") + (surroundingsReused > 0 ? ", surroundings reused" : "");
    }
}
