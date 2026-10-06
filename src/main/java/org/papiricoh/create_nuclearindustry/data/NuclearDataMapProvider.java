package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

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
    }

    private static void add(Builder<Radioactivity, Item> builder, Supplier<? extends Item> item, float perSecond, boolean scaled) {
        builder.add(item.get().builtInRegistryHolder(), new Radioactivity(perSecond, scaled), false);
    }
}
