package org.papiricoh.create_nuclearindustry.ruins;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the abandoned nuclear sites, under {@code ruins.*}. Each kind of ruin has its own
 * switch and a chance that a placement its structure set picks really gets one (the set's spacing
 * already makes them rare). Read while chunks generate, so a change only reaches chunks generated
 * afterwards. Defined on the shared builder by {@link org.papiricoh.create_nuclearindustry.Config}
 * and copied into public statics on load.
 */
public final class RuinsConfig {
    public static boolean powerPlants = true;
    public static double powerPlantChance = 1.0;
    /** Largest height difference, in blocks, between the ground samples under an abandoned plant. */
    public static int powerPlantMaxSlope = 4;
    public static boolean civilShelters = true;
    public static double civilShelterChance = 1.0;
    public static boolean militaryBunkers = true;
    public static double militaryBunkerChance = 1.0;

    private static ModConfigSpec.BooleanValue POWER_PLANTS;
    private static ModConfigSpec.DoubleValue POWER_PLANT_CHANCE;
    private static ModConfigSpec.IntValue POWER_PLANT_SLOPE;
    private static ModConfigSpec.BooleanValue CIVIL_SHELTERS;
    private static ModConfigSpec.DoubleValue CIVIL_SHELTER_CHANCE;
    private static ModConfigSpec.BooleanValue MILITARY_BUNKERS;
    private static ModConfigSpec.DoubleValue MILITARY_BUNKER_CHANCE;

    private RuinsConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        POWER_PLANTS = builder.comment("Abandoned nuclear power plants generate in flat biomes (only in newly generated chunks).")
                .define("ruins.powerPlants.enabled", true);
        POWER_PLANT_CHANCE = builder.comment("Chance that a place picked for an abandoned power plant really gets one.")
                .defineInRange("ruins.powerPlants.chance", 1.0, 0.0, 1.0);
        POWER_PLANT_SLOPE = builder.comment("Largest height difference, in blocks, between the ground samples under an abandoned power plant; rougher ground gets none.")
                .defineInRange("ruins.powerPlants.maxSlope", 4, 0, 64);
        CIVIL_SHELTERS = builder.comment("Abandoned civil fallout shelters generate (only in newly generated chunks).")
                .define("ruins.civilShelters.enabled", true);
        CIVIL_SHELTER_CHANCE = builder.comment("Chance that a place picked for a civil shelter really gets one.")
                .defineInRange("ruins.civilShelters.chance", 1.0, 0.0, 1.0);
        MILITARY_BUNKERS = builder.comment("Abandoned military bunkers generate (only in newly generated chunks).")
                .define("ruins.militaryBunkers.enabled", true);
        MILITARY_BUNKER_CHANCE = builder.comment("Chance that a place picked for a military bunker really gets one.")
                .defineInRange("ruins.militaryBunkers.chance", 1.0, 0.0, 1.0);
    }

    public static void bake() {
        powerPlants = POWER_PLANTS.get();
        powerPlantChance = POWER_PLANT_CHANCE.get();
        powerPlantMaxSlope = POWER_PLANT_SLOPE.get();
        civilShelters = CIVIL_SHELTERS.get();
        civilShelterChance = CIVIL_SHELTER_CHANCE.get();
        militaryBunkers = MILITARY_BUNKERS.get();
        militaryBunkerChance = MILITARY_BUNKER_CHANCE.get();
    }
}
