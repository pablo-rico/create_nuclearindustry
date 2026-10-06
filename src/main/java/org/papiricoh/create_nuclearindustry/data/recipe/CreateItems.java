package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Create items looked up by id. Create's own AllItems/AllBlocks entries are Registrate
 * types, and Registrate is not on this project's compile classpath.
 */
final class CreateItems {

    private CreateItems() {}

    static Item get(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", path));
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing Create item create:" + path);
        }
        return item;
    }
}
