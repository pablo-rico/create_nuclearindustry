package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.EnrichedUraniumBlendRecipe;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;

public class AllNuclearItems {

    // Create a Deferred Register to hold Items which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Create_NuclearIndustry.MODID);

    //Blocks

    // Creates a new BlockItem with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final DeferredHolder<Item, ? extends Item> URANIUM_ORE = ITEMS.register("uranium_ore", () -> new BlockItem(AllNuclearBlocks.URANIUM_ORE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> BORAX_ORE = ITEMS.register("borax_ore", () -> new BlockItem(AllNuclearBlocks.BORAX_ORE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> CONTROL_ROD = ITEMS.register("control_rod", () -> new BlockItem(AllNuclearBlocks.CONTROL_ROD.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> URANIUM_ROD = ITEMS.register("uranium_rod", () -> new BlockItem(AllNuclearBlocks.URANIUM_ROD.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> HEAT_EXCHANGER = ITEMS.register("heat_exchanger", () -> new BlockItem(AllNuclearBlocks.HEAT_EXCHANGER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_CASING = ITEMS.register("reactor_casing", () -> new BlockItem(AllNuclearBlocks.REACTOR_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_CONTROLLER = ITEMS.register("reactor_controller", () -> new BlockItem(AllNuclearBlocks.REACTOR_CONTROLLER.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_FLUID_PORT = ITEMS.register("reactor_fluid_port", () -> new BlockItem(AllNuclearBlocks.REACTOR_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_FUEL_PORT = ITEMS.register("reactor_fuel_port", () -> new BlockItem(AllNuclearBlocks.REACTOR_FUEL_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> REACTOR_TEMPERATURE_SENSOR = ITEMS.register("reactor_temperature_sensor", () -> new BlockItem(AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> DUAL_FLUID_PIPE = ITEMS.register("dual_fluid_pipe", () -> new BlockItem(AllNuclearBlocks.DUAL_FLUID_PIPE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> CENTRIFUGE = ITEMS.register("centrifuge", () -> new BlockItem(AllNuclearBlocks.CENTRIFUGE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_CASING = ITEMS.register("turbine_casing", () -> new BlockItem(AllNuclearBlocks.TURBINE_CASING.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_ROTOR = ITEMS.register("turbine_rotor", () -> new BlockItem(AllNuclearBlocks.TURBINE_ROTOR.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_OUTPUT = ITEMS.register("turbine_output", () -> new BlockItem(AllNuclearBlocks.TURBINE_OUTPUT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> TURBINE_FLUID_PORT = ITEMS.register("turbine_fluid_port", () -> new BlockItem(AllNuclearBlocks.TURBINE_FLUID_PORT.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> NUCLEAR_BOMB = ITEMS.register("nuclear_bomb", () -> new BlockItem(AllNuclearBlocks.NUCLEAR_BOMB.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> LAUNCH_PAD = ITEMS.register("launch_pad", () -> new BlockItem(AllNuclearBlocks.LAUNCH_PAD.value(), new Item.Properties()));




    //Items

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredHolder<Item, ? extends Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM = ITEMS.register("uranium", () -> new UraniumItem(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), UraniumItem.NATURAL_ENRICHMENT)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM_REACTOR_FUEL = ITEMS.register("uranium_reactor_fuel", () -> new Item(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), EnrichedUraniumBlendRecipe.REQUIRED_ENRICHMENT)));
    public static final DeferredHolder<Item, ? extends Item> DEPLETED_URANIUM_REACTOR_FUEL = ITEMS.register("depleted_uranium_reactor_fuel", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> ENRICHED_URANIUM_BLEND = ITEMS.register("enriched_uranium_blend", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> WEAPONS_GRADE_URANIUM_CORE = ITEMS.register("weapons_grade_uranium_core", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> BORAX_SALT = ITEMS.register("borax_salt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> BORON = ITEMS.register("boron", () -> new Item(new Item.Properties().stacksTo(64)));

    // Sistema de misiles
    public static final DeferredHolder<Item, ? extends Item> MISSILE = ITEMS.register("missile", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> TARGET_DESIGNATOR = ITEMS.register("target_designator", () -> new TargetDesignatorItem(new Item.Properties().stacksTo(1)));



    public static void init() {}
}
