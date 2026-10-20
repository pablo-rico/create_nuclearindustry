package org.papiricoh.create_nuclearindustry.missile.terrain;

/** What the land of a terrain sample is, as the holograms tint it; pure. Stored in the low bits of a sample's surface byte. */
public enum TerrainKind {
    OTHER, PLAINS, FOREST, JUNGLE, SWAMP, DESERT, BADLANDS, SNOW, MOUNTAIN, BEACH, RIVER, OCEAN, DEEP_OCEAN, MUSHROOM;

    private static final TerrainKind[] VALUES = values();

    public static TerrainKind of(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OTHER;
    }
}
