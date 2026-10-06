package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;
import java.util.function.Supplier;

/** Flat item models for items added by the rewrite; older items keep hand-written models. */
public class NuclearItemModelProvider extends ItemModelProvider {

    public NuclearItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<Supplier<? extends Item>> flat = List.of(
                AllNuclearItems.CRUSHED_RAW_URANIUM,
                AllNuclearItems.URANIUM_CONCENTRATE,
                AllNuclearItems.YELLOWCAKE,
                AllNuclearItems.URANIUM_DIOXIDE,
                AllNuclearItems.SPENT_FUEL_FRAGMENTS,
                AllNuclearItems.URANYL_NITRATE,
                AllNuclearItems.PLUTONIUM_DIOXIDE,
                AllNuclearItems.PLUTONIUM,
                AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE,
                AllNuclearItems.URANIUM_FUEL_PELLET,
                AllNuclearItems.INCOMPLETE_URANIUM_REACTOR_FUEL,
                AllNuclearItems.RADIOACTIVE_WASTE,
                AllNuclearItems.FLUORITE,
                AllNuclearItems.RAW_SPODUMENE,
                AllNuclearItems.CRUSHED_SPODUMENE,
                AllNuclearItems.SPODUMENE_CONCENTRATE,
                AllNuclearItems.LITHIUM_CARBONATE,
                AllNuclearItems.EMPTY_GAS_CELL,
                AllNuclearItems.GEIGER_COUNTER,
                AllNuclearItems.HAZMAT_HELMET,
                AllNuclearItems.HAZMAT_CHESTPLATE,
                AllNuclearItems.HAZMAT_LEGGINGS,
                AllNuclearItems.HAZMAT_BOOTS,
                AllNuclearItems.DEPLETED_URANIUM_HELMET,
                AllNuclearItems.DEPLETED_URANIUM_CHESTPLATE,
                AllNuclearItems.DEPLETED_URANIUM_LEGGINGS,
                AllNuclearItems.DEPLETED_URANIUM_BOOTS);
        for (Supplier<? extends Item> item : flat) {
            basicItem(item.get());
        }

        // Bucket drawn with the fluid's own texture and tint.
        withExistingParent("steam_bucket", ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(AllNuclearFluids.STEAM.get());
    }
}
