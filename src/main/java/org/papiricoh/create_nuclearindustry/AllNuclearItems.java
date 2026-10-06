package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;

public class AllNuclearItems {

    // Create a Deferred Register to hold Items which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Create_NuclearIndustry.MODID);

    //Blocks

    // Creates a new BlockItem with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final DeferredHolder<Item, ? extends Item> URANIUM_ORE = ITEMS.register("uranium_ore", () -> new BlockItem(AllNuclearBlocks.URANIUM_ORE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> BORAX_ORE = ITEMS.register("borax_ore", () -> new BlockItem(AllNuclearBlocks.BORAX_ORE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> SPODUMENE_ORE = ITEMS.register("spodumene_ore", () -> new BlockItem(AllNuclearBlocks.SPODUMENE_ORE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> CONTROL_ROD = ITEMS.register("control_rod", () -> new BlockItem(AllNuclearBlocks.CONTROL_ROD.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> URANIUM_ROD = ITEMS.register("uranium_rod", () -> new BlockItem(AllNuclearBlocks.URANIUM_ROD.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> HEAT_EXCHANGER = ITEMS.register("heat_exchanger", () -> new BlockItem(AllNuclearBlocks.HEAT_EXCHANGER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_CASING = ITEMS.register("reactor_casing", () -> new BlockItem(AllNuclearBlocks.REACTOR_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_CONTROLLER = ITEMS.register("reactor_controller", () -> new BlockItem(AllNuclearBlocks.REACTOR_CONTROLLER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_FLUID_PORT = ITEMS.register("reactor_fluid_port", () -> new BlockItem(AllNuclearBlocks.REACTOR_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_FUEL_PORT = ITEMS.register("reactor_fuel_port", () -> new BlockItem(AllNuclearBlocks.REACTOR_FUEL_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_TEMPERATURE_SENSOR = ITEMS.register("reactor_temperature_sensor", () -> new BlockItem(AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_REDSTONE_PORT = ITEMS.register("reactor_redstone_port", () -> new BlockItem(AllNuclearBlocks.REACTOR_REDSTONE_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> COOLING_TOWER_CASING = ITEMS.register("cooling_tower_casing", () -> new BlockItem(AllNuclearBlocks.COOLING_TOWER_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> COOLING_TOWER_CONTROLLER = ITEMS.register("cooling_tower_controller", () -> new BlockItem(AllNuclearBlocks.COOLING_TOWER_CONTROLLER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> COOLING_TOWER_FLUID_PORT = ITEMS.register("cooling_tower_fluid_port", () -> new BlockItem(AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> COOLING_TOWER_FAN = ITEMS.register("cooling_tower_fan", () -> new BlockItem(AllNuclearBlocks.COOLING_TOWER_FAN.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> DUAL_FLUID_PIPE = ITEMS.register("dual_fluid_pipe", () -> new BlockItem(AllNuclearBlocks.DUAL_FLUID_PIPE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> CENTRIFUGE = ITEMS.register("centrifuge", () -> new BlockItem(AllNuclearBlocks.CENTRIFUGE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> ELECTROLYZER = ITEMS.register("electrolyzer", () -> new BlockItem(AllNuclearBlocks.ELECTROLYZER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> RADIOACTIVE_WASTE_BARREL = ITEMS.register("radioactive_waste_barrel", () -> new BlockItem(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REINFORCED_TURBINE_GLASS = ITEMS.register("reinforced_turbine_glass", () -> new BlockItem(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_CASING = ITEMS.register("turbine_casing", () -> new BlockItem(AllNuclearBlocks.TURBINE_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_ROTOR = ITEMS.register("turbine_rotor", () -> new BlockItem(AllNuclearBlocks.TURBINE_ROTOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_OUTPUT = ITEMS.register("turbine_output", () -> new BlockItem(AllNuclearBlocks.TURBINE_OUTPUT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_FLUID_PORT = ITEMS.register("turbine_fluid_port", () -> new BlockItem(AllNuclearBlocks.TURBINE_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> NUCLEAR_BOMB = ITEMS.register("nuclear_bomb", () -> new BlockItem(AllNuclearBlocks.NUCLEAR_BOMB.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> LAUNCH_PAD = ITEMS.register("launch_pad", () -> new BlockItem(AllNuclearBlocks.LAUNCH_PAD.value(), new Item.Properties()));

