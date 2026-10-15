package org.papiricoh.create_nuclearindustry.ruins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Tags of the abandoned nuclear sites: where each one generates, and what their processors touch. */
public final class RuinsTags {
    public static final TagKey<Biome> HAS_ABANDONED_POWER_PLANT = biome("has_structure/abandoned_power_plant");
    public static final TagKey<Biome> HAS_CIVIL_SHELTER = biome("has_structure/civil_shelter");
    public static final TagKey<Biome> HAS_MILITARY_BUNKER = biome("has_structure/military_bunker");

    /**
     * Blocks a ruin must never hold: every multiblock controller and the bombs. The templates never
     * place them; the ruins' processors still turn any into plain reactor casing, as a safety net.
     */
    public static final TagKey<Block> RUIN_FORBIDDEN = block("ruin_forbidden");
    /** What decay may knock out of a ruined plant: walls, floors, panes. Never containers or waste. */
    public static final TagKey<Block> RUIN_ROTTABLE = block("ruin_rottable");

    private RuinsTags() {}

    private static TagKey<Biome> biome(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }
}
