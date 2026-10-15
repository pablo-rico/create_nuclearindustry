package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The datagen of one area of the mod, so areas never edit the shared providers: each provider
 * calls the matching hook of every area in {@link #ALL}. Lang entries go through
 * {@link LangEntries#add}, {@link LangEntries#item}, {@link LangEntries#block} and {@link LangEntries#fluid}.
 */
abstract class AreaDataGen {
    static final List<AreaDataGen> ALL = List.of(
            PwrDataGen.INSTANCE,
            SpentFuelDataGen.INSTANCE,
            WasteDataGen.INSTANCE,
            ControlRoom2DataGen.INSTANCE,
            CivilDefenseDataGen.INSTANCE,
            PlantPonderDataGen.INSTANCE,
            PlantJeiDataGen.INSTANCE,
            FusionDataGen.INSTANCE,
            CryoDataGen.INSTANCE,
            PlasmaHeatingDataGen.INSTANCE,
            ReactorVisualsDataGen.INSTANCE,
            FuelCycleDataGen.INSTANCE,
            GuideDataGen.INSTANCE,
            AcceleratorDataGen.INSTANCE,
            AntimatterDataGen.INSTANCE,
            EnergyDataGen.INSTANCE,
            IsotopeDataGen.INSTANCE,
            MaintenanceDataGen.INSTANCE,
            ExplosiveDataGen.INSTANCE);

    void lang() {}

    void blockStates(NuclearBlockStateProvider provider) {}

    void itemModels(NuclearItemModelProvider provider) {}

    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {}

    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {}

    void fluidTags(NuclearFluidTagsProvider provider, HolderLookup.Provider lookup) {}

    void dataMaps(NuclearDataMapProvider provider) {}

    /** Loot tables other than "drops itself", which every block gets by default. */
    void blockLoot(NuclearBlockLoot loot) {}

    /** Extra providers, such as this area's recipe generators. */
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {}
}
