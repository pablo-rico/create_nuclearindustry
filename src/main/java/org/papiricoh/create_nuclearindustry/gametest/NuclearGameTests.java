package org.papiricoh.create_nuclearindustry.gametest;

/**
 * Shared constants for the mod's GameTests. Arenas are empty templates written by
 * {@code tools/structgen/empty_template.py}; every multiblock is built in code.
 */
public final class NuclearGameTests {
    public static final String SMALL_ARENA = "gametest/empty_13x16x13";
    public static final String TOKAMAK_ARENA = "gametest/empty_41x9x41";
    public static final String LONG_ARENA = "gametest/empty_11x10x20";
    public static final String TOWER_ARENA = "gametest/empty_19x26x19";
    /** Room for a reactor, its pressurizer, pumps, a steam generator and a containment around them. */
    public static final String PLANT_ARENA = "gametest/empty_25x18x25";
    /** Flat and wide, for blast waves, sirens and shelters. */
    public static final String BLAST_ARENA = "gametest/empty_41x9x41";

    private NuclearGameTests() {}
}
