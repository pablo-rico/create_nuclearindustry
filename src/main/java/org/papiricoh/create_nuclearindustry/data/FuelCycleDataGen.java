package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes;

/** Lang, models and tags for the renewable fuel cycle: its items and fluids, and the acid leaching and irradiation fan treatments. */
final class FuelCycleDataGen extends AreaDataGen {
    static final FuelCycleDataGen INSTANCE = new FuelCycleDataGen();

    private FuelCycleDataGen() {}

    @Override
    void lang() {
        LangEntries.item("granite_fines", "Granite Fines", "Finos de granito");
        LangEntries.item("tuff_fines", "Tuff Fines", "Finos de toba");
        LangEntries.item("deepslate_fines", "Deepslate Fines", "Finos de pizarra profunda");
        LangEntries.item("heavy_mineral_sand", "Heavy Mineral Sand", "Arena de minerales pesados");
        LangEntries.item("amidoxime_adsorbent", "Amidoxime Adsorbent", "Adsorbente de amidoxima");
        LangEntries.item("loaded_adsorbent", "Loaded Adsorbent", "Adsorbente cargado");
        LangEntries.item("radioactive_sludge", "Radioactive Sludge", "Lodo radiactivo");
        LangEntries.item("thorium_dioxide", "Thorium Dioxide", "Dióxido de torio");
        LangEntries.item("irradiated_thorium", "Irradiated Thorium", "Torio irradiado");
        LangEntries.item("irradiated_depleted_uranium", "Irradiated Depleted Uranium", "Uranio empobrecido irradiado");
        LangEntries.item("uranium_233_dioxide", "Uranium-233 Dioxide", "Dióxido de uranio-233");
        LangEntries.item("thorium_fuel_pellet", "Thorium Fuel Pellet", "Pastilla de combustible de torio");
        LangEntries.item("thorium_reactor_fuel", "Thorium Reactor Fuel", "Combustible de torio para reactor");
        LangEntries.item("depleted_thorium_reactor_fuel", "Depleted Thorium Reactor Fuel", "Combustible de torio gastado");
        LangEntries.item("thorium_spent_fragments", "Spent Thorium Fragments", "Fragmentos de torio gastado");

        LangEntries.fluid("brine", "Brine", "Salmuera");
        LangEntries.fluid("thorium_fuel_solution", "Thorium Fuel Solution", "Disolución de combustible de torio");
        LangEntries.fluid("uranium_233_nitrate", "Uranium-233 Nitrate", "Nitrato de uranio-233");
        LangEntries.block("sulfuric_acid", "Sulfuric Acid", "Ácido sulfúrico");
        LangEntries.item("sulfuric_acid_bucket", "Sulfuric Acid Bucket", "Cubo de ácido sulfúrico");

        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "acid_sizzle", "Acid sizzles", "Chisporrotea el ácido");
        LangEntries.add(s + "irradiation_crackle", "Radiation crackles", "Crepita la radiación");

        LangEntries.add("jei.create_nuclearindustry.info.renewable_fuel_cycle",
                "Base materials won without ore, by the acid leaching and irradiation fan treatments.",
                "Materiales base obtenidos sin mena, con los tratamientos de lixiviación ácida e irradiación con ventilador.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        // Fluid blocks render from their fluid type; the model only needs a particle texture.
        provider.simpleBlock(AllNuclearFluids.SULFURIC_ACID_BLOCK.get(),
                provider.models().getBuilder("sulfuric_acid").texture("particle", provider.mcLoc("block/water_still")));
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        FuelCycleRegistry.ITEMS.forEach(item -> provider.basicItem(item.get()));
        provider.withExistingParent("sulfuric_acid_bucket", ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(AllNuclearFluids.SULFURIC_ACID.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(NuclearFanTypes.ACID_LEACHING_BLOCKS);
        provider.tagFor(NuclearFanTypes.IRRADIATION_BLOCKS);
    }

    @Override
    void fluidTags(NuclearFluidTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(NuclearFanTypes.ACID_LEACHING_FLUIDS)
                .add(AllNuclearFluids.SULFURIC_ACID.get(), AllNuclearFluids.SULFURIC_ACID_FLUID.flowing().get());
    }
}
