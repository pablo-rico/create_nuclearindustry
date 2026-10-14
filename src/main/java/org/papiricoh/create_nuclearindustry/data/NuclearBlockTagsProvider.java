package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NuclearBlockTagsProvider extends BlockTagsProvider {
    /** Provided by the Create Big Cannons integration, only registered when that mod is present. */
    private static final ResourceLocation NUCLEAR_CANNON_SHELL =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_cannon_shell");
    private static final ResourceLocation DEPLETED_URANIUM_AP_SHOT =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "depleted_uranium_ap_shot");

    public NuclearBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Block[] ores = {AllNuclearBlocks.URANIUM_ORE.get(), AllNuclearBlocks.BORAX_ORE.get(), AllNuclearBlocks.SPODUMENE_ORE.get()};

        tag(BlockTags.NEEDS_IRON_TOOL).add(ores);
        tag(AllNuclearTags.Blocks.GEOLOGICALLY_STORABLE).add(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get());
        tag(AllNuclearTags.Blocks.C_ORES_URANIUM).add(AllNuclearBlocks.URANIUM_ORE.get());
        tag(AllNuclearTags.Blocks.C_ORES_LITHIUM).add(AllNuclearBlocks.SPODUMENE_ORE.get());
        tag(AllNuclearTags.Blocks.C_ORES_BORAX).add(AllNuclearBlocks.BORAX_ORE.get());
        tag(AllNuclearTags.Blocks.C_ORES).addTags(AllNuclearTags.Blocks.C_ORES_URANIUM,
                AllNuclearTags.Blocks.C_ORES_LITHIUM, AllNuclearTags.Blocks.C_ORES_BORAX);

        // What a nuclear crater leaves is ground and wood, not a machine: its own area tags it.
        List<Block> terrain = List.of(org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.TRINITITE.get(),
                org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.CHARRED_LOG.get());
        List<Block> machines = AllNuclearBlocks.BLOCKS.getEntries().stream()
                .map(holder -> (Block) holder.get())
                .filter(block -> !List.of(ores).contains(block) && !terrain.contains(block))
                .toList();
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ores).add(machines.toArray(Block[]::new)).addOptional(NUCLEAR_CANNON_SHELL).addOptional(DEPLETED_URANIUM_AP_SHOT);
        tag(AllNuclearTags.Blocks.WRENCH_PICKUP).add(machines.toArray(Block[]::new)).addOptional(NUCLEAR_CANNON_SHELL).addOptional(DEPLETED_URANIUM_AP_SHOT);

        tag(MultiblockTags.MULTIBLOCK_PARTS).add(
                AllNuclearBlocks.REACTOR_CASING.get(),
                AllNuclearBlocks.REACTOR_FLUID_PORT.get(),
                AllNuclearBlocks.REACTOR_FUEL_PORT.get(),
                AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get(),
                AllNuclearBlocks.REACTOR_REDSTONE_PORT.get(),
                AllNuclearBlocks.TURBINE_EXHAUST_PORT.get(),
                AllNuclearBlocks.COOLING_TOWER_CASING.get(),
                AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get(),
                AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get(),
                AllNuclearBlocks.COOLING_TOWER_FAN.get(),
                AllNuclearBlocks.URANIUM_ROD.get(),
                AllNuclearBlocks.HEAT_EXCHANGER.get(),
                AllNuclearBlocks.GRAPHITE_MODERATOR.get(),
                AllNuclearBlocks.BERYLLIUM_REFLECTOR.get(),
                AllNuclearBlocks.CONTROL_ROD.get(),
                AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get(),
                AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get(),
                AllNuclearBlocks.FUSION_ELECTROMAGNET.get(),
                AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get(),
                AllNuclearBlocks.FUSION_MAGNET_INPUT.get(),
                AllNuclearBlocks.FUSION_FLUID_PORT.get(),
                AllNuclearBlocks.FUSION_FUEL_INJECTOR.get(),
                AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(),
                AllNuclearBlocks.FUSION_BREEDING_BLANKET.get(),
                AllNuclearBlocks.FUSION_CONTROLLER.get(),
                AllNuclearBlocks.TURBINE_CASING.get(),
                AllNuclearBlocks.TURBINE_ROTOR.get(),
                AllNuclearBlocks.TURBINE_OUTPUT.get(),
                AllNuclearBlocks.TURBINE_FLUID_PORT.get(),
                AllNuclearBlocks.FUSION_TURBINE_CASING.get(),
                AllNuclearBlocks.FUSION_TURBINE_ROTOR.get(),
                AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.get(),
                AllNuclearBlocks.FUSION_PLASMA_TURBINE.get(),
                AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
        tag(BlockTags.IMPERMEABLE).add(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
        AreaDataGen.ALL.forEach(area -> area.blockTags(this, provider));
    }

    /** {@link #tag} for the areas' datagen, which lives in other classes. */
    public IntrinsicTagAppender<Block> tagFor(net.minecraft.tags.TagKey<Block> tag) {
        return tag(tag);
    }
}
