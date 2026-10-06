package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Block states and models for blocks added by the rewrite; older blocks keep hand-written ones. */
public class NuclearBlockStateProvider extends BlockStateProvider {

    public NuclearBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(AllNuclearBlocks.SPODUMENE_ORE.get(), cubeAll(AllNuclearBlocks.SPODUMENE_ORE.get()));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(),
                modLoc("block/fusion_central_solenoid_side"), modLoc("block/fusion_central_solenoid_top"));
        simpleBlockItem(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(), models().getExistingFile(modLoc("block/fusion_central_solenoid")));
        simpleBlockWithItem(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get(), cubeAll(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()));
        simpleBlockWithItem(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), models()
                .cubeAll("reinforced_turbine_glass", modLoc("block/reinforced_turbine_glass"))
                .renderType("cutout"));
        simpleBlockWithItem(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get(), models().cubeBottomTop("radioactive_waste_barrel",
                modLoc("block/radioactive_waste_barrel_side"),
                modLoc("block/radioactive_waste_barrel_top"),
                modLoc("block/radioactive_waste_barrel_top")));
    }
}
