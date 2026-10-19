package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.ruins.RuinsTags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tags of the abandoned nuclear sites: the blocks their processors may never place or may knock out,
 * and the biomes each one generates in ({@link RuinsBiomeTagsProvider}). The structures themselves are
 * in {@link RuinsWorldgen} and the chests' loot in {@link RuinsChestLoot}.
 */
final class RuinsDataGen extends AreaDataGen {
    static final RuinsDataGen INSTANCE = new RuinsDataGen();

    /** Every block whose block entity owns a multiblock, and the bombs: a ruin holds none of them. */
    static final List<String> FORBIDDEN = List.of("reactor_controller", "cooling_tower_controller", "turbine_output",
            "fusion_controller", "fusion_plasma_turbine", "cryoplant_controller", "steam_generator_controller",
            "containment_monitor", "particle_accelerator_controller", "shelter_filtration_unit", "blast_door_controller",
            "nuclear_bomb", "dirty_bomb", "thermonuclear_bomb", "silo_controller");

    private RuinsDataGen() {}

    /** The ruins' signs, hung by tools/structgen/worldgen_*.py with translated text. */
    @Override
    void lang() {
        String s = "create_nuclearindustry.ruins.sign.";
        sign(s + "shelter", "FALLOUT", "SHELTER", "REFUGIO", "NUCLEAR");
        sign(s + "restricted", "RESTRICTED", "AREA", "ZONA", "RESTRINGIDA");
        sign(s + "radiation", "DANGER", "RADIATION", "PELIGRO", "RADIACIÓN");
        sign(s + "checkpoint", "PLANT ACCESS", "SHOW PASS", "ACCESO PLANTA", "MUESTRE PASE");
        sign(s + "control", "MAIN CONTROL", "ROOM", "SALA DE", "CONTROL");
        sign(s + "waste", "RADIOACTIVE", "WASTE", "RESIDUOS", "RADIACTIVOS");
        sign(s + "blast_door", "KEEP CLEAR", "BLAST DOOR", "NO OBSTRUIR", "PUERTA BLINDADA");
    }

    private static void sign(String key, String english1, String english2, String spanish1, String spanish2) {
        LangEntries.add(key + ".1", english1, spanish1);
        LangEntries.add(key + ".2", english2, spanish2);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(RuinsTags.RUIN_FORBIDDEN).add(FORBIDDEN.stream().map(RuinsDataGen::block).toArray(Block[]::new));
        provider.tagFor(RuinsTags.RUIN_ROTTABLE).add(
                Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_STONE,
                Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.POLISHED_ANDESITE,
                Blocks.ANDESITE, Blocks.BRICKS, Blocks.IRON_BARS, Blocks.GLASS_PANE, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS,
                Blocks.SPRUCE_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.CHAIN);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new RuinsBiomeTagsProvider(output, lookup, existingFiles));
    }

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id));
        if (block == Blocks.AIR) {
            throw new IllegalStateException("ruin_forbidden names a missing block: " + id);
        }
        return block;
    }
}
