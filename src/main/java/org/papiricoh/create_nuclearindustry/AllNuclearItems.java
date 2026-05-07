package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

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
    public static final DeferredHolder<Item, ? extends Item> DUAL_FLUID_PIPE = ITEMS.register("dual_fluid_pipe", () -> new BlockItem(AllNuclearBlocks.DUAL_FLUID_PIPE.value(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> CENTRIFUGE = ITEMS.register("centrifuge", () -> new BlockItem(AllNuclearBlocks.CENTRIFUGE.value(), new Item.Properties()));




    //Items

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredHolder<Item, ? extends Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM = ITEMS.register("uranium", () -> new UraniumItem(new Item.Properties()
            .stacksTo(16)
            .component(AllNuclearDataComponents.ENRICHMENT.get(), UraniumItem.NATURAL_ENRICHMENT)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM_238 = ITEMS.register("uranium_238", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> URANIUM_235 = ITEMS.register("uranium_235", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, ? extends Item> BORAX_SALT = ITEMS.register("borax_salt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, ? extends Item> BORON = ITEMS.register("boron", () -> new Item(new Item.Properties().stacksTo(64)));



    public static void init() {}
}
