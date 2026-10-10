package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The JEI information pages of the power plant areas (pressurized water reactor, spent fuel, waste,
 * control room instruments and civil defense): which stacks and fluids each page is shown for.
 * Free of JEI classes, so a GameTest can check every page has its text and every item has a page.
 */
public final class PlantJeiInfo {
    public static final String PREFIX = "jei.create_nuclearindustry.info.";

    /**
     * One page: its text under {@code PREFIX + id}, shown for these stacks and fluids.
     */
    public record Page(String id, List<ItemStack> items, List<FluidStack> fluids) {
        public String key() {
            return PREFIX + id;
        }
    }

    private PlantJeiInfo() {}

    /** Built on demand: the items must already be registered. */
    public static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        // Pressurized water reactor
        pages.add(new Page("pressurizer", stacks(PwrRegistry.PRESSURIZER_ITEM, PwrRegistry.PRESSURIZER_HEATER_PORT_ITEM,
                PwrRegistry.RELIEF_VALVE_ITEM, AllNuclearFluids.PRIMARY_COOLANT_BUCKET),
                fluids(AllNuclearFluids.PRIMARY_COOLANT, AllNuclearFluids.HOT_PRIMARY_COOLANT)));
        pages.add(items("steam_generator", PwrRegistry.STEAM_GENERATOR_CONTROLLER_ITEM, PwrRegistry.STEAM_GENERATOR_CASING_ITEM,
                PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE_ITEM, PwrRegistry.STEAM_GENERATOR_PORT_ITEM));
        pages.add(items("containment", PwrRegistry.CONTAINMENT_MONITOR_ITEM, PwrRegistry.CONTAINMENT_CONCRETE_ITEM,
                PwrRegistry.STEEL_LINER_ITEM, PwrRegistry.CONTAINMENT_PENETRATION_ITEM, PwrRegistry.RECOMBINER_ITEM,
                PwrRegistry.FILTERED_VENT_ITEM));
        pages.add(items("corium", PwrRegistry.CORE_CATCHER_ITEM, PwrRegistry.CORIUM_ITEM, PwrRegistry.SOLIDIFIED_CORIUM_ITEM));

        // Spent fuel: every enrichment the crushing recipes show, so the page follows the fuel there.
        List<ItemStack> spent = new ArrayList<>(stacks(SpentFuelRegistry.SPENT_FUEL_RACK_ITEM, SpentFuelRegistry.POOL_COOLER_ITEM,
                SpentFuelRegistry.DRY_CASK_ITEM, AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL));
        for (EnrichmentTier tier : List.of(EnrichmentTier.LOW, EnrichmentTier.HIGH)) {
            spent.add(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()), tier.nominal()));
        }
        spent.add(new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get()));
        pages.add(new Page("spent_fuel", spent, List.of()));

        // Waste
        pages.add(items("waste_classes", WasteRegistry.LOW_LEVEL_WASTE, AllNuclearItems.RADIOACTIVE_WASTE, WasteRegistry.HIGH_LEVEL_WASTE));
        pages.add(items("vitrification_melter", WasteRegistry.VITRIFICATION_MELTER_ITEM));
        pages.add(items("waste_storage", AllNuclearItems.RADIOACTIVE_WASTE_BARREL, WasteRegistry.COMPACTED_WASTE_DRUM_ITEM,
                WasteRegistry.CEMENTED_WASTE_DRUM_ITEM, WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM, WasteRegistry.BENTONITE_ITEM,
                WasteRegistry.REPOSITORY_INSPECTOR_ITEM));

        // Control room instruments
        pages.add(items("chart_recorder", ControlRoom2Registry.CHART_RECORDER_ITEM, ControlRoom2Registry.CHART_ROLL));
        pages.add(items("annunciator_panel", ControlRoom2Registry.ANNUNCIATOR_PANEL_ITEM));
        pages.add(items("controller_module", AllNuclearItems.CONTROL_MODULES.get(ModuleType.CONTROLLER)));

        // Civil defense
        pages.add(items("sirens", CivilDefenseRegistry.AIR_RAID_SIREN_ITEM, CivilDefenseRegistry.HAND_CRANK_SIREN_ITEM));
        pages.add(items("seismic_detector", CivilDefenseRegistry.SEISMIC_DETECTOR_ITEM));
        pages.add(items("blast_door", CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_ITEM, CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM,
                CivilDefenseRegistry.STEEL_BLAST_DOOR_ITEM, CivilDefenseRegistry.LEAD_BLAST_DOOR_ITEM));
        pages.add(items("shelter", CivilDefenseRegistry.SHELTER_FILTRATION_UNIT_ITEM));

        // Cryogenic plant and the tokamak's vacuum
        pages.add(new Page("cryoplant", stacks(CryoRegistry.CRYOPLANT_CONTROLLER_ITEM, CryoRegistry.CRYOPLANT_CASING_ITEM,
                CryoRegistry.CRYOPLANT_PORT_ITEM, CryoRegistry.CRYOPLANT_COMPRESSOR_ITEM),
                fluids(AllNuclearFluids.LIQUID_AIR, AllNuclearFluids.LIQUID_NITROGEN)));
        pages.add(new Page("tokamak_cryo_port", stacks(CryoRegistry.FUSION_CRYO_PORT_ITEM),
                fluids(AllNuclearFluids.LIQUID_HELIUM, AllNuclearFluids.HELIUM)));
        pages.add(items("tokamak_vacuum_pump", CryoRegistry.FUSION_VACUUM_PUMP_ITEM));
        return pages;
    }

    @SafeVarargs
    private static Page items(String id, Supplier<? extends Item>... items) {
        return new Page(id, stacks(items), List.of());
    }

    @SafeVarargs
    private static List<ItemStack> stacks(Supplier<? extends Item>... items) {
        return Stream.of(items).map(item -> new ItemStack(item.get())).toList();
    }

    @SafeVarargs
    private static List<FluidStack> fluids(Supplier<? extends Fluid>... fluids) {
        return Stream.of(fluids).map(fluid -> new FluidStack(fluid.get(), 1000)).toList();
    }
}
