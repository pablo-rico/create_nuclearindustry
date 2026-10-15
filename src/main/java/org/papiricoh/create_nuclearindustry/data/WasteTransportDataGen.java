package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.data.recipe.WasteTransportRecipes;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteTags;
import org.papiricoh.create_nuclearindustry.wastetransport.FluidRadioactivity;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportTags;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportText;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlock;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Lang, models, tags, the fluid radioactivity data map and recipes of radioactive waste transport. */
final class WasteTransportDataGen extends AreaDataGen {
    static final WasteTransportDataGen INSTANCE = new WasteTransportDataGen();

    private WasteTransportDataGen() {}

    @Override
    void lang() {
        LangEntries.block("radioactive_waste_cistern", "Radioactive Waste Cistern", "Cisterna de residuos radiactivos");
        LangEntries.block("shielded_waste_cask", "Shielded Waste Cask", "Contenedor blindado de residuos");

        String goggles = WasteTransportText.PREFIX + "goggles.";
        LangEntries.add(goggles + "sealed", "Sealed: shielding intact", "Sellado: blindaje intacto");
        LangEntries.add(goggles + "breached", "BREACHED: %s%% damage", "ROTO: %s%% de daño");
        LangEntries.add(goggles + "repair_hint", "Patch it with a Maintenance Kit", "Repáralo con un kit de mantenimiento");
        LangEntries.add(goggles + "activity", "Cargo activity: %s rad/s", "Actividad de la carga: %s rad/s");
        LangEntries.add(goggles + "emission", "Gets out: %s rad/s", "Sale: %s rad/s");
        LangEntries.add(goggles + "cask_size", "%s blocks, %s slots", "%s bloques, %s huecos");

        LangEntries.add("jei.create_nuclearindustry.info.radioactive_waste_cistern",
                "A fluid tank for radioactive fluids only (spent fuel solution, plutonium and uranium-233 nitrate, thorium fuel "
                        + "solution, contaminated water, depleted hexafluoride). Connects like Create's tank, up to 3x3 across, 8 buckets a "
                        + "block, with no window. Sealed it lets 0.2% of its radiation out. It rides trains and other contraptions; a "
                        + "derailment, crash or explosion breaches it: half the fluid spills, the ground is contaminated in proportion to its "
                        + "activity and it keeps leaking and irradiating until two uses of a Maintenance Kit patch it.",
                "Un tanque solo para fluidos radiactivos (solución de combustible gastado, nitrato de plutonio y de uranio-233, solución "
                        + "de combustible de torio, agua contaminada, hexafluoruro empobrecido). Se conecta como el tanque de Create, hasta 3x3, "
                        + "8 cubos por bloque, sin ventana. Sellado deja salir el 0,2% de su radiación. Viaja en trenes y otros artefactos; un "
                        + "descarrilamiento, choque o explosión lo rompe: se derrama la mitad del fluido, el suelo se contamina según su "
                        + "actividad y sigue fugando e irradiando hasta que dos usos del kit de mantenimiento lo reparan.");
        LangEntries.add("jei.create_nuclearindustry.info.shielded_waste_cask",
                "A lead and concrete vault for solid waste: low-, intermediate- and high-level waste, sludge, waste packages, spent "
                        + "fragments, depleted fuel and RTG pellets. Hot spent fuel is refused. Joins along its axis like Create's Item Vault; "
                        + "9 slots a block. Sealed it lets 0.2% of the cargo's radiation out. On a train, Portable Storage Interfaces and "
                        + "schedule cargo conditions see its contents. A derailment, crash or explosion breaches it until a Maintenance Kit "
                        + "(two uses, by hand or from a Deployer) patches it.",
                "Una cámara de plomo y hormigón para residuos sólidos: de baja, media y alta actividad, lodos, bultos de residuos, "
                        + "fragmentos gastados, combustible agotado y pellets de RTG. El combustible gastado caliente se rechaza. Se une a lo "
                        + "largo de su eje como el almacén de Create; 9 huecos por bloque. Sellado deja salir el 0,2% de la radiación. En un "
                        + "tren, las interfaces de almacenamiento portátil y las condiciones de carga del horario ven lo que lleva. Un "
                        + "descarrilamiento, choque o explosión lo rompe hasta que un kit de mantenimiento (dos usos, a mano o con un "
                        + "desplegador) lo repara.");

        String ponder = "create_nuclearindustry.ponder.waste_transport.";
        LangEntries.add(ponder + "header", "Moving Radioactive Waste", "Transporte de residuos radiactivos");
        LangEntries.add(ponder + "text_1",
                "Radioactive Waste Cisterns connect like Create's fluid tanks: up to 3x3 across, 8 buckets a block.",
                "Las cisternas de residuos radiactivos se conectan como los tanques de Create: hasta 3x3, 8 cubos por bloque.");
        LangEntries.add(ponder + "text_2",
                "Fill them with pipes. They take only radioactive fluids: spent fuel solution, nitrates, contaminated water...",
                "Llénalas con tuberías. Solo aceptan fluidos radiactivos: solución de combustible gastado, nitratos, agua contaminada...");
        LangEntries.add(ponder + "text_3",
                "Anything else is refused, so nothing else can be pumped in, not even into a moving train.",
                "Todo lo demás se rechaza, así que no se puede bombear nada más, ni siquiera en un tren en marcha.");
        LangEntries.add(ponder + "text_4",
                "Shielded Waste Casks join along their axis like Item Vaults. They hold solid waste, packages and cooled spent fuel; Hot fuel is refused.",
                "Los contenedores blindados de residuos se unen a lo largo de su eje como los almacenes. Guardan residuos sólidos, bultos y combustible gastado enfriado; el caliente se rechaza.");
        LangEntries.add(ponder + "text_5",
                "Sealed, their shell lets almost no radiation out. Both ride Create trains, where Portable Storage Interfaces and schedule conditions see the cargo.",
                "Sellados, su carcasa apenas deja salir radiación. Los dos viajan en trenes de Create, donde las interfaces portátiles y las condiciones del horario ven la carga.");
        LangEntries.add(ponder + "text_6",
                "A derailment, a crash or an explosion breaches them: the waste spills into the ground, and they leak and irradiate until mended.",
                "Un descarrilamiento, un choque o una explosión los rompe: el residuo se derrama en el suelo y siguen fugando e irradiando hasta repararlos.");
        LangEntries.add(ponder + "text_7",
                "Two uses of a Maintenance Kit, by hand or from a Deployer, patch the breach.",
                "Dos usos de un kit de mantenimiento, a mano o con un desplegador, tapan la brecha.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile cistern = provider.models().cubeBottomTop("radioactive_waste_cistern",
                provider.modLoc("block/radioactive_waste_cistern_side"), provider.modLoc("block/radioactive_waste_cistern_top"),
                provider.modLoc("block/radioactive_waste_cistern_top"));
        provider.getVariantBuilder(WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(cistern).build());
        provider.simpleBlockItem(WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get(), cistern);

        // Hand-written, like Create's vault model: the long sides are drawn turned along the axis.
        ModelFile cask = provider.models().getExistingFile(provider.modLoc("block/shielded_waste_cask"));
        provider.getVariantBuilder(WasteTransportRegistry.SHIELDED_WASTE_CASK.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(cask)
                        .rotationY(state.getValue(WasteCaskBlock.HORIZONTAL_AXIS) == net.minecraft.core.Direction.Axis.X ? 90 : 0)
                        .build());
        provider.simpleBlockItem(WasteTransportRegistry.SHIELDED_WASTE_CASK.get(), cask);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.NEEDS_IRON_TOOL).add(WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get(),
                WasteTransportRegistry.SHIELDED_WASTE_CASK.get());
        provider.tagFor(NuclearBlockTagsProvider.SABLE_HEAVY).add(WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get(),
                WasteTransportRegistry.SHIELDED_WASTE_CASK.get());
    }

    @Override
    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(WasteTransportTags.WASTE_CASK_CARGO)
                .addTag(WasteTags.BARREL_WASTE)
                .add(WasteRegistry.LOW_LEVEL_WASTE.get(), AllNuclearItems.RADIOACTIVE_WASTE.get(), WasteRegistry.HIGH_LEVEL_WASTE.get(),
                        FuelCycleRegistry.RADIOACTIVE_SLUDGE.get(), AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(),
                        FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS.get(), AllNuclearItems.RADIOACTIVE_WASTE_BARREL.get(),
                        WasteRegistry.COMPACTED_WASTE_DRUM_ITEM.get(), WasteRegistry.CEMENTED_WASTE_DRUM_ITEM.get(),
                        WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM.get(), AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get(),
                        AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get(), FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get(),
                        AllNuclearItems.DEPLETED_RTG_PELLET.get(), AllNuclearItems.SPENT_DT_PELLET.get(),
                        ExplosiveRegistry.TRINITITE_ITEM.get());
    }

    @Override
    void fluidTags(NuclearFluidTagsProvider provider, HolderLookup.Provider lookup) {
        var tag = provider.tagFor(WasteTransportTags.RADIOACTIVE_WASTE_FLUIDS);
        for (AllNuclearFluids.NuclearFluid fluid : radioactiveFluids()) {
            tag.add(fluid.source().get(), fluid.flowing().get());
        }
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Per bucket: dissolved spent fuel is about as hot as an assembly; separated nitrates less; the rest faint.
        var radioactivity = provider.builderFor(FluidRadioactivity.DATA_MAP);
        float[] perBucket = {4.0f, 2.0f, 3.5f, 1.5f, 0.3f, 0.2f};
        AllNuclearFluids.NuclearFluid[] fluids = radioactiveFluids();
        for (int i = 0; i < fluids.length; i++) {
            radioactivity.add(holder(fluids[i].source()), new FluidRadioactivity(perBucket[i]), false);
            radioactivity.add(holder(fluids[i].flowing()), new FluidRadioactivity(perBucket[i]), false);
        }
    }

    /** The radioactive fluids a cistern takes, in the order of the data map values above. */
    private static AllNuclearFluids.NuclearFluid[] radioactiveFluids() {
        return new AllNuclearFluids.NuclearFluid[]{AllNuclearFluids.SPENT_FUEL_SOLUTION_FLUID, AllNuclearFluids.PLUTONIUM_NITRATE_FLUID,
                AllNuclearFluids.THORIUM_FUEL_SOLUTION_FLUID, AllNuclearFluids.URANIUM_233_NITRATE_FLUID,
                AllNuclearFluids.CONTAMINATED_WATER_FLUID, AllNuclearFluids.DEPLETED_URANIUM_HEXAFLUORIDE_FLUID};
    }

    @SuppressWarnings("deprecation")
    private static net.minecraft.core.Holder<Fluid> holder(Supplier<? extends Fluid> fluid) {
        return fluid.get().builtInRegistryHolder();
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new WasteTransportRecipes(output, lookup));
    }
}
