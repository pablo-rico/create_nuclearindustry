package org.papiricoh.create_nuclearindustry.wastetransport;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** What the waste containers may hold. */
public final class WasteTransportTags {
    /** Fluids a Radioactive Waste Cistern accepts: only the mod's radioactive liquors and gases. */
    public static final TagKey<Fluid> RADIOACTIVE_WASTE_FLUIDS = TagKey.create(Registries.FLUID, id("radioactive_waste_fluids"));
    /** Items a Shielded Waste Cask accepts: waste of every class, packages and spent material. */
    public static final TagKey<Item> WASTE_CASK_CARGO = TagKey.create(Registries.ITEM, id("waste_cask_cargo"));

    private WasteTransportTags() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
