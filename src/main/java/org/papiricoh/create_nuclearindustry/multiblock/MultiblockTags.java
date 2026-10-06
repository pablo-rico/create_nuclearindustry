package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

public final class MultiblockTags {
    /** Blocks that can make an unformed multiblock form when placed. */
    public static final TagKey<Block> MULTIBLOCK_PARTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "multiblock_parts"));

    private MultiblockTags() {}
}
