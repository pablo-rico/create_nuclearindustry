package org.papiricoh.create_nuclearindustry.ruins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;

/**
 * The chests' loot tables of the abandoned sites and every structure template they are built from.
 * The template names must match what {@code tools/structgen/worldgen_*.py} writes under
 * {@code structure/ruins/}; a GameTest checks both ways.
 */
public final class RuinsLoot {
    public static final String PLANT = "abandoned_power_plant";
    public static final String SHELTER = "civil_shelter";
    public static final String BUNKER = "military_bunker";

    public static final ResourceKey<LootTable> PLANT_REACTOR = chest(PLANT, "reactor_hall");
    public static final ResourceKey<LootTable> PLANT_TURBINE = chest(PLANT, "turbine_hall");
    public static final ResourceKey<LootTable> PLANT_CONTROL = chest(PLANT, "control_building");
    public static final ResourceKey<LootTable> PLANT_WASTE = chest(PLANT, "waste_yard");
    public static final ResourceKey<LootTable> SHELTER_DORMITORY = chest(SHELTER, "dormitory");
    public static final ResourceKey<LootTable> SHELTER_SUPPLIES = chest(SHELTER, "supplies");
    public static final ResourceKey<LootTable> BUNKER_GUARD = chest(BUNKER, "guard_house");
    public static final ResourceKey<LootTable> BUNKER_COMMAND = chest(BUNKER, "command");
    public static final ResourceKey<LootTable> BUNKER_DORMITORY = chest(BUNKER, "dormitory");
    public static final ResourceKey<LootTable> BUNKER_ARMORY = chest(BUNKER, "armory");
    public static final ResourceKey<LootTable> BUNKER_LAB = chest(BUNKER, "lab");
    public static final ResourceKey<LootTable> BUNKER_WASTE = chest(BUNKER, "waste_store");

    public static final List<ResourceKey<LootTable>> TABLES = List.of(PLANT_REACTOR, PLANT_TURBINE, PLANT_CONTROL,
            PLANT_WASTE, SHELTER_DORMITORY, SHELTER_SUPPLIES, BUNKER_GUARD, BUNKER_COMMAND, BUNKER_DORMITORY,
            BUNKER_ARMORY, BUNKER_LAB, BUNKER_WASTE);

    /** Every template, as {@code <structure>/<piece>} under {@code create_nuclearindustry:ruins/}. */
    public static final List<String> PIECES = List.of(
            PLANT + "/reactor_hall", PLANT + "/turbine_hall", PLANT + "/control_building", PLANT + "/waste_yard",
            PLANT + "/cooling_tower_stub", PLANT + "/road_straight", PLANT + "/road_checkpoint", PLANT + "/road_end",
            SHELTER + "/entrance", SHELTER + "/stair_corridor", SHELTER + "/dormitory", SHELTER + "/supplies",
            SHELTER + "/vent_room", SHELTER + "/end_wall",
            BUNKER + "/guard_house", BUNKER + "/shaft_segment", BUNKER + "/shaft_bottom", BUNKER + "/corridor_straight",
            BUNKER + "/corridor_cross", BUNKER + "/corridor_tee", BUNKER + "/corridor_corner", BUNKER + "/corridor_blast_door",
            BUNKER + "/command", BUNKER + "/dormitory", BUNKER + "/armory", BUNKER + "/filtration", BUNKER + "/waste_store",
            BUNKER + "/lab", BUNKER + "/collapsed_end");

    private RuinsLoot() {}

    public static ResourceLocation template(String piece) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ruins/" + piece);
    }

    private static ResourceKey<LootTable> chest(String structure, String room) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "chests/" + structure + "/" + room));
    }
}
