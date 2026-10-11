package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.data.recipe.FuelCycleRecipes;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Lang, models, tags, radioactivity and recipes for the renewable fuel cycle: its items and fluids,
 * the irradiation source, and the acid leaching and irradiation fan treatments.
 */
final class FuelCycleDataGen extends AreaDataGen {
    static final FuelCycleDataGen INSTANCE = new FuelCycleDataGen();

    private static final String INFO = "jei.create_nuclearindustry.info.";

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
        LangEntries.item("incomplete_thorium_reactor_fuel", "Incomplete Thorium Reactor Fuel", "Combustible de torio incompleto");
        LangEntries.item("depleted_thorium_reactor_fuel", "Depleted Thorium Reactor Fuel", "Combustible de torio gastado");
        LangEntries.item("thorium_spent_fragments", "Spent Thorium Fragments", "Fragmentos de torio gastado");
        LangEntries.block("irradiation_source", "Irradiation Source", "Fuente de irradiación");

        LangEntries.fluid("brine", "Brine", "Salmuera");
        LangEntries.fluid("thorium_fuel_solution", "Thorium Fuel Solution", "Disolución de combustible de torio");
        LangEntries.fluid("uranium_233_nitrate", "Uranium-233 Nitrate", "Nitrato de uranio-233");
        LangEntries.block("sulfuric_acid", "Sulfuric Acid", "Ácido sulfúrico");
        LangEntries.item("sulfuric_acid_bucket", "Sulfuric Acid Bucket", "Cubo de ácido sulfúrico");

        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "acid_sizzle", "Acid sizzles", "Chisporrotea el ácido");
        LangEntries.add(s + "irradiation_crackle", "Radiation crackles", "Crepita la radiación");

        LangEntries.add("death.attack.create_nuclearindustry.acid", "%1$s was dissolved by an acid spray",
                "%1$s se disolvió en un rocío de ácido");
        LangEntries.add("death.attack.create_nuclearindustry.acid.player", "%1$s was dissolved by an acid spray while fighting %2$s",
                "%1$s se disolvió en un rocío de ácido mientras luchaba contra %2$s");

        // JEI: the two fan treatments, and the fan shown as their catalyst.
        LangEntries.add("jei.create_nuclearindustry.category.fan_acid_leaching", "Bulk Acid Leaching", "Lixiviación ácida a granel");
        LangEntries.add("jei.create_nuclearindustry.category.fan_irradiation", "Bulk Irradiation", "Irradiación a granel");
        LangEntries.add("create_nuclearindustry.recipe.fan_acid_leaching.fan", "Fan behind Sulfuric Acid", "Ventilador tras ácido sulfúrico");
        LangEntries.add("create_nuclearindustry.recipe.fan_irradiation.fan", "Fan behind an Irradiation Source",
                "Ventilador tras una fuente de irradiación");

        LangEntries.add(INFO + "acid_leaching",
                "Bulk Acid Leaching: an Encased Fan blowing through sulfuric acid (a placed source or flowing acid) treats the items its "
                        + "air current carries past the acid, on the ground, on belts and on depots, as washing does with water. Acid "
                        + "leaches traces out of rock fines and heavy mineral sand, strips uranium off a loaded adsorbent and washes "
                        + "contamination off waste into Radioactive Sludge. The last catalyst the air passes decides: water then acid "
                        + "leaches, acid then water washes. The spray burns creatures caught in it. Sulfuric acid is mixed from water "
                        + "and gunpowder in a heated basin and is not used up.",
                "Lixiviación ácida a granel: un ventilador que sopla a través de ácido sulfúrico (una fuente colocada o ácido que "
                        + "fluye) trata los ítems que su corriente lleva más allá del ácido, en el suelo, en cintas y en depósitos, como "
                        + "el lavado con agua. El ácido extrae trazas de los finos de roca y de la arena de minerales pesados, despega el "
                        + "uranio de un adsorbente cargado y lava la contaminación de los residuos en lodo radiactivo. Manda el último "
                        + "catalizador que cruza el aire: agua y luego ácido lixivia; ácido y luego agua lava. El rocío quema a las "
                        + "criaturas atrapadas. El ácido sulfúrico se mezcla con agua y pólvora en un cuenco calentado y no se gasta.");
        LangEntries.add(INFO + "irradiation",
                "Bulk Irradiation: an Encased Fan blowing past an Irradiation Source (a capsule of waste in an open cage, crafted "
                        + "from Radioactive Waste) bathes the items in its air current in neutrons. Air passes through the source's cage; "
                        + "a full block would stop the current, so ordinary radioactive blocks do not work. Thorium dioxide becomes "
                        + "irradiated thorium, which dissolves in nitric acid into a little uranium-233 dioxide and thorium back. "
                        + "Depleted uranium (oxide or metal) becomes irradiated depleted uranium, which dissolves in nitric acid into "
                        + "uranyl nitrate and a trace of spent fuel solution for plutonium. The green current doses creatures caught in it.",
                "Irradiación a granel: un ventilador que sopla a través de una fuente de irradiación (una cápsula de residuos en una "
                        + "jaula abierta, fabricada con residuos radiactivos) baña de neutrones los ítems de su corriente. El aire pasa "
                        + "por la jaula; un bloque completo cortaría la corriente, así que los bloques radiactivos corrientes no sirven. "
                        + "El dióxido de torio se convierte en torio irradiado, que con ácido nítrico da un poco de dióxido de uranio-233 "
                        + "y devuelve torio. El uranio empobrecido (óxido o metal) se convierte en uranio empobrecido irradiado, que con "
                        + "ácido nítrico da nitrato de uranilo y trazas de disolución de combustible gastado para el plutonio. La "
                        + "corriente verde irradia a las criaturas atrapadas.");
        LangEntries.add(INFO + "rock_fines",
                "Crushing granite, tuff, deepslate or cobbled deepslate gives fines with the rock's usual by-products. Acid "
                        + "leaching the fines washes out traces: granite gives uranium concentrate and sometimes thorium dioxide, tuff "
                        + "spodumene concentrate, deepslate uranium concentrate and fluorite. Rock is everywhere, so this never runs out, "
                        + "but each unit costs between one and two times what the same unit costs from ore.",
                "Triturar granito, toba, pizarra profunda o pizarra profunda labrada da finos con los subproductos habituales de "
                        + "la roca. Lixiviar los finos con ácido extrae trazas: el granito da concentrado de uranio y a veces dióxido de "
                        + "torio; la toba, concentrado de espodumena; la pizarra profunda, concentrado de uranio y fluorita. La roca está "
                        + "en todas partes y nunca se acaba, pero cada unidad cuesta entre una y dos veces lo que cuesta desde la mena.");
        LangEntries.add(INFO + "heavy_mineral_sand",
                "Washing sand, red sand or gravel sometimes pans out heavy mineral sand (monazite and zircon). Acid leaching it is "
                        + "the only source of thorium dioxide, with a chance of uranium concentrate and of beryllium. Thorium is fertile, "
                        + "not fissile: it becomes fuel when seeded and burnt in a reactor, or irradiated by a fan.",
                "Lavar arena, arena roja o grava a veces separa arena de minerales pesados (monacita y circón). Lixiviarla con ácido "
                        + "es la única fuente de dióxido de torio, con probabilidad de concentrado de uranio y de berilio. El torio es "
                        + "fértil, no fisible: se vuelve combustible cuando se siembra y se quema en un reactor, o se irradia con un "
                        + "ventilador.");
        LangEntries.add(INFO + "seawater",
                "Water boiled down in a heated basin becomes brine (4:1). A centrifuge spins brine into lithium carbonate, borax and "
                        + "fresh water. String and bone meal mixed with water make an amidoxime adsorbent, which picks up the uranium of "
                        + "sea water when mixed with water or, faster, with a little brine; acid leaching the loaded adsorbent gives "
                        + "uranium concentrate and returns half of the adsorbent.",
                "El agua hervida en un cuenco calentado se convierte en salmuera (4:1). Una centrifugadora separa la salmuera en "
                        + "carbonato de litio, bórax y agua dulce. El hilo y la harina de hueso mezclados con agua forman un adsorbente "
                        + "de amidoxima, que recoge el uranio del agua de mar al mezclarlo con agua o, con menos cantidad, con salmuera; "
                        + "lixiviar con ácido el adsorbente cargado da concentrado de uranio y devuelve la mitad del adsorbente.");
        LangEntries.add(INFO + "decontamination",
                "Acid leaching decontaminates: low-level waste, spent D-T pellets and depleted RTG pellets come out as clean iron "
                        + "nuggets, and their activity as Radioactive Sludge. An Item Drain pours the sludge out as contaminated water, "
                        + "which a centrifuge cleans into water and a little low-level waste.",
                "La lixiviación ácida descontamina: los residuos de baja actividad, las pastillas D-T gastadas y las de RTG agotadas "
                        + "salen como pepitas de hierro limpias, y su actividad como lodo radiactivo. Un drenaje de ítems vierte el lodo "
                        + "como agua contaminada, que una centrifugadora limpia en agua y un poco de residuo de baja actividad.");
        LangEntries.add(INFO + "thorium_fuel",
                "Thorium fuel: three thorium dioxide and one fissile seed (low-enriched uranium dioxide or uranium-233 dioxide) "
                        + "compact into four pellets, assembled into fuel like uranium. In the reactor it starts weak, only the seed "
                        + "fissions, and grows more reactive as the thorium breeds uranium-233, peaking about half way; it lasts 40% "
                        + "longer than low-enriched uranium. Spent thorium fuel cools in pools and casks like the others; crush it, "
                        + "dissolve the fragments in nitric acid, and centrifuge the solution into uranium-233 nitrate, recovered acid, "
                        + "thorium dioxide and high-level waste. Denitrated, the uranium-233 seeds new pellets.",
                "Combustible de torio: tres de dióxido de torio y una semilla fisible (dióxido de uranio poco enriquecido o de "
                        + "uranio-233) se compactan en cuatro pastillas, que se ensamblan como el uranio. En el reactor empieza débil, "
                        + "solo fisiona la semilla, y gana reactividad a medida que el torio genera uranio-233, con el máximo hacia la "
                        + "mitad; dura un 40% más que el uranio poco enriquecido. El combustible de torio gastado se enfría en piscinas y "
                        + "contenedores como los demás; tritúralo, disuelve los fragmentos en ácido nítrico y centrifuga la disolución en "
                        + "nitrato de uranio-233, ácido recuperado, dióxido de torio y residuo de alta actividad. Desnitrado, el uranio-233 "
                        + "siembra pastillas nuevas.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        // Fluid blocks render from their fluid type; the model only needs a particle texture.
        provider.simpleBlock(AllNuclearFluids.SULFURIC_ACID_BLOCK.get(),
                provider.models().getBuilder("sulfuric_acid").texture("particle", provider.mcLoc("block/water_still")));
        // Hand-written: an open cage around a glowing capsule.
        ModelFile source = provider.models().getExistingFile(provider.modLoc("block/irradiation_source"));
        provider.simpleBlock(FuelCycleRegistry.IRRADIATION_SOURCE.get(), source);
        provider.simpleBlockItem(FuelCycleRegistry.IRRADIATION_SOURCE.get(), source);
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        FuelCycleRegistry.ITEMS.stream().filter(item -> item != FuelCycleRegistry.IRRADIATION_SOURCE_ITEM)
                .forEach(item -> provider.basicItem(item.get()));
        provider.basicItem(FuelCycleRegistry.INCOMPLETE_THORIUM_REACTOR_FUEL.get());
        provider.withExistingParent("sulfuric_acid_bucket", ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(AllNuclearFluids.SULFURIC_ACID.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(NuclearFanTypes.ACID_LEACHING_BLOCKS);
        provider.tagFor(NuclearFanTypes.IRRADIATION_BLOCKS).add(FuelCycleRegistry.IRRADIATION_SOURCE.get());
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(FuelCycleRegistry.IRRADIATION_SOURCE.get());
    }

    @Override
    void fluidTags(NuclearFluidTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(NuclearFanTypes.ACID_LEACHING_FLUIDS)
                .add(AllNuclearFluids.SULFURIC_ACID.get(), AllNuclearFluids.SULFURIC_ACID_FLUID.flowing().get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Monazite and thoria are barely active; what a neutron has touched is hot, spent fuel hottest.
        var radioactivity = provider.builderFor(AllNuclearRadiation.RADIOACTIVITY);
        add(radioactivity, FuelCycleRegistry.HEAVY_MINERAL_SAND, 0.02f);
        add(radioactivity, FuelCycleRegistry.LOADED_ADSORBENT, 0.01f);
        add(radioactivity, FuelCycleRegistry.RADIOACTIVE_SLUDGE, 0.5f);
        add(radioactivity, FuelCycleRegistry.THORIUM_DIOXIDE, 0.05f);
        add(radioactivity, FuelCycleRegistry.IRRADIATED_THORIUM, 2.0f);
        add(radioactivity, FuelCycleRegistry.IRRADIATED_DEPLETED_URANIUM, 1.5f);
        // Uranium-233 always carries some uranium-232, whose daughters are strong gamma emitters.
        add(radioactivity, FuelCycleRegistry.URANIUM_233_DIOXIDE, 2.0f);
        add(radioactivity, FuelCycleRegistry.THORIUM_FUEL_PELLET, 0.8f);
        add(radioactivity, FuelCycleRegistry.INCOMPLETE_THORIUM_REACTOR_FUEL, 1.0f);
        add(radioactivity, FuelCycleRegistry.THORIUM_REACTOR_FUEL, 2.0f);
        add(radioactivity, FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL, 5.0f);
        add(radioactivity, FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS, 3.0f);
        // Above fuelcycle.irradiationMinActivity: also what makes it irradiate players standing near it.
        add(radioactivity, FuelCycleRegistry.IRRADIATION_SOURCE_ITEM, 3.0f);
    }

    private static void add(NuclearDataMapProvider.Builder<Radioactivity, Item> builder, Supplier<? extends Item> item, float perSecond) {
        builder.add(item.get().builtInRegistryHolder(), new Radioactivity(perSecond, false), false);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new FuelCycleRecipes(output, lookup));
    }
}
