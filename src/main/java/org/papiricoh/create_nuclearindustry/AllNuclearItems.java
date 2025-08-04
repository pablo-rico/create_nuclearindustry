package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AllNuclearItems {

    // Create a Deferred Register to hold Items which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Create_NuclearIndustry.MODID);

    //Blocks

    // Creates a new BlockItem with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final RegistryObject<Item> URANIUM_ORE = ITEMS.register("uranium_ore", () -> new BlockItem(AllNuclearBlocks.URANIUM_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BORAX_ORE = ITEMS.register("borax_ore", () -> new BlockItem(AllNuclearBlocks.BORAX_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> CONTROL_ROD = ITEMS.register("control_rod", () -> new BlockItem(AllNuclearBlocks.CONTROL_ROD.get(), new Item.Properties()));
    public static final RegistryObject<Item> URANIUM_ROD = ITEMS.register("uranium_rod", () -> new BlockItem(AllNuclearBlocks.URANIUM_ROD.get(), new Item.Properties()));
    public static final RegistryObject<Item> HEAT_EXCHANGER = ITEMS.register("heat_exchanger", () -> new BlockItem(AllNuclearBlocks.HEAT_EXCHANGER.get(), new Item.Properties()));




    //Items

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> URANIUM_238 = ITEMS.register("uranium_238", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> URANIUM_235 = ITEMS.register("uranium_235", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BORAX_SALT = ITEMS.register("borax_salt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BORON = ITEMS.register("boron", () -> new Item(new Item.Properties().stacksTo(64)));



    public static void init() {}
}
