package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.radiation.RadiationShielding;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Dose per second per carried item. Enriched uranium is given at weapons grade and scaled by
 * enrichment, so natural material barely registers while spent fuel and waste are the real hazard.
 */
public class NuclearDataMapProvider extends DataMapProvider {

    public NuclearDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather() {
        var radioactivity = builder(AllNuclearRadiation.RADIOACTIVITY);
        add(radioactivity, AllNuclearItems.RAW_URANIUM, 0.01f, false);
        add(radioactivity, AllNuclearItems.CRUSHED_RAW_URANIUM, 0.01f, false);
        add(radioactivity, AllNuclearItems.URANIUM_CONCENTRATE, 0.02f, false);
        add(radioactivity, AllNuclearItems.YELLOWCAKE, 0.03f, false);
        add(radioactivity, AllNuclearItems.URANIUM, 1.5f, true);
        add(radioactivity, AllNuclearItems.URANIUM_DIOXIDE, 1.5f, true);
        add(radioactivity, AllNuclearItems.URANIUM_FUEL_PELLET, 1.5f, true);
        add(radioactivity, AllNuclearItems.URANIUM_REACTOR_FUEL, 3.0f, true);
        add(radioactivity, AllNuclearItems.ENRICHED_URANIUM_BLEND, 0.3f, false);
        add(radioactivity, AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE, 12.0f, false);
        add(radioactivity, AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL, 4.0f, false);
        add(radioactivity, AllNuclearItems.RADIOACTIVE_WASTE, 6.0f, false);
        add(radioactivity, AllNuclearItems.RADIOACTIVE_WASTE_BARREL, 0.1f, false);
        add(radioactivity, AllNuclearItems.TRITIUM_CELL, 0.05f, false);
        add(radioactivity, AllNuclearItems.DT_FUEL_PELLET, 0.05f, false);
        add(radioactivity, AllNuclearItems.SPENT_DT_PELLET, 0.2f, false);
        add(radioactivity, AllNuclearItems.SPENT_FUEL_FRAGMENTS, 3.0f, false);
        add(radioactivity, AllNuclearItems.URANYL_NITRATE, 1.5f, true);
        // Plutonium is far hotter than uranium, in dose and in decay heat.
        radioactivity.add(AllNuclearItems.PLUTONIUM_DIOXIDE.get().builtInRegistryHolder(), new Radioactivity(6.0f, false, 0.75f), false);
        radioactivity.add(AllNuclearItems.PLUTONIUM.get().builtInRegistryHolder(), new Radioactivity(8.0f, false, 1.0f), false);
        radioactivity.add(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get().builtInRegistryHolder(), new Radioactivity(25.0f, false, 4.0f), false);

        // Fraction of radiation one block lets through. Unlisted solid blocks pass 90% (config).
        var transmission = builder(AllNuclearRadiation.TRANSMISSION);
        transmission.add(BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead")),
                new RadiationTransmission(0.2f), false);
        for (Supplier<? extends Block> part : List.<Supplier<? extends Block>>of(AllNuclearBlocks.REACTOR_CASING,
                AllNuclearBlocks.REACTOR_CONTROLLER, AllNuclearBlocks.REACTOR_FLUID_PORT, AllNuclearBlocks.REACTOR_FUEL_PORT,
                AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR, AllNuclearBlocks.REACTOR_REDSTONE_PORT)) {
            transmission.add(part.get().builtInRegistryHolder(), new RadiationTransmission(0.35f), false);
        }
        transmission.add(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get().builtInRegistryHolder(), new RadiationTransmission(0.1f), false);
        transmission.add(AllNuclearBlocks.COOLING_TOWER_CASING.get().builtInRegistryHolder(), new RadiationTransmission(0.6f), false);
        transmission.add(Blocks.IRON_BLOCK.builtInRegistryHolder(), new RadiationTransmission(0.5f), false);
        transmission.add(Blocks.OBSIDIAN.builtInRegistryHolder(), new RadiationTransmission(0.6f), false);
        transmission.add(Blocks.CRYING_OBSIDIAN.builtInRegistryHolder(), new RadiationTransmission(0.6f), false);
        transmission.add(Blocks.WATER.builtInRegistryHolder(), new RadiationTransmission(0.85f), false);

        // Fraction of radiation one worn piece stops. A full hazmat suit stops 95%.
        var shielding = builder(AllNuclearRadiation.SHIELDING);
        shield(shielding, AllNuclearItems.HAZMAT_HELMET, 0.25f);
        shield(shielding, AllNuclearItems.HAZMAT_CHESTPLATE, 0.30f);
        shield(shielding, AllNuclearItems.HAZMAT_LEGGINGS, 0.25f);
        shield(shielding, AllNuclearItems.HAZMAT_BOOTS, 0.15f);
        // Depleted uranium plate: a full set reaches the 98% cap.
        shield(shielding, AllNuclearItems.DEPLETED_URANIUM_HELMET, 0.25f);
        shield(shielding, AllNuclearItems.DEPLETED_URANIUM_CHESTPLATE, 0.35f);
        shield(shielding, AllNuclearItems.DEPLETED_URANIUM_LEGGINGS, 0.28f);
        shield(shielding, AllNuclearItems.DEPLETED_URANIUM_BOOTS, 0.12f);
    }

    private static void shield(Builder<RadiationShielding, Item> builder, Supplier<? extends Item> item, float fraction) {
        builder.add(item.get().builtInRegistryHolder(), new RadiationShielding(fraction), false);
    }

    private static void add(Builder<Radioactivity, Item> builder, Supplier<? extends Item> item, float perSecond, boolean scaled) {
        builder.add(item.get().builtInRegistryHolder(), new Radioactivity(perSecond, scaled), false);
    }
}
