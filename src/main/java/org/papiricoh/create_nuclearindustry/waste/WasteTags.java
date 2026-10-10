package org.papiricoh.create_nuclearindustry.waste;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Tags the geological repository and the waste recipes read. */
public final class WasteTags {
    /** Sound, low-permeability rock a repository can be dug into (deepslate, tuff, basalt...). */
    public static final TagKey<Block> REPOSITORY_HOST_ROCK = block("repository_host_rock");
    /** Swelling clay packed against packages to seal them; other packages count as backfill too. */
    public static final TagKey<Block> REPOSITORY_BACKFILL = block("repository_backfill");

    /** Waste a plain barrel can still be compacted from: intermediate and high level. */
    public static final TagKey<Item> BARREL_WASTE = item("barrel_waste");

    private WasteTags() {}

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }
}
