package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags the mod reads at runtime or exports for other mods.
 *
 * <p>{@link Items#RAW_URANIUM} is the machine-facing tag: it is populated from the common
 * {@code c:raw_materials/uranium} conventions, so raw uranium from any other mod is
 * processed like this mod's own.
 */
public final class AllNuclearTags {

    public static final class Items {
        /** Any raw uranium, from this mod or any other. */
        public static final TagKey<Item> RAW_URANIUM = mod("raw_uranium");
        /** Any uranium ingot, from this mod or any other. */
        public static final TagKey<Item> URANIUM_INGOTS = mod("uranium_ingots");
        /** Weapons-grade cores, uranium or plutonium, for warhead recipes. */
        public static final TagKey<Item> NUCLEAR_WARHEAD_CORES = mod("nuclear_warhead_cores");
        /** What a thermonuclear warhead's tamper is made of: depleted uranium blocks. */
        public static final TagKey<Item> THERMONUCLEAR_TAMPERS = mod("thermonuclear_tampers");
        /** The parts of a thermonuclear warhead, which a missile cannot carry. */
        public static final TagKey<Item> THERMONUCLEAR_COMPONENTS = mod("thermonuclear_components");

        public static final TagKey<Item> C_RAW_MATERIALS = common("raw_materials");
        public static final TagKey<Item> C_RAW_URANIUM = common("raw_materials/uranium");
        public static final TagKey<Item> C_RAW_LITHIUM = common("raw_materials/lithium");
        public static final TagKey<Item> C_INGOTS_URANIUM = common("ingots/uranium");
        public static final TagKey<Item> C_GEMS_FLUORITE = common("gems/fluorite");
        /** Saltpetre from other mods, for nitric acid. */
        public static final TagKey<Item> SALTPETER_DUSTS = common("dusts/saltpeter");

        private static TagKey<Item> mod(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
        }

        private static TagKey<Item> common(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private Items() {}
    }

    public static final class Blocks {
        public static final TagKey<Block> C_ORES = common("ores");
        public static final TagKey<Block> C_ORES_URANIUM = common("ores/uranium");
        public static final TagKey<Block> C_ORES_LITHIUM = common("ores/lithium");
        public static final TagKey<Block> C_ORES_BORAX = common("ores/borax");
        /** Radioactive blocks that stop irradiating and leaking below the geological storage depth. */
        public static final TagKey<Block> GEOLOGICALLY_STORABLE = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(org.papiricoh.create_nuclearindustry.Create_NuclearIndustry.MODID, "geologically_storable"));
        public static final TagKey<Block> WRENCH_PICKUP = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup"));

        private static TagKey<Block> common(String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private Blocks() {}
    }

    private AllNuclearTags() {}
}