    // Fusion reactor blocks
    public static final DeferredHolder<Item, ? extends Item> FUSION_CONTROLLER = ITEMS.register("fusion_controller", () -> new BlockItem(AllNuclearBlocks.FUSION_CONTROLLER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_ACCELERATOR_SEGMENT = ITEMS.register("fusion_accelerator_segment", () -> new BlockItem(AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_ACCELERATOR_CORNER = ITEMS.register("fusion_accelerator_corner", () -> new BlockItem(AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_ELECTROMAGNET = ITEMS.register("fusion_electromagnet", () -> new BlockItem(AllNuclearBlocks.FUSION_ELECTROMAGNET.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_CRYOSTAT_CASING = ITEMS.register("fusion_cryostat_casing", () -> new BlockItem(AllNuclearBlocks.FUSION_CRYOSTAT_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_CENTRAL_SOLENOID = ITEMS.register("fusion_central_solenoid", () -> new BlockItem(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_BREEDING_BLANKET = ITEMS.register("fusion_breeding_blanket", () -> new BlockItem(AllNuclearBlocks.FUSION_BREEDING_BLANKET.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_MAGNET_INPUT = ITEMS.register("fusion_magnet_input", () -> new BlockItem(AllNuclearBlocks.FUSION_MAGNET_INPUT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_FLUID_PORT = ITEMS.register("fusion_fluid_port", () -> new BlockItem(AllNuclearBlocks.FUSION_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_FUEL_INJECTOR = ITEMS.register("fusion_fuel_injector", () -> new BlockItem(AllNuclearBlocks.FUSION_FUEL_INJECTOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_TURBINE_CASING = ITEMS.register("fusion_turbine_casing", () -> new BlockItem(AllNuclearBlocks.FUSION_TURBINE_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_TURBINE_ROTOR = ITEMS.register("fusion_turbine_rotor", () -> new BlockItem(AllNuclearBlocks.FUSION_TURBINE_ROTOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_TURBINE_FLUID_PORT = ITEMS.register("fusion_turbine_fluid_port", () -> new BlockItem(AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> FUSION_PLASMA_TURBINE = ITEMS.register("fusion_plasma_turbine", () -> new BlockItem(AllNuclearBlocks.FUSION_PLASMA_TURBINE.value(), new Item.Properties()));

    // Deuterium/Tritium crafting chain (very-late-game)
    public static final DeferredHolder<Item, ? extends Item> LITHIUM = ITEMS.register("lithium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> LITHIUM_6 = ITEMS.register("lithium_6", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> BERYLLIUM = ITEMS.register("beryllium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> SUPERCONDUCTOR_INGOT = ITEMS.register("superconductor_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> LITHIUM_6_BREEDER_ASSEMBLY = ITEMS.register("lithium_6_breeder_assembly", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> DEUTERIUM_CELL = ITEMS.register("deuterium_cell", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> TRITIUM_CELL = ITEMS.register("tritium_cell", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> DT_FUEL_PELLET = ITEMS.register("dt_fuel_pellet", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> SPENT_DT_PELLET = ITEMS.register("spent_dt_pellet", () -> new Item(new Item.Properties().stacksTo(16)));




    //Items

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredHolder<Item, ? extends Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM = ITEMS.register("uranium", () -> new UraniumItem(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), UraniumItem.NATURAL_ENRICHMENT)));
    // Fuel made before enrichment tiers existed carries no component and reads as the old 20% default.
    public static final DeferredHolder<Item, ? extends Item> URANIUM_REACTOR_FUEL = ITEMS.register("uranium_reactor_fuel", () -> new UraniumItem(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), UraniumItem.REACTOR_FUEL_ENRICHMENT)));
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_REACTOR_FUEL = ITEMS.register("depleted_uranium_reactor_fuel", () -> new UraniumItem(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), EnrichmentTier.LOW.nominal())));
    public static final DeferredHolder<Item, ? extends Item> ENRICHED_URANIUM_BLEND = ITEMS.register("enriched_uranium_blend", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> WEAPONS_GRADE_URANIUM_CORE = ITEMS.register("weapons_grade_uranium_core", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> WEAPONS_GRADE_PLUTONIUM_CORE = ITEMS.register("weapons_grade_plutonium_core", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> BORAX_SALT = ITEMS.register("borax_salt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> BORON = ITEMS.register("boron", () -> new Item(new Item.Properties().stacksTo(64)));

    // Uranium processing chain
    public static final DeferredHolder<Item, ? extends Item> CRUSHED_RAW_URANIUM = simple("crushed_raw_uranium");
    public static final DeferredHolder<Item, ? extends Item> URANIUM_CONCENTRATE = simple("uranium_concentrate");
    public static final DeferredHolder<Item, ? extends Item> YELLOWCAKE = simple("yellowcake");
    public static final DeferredHolder<Item, ? extends Item> URANIUM_DIOXIDE = enriched("uranium_dioxide", 64);
    public static final DeferredHolder<Item, ? extends Item> URANIUM_FUEL_PELLET = enriched("uranium_fuel_pellet", 64);
    public static final DeferredHolder<Item, ? extends Item> INCOMPLETE_URANIUM_REACTOR_FUEL = ITEMS.register("incomplete_uranium_reactor_fuel",
            () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ? extends Item> RADIOACTIVE_WASTE = simple("radioactive_waste");

    // Spent fuel reprocessing
    public static final DeferredHolder<Item, ? extends Item> SPENT_FUEL_FRAGMENTS = enriched("spent_fuel_fragments", 64);
    public static final DeferredHolder<Item, ? extends Item> URANYL_NITRATE = enriched("uranyl_nitrate", 64);
    public static final DeferredHolder<Item, ? extends Item> PLUTONIUM_DIOXIDE = simple("plutonium_dioxide");
    public static final DeferredHolder<Item, ? extends Item> PLUTONIUM = ITEMS.register("plutonium", () -> new Item(new Item.Properties().stacksTo(8)));

    // MOX fuel: plutonium dioxide mixed into depleted uranium dioxide
    public static final DeferredHolder<Item, ? extends Item> MOX_FUEL_PELLET = simple("mox_fuel_pellet");
    public static final DeferredHolder<Item, ? extends Item> INCOMPLETE_MOX_REACTOR_FUEL = ITEMS.register("incomplete_mox_reactor_fuel",
            () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ? extends Item> MOX_REACTOR_FUEL = ITEMS.register("mox_reactor_fuel", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_MOX_REACTOR_FUEL = ITEMS.register("depleted_mox_reactor_fuel", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> FLUORITE = simple("fluorite");

    // Lithium chain
    public static final DeferredHolder<Item, ? extends Item> RAW_SPODUMENE = simple("raw_spodumene");
    public static final DeferredHolder<Item, ? extends Item> CRUSHED_SPODUMENE = simple("crushed_spodumene");
    public static final DeferredHolder<Item, ? extends Item> SPODUMENE_CONCENTRATE = simple("spodumene_concentrate");
    public static final DeferredHolder<Item, ? extends Item> LITHIUM_CARBONATE = simple("lithium_carbonate");
    public static final DeferredHolder<Item, ? extends Item> EMPTY_GAS_CELL = ITEMS.register("empty_gas_cell", () -> new Item(new Item.Properties().stacksTo(16)));

    // Sistema de misiles
    public static final DeferredHolder<Item, ? extends Item> MISSILE = ITEMS.register("missile", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> TARGET_DESIGNATOR = ITEMS.register("target_designator", () -> new TargetDesignatorItem(new Item.Properties().stacksTo(1)));



    // Radiation protection
    public static final DeferredHolder<Item, ? extends Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new org.papiricoh.create_nuclearindustry.radiation.GeigerCounterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ? extends Item> HAZMAT_HELMET = hazmat("hazmat_helmet", net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final DeferredHolder<Item, ? extends Item> HAZMAT_CHESTPLATE = hazmat("hazmat_chestplate", net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final DeferredHolder<Item, ? extends Item> HAZMAT_LEGGINGS = hazmat("hazmat_leggings", net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final DeferredHolder<Item, ? extends Item> HAZMAT_BOOTS = hazmat("hazmat_boots", net.minecraft.world.item.ArmorItem.Type.BOOTS);

    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_BLOCK = ITEMS.register("depleted_uranium_block", () -> new BlockItem(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_HELMET = depletedUraniumArmor("depleted_uranium_helmet", net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_CHESTPLATE = depletedUraniumArmor("depleted_uranium_chestplate", net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_LEGGINGS = depletedUraniumArmor("depleted_uranium_leggings", net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_BOOTS = depletedUraniumArmor("depleted_uranium_boots", net.minecraft.world.item.ArmorItem.Type.BOOTS);

    private static DeferredHolder<Item, ? extends Item> depletedUraniumArmor(String name, net.minecraft.world.item.ArmorItem.Type type) {
        return ITEMS.register(name, () -> new org.papiricoh.create_nuclearindustry.radiation.DepletedUraniumArmorItem(
                org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.DEPLETED_URANIUM_MATERIAL, type,
                org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.depletedUraniumProperties(type)));
    }

    private static DeferredHolder<Item, ? extends Item> hazmat(String name, net.minecraft.world.item.ArmorItem.Type type) {
        return ITEMS.register(name, () -> new net.minecraft.world.item.ArmorItem(
                org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.hazmat(), type,
                org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.hazmatProperties(type)));
    }

    private static DeferredHolder<Item, ? extends Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Item, ? extends Item> enriched(String name, int stackSize) {
        return ITEMS.register(name, () -> new UraniumItem(new Item.Properties()
                .stacksTo(stackSize)
                .component(AllNuclearDataComponents.ENRICHMENT.get(), UraniumItem.NATURAL_ENRICHMENT)));
    }

    public static void init() {}
}
