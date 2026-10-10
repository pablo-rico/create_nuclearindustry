package org.papiricoh.create_nuclearindustry.reactor.pwr;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Tags of the pressurized water loop, containment and accident chain. */
public final class PwrTags {
    /** Corium cannot melt these; it spreads over them instead of going down. */
    public static final TagKey<Block> CORIUM_RESISTANT = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "corium_resistant"));

    private PwrTags() {}
}
