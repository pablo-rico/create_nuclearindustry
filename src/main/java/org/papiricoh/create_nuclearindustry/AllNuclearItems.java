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



    //Items

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(16)));



    public static void init() {}
}
