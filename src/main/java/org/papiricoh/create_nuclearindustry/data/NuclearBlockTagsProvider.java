package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

import java.util.concurrent.CompletableFuture;

public class NuclearBlockTagsProvider extends BlockTagsProvider {

    public NuclearBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(MultiblockTags.MULTIBLOCK_PARTS).add(
                AllNuclearBlocks.REACTOR_CASING.get(),
                AllNuclearBlocks.REACTOR_FLUID_PORT.get(),
                AllNuclearBlocks.REACTOR_FUEL_PORT.get(),
                AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get(),
                AllNuclearBlocks.URANIUM_ROD.get(),
                AllNuclearBlocks.HEAT_EXCHANGER.get(),
                AllNuclearBlocks.CONTROL_ROD.get(),
                AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get(),
                AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get(),
                AllNuclearBlocks.FUSION_ELECTROMAGNET.get(),
                AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get(),
                AllNuclearBlocks.FUSION_MAGNET_INPUT.get(),
                AllNuclearBlocks.FUSION_FLUID_PORT.get(),
                AllNuclearBlocks.FUSION_FUEL_INJECTOR.get());
    }
}
