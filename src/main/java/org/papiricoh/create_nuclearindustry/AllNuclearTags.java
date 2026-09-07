package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Tags the mod reads at runtime.
 *
 * <p>{@link #RAW_URANIUM} is the machine-facing tag: it is populated from the common
 * {@code c:raw_materials/uranium} conventions, so raw uranium from any other mod is
 * accepted by the centrifuge and is refined into this mod's uranium.
 */
public class AllNuclearTags {

    public static class Items {
        /** Any raw uranium, from this mod or any other. */
        public static final TagKey<Item> RAW_URANIUM = tag("raw_uranium");
        public static final TagKey<Item> URANIUM_INGOTS = tag("uranium_ingots");

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
        }
    }

    private AllNuclearTags() {
    }
}
